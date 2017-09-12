import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ReceiverWindow extends Window {
	private ArrayList<Integer> added = new ArrayList<Integer>();

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
					receivedFile.write(super.window.get(0).getData());
					added.add(super.window.get(0).getSeqNumber());
				}
				this.window.remove(super.window.get(0));
			}
		}
		return ackNum;
	}

}
