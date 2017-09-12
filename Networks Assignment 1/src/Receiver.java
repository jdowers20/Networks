import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;

public class Receiver {
	private int receiver_port;
	private String file;
	private int ackNum;
	private int seqNum;
	private ByteArrayOutputStream receivedFile = new ByteArrayOutputStream(); 
	private InetAddress senderAddress;
	private int senderPort;
	
	private FileWriter logger;
	
	ReceiverWindow serverWindow;
	private long startTime;
	
	public Receiver(String[] args) throws IOException{
		this.receiver_port = Integer.parseInt(args[0]);
		this.file = args[1];
		this.startTime = System.nanoTime();
		
		this.serverWindow = new ReceiverWindow(1024*10);
		
		this.logger = new FileWriter("Receiver_log.txt");
		this.logger.write("                 seq   size  ack\n");
		this.logger.flush();
	}
	
	public static void main(String[] args) throws Exception{
		if (args.length != 2){
			System.err.println("Usage: java Receiver receiver_port file");
			System.exit(1);
		}
		Receiver receiver = new Receiver(args);
		DatagramSocket receiverSocket = receiver.waitForConnection();
		receiver.receiveFile(receiverSocket);
		//Note, we have already received the first fin at this stage
		receiver.closeConnection(receiverSocket);
		receiver.logger.close();
		
		FileWriter fileOut = new FileWriter(receiver.file);
		fileOut.write(new String(receiver.receivedFile.toByteArray()));
		fileOut.close();
		
	}

	public DatagramSocket waitForConnection() throws Exception{
		DatagramSocket serverSocket = new DatagramSocket(this.receiver_port);
		
		while (true){
			DatagramPacket welcomePacket = new DatagramPacket(new byte[Segment.HEADER_SIZE], Segment.HEADER_SIZE);
			serverSocket.receive(welcomePacket);
			this.senderAddress = welcomePacket.getAddress();
			this.senderPort = welcomePacket.getPort();
			Segment receivedSegment = Segment.byteArrayToSegment(welcomePacket.getData());
			this.startTime = System.nanoTime();
			Logger.logSegment("rcv", receivedSegment, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
			
			if(receivedSegment.isSyn()){
				int serverISN = (ThreadLocalRandom.current().nextInt()) % 1000;
				if (serverISN < 0){serverISN = serverISN*-1;}
				Segment synAck = new Segment(receivedSegment.getDestPort(), receivedSegment.getSourcePort(), serverISN, receivedSegment.getSeqNumber() + 1);
				synAck.setSyn(true);
				synAck.setAck(true);
				
				DatagramPacket reply = new DatagramPacket(synAck.allDataToBytes(), synAck.allDataToBytes().length, welcomePacket.getAddress(), welcomePacket.getPort());
				serverSocket.send(reply);
				Logger.logSegment("snd", synAck, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
				serverSocket.setSoTimeout(1000);
				this.seqNum = serverISN + 1;
				
				while (true){
					DatagramPacket finalAck = new DatagramPacket(new byte[Segment.HEADER_SIZE], Segment.HEADER_SIZE);
					try {
						serverSocket.receive(finalAck);
					} catch (SocketTimeoutException ex) {
						serverSocket.send(reply);
						Logger.logSegment("snd", synAck, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
						continue;
					}
					Segment ackSeg = Segment.byteArrayToSegment(finalAck.getData());
					if (ackSeg.isAck()){
						Logger.logSegment("rcv", ackSeg, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
						this.ackNum = ackSeg.getSeqNumber() + 1;
						break;	
					}
				}
				break;
			} else {
				continue;
			}
		}
		
		return serverSocket;
	}
	
	public void receiveFile(DatagramSocket receiverSocket) throws Exception{
		receiverSocket.setSoTimeout(0);
		while (true){
			DatagramPacket receivedPacket = new DatagramPacket(new byte[50000], 50000);
			receiverSocket.receive(receivedPacket);
			
			Segment receivedSegment = Segment.byteArrayToSegment(receivedPacket.getData());
			
			Logger.logSegment("rcv", receivedSegment, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
			if (receivedSegment.isFin()){
				break;
			}
			
			if (!serverWindow.windowContainsSegment(receivedSegment)){
				//System.out.println("Added " + receivedSegment.getSeqNumber());
				this.ackNum = this.serverWindow.addSegment(receivedSegment, this.ackNum, this.receivedFile);
			}
			Segment returnSegment = new Segment(this.receiver_port, this.senderPort, this.seqNum, this.ackNum);
			returnSegment.setAck(true);
			this.seqNum++;
			DatagramPacket returnPacket = new DatagramPacket(returnSegment.allDataToBytes(), returnSegment.allDataToBytes().length, this.senderAddress, this.senderPort);
			
			receiverSocket.send(returnPacket);
			Logger.logSegment("snd", returnSegment, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
		}
	}
	
	private void closeConnection(DatagramSocket receiverSocket) throws IOException {
		Segment finAck = new Segment(receiverSocket.getPort(), this.senderPort, this.seqNum, this.ackNum);
		finAck.setAck(true);
		finAck.setFin(true);
		this.seqNum++;
		this.ackNum++;
		
		receiverSocket.setSoTimeout(100);
		while (true){
			receiverSocket.send(new DatagramPacket(finAck.allDataToBytes(), finAck.allDataToBytes().length, this.senderAddress, this.senderPort));
			Logger.logSegment("snd", finAck, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
			
			DatagramPacket finalAck = new DatagramPacket(new byte[Segment.HEADER_SIZE], Segment.HEADER_SIZE);
			try {
				receiverSocket.receive(finalAck);
			} catch (SocketTimeoutException ex){
				continue;
			}
			Segment finalAckSeg = Segment.byteArrayToSegment(finalAck.getData());
			Logger.logSegment("rcv", finalAckSeg, this.logger, this.serverWindow, System.nanoTime() - this.startTime);
			if (finalAckSeg.isAck()){
				break;
			}
		}
		
	}

	public String getFile() {
		return file;
	}
	
}
