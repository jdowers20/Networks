import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;

public class Segment {
	private int sourcePort;
	private int destPort;
	private int seqNumber;
	private int ackNumber;
	
	public static final int HEADER_SIZE = 36;
	private int headerLength = HEADER_SIZE;
	
	private boolean ack = false;
	private boolean syn = false;
	private boolean fin = false;
	
	private int checksum;
	
	private byte[] data;
	
	public static Segment byteArrayToSegment(byte[] input){
		Segment output;
		
		ByteBuffer buffer = ByteBuffer.wrap(input);

		int newSourcePort = buffer.getInt();
		int newDestPort = buffer.getInt();
		int newSeqNumber = buffer.getInt();
		int newAckNumber = buffer.getInt();
		
		int newHeaderLength = buffer.getInt();
		
		boolean newAck = intToBool(buffer.getInt());
		boolean newSyn = intToBool(buffer.getInt());
		boolean newFin = intToBool(buffer.getInt());
		
		int newChecksum = buffer.getInt();
		
		
		byte[] newData = Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.array().length);
		for (int i = 0; i < newData.length; i++){
			if (newData[i] == 0){
				newData = Arrays.copyOfRange(newData, 0, i);
				break;
			}
		}
		
		output = new Segment(newSourcePort, newDestPort, newSeqNumber, newAckNumber);
		output.setHeaderLength(newHeaderLength);
		output.setAck(newAck);
		output.setSyn(newSyn);
		output.setFin(newFin);
		output.setChecksum(newChecksum);
		output.setData(newData);
		
		return output;
	}
	
	public boolean runCheckSum(){
		int calculatedCheckSum = this.calculateCheckSum();
		if (!(calculatedCheckSum == this.checksum)){
			System.out.println("Failed CheckSum");
		}
		
		return (calculatedCheckSum == this.checksum);
	}
	
	public int calculateCheckSum(){
		int sum = this.sourcePort;
		sum += this.destPort;
		sum += this.seqNumber;
		sum += this.ackNumber;
		sum += this.headerLength;
		sum += boolToInt(this.ack);
		sum += boolToInt(this.syn);
		sum += boolToInt(this.fin);
		
		return ~sum;
	}
	
	public Segment(int sourcePort, int destPort, int seqNumber, int ackNumber){
		this.sourcePort = sourcePort;
		this.destPort = destPort;
		this.seqNumber = seqNumber;
		this.ackNumber = ackNumber;
	}
	
	public byte[] allDataToBytes() throws IOException{
		ByteArrayOutputStream convert = new ByteArrayOutputStream();
		convert.write(ByteBuffer.allocate(4).putInt(sourcePort).array());
		convert.write(ByteBuffer.allocate(4).putInt(destPort).array());
		convert.write(ByteBuffer.allocate(4).putInt(seqNumber).array());
		convert.write(ByteBuffer.allocate(4).putInt(ackNumber).array());

		convert.write(ByteBuffer.allocate(4).putInt(headerLength).array());

		convert.write(ByteBuffer.allocate(4).putInt(boolToInt(this.ack)).array());
		convert.write(ByteBuffer.allocate(4).putInt(boolToInt(this.syn)).array());
		convert.write(ByteBuffer.allocate(4).putInt(boolToInt(this.fin)).array());

		this.checksum = this.calculateCheckSum();
		convert.write(ByteBuffer.allocate(4).putInt(checksum).array());
		
		if (this.data != null){
			convert.write(this.data);
		}
		
		byte[] segmentData = convert.toByteArray();
		
		return segmentData;
	}
	
	public int boolToInt(boolean b){
		if (b){
			return 1;
		} else {
			return 0;
		}
	}
	
	public static boolean intToBool(int i) {
		if (i == 0){
			return false;
		} else {
			return true;
		}
	}
	
	public int getHeaderLength() {
		return headerLength;
	}

	public void setHeaderLength(int headerLength) {
		this.headerLength = headerLength;
	}

	public boolean isAck() {
		return ack;
	}

	public void setAck(boolean ack) {
		this.ack = ack;
	}

	public boolean isSyn() {
		return syn;
	}

	public void setSyn(boolean syn) {
		this.syn = syn;
	}

	public boolean isFin() {
		return fin;
	}

	public void setFin(boolean fin) {
		this.fin = fin;
	}

	public int getChecksum() {
		return checksum;
	}

	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}

	public byte[] getData() {
		if (this.data == null){
			return "".getBytes();
		}
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int getSourcePort() {
		return sourcePort;
	}

	public int getDestPort() {
		return destPort;
	}

	public int getSeqNumber() {
		return seqNumber;
	}

	public int getAckNumber() {
		return ackNumber;
	}
	
	public int getDataLength(){
		return this.data.length;
	}
	
	public void printSegment(){
		System.out.println("Segment:");
		System.out.println("   -Source Port: " + this.sourcePort);
		System.out.println("   -Dest Port: " + this.destPort);
		System.out.println("   -Seq Num: " + this.seqNumber);
		System.out.println("   -Ack Num: " + this.ackNumber);

		System.out.println("   -Header Length: " + this.headerLength);
		
		System.out.println("   -Ack: " + this.ack);
		System.out.println("   -Syn: " + this.syn);
		System.out.println("   -Fin: " + this.fin);

		System.out.println("   -CheckSum: " + this.checksum);
		
		if (this.data != null){
			System.out.println("   -Data: " + new String(this.data) + "(" + this.data.length +")");
		} else {
			System.out.println("   -No data");
		}
		System.out.println("");
	}
}
