import java.util.Random;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.FileInputStream;
import java.util.concurrent.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.Semaphore;
public class TCPSocketImpl extends TCPSocket {
	private int mPort;
	private InetAddress mIp;
	private EnhancedDatagramSocket socket;
	private int windowSize;
	private int firstSegmentNumWaitingToGetAck;
	private int SSThreshold; 
	private AtomicInteger baseSeqNum;
	private AtomicInteger nextToBeSentSeqNum;
	public LinkedHashMap<Integer,DatagramPacket> inFlightSegments;
	private FileInputStream file;
	private Timer retransmissionTimer;
	private SegmentReceiver segmentReceiver;
	public TCPSocketImpl
    public TCPSocketImpl(String ip, int port) throws Exception {
        super(ip, port);
		mPort=port;
		mIp=InetAddress.getByName(ip);
		retransmissionTimer=new Timer("Timer");
		segmentReceiver=new SegmentReceiver(this);
		socket=new EnhancedDatagramSocket(1800);//haminjoori
		TCPHeader tcpHeader=new TCPHeader();
		baseSeqNum=new AtomicInteger(ThreadLocalRandom.current().nextInt(0,Integer.MAX_VALUE));
		nextToBeSentSeqNum=new AtomicInteger(baseSeqNum.intValue()+1);
	}
	public void setSocket(EnhancedDatagramSocket s){
		socket=s;
	}
	private void setupConnection(){
		//synchronize packet
		tcpHeader.setSYN();
		tcpHeader.setSEQ(baseSeqNum.intValue());
		byte[] readydata=tcpHeader.attachTo(new byte[]{});
		DatagramPacket syncPacket=new DatagramPacket(readydata,readydata.length,mIp,mPort);
		socket.send(syncPacket);
		//synchronize ack packet
		byte[] synAckData=new byte[9];
		DatagramPacket synAckPacket=new DatagramPacket(synAckData,synAckData.length);
		socket.receive(synAckPacket);
		tcpHeader.extractFrom(synAckPacket.getData());
		if(!(tcpHeader.isACK()&& tcpHeader.isSYN() ))
			throw new ConnectionRefusedException();
		// ack packet
		tcpHeader.unSetAll();
		tcpHeader.setACK();
		tcpHeader.setSEQ(baseSeqNum.intValue());
		byte[] finalAckData=tcpHeader.attachTo(new byte[]{});
		DatagramPacket finalAckPacket=new DatagramPacket(finalAckData,finalAckData.length,mIp,mPort);
		socket.send(finalAckPacket);
	}
    
    public void send(String pathToFile) throws Exception {
		setupConnection();
		this.file=new FileInputStream(pathToFile);
		windowSize=1;
		inFlightSegments=new LinkedHashMap<>();
		segmentReceiver.start();
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
					inFlightSegments.offer(Integer.valueOf(seqnum),Packet);
				}
			}
			catch(Exception e){
				//its either RanOutOfDataException or IOException
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
