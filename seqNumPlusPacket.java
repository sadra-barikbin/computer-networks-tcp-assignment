import java.net.DatagramPacket;
public class seqNumPlusPacket {
	public Integer seqNum;
	public DatagramPacket packet;
	public seqNumPlusPacket(Integer seq,DatagramPacket p){
		seqNum=seq;
		packet=p;
	}
}