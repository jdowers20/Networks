import java.util.Iterator;

public class SenderWindow extends Window {
	TimeoutWatchdog tw;
	
	public SenderWindow(int mws, TimeoutWatchdog tw) {
		super(mws);
		this.tw = tw;
	}
	
	public synchronized void acknowledge(int ackNum){
		//System.out.print("Just received acknum " + ackNum+ " ");
		Iterator<Segment> iter = this.window.iterator();
		Segment s = null;
		while (iter.hasNext()) {
		    s = iter.next();

		    if (s.getSeqNumber() < ackNum){
		        iter.remove();
				//System.out.println("Ack "+ ackNum + " - Remove " + s.getSeqNumber() + " AckNum in window " + this.windowContainsSegment(ackNum));
		    } else {
		    	break;
		    }
		}
		try {
			this.tw.updateWatchedSegment(s, Integer.toString(ackNum));
			if (s == null){
				this.tw.stopRunningThread();
			}
		} catch (IndexOutOfBoundsException ex){
			//System.out.println("Tried updating but error occurred");
			//System.out.println(ex.getMessage());
		}
	}
	
	@Override
	public synchronized boolean addSegment(Segment segment){
		boolean added = super.addSegment(segment);
		//System.out.println("Add " + segment.getSeqNumber());
		
		if (this.tw.getWatchedSegment() == null){
			this.tw.updateWatchedSegment(segment, "s");
			this.tw.startRunningThread();
			try {
				this.tw.start();
			} catch (IllegalThreadStateException ex){
				//ignore exception
			}
		}
		
		return added;
	}
	
	public synchronized void triggerFastReTransmit(int i){
		System.out.println("Fast Retransmit: " + i + " was doubled up, " + this.window.get(0).getSeqNumber() + " is first in line");
		this.tw.fastRetransmit();
	}

}
