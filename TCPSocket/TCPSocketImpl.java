package TCPSocket;
import java.util.Random;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.FileInputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.io.IOException;
public class TCPSocketImpl extends TCPSocket {
	
	/////congestion control///
	private int SSThreshold; 
	/////////////////////////
	
	private int mPort;
	private InetAddress mIp;
	private EnhancedDatagramSocket socket;
	private FileInputStream file;
	private TCPHeader tcpHeader;
	
	////for Go-Back-N //////////////
	private int windowSize;
	private AtomicInteger baseSeqNum;
	private AtomicInteger receiveBaseSeqNum;
	private AtomicInteger nextToBeSentSeqNum;
	public LinkedList<seqNumPlusPacket> inFlightSegments;
	public AtomicBoolean retransmit;
	private Timer retransmissionTimer;
	private SegmentReceiver segmentReceiver;
	/////////////////////////
	
	
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
		mPort=port;
		mIp=InetAddress.getByName(ip);
		retransmissionTimer=new Timer("Timer");
		segmentReceiver=new SegmentReceiver(this);
		socket=new EnhancedDatagramSocket(ThreadLocalRandom.current().nextInt(1025,65535));
		socket.setSoTimeout(100);
		tcpHeader=new TCPHeader();
		baseSeqNum=new AtomicInteger(ThreadLocalRandom.current().nextInt(0,Integer.MAX_VALUE));
		nextToBeSentSeqNum=new AtomicInteger(baseSeqNum.intValue()+1);
		retransmit=new AtomicBoolean(false);
		setupConnection();
	}
	TCPSocketImpl(InetAddress ip,int port,EnhancedDatagramSocket ReceiveSocket)throws Exception{
		super(ip.toString(),port );
		mPort=port;
		mIp=ip;
		retransmissionTimer=new Timer("Timer");
		segmentReceiver=new SegmentReceiver(this);
		socket=ReceiveSocket;
		socket.setSoTimeout(100);
		tcpHeader=new TCPHeader();
		retransmit=new AtomicBoolean(false);
	}
	/*public void setSocket(EnhancedDatagramSocket s){
		socket=s;
	}*/
	private void setupConnection() throws ConnectionRefusedException{
		baseSeqNum=new AtomicInteger(ThreadLocalRandom.current().nextInt(0,Integer.MAX_VALUE));
		nextToBeSentSeqNum=new AtomicInteger(baseSeqNum.intValue()+1);
		//synchronize packet
		tcpHeader.setSYN();
		tcpHeader.setSEQ(baseSeqNum.intValue());
		byte[] readydata=tcpHeader.attachTo(new byte[]{});
		DatagramPacket syncPacket=new DatagramPacket(readydata,readydata.length,mIp,mPort);
		try{
			socket.send(syncPacket);
		}catch(IOException e){
			e.printStackTrace();
		}
		//synchronize ack packet
		byte[] synAckData=new byte[9];
		DatagramPacket synAckPacket=new DatagramPacket(synAckData,synAckData.length);
		while(true){
			try{
				socket.receive(synAckPacket);
				if(synAckPacket.getPort()!=mPort || !synAckPacket.getAddress().equals(mIp))
					continue;
				else
					break;
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		tcpHeader.extractFrom(synAckPacket.getData());
		if(!(tcpHeader.isACK()&& tcpHeader.isSYN() ))
			throw new ConnectionRefusedException();
		receiveBaseSeqNum=new AtomicInteger(tcpHeader.getSEQ());
		// ack packet
		tcpHeader.unSetAll();
		tcpHeader.setACK();
		tcpHeader.setSEQ(baseSeqNum.intValue());
		tcpHeader.setAckNum(baseSeqNum.intValue()+1);
		byte[] finalAckData=tcpHeader.attachTo(new byte[]{});
		DatagramPacket finalAckPacket=new DatagramPacket(finalAckData,finalAckData.length,mIp,mPort);
		try{
			socket.send(finalAckPacket);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
    void setStartSeqForSend(int val){
		baseSeqNum.set(val);
	}
	void setStartSeqForReceive(int val){
		receiveBaseSeqNum.set(val);
	}
    public void send(String pathToFile) throws Exception {
		this.file=new FileInputStream(pathToFile);
		windowSize=1;
		inFlightSegments=new LinkedList<>();
		while(true){
			try{
				segment newSegment=this.getNewSegment();
				if(inFlightSegments.size()<windowSize){
					newSegment.setSeqNum(this.nextToBeSentSeqNum.intValue());
					if(nextToBeSentSeqNum.compareAndSet(baseSeqNum.intValue(),baseSeqNum.intValue()))
						retransmissionTimer.schedule(new RetransmissionTimerTask(this),500);
					int seqnum=nextToBeSentSeqNum.getAndAdd(newSegment.size());//i supposed the header is excluded
													   //from segment size
					byte[] readydata=newSegment.toBytes();
					DatagramPacket Packet=new DatagramPacket(readydata,readydata.length,mIp,mPort);
					socket.send(Packet);
					inFlightSegments.offerLast(new seqNumPlusPacket(Integer.valueOf(seqnum),Packet));//agar enja natoone 
																									//ezafe kone dade mippare!
																									//khoobe handle beshe
				}
			}
			catch(Exception e){
				//its either RanOutOfDataException or IOException
			}
			segmentReceiver.AckReceive();
			if(retransmit.getAndSet(false))
			{
				int current_size=inFlightSegments.size();
				int first_size=inFlightSegments.size();
				seqNumPlusPacket curr;
				while(current_size!=0){
					curr=inFlightSegments.pollFirst();
					socket.send(curr.packet);
					if(first_size==current_size)
						retransmissionTimer.schedule(new RetransmissionTimerTask(this),500);
					inFlightSegments.offerLast(curr);
					current_size--;
				}
			}
		}
    }
	public segment getNewSegment() throws Exception{
			if(file.available()!=0){
				segment newSegment=new segment();
				file.read(newSegment.data);
				return newSegment;
			}
			else 
				throw new RanOutOfDataException();
	}
    @Override
    public void receive(String pathToFile) throws Exception {
        //az SegmentReceiver.dataSegmentReceive() estefade konid va oon ro ham kamel konid
    }
	public EnhancedDatagramSocket getSocket(){
		return socket;
	}
	public AtomicInteger getBaseSeqNum(){
		return baseSeqNum;
	}
	public AtomicInteger getReceiveBaseSeqNum(){
		return receiveBaseSeqNum;
	}
	public AtomicInteger getNextToBeSentSeqNum(){
		return nextToBeSentSeqNum;
	}
	public Timer getRetransmissionTimer(){
		return retransmissionTimer;
	}
    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getSSThreshold() {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public long getWindowSize() {
        throw new RuntimeException("Not implemented!");
    }
	public static class RanOutOfDataException extends Exception{}
	public static class ConnectionRefusedException extends Exception{}
}
