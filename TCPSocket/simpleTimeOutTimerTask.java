package TCPSocket;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
class simpleTimeOutTimerTask extends TimerTask{
	@Override
	public void run(){
		toSetOnTimeOut.set(true);
	}
	AtomicBoolean toSetOnTimeOut;
	public simpleTimeOutTimerTask(AtomicBoolean toSetOnTimeOut){
		this.toSetOnTimeOut=toSetOnTimeOut;
	}
}