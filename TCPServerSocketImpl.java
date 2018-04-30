import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.InetAddress;
public class TCPServerSocketImpl extends TCPServerSocket {
	private TCPSocketImpl socket;
	int portToListen;
	int receiveSeqNum;
	int sendSeqNum;
	int senderPort;
	InetAddress senderIp;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
		portToListen=port;
    }

    @Override
    public TCPSocket accept() throws Exception {
		TCPHeader tcpHeader=new TCPHeader();
		while(true){
			//receiving SYN
			byte[] synData=new byte[9];
			DatagramPacket synPacket=new DatagramPacket(synData,synData.length);
			socket.receive(synPacket);
			tcpHeader.extractFrom(synPacket.getData());
			if(!tcpHeader.isSYN())
				continue;
			receiveSeqNum=tcpHeader.getSeqNum();
			senderIp=synPacket.getAddress();
			senderPort=synPacket.getPort();
			//sending SYN ACK
			tcpHeader.unSetAll();
			tcpHeader.setSYN();
			tcpHeader.setACK();
			sendSeqNum=ThreadLocalRandom.current().nextInt(0,Integer.MAX_VALUE);
			tcpHeader.setSEQ(sendSeqNum);
			tcpHeader.setAckNum(receiveSeqNum+1);
			byte[] readydata=tcpHeader.attachTo(new byte[]{});
			DatagramPacket synAckPacket=new DatagramPacket(readydata,readydata.length,synPacket.getAddress(),synPacket.getPort());
			try{
				socket.send(synAckcPacket);
			}catch(IOException e){
				e.printStackTrace();
			}
			//receiving final ack 
			byte[] AckData=new byte[9];
			DatagramPacket finalAckPacket=new DatagramPacket(AckData,AckData.length);
			while(true){
				try{
					socket.receive(finalAckPacket);
					if(finalAckPacket.getPort()!=senderPort || !finalAckPacket.getAddress.equals(senderIp))
						continue;
					else
						break;
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			tcpHeader.unSetAll();
			tcpHeader.extractFrom(finalAckPacket.getData());
			if(!tcpHeader.isACK())
				continue;
			TCPSocketImpl client_sock=new TCPSocketImpl(senderIp,senderPort,portToListen);
			//setting seqnumber of client and my seqnumber 
			break;
		}
		return new TCPSocketImpl();
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
