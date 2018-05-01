package TCPSocket;
import java.net.DatagramPacket;
protected class seqNumPlusPacket {
	public Integer seqNum;
	public DatagramPacket packet;
	public seqNumPlusPacket(Integer seq,DatagramPacket p){
		seqNum=seq;
		packet=p;
	}
}