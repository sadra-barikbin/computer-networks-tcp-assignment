package TCPSocket;
import java.net.DatagramPacket;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Timer;
class SegmentReceiver {
	private TCPSocketImpl tcpSocketImpl;
	private TCPHeader tcpHeader;

	public void AckReceive(){
		byte[] ackPack=new byte[TCPHeader.size];
		DatagramPacket AckPacket=new DatagramPacket(ackPack,ackPack.length);
		try{
			tcpSocketImpl.getSocket().receive(AckPacket);
		}catch(IOException  e){// SocketTimeoutException
			//System.out.println("at sender in AckReceive:"+e.getMessage());
			return;
		}
		tcpHeader.extractFrom(AckPacket.getData());
		System.out.println("ack number "+tcpHeader.getAckNum()+" has just been received");
		if(!( tcpHeader.getAckNum() <= tcpSocketImpl.getBaseSeqNum().get()))
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
				//tcpSocketImpl.getRetransmissionTimer().cancel();
				tcpSocketImpl.getRetransmissionTimer().schedule(new RetransmissionTimerTask(tcpSocketImpl),tcpSocketImpl.getTimeOut());
			}
			while(true){
				if(tcpSocketImpl.inFlightSegments.peekFirst()!=null){
					if(tcpSocketImpl.inFlightSegments.peekFirst().seqNum<tcpHeader.getAckNum())
					{
						tcpSocketImpl.inFlightSegments.remove();
					}
					else
						break;
				}
				else
					break;
			}
		}
	}
	public byte[] dataSegmentReceive(){
		byte[] dataPack=new byte[1408];
		DatagramPacket dataPacket=new DatagramPacket(dataPack,dataPack.length);
		try{
			tcpSocketImpl.getSocket().receive(dataPacket);//aya eenja lazeme check konim ferestandeye baste hamoonie ke bahash dar ertebat hastim?
		}catch(IOException  e){// SocketTimeoutException
			//System.out.println("at receiver in dataSegmentReceive : "+e.getMessage());
			return null;
		}
		return dataPacket.getData();
	}
	public SegmentReceiver(TCPSocketImpl forCallBack){
		tcpSocketImpl=forCallBack;
		tcpHeader=new TCPHeader();
	}
}