import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
public class TCPServerSocketImpl extends TCPServerSocket {
	EnhancedDatagramSocket socket;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
		socket=new EnhancedDatagramSocket(port);
    }

    @Override
    public TCPSocket accept() throws Exception {
		TCPHeader tcpHeader=new TCPHeader();
		while(true){
			byte[] synData=new byte[9];
			DatagramPacket synPacket=new DatagramPacket(synData,synData.length);
			socket.receive(synPacket);
			tcpHeader.extractFrom(synPacket.getData());
			if(!tcpHeader.isSYN())
				continue;
			//getting seq num
			tcpHeader.setSYN();
			//tcpHeader.setSEQ(baseSeqNum.intValue());
			byte[] readydata=tcpHeader.attachTo(new byte[]{});
			DatagramPacket syncPacket=new DatagramPacket(readydata,readydata.length,synPacket.getAddress(),synPacket.getPort());
			try{
				socket.send(syncPacket);
			}catch(IOException e){
				e.printStackTrace();
			}
			byte[] synAckData=new byte[9];
			DatagramPacket synAckPacket=new DatagramPacket(synAckData,synAckData.length);
			socket.receive(synAckPacket);
			tcpHeader.unSetAll();
			tcpHeader.extractFrom(synPacket.getData());
			if(!tcpHeader.isACK())
				continue;
			//constructing socket 
		}
    }

    @Override
    public void close() throws Exception {
        throw new RuntimeException("Not implemented!");
    }
}
