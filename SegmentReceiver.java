import java.net.DatagramPacket;
import java.io.IOException;
public class SegmentReceiver extends Thread {
	private TCPSocketImpl tcpSocketImpl;
	private TCPHeader tcpHeader;
	@Override
	public void run(){
		while(true){
			byte[] ackPack=new byte[TCPHeader.size];
			DatagramPacket AckPacket=new DatagramPacket(ackPack,ackPack.length);
			try{
				tcpSocketImpl.getSocket().receive(AckPacket);
			}catch(IOException e){
				System.out.println(e.getMessage());
			}
			tcpHeader.extractFrom(AckPacket.getData());
			if(! tcpHeader.getAckNum() <= tcpSocketImpl.getBaseSeqNum())
			{
				tcpSocketImpl.getBaseSeqNum().getAndSet(tcpHeader.getAckNum());//i supposed receiver sets
																		   //ack number
																		   //as the next expected 
																		   //sequence number not last
																		   // acked one
				//deleting acked packets
				if(tcpSocketImpl.getBaseSeqNum().intValue()==tcpSocketImpl.getNextToBeSentSeqNum().intValue())
					tcpSocketImpl.getRetransmissionTimer().cancel();
				else{
					tcpSocketImpl.getRetransmissionTimer().cancel();
					tcpSocketImpl.getRetransmissionTimer().schedule(new RetransmissionTimerTask(tcpSocketImpl),500);
				}
			}
		}
	}
	public SegmentReceiver(TCPSocketImpl forCallBack){
		tcpSocketImpl=forCallBack;
		tcpHeader=new TCPHeader();
	}
}