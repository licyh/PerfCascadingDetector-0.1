package dt.da;


public class Analysis {
	
	public static void main (String [] args) {
		if (args.length != 1) {
			System.out.println("JX - ERROR - args.length != 1");
			return;
		}
		
		String inputDir = args[0];               //inputDir, like  ~/JXCascading-detector/input/MR-4813  // the directory containing log files to process
		String xmlDir = inputDir + "-xml";                                    	
	
		// transform inputDir to xmlDir
		String[] argv = { inputDir, null, null };
		TexttoXml.main( argv );
		
		// handle xmlDir
		LoopAnalyzer loopAnalyzer = new LoopAnalyzer( xmlDir );
		loopAnalyzer.doWork();
	}
	
	


}
