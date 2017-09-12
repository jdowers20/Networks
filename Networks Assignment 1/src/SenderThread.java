import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ConcurrentModificationException;

public class SenderThread extends Thread {
	public boolean stopThread = false;
	
	//private SenderWindow window;
	private String type;		//either 'rcv' for datagram receiver, or 'tim' for timeout timer
	private DatagramSocket clientSocket;
	private FileWriter logger;
	private int ackNum;
	private long startTime;
	
	@Override
	public void run() {
		try {
			receiverThread();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public SenderThread(DatagramSocket clientSocket, String type, FileWriter logger, int ackNum, long startTime){
		//this.window = window;
		this.type = type;
		this.clientSocket = clientSocket;
		this.logger = logger;
		this.ackNum = ackNum;
		this.startTime = startTime;
	}
	
	public void receiverThread() throws IOException{
		int previousTracker = 0;
		int fastRTCount = 0;
		this.clientSocket.setSoTimeout(1);
		while (!this.stopThread){
			System.out.println("thread");
			DatagramPacket receiveAck = new DatagramPacket(new byte[Segment.HEADER_SIZE], Segment.HEADER_SIZE);
			//this.clientSocket.setSoTimeout(0);
			
			try {
				this.clientSocket.receive(receiveAck);
			} catch (SocketTimeoutException ex){
				//System.out.println("Timeout"); 
				continue;
			} catch (SocketException e) {
				//System.out.println("Broken");
				break;
			}
			
			Segment ackSeg = Segment.byteArrayToSegment(receiveAck.getData());
			Logger.logSegment("rcv", ackSeg, this.logger, Sender.clientWindow, System.nanoTime() - this.startTime);
			if (ackSeg.getAckNumber() == previousTracker){
				fastRTCount++;
				if (fastRTCount == 2){
					//Sender.clientWindow.triggerFastReTransmit(ackSeg.getAckNumber());
					fastRTCount = 0;
					previousTracker = 0;
				}
			} else {
				previousTracker = ackSeg.getAckNumber();
				fastRTCount = 0;
			}
			
			
			if (ackSeg.isAck()){
				this.ackNum = ackSeg.getSeqNumber() + 1;
				Sender.clientWindow.acknowledge(ackSeg.getAckNumber());
			}
			
		}
		this.clientSocket.setSoTimeout(0);
	}
	
	public int getAckNum(){
		return this.ackNum;
	}

}
