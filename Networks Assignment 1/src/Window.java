import java.util.ArrayList;

public class Window {
	protected int mws;
	protected ArrayList<Segment> window = new ArrayList<Segment>();
	
	public Window(int mws){
		this.mws = mws;
	}
	
	public synchronized int getMaxSize() {
		return this.mws;
	}
	
	public synchronized boolean addSegment(Segment segment) {
		if (this.getCurrentSize() + segment.getDataLength() > mws){
			return false;
		}
		for (Segment s : this.window){
			if (segment.getSeqNumber() < s.getSeqNumber()){
				this.window.add(this.window.indexOf(s),segment);
				return true;
			}
		}
		this.window.add(segment);
		return true;
	}
	
	public synchronized int getCurrentSize(){
		int size = 0;
		for (Segment s : this.window){
			size += s.getData().length;
		}
		return size;
	}
	
	public synchronized boolean windowContainsSegment(Segment check){
		for (Segment s : this.window){
			if (s.getSeqNumber() == check.getSeqNumber()){
				return true;
			}
		}
		return false;
	}

	public synchronized boolean windowContainsSegment(int seq){
		for (Segment s : this.window){
			if (s.getSeqNumber() == seq){
				return true;
			}
		}
		return false;
	}
	public synchronized boolean isWindowEmpty(){
		return this.window.isEmpty();
	}
	
	public synchronized String windowSeqNumsToString(){
		String s = "[ ";
		
		for(Segment seg : this.window){
			s += seg.getSeqNumber();
			s += "(";
			s += this.window.indexOf(seg);
			s += ")";
			s += " -> ";
		}
		
		s+= " X ]";
		
		return s;
	}
}
