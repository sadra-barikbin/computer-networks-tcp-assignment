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
	private int mPort;
	private InetAddress mIp;
	private EnhancedDatagramSocket socket;
	private int windowSize;
	private int firstSegmentNumWaitingToGetAck;
	private int SSThreshold; 
	private AtomicInteger baseSeqNum;
	private AtomicInteger nextToBeSentSeqNum;
	public LinkedList<seqNumPlusPacket> inFlightSegments;
	private FileInputStream file;
	private Timer retransmissionTimer;
	private SegmentReceiver segmentReceiver;
	public AtomicBoolean retransmit;
	private TCPHeader tcpHeader;
	
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
		mPort=port;
		mIp=InetAddress.getByName(ip);
		retransmissionTimer=new Timer("Timer");
		segmentReceiver=new SegmentReceiver(this);
		socket=new EnhancedDatagramSocket(1800);//haminjoori
		tcpHeader=new TCPHeader();
		baseSeqNum=new AtomicInteger(ThreadLocalRandom.current().nextInt(0,Integer.MAX_VALUE));
		nextToBeSentSeqNum=new AtomicInteger(baseSeqNum.intValue()+1);
		retransmit=new AtomicBoolean(false);
	}
	public void setSocket(EnhancedDatagramSocket s){
		socket=s;
	}
	private void setupConnection() throws ConnectionRefusedException{
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
		try{
			socket.receive(synAckPacket);
		}catch(IOException e){
			e.printStackTrace();
		}
		tcpHeader.extractFrom(synAckPacket.getData());
		if(!(tcpHeader.isACK()&& tcpHeader.isSYN() ))
			throw new ConnectionRefusedException();
		// ack packet
		tcpHeader.unSetAll();
		tcpHeader.setACK();
		tcpHeader.setSEQ(baseSeqNum.intValue());
		byte[] finalAckData=tcpHeader.attachTo(new byte[]{});
		DatagramPacket finalAckPacket=new DatagramPacket(finalAckData,finalAckData.length,mIp,mPort);
		try{
			socket.send(finalAckPacket);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
    
    public void send(String pathToFile) throws Exception {
		setupConnection();
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
			segmentReceiver.receive();
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
        throw new RuntimeException("Not implemented!");
    }
	public EnhancedDatagramSocket getSocket(){
		return socket;
	}
	public AtomicInteger getBaseSeqNum(){
		return baseSeqNum;
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
