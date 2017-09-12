import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

public class Logger {

	
	/**Send output to log file
	 * 
	 * @param type	EITHER 'snd' or 'rcv' ('drop' only used for Sender)
	 * @param segment
	 * @param window 
	 * @throws IOException 
	 */
	public synchronized static void logSegment(String type, Segment segment, FileWriter logger, Window window, long time) throws IOException{
		int spaces = 0;
		logger.write(type);
		logger.write(" ");
		if (type != "drop" && type != "dtop"){
			logger.write(" ");
		}
		
		double dTime = (new Long(time).doubleValue()/1000000.0);
		DecimalFormat df = new DecimalFormat("#000.000");
		logger.write(df.format(dTime));
		logger.write(" ");
		
		spaces = 3;
		if(segment.isSyn()){
			logger.write("S");
			spaces--;
		}
		if(segment.isFin()){
			logger.write("F");
			spaces--;
		}
		if(segment.isAck()){
			logger.write("A");
			spaces--;
		}
		if (!segment.isAck() && !segment.isFin() && !segment.isSyn()){
			logger.write("D");
			spaces--;
		}
		logger.write(repeatedSpaces(spaces));
		
		logger.write(Integer.toString(segment.getSeqNumber()));
		logger.write(repeatedSpaces(5-Math.log10(  (segment.getSeqNumber() == 0 ? 1 : segment.getSeqNumber()) )));
		
		if (segment.getData() == null){
			logger.write("0");
			logger.write(repeatedSpaces(4.0));
		} else {
			logger.write(Integer.toString(segment.getData().length));
			logger.write(repeatedSpaces(5-Math.log10(  (segment.getData().length == 0 ? 1 : segment.getData().length) )));
		}
		
		logger.write(Integer.toString(segment.getAckNumber()));
		//logger.write(" - '");
		
		//if (window != null){logger.write(window.windowSeqNumsToString());}
		//logger.write(Long.toString(time));
		logger.write("\n");
		logger.flush();
		/*switch (type){
		case "snd":
			System.out.print("SENT ");
			segment.printSegment();
			break;
		case "rcv":
			System.out.print("RECEIVED ");
			segment.printSegment();
			break;
		}*/
		//if (logger.equals(Sender.logger)){System.out.println(type.toUpperCase() + " " + segment.seqNumber + " " + segment.ackNumber);}
	}
	
	public static String repeatedSpaces(double d){
		String s = "";
		while (d > 0){
			s = s + " ";
			d--;
		}
		return s;
	}
}
