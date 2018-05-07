package TCPSocket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Timer;
import java.util.TimerTask;
public class TCPServerSocketImpl extends TCPServerSocket {
	private EnhancedDatagramSocket socket;
	int portToListen;
	int receiveSeqNum;
	int sendSeqNum;
	int senderPort;
	TCPHeader tcpHeader;
	AtomicBoolean listenTimeOut;
	InetAddress senderIp;
    public TCPServerSocketImpl(int port) throws Exception {
        super(port);
		listenTimeOut=new AtomicBoolean(false);
		socket=new EnhancedDatagramSocket(port);
		portToListen=port;
		new Timer("for listen timeout").schedule(new simpleTimeOutTimerTask(listenTimeOut),7000);
		socket.setSoTimeout(100);
    }

    @Override
    public TCPSocket accept() throws Exception {
		tcpHeader=new TCPHeader();
		TCPSocketImpl client_sock=null;
		while(!listenTimeOut.get()){
			//receiving SYN
			byte[] synData=new byte[TCPHeader.size];
			DatagramPacket synPacket=new DatagramPacket(synData,synData.length);
			try{
				socket.receive(synPacket);
			}
			catch(Exception e){
				continue;
			}
			tcpHeader.extractFrom(synPacket.getData());
			if(!tcpHeader.isSYN())
				continue;
			receiveSeqNum=tcpHeader.getSEQ();
			senderIp=synPacket.getAddress();
			senderPort=synPacket.getPort();
			sendSeqNum=ThreadLocalRandom.current().nextInt(0,Integer.MAX_VALUE);
			//sending SYN ACK
			this.sendSYN_ACK();
			//receiving final ack 
			byte[] AckData=new byte[TCPHeader.size];
			DatagramPacket finalAckPacket=new DatagramPacket(AckData,AckData.length);
			while(true){
				try{
					socket.receive(finalAckPacket);
					if(finalAckPacket.getPort()!=senderPort || !finalAckPacket.getAddress().equals(senderIp))
						continue;
					else
						break;
				}catch(Exception e){
					e.printStackTrace();
					this.sendSYN_ACK();
					break;
				}
			}
			//tcpHeader.unSetAll();
			//tcpHeader.extractFrom(finalAckPacket.getData());
			//if(!tcpHeader.isACK())
			//	continue;
			client_sock=new TCPSocketImpl(senderIp,senderPort,socket);
			client_sock.setStartSeqForSend(sendSeqNum);
			client_sock.setStartSeqForReceive(receiveSeqNum+1);
			//setting seqnumber of client and my seqnumber 
			break;
		}
		if(client_sock==null)
			throw new TimeOutException();
		return client_sock;
    }
	public static class TimeOutException extends Exception {}
    @Override
    public void close() throws Exception {
        
    }
	private void sendSYN_ACK(){
		tcpHeader.unSetAll();
		tcpHeader.setSYN();
		tcpHeader.setACK();
		tcpHeader.setSEQ(sendSeqNum);
		tcpHeader.setAckNum(receiveSeqNum+1);
		byte[] readydata=tcpHeader.attachTo(new byte[]{});
		DatagramPacket synAckPacket=new DatagramPacket(readydata,readydata.length,senderIp,senderPort);
		try{
			socket.send(synAckPacket);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
