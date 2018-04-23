public class segment {
	public byte[] data;
	TCPHeader tcpHeader;
	public segment(){
		data=new byte[1408-TCPHeader.size];
		tcpHeader=new TCPHeader();
	}
	public byte[] toBytes(){
		return tcpHeader.attachTo(this.data);
	}
	public void setSeqNum(int seqNum){
		tcpHeader.setSEQ(seqNum);
	}
	public int size(){
		return data.length;
	}
}
		