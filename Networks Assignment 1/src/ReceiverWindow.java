import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ReceiverWindow extends Window {
	private ArrayList<Integer> added = new ArrayList<Integer>();
	private int totalReceivedBytes = 0;

	public ReceiverWindow(int mws) {
		super(mws);
	}
	
	public int addSegment(Segment segment, int ackNum, ByteArrayOutputStream receivedFile) throws IOException{
		if (super.addSegment(segment)){
			while (!super.isWindowEmpty() && super.window.get(0).getSeqNumber() <= ackNum){
				if (ackNum == super.window.get(0).getSeqNumber()){
					ackNum += super.window.get(0).getData().length;
				}
				if (!added.contains(super.window.get(0).getSeqNumber())){
					this.totalReceivedBytes += super.window.get(0).getData().length;
					receivedFile.write(super.window.get(0).getData());
					receivedFile.flush();
					added.add(super.window.get(0).getSeqNumber());
				}
				this.window.remove(super.window.get(0));
			}
		}
		return ackNum;
	}
	
	public int getTotalBytes(){
		return this.totalReceivedBytes;
	}

}
