import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TimeoutWatchdog extends Thread{
	private long startSenderTime;
	private long startWatchTime; //milliseconds
	private Segment watchedSegment;
	private DatagramSocket reTransmitSocket;
	private boolean stopThread = true;
	private long timeout;
	private InetAddress destIP;
	private boolean terminate = false;
	
	public TimeoutWatchdog(long senderStartTime, long startTime, Segment firstSeg, DatagramSocket socket, long timeout, InetAddress dest){
		this.startSenderTime = senderStartTime;
		this.startWatchTime = startTime;
		this.watchedSegment = firstSeg;
		this.reTransmitSocket = socket;
		this.timeout = timeout;
		this.destIP = dest;
	}
	
	@Override
	public void run(){
		while (true){
			while (!this.stopThread){
				if (System.currentTimeMillis() - this.startWatchTime >= timeout){
					//System.out.println("Retransmit " + this.watchedSegment.getSeqNumber());
					this.reTransmit();
				}
			}
			if (this.terminate){
				//System.out.println("Terminated");
				break;
			}
		}
	}
	
	public void reTransmit(){
		if (this.watchedSegment == null){
			//System.out.println("Tried retransmitting null seg");
			return;
		}
		//System.out.println("ReTrasnmit " + this.watchedSegment.getSeqNumber());
		this.startWatchTime = System.currentTimeMillis();
		DatagramPacket reSend = null;
		try {
			reSend = new DatagramPacket(this.watchedSegment.allDataToBytes(), this.watchedSegment.allDataToBytes().length, this.destIP, this.watchedSegment.getDestPort());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException ex){
			return;
		}
		
		try {
			if (Sender.pldModule() == 0){
				this.reTransmitSocket.send(reSend);
				Logger.logSegment("snd", watchedSegment, Sender.logger, Sender.clientWindow, System.nanoTime() - this.startSenderTime);
				
			} else {
				Logger.logSegment("drop", watchedSegment, Sender.logger, Sender.clientWindow, System.nanoTime() - this.startSenderTime);
			}
		} catch (IOException e) {
				
		}
	}
	
	public void fastRetransmit(){
		//System.out.println("Fast re-trasnmit seg " + this.watchedSegment.getSeqNumber());
		reTransmit();
	}
	
	public Segment getWatchedSegment(){
		return this.watchedSegment;
	}
	
	public void updateWatchedSegment(Segment s, String string){
		if (this.watchedSegment != null && s!= null && this.watchedSegment.getSeqNumber() == s.getSeqNumber()){
			//System.out.println("Ignore duplicate update of " + s.getSeqNumber());
			return;
		}
		this.watchedSegment = s;
		this.startWatchTime = System.currentTimeMillis();
		if (s!=null){
			//System.out.println(string + " Updating watched seg to " + s.getSeqNumber() + "(" + (System.currentTimeMillis() - this.startWatchTime) + ")");
		} else {
			//System.out.println(string + " Updating to watch null val");
		}
	}
	
	public void stopRunningThread(){
		//System.out.println("stop");
		this.stopThread = true;
	}
	
	public void startRunningThread(){
		//System.out.println("start");
		this.startWatchTime = System.currentTimeMillis();
		this.stopThread = false;
	}
	
	public void terminateThread(){
		this.terminate  = true;
	}

	public void testLog(Segment s) {
		
	}
}
