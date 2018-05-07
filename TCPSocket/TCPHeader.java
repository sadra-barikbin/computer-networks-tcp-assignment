package TCPSocket;
import java.nio.ByteBuffer;
class TCPHeader{
	public int sequenceNumber;
	public int acknowledgeNumber;
	public short payload_size;
	public byte Flags;
	public static byte ACK=0x01;
	public static byte SYN=0x02;
	public static byte FIN=0x04;
	public static byte RST=0x08;
	public static int size=11;
	public byte[] extractAndGetOtherData(byte[] packet){
		sequenceNumber=ByteBuffer.wrap(new byte[]{packet[0],packet[1],
			packet[2],packet[3]}).getInt();
		acknowledgeNumber=ByteBuffer.wrap(new byte[]{packet[4],packet[5],
			packet[6],packet[7]}).getInt();
		Flags=packet[8];
		payload_size=ByteBuffer.wrap(new byte[]{packet[9],packet[10]}).getShort();
		byte[] res=new byte[payload_size];
		System.arraycopy(packet,TCPHeader.size,res,0,payload_size);
		return res;
	}
	public void extractFrom(byte[] packet){
		unSetAll();
		sequenceNumber=ByteBuffer.wrap(new byte[]{packet[0],packet[1],
			packet[2],packet[3]}).getInt();
		acknowledgeNumber=ByteBuffer.wrap(new byte[]{packet[4],packet[5],
			packet[6],packet[7]}).getInt();
		Flags=packet[8];
		payload_size=ByteBuffer.wrap(new byte[]{packet[9],packet[10]}).getShort();
	}
	public byte[] attachTo(byte[] packet){
		byte[] result=new byte[packet.length+TCPHeader.size];
		System.arraycopy(packet,0,result,TCPHeader.size,packet.length);
		System.arraycopy(ByteBuffer.allocate(2).putShort(payload_size).array(),0,result,9,2);
		result[8]=Flags;
		System.arraycopy(ByteBuffer.allocate(4).putInt(sequenceNumber).array(),0,result,0,4);
		System.arraycopy(ByteBuffer.allocate(4).putInt(acknowledgeNumber).array(),0,result,4,4);
		return result;
	}
	public TCPHeader(){
		sequenceNumber=0;
		acknowledgeNumber=0;
		Flags=0x00;
		payload_size=0;
	}
	public void setSEQ(int seq){
		sequenceNumber=seq;
	}
	public int getSEQ(){
		return sequenceNumber;
	}
	public int getAckNum(){
		return acknowledgeNumber;
	}
	public void setAckNum(int val){
		acknowledgeNumber=val;
	}
	public void setPayloadSize(int size){
		payload_size=Integer.valueOf(size).shortValue();
	}
	public int getPayloadSize(){
		return Short.valueOf(payload_size).intValue();
	}
	public boolean isACK(){
		return (Flags & TCPHeader.ACK)==0?false:true;
	}
	public boolean isSYN(){
		return (Flags & TCPHeader.SYN)==0?false:true;
	}
	public void setACK(){
		Flags=(byte)(Flags | TCPHeader.ACK);
	}
	public void unSetACK(){
		Flags = (byte)(Flags &(~ TCPHeader.ACK));
	}
	public void unSetSYN(){
		Flags =(byte)( Flags &(~ TCPHeader.SYN));
	}
	public void unSetAll(){
		Flags=0x00;
		sequenceNumber=0;
		acknowledgeNumber=0;
	}
	public void setSYN(){
		Flags=(byte)(Flags | TCPHeader.SYN);
	}
	/*public static void main(String[] args){
		byte[] arg=args[0].getBytes();
		TCPHeader tcpHeader=new TCPHeader();
		tcpHeader.setACK();
		tcpHeader.setSEQ(123234);
		byte[] attached=tcpHeader.attachTo(arg);
		byte[] data=tcpHeader.extractAndGetOtherData(attached);
		System.out.println(Integer.valueOf(tcpHeader.getSEQ()));
		System.out.println(tcpHeader.isACK());
		System.out.println(tcpHeader.isSYN());
		System.out.print(new String(data));
	}*/
}