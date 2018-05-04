package TCPSocket;
import java.net.DatagramPacket;
import java.io.IOException;
import java.net.SocketTimeoutException;
class SegmentReceiver {
	private TCPSocketImpl tcpSocketImpl;
	private TCPHeader tcpHeader;

	public void AckReceive(){
		byte[] ackPack=new byte[TCPHeader.size];
		DatagramPacket AckPacket=new DatagramPacket(ackPack,ackPack.length);
		try{
			tcpSocketImpl.getSocket().receive(AckPacket);
		}catch(IOException | SocketTimeoutException e){
			System.out.println(e.getMessage());
			return;
		}
		tcpHeader.extractFrom(AckPacket.getData());
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
				tcpSocketImpl.getRetransmissionTimer().cancel();
				tcpSocketImpl.getRetransmissionTimer().schedule(new RetransmissionTimerTask(tcpSocketImpl),500);
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
	public void dataSegmentReceive(){
		byte[] dataPack=new byte[TCPHeader.size];
		DatagramPacket dataPacket=new DatagramPacket(dataPack,dataPack.length);
		try{
			tcpSocketImpl.getSocket().receive(dataPacket);//aya eenja lazeme check konim ferestandeye baste hamoonie ke bahash dar ertebat hastim?
		}catch(IOException | SocketTimeoutException e){
			System.out.println(e.getMessage());
			return;
		}
		tcpHeader.extractFrom(dataPacket.getData());
		if(tcpHeader.getSEQ()>=tcpSocketImpl.getReceiveBaseSeqNum().get()){
			
		}
	}
	public SegmentReceiver(TCPSocketImpl forCallBack){
		tcpSocketImpl=forCallBack;
		tcpHeader=new TCPHeader();
	}
}