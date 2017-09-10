import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

public class Sender {
	
	private InetAddress receiver_host_ip;
	private int receiver_port;
	private String file;
	private int mws;						//maximum window size
	private int mss;						//maximum segment size
	private int timeout;
	private double pdrop;
	private int seed;
	
	Window clientWindow;
	
	private FileWriter logger;
	
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
		this.pdrop = Double.parseDouble(args[6]);
		this.seed = Integer.parseInt(args[7]);
		//this.printSenderDetails();
		
		this.clientWindow = new Window(this.mws);
		this.logger = new FileWriter("Sender_Log.txt");
	}
	
	public static void main(String args[]) throws Exception{
		if (args.length != 8){
			System.err.println("Usage: java Sender receiver_host_ip receiver_port file mws mss timeout pdrop seed");
			System.exit(1);
		}
		Sender sender = new Sender(args);
		sender.startUp();
		sender.logger.close();
	}
	
	public DatagramSocket startUp() throws IOException, InstantiationException, IllegalAccessException {
		DatagramSocket startupSocket = new DatagramSocket();
		
		int clientISN = (ThreadLocalRandom.current().nextInt()) % 1000;
		if (clientISN < 0){clientISN = clientISN*-1;}
		Segment initialSyn = new Segment(startupSocket.getLocalPort(), this.receiver_port, clientISN, 0);
		initialSyn.setSyn(true);
		
		DatagramPacket sendSyn = new DatagramPacket(initialSyn.allDataToBytes(), initialSyn.allDataToBytes().length, this.receiver_host_ip, this.receiver_port);
		startupSocket.send(sendSyn);
		System.out.print("SENT ");
		initialSyn.printSegment();
		
		startupSocket.setSoTimeout(this.timeout);
		while(true){
			DatagramPacket getSynAck = new DatagramPacket(new byte[1024], 1024);
			try {
				startupSocket.receive(getSynAck);
			} catch (SocketTimeoutException ex){
				startupSocket.send(sendSyn);
				System.out.print("SENT ");
				initialSyn.printSegment();
				continue;
			}
			Segment synAck = Segment.byteArrayToSegment(getSynAck.getData());
			System.out.print("RECEIVED ");
			synAck.printSegment();
			if (synAck.isAck() && synAck.isSyn()){
				Segment lastAck = new Segment(startupSocket.getLocalPort(), this.receiver_port, synAck.getAckNumber(), synAck.getSeqNumber() + 1);
				lastAck.setAck(true);
				DatagramPacket finalAck = new DatagramPacket(lastAck.allDataToBytes(), lastAck.allDataToBytes().length, this.receiver_host_ip, this.receiver_port);
				startupSocket.send(finalAck);
				System.out.print("SENT ");
				lastAck.printSegment();				
				break;
			}
		}
		
		return startupSocket;
	}
	
	public void printSenderDetails(){
		System.out.println("Obtained:");
		System.out.println("    -receiver_host_ip: " + this.receiver_host_ip.toString());
		System.out.println("    -receiver_port: " + this.receiver_port);
		System.out.println("    -file: " + this.file);
		System.out.println("    -mws: " + this.mws);
		System.out.println("    -mss: " + this.mss);
		System.out.println("    -timeout: " + this.timeout);
		System.out.println("    -pdrop: " + this.pdrop);
		System.out.println("    -seed: " + this.seed);
	}
}
