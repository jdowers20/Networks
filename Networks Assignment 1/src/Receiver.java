import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;

public class Receiver {
	private int receiver_port;
	private String file;
	
	Window serverWindow;
	
	public Receiver(String[] args){
		this.receiver_port = Integer.parseInt(args[0]);
		this.file = args[1];
		
		this.serverWindow = new Window(1024*10);
	}
	
	public static void main(String[] args) throws Exception{
		if (args.length != 2){
			System.err.println("Usage: java Receiver receiver_port file");
			System.exit(1);
		}
		Receiver receiver = new Receiver(args);
		receiver.waitForConnection();
	}
	
	public DatagramSocket waitForConnection() throws Exception{
		DatagramSocket serverSocket = new DatagramSocket(this.receiver_port);
		
		while (true){
			DatagramPacket welcomePacket = new DatagramPacket(new byte[1024], 1024);
			serverSocket.receive(welcomePacket);			
			Segment receivedSegment = Segment.byteArrayToSegment(welcomePacket.getData());
			System.out.print("RECEIVED ");
			receivedSegment.printSegment();
			
			if(receivedSegment.isSyn()){
				int serverISN = (ThreadLocalRandom.current().nextInt()) % 1000;
				if (serverISN < 0){serverISN = serverISN*-1;}
				Segment synAck = new Segment(receivedSegment.getDestPort(), receivedSegment.getSourcePort(), serverISN, receivedSegment.getSeqNumber() + 1);
				synAck.setSyn(true);
				synAck.setAck(true);
				
				DatagramPacket reply = new DatagramPacket(synAck.allDataToBytes(), synAck.allDataToBytes().length, welcomePacket.getAddress(), welcomePacket.getPort());
				serverSocket.send(reply);
				System.out.print("SENT ");
				synAck.printSegment();
				serverSocket.setSoTimeout(1000);
				
				while (true){
					DatagramPacket finalAck = new DatagramPacket(new byte[1024], 1024);
					try {
						serverSocket.receive(finalAck);
					} catch (SocketTimeoutException ex) {
						serverSocket.send(reply);
						System.out.print("SENT ");
						synAck.printSegment();
						continue;
					}
					Segment ackSeg = Segment.byteArrayToSegment(finalAck.getData());
					if (ackSeg.isAck()){
						System.out.print("RECEIVED ");
						ackSeg.printSegment();
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

	public String getFile() {
		return file;
	}
}
