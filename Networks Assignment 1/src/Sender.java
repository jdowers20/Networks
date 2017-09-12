import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Random;

public class Sender {
	
	private InetAddress receiver_host_ip;
	private int receiver_port;
	private String file;
	private int mws;						//maximum window size
	private int mss;						//maximum segment size
	private long timeout;
	private static double pdrop;
	private int seed;
	
	static SenderWindow clientWindow;
	TimeoutWatchdog tw;
	private int seqNum;
	private int ackNum;
	
	static FileWriter logger;
	private static Random randomNumberGenerator;
	private long startTime;
	
	public Sender(String args[]) throws IOException{
		try {
			this.receiver_host_ip = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			System.err.println(args[0] + " is an invalid IP address");
			e.printStackTrace();
		}
		this.receiver_port = Integer.parseInt(args[1]);
		this.file = args[2];
		this.mws = Integer.parseInt(args[3]);
		this.mss = Integer.parseInt(args[4]);
		this.timeout = Integer.parseInt(args[5]);
		Sender.pdrop = Double.parseDouble(args[6]);
		this.seed = Integer.parseInt(args[7]);
		//this.printSenderDetails();
		Sender.randomNumberGenerator = new Random(this.seed);
		this.startTime = System.nanoTime();
		
		Sender.logger = new FileWriter("Sender_log.txt");
		Sender.logger.write("                 seq   size  ack\n");
		Sender.logger.flush();
	}
	
	public static void main(String args[]) throws Exception{
		if (args.length != 8){
			System.err.println("Usage: java Sender receiver_host_ip receiver_port file mws mss timeout pdrop seed");
			System.exit(1);
		}
		Sender sender = new Sender(args);
		DatagramSocket clientSocket = sender.startUp();
		sender.sendFile(clientSocket);
		sender.closeConnection(clientSocket);
		clientSocket.close();
		
		Sender.logger.close();
	}
	
	public DatagramSocket startUp() throws IOException, InstantiationException, IllegalAccessException {
		DatagramSocket startupSocket = new DatagramSocket();
		
		int clientISN = randomNumberGenerator.nextInt(1000);
		if (clientISN < 0){clientISN = clientISN*-1;}
		Segment initialSyn = new Segment(startupSocket.getLocalPort(), this.receiver_port, clientISN, 0);
		initialSyn.setSyn(true);
		
		DatagramPacket sendSyn = new DatagramPacket(initialSyn.allDataToBytes(), initialSyn.allDataToBytes().length, this.receiver_host_ip, this.receiver_port);
		startupSocket.send(sendSyn);
		this.seqNum = clientISN + 1;
		this.startTime = System.nanoTime();
		Logger.logSegment("snd", initialSyn, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
		startupSocket.setSoTimeout(1000);
		
		while(true){
			DatagramPacket getSynAck = new DatagramPacket(new byte[Segment.HEADER_SIZE], Segment.HEADER_SIZE);
			try {
				startupSocket.receive(getSynAck);
			} catch (SocketTimeoutException ex){
				startupSocket.send(sendSyn);
				Logger.logSegment("snd", initialSyn, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
				continue;
			}
			Segment synAck = Segment.byteArrayToSegment(getSynAck.getData());
			Logger.logSegment("rcv", synAck, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
			this.ackNum = synAck.getSeqNumber() + 1;
			
			if (synAck.isAck() && synAck.isSyn()){
				Segment lastAck = new Segment(startupSocket.getLocalPort(), this.receiver_port, this.seqNum, this.ackNum);
				lastAck.setAck(true);
				DatagramPacket finalAck = new DatagramPacket(lastAck.allDataToBytes(), lastAck.allDataToBytes().length, this.receiver_host_ip, this.receiver_port);
				startupSocket.send(finalAck);
				Logger.logSegment("snd", lastAck, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
				this.seqNum++;
				break;
			}
		}
		
		return startupSocket;
	}
	
	public void sendFile(DatagramSocket clientSocket) throws IOException{
		this.tw = new TimeoutWatchdog(this.startTime, 0, null, clientSocket, this.timeout, this.receiver_host_ip);
		Sender.clientWindow = new SenderWindow(this.mws, this.tw);
		//start the receiver thread
		SenderThread replyReceiver = new SenderThread(clientSocket, "rcv", Sender.logger, this.ackNum, this.startTime);
		replyReceiver.start();
		
		//start adding segments to the window to send
		BufferedReader in = new BufferedReader(new FileReader(this.file));
		boolean endLoop = false;
		clientSocket.setSoTimeout(0);
		
		while (!endLoop || !Sender.clientWindow.isWindowEmpty()){
			//System.out.println(!endLoop + " || " + !Sender.clientWindow.isWindowEmpty());
			char[] segData = new char[this.mss];
			int numCharsRead = 0;
			
			if (!endLoop){
				numCharsRead = in.read(segData);
				byte[] segDataAsBytes = new String(segData, 0, numCharsRead).getBytes();
				Segment sendSegment = new Segment(clientSocket.getPort(), this.receiver_port, this.seqNum, replyReceiver.getAckNum());
				sendSegment.setData(segDataAsBytes);
				DatagramPacket sendPacket = new DatagramPacket(sendSegment.allDataToBytes(), sendSegment.allDataToBytes().length, this.receiver_host_ip, this.receiver_port);
				
				boolean print = false;
				while (!Sender.clientWindow.addSegment(sendSegment)){
					if (!print){System.out.println("Waiting/Dropped Seg " + sendSegment.getSeqNumber());}
					print = true;
				}
				if (Sender.pldModule() == 0){
					Logger.logSegment("snd", sendSegment, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
					clientSocket.send(sendPacket);
				} else {
					Logger.logSegment("drop", sendSegment, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
				}
				this.seqNum += sendSegment.getData().length;
			}
			
			if (numCharsRead < this.mss){
				endLoop = true;
			}
		}
		this.ackNum = replyReceiver.getAckNum();
		replyReceiver.stopThread = true;
		//clientSocket.disconnect();
		//System.out.println(!endLoop + " || " + !Sender.clientWindow.isWindowEmpty());
		this.tw.stopRunningThread();
		this.tw.terminateThread();
		in.close();
			
	}
	
	public void closeConnection(DatagramSocket clientSocket) throws IOException{
		Segment firstFinSegment = new Segment(clientSocket.getPort(), this.receiver_port, this.seqNum, this.ackNum);
		firstFinSegment.setFin(true);
		this.seqNum++;
		DatagramPacket firstFinPacket = new DatagramPacket(firstFinSegment.allDataToBytes(), firstFinSegment.allDataToBytes().length, this.receiver_host_ip, this.receiver_port);
		
		clientSocket.setSoTimeout(0);
		System.out.println("HERE");
		while (true){
			clientSocket.send(firstFinPacket);
			Logger.logSegment("snd", firstFinSegment, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
			
			DatagramPacket firstReply = new DatagramPacket(new byte[Segment.HEADER_SIZE], Segment.HEADER_SIZE);
			try {
				clientSocket.receive(firstReply);
			} catch (SocketTimeoutException ex) {
				continue;
			}
			Segment firstReplySeg = Segment.byteArrayToSegment(firstReply.getData());
			Logger.logSegment("rcv", firstReplySeg, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
			this.ackNum++;
			
			if ( !(firstReplySeg.isFin() && firstReplySeg.isAck()) ){
				continue;
			}
			
			Segment finalAck = new Segment(clientSocket.getPort(), this.receiver_port, this.seqNum, this.ackNum);
			finalAck.setAck(true);
			clientSocket.send(new DatagramPacket(finalAck.allDataToBytes(), finalAck.allDataToBytes().length, this.receiver_host_ip, this.receiver_port));
			Logger.logSegment("snd", finalAck, Sender.logger, Sender.clientWindow, (System.nanoTime() - this.startTime));
			clientSocket.close();
			break;
		}
	}
	
	public static int pldModule(){
		double randVal = randomNumberGenerator.nextDouble();
		if (randVal <= Sender.pdrop){
			return 1;
		}
		
		return 0;
	}
	
	public void printSenderDetails(){
		System.out.println("Obtained:");
		System.out.println("    -receiver_host_ip: " + this.receiver_host_ip.toString());
		System.out.println("    -receiver_port: " + this.receiver_port);
		System.out.println("    -file: " + this.file);
		System.out.println("    -mws: " + this.mws);
		System.out.println("    -mss: " + this.mss);
		System.out.println("    -timeout: " + this.timeout);
		System.out.println("    -pdrop: " + Sender.pdrop);
		System.out.println("    -seed: " + this.seed);
	}
	
}
