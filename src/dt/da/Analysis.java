package dt.da;

import com.xml.TexttoXml;

public class Analysis {
	
	public static void main (String [] args) {
		if (args.length != 1) {
			System.out.println("JX - ERROR - args.length != 1");
			return;
		}
		
		String inputDir = args[0];               //inputDir, like  ~/JXCascading-detector/input/MR-4813  // the directory containing log files to process
	
		// transform inputDir to xmlDir
		System.out.println("JX - INFO - covert text to xml ...");
		String[] argv = { inputDir, "null", "null" };
		TexttoXml.main( argv );
		
		// handle xmlDir
		System.out.println("JX - INFO - loop analysis ...");
		String xmlDir = inputDir + "-xml";
		LoopAnalyzer loopAnalyzer = new LoopAnalyzer( xmlDir );
		loopAnalyzer.doWork();
	}
	
	


}
