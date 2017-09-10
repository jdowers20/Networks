import java.util.ArrayList;

public class Window {
	private int mws;
	private ArrayList<Segment> window = new ArrayList<Segment>();
	
	public Window(int mws){
		this.mws = mws;
	}
	
	public int getMaxSize() {
		return this.mws;
	}
	
	public boolean addSegment(Segment segment) {
		if (this.getCurrentSize() + segment.getDataLength() > mws){
			return false;
		}
		this.window.add(segment);
		
		return true;
	}
	
	public int getCurrentSize(){
		int size = 0;
		for (Segment s : this.window){
			size += s.getData().length;
		}
		return size;
	}
}
