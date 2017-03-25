package sa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import sa.tc.MRrpc;


// time-consuming loop pruning/identifying
public class StaticPruning {
	
	Map<Integer, List<LoopInfo>> functions_with_loops;  // map: function CGNode id -> loops, ONLY covers functions that really involve loops
	String daDir;
	String medianchain = "output/medianchain_bugpool.txt";      //read
	String simplechain = "output/simplechain_bugpool.txt";      //read
	String median = "output/median_bugpool.txt";                //read
	String simple = "output/simple_bugpool.txt";                //read   
	String FINALmedianchain = "output/FINAL_medianchain_bugpool.txt";      //output
	String FINALsimplechain = "output/FINAL_simplechain_bugpool.txt";      //output
	String FINALmedian = "output/FINAL_median_bugpool.txt";                //output
	String FINALsimple = "output/FINAL_simple_bugpool.txt";                //output
    
    StaticPruning(Map<Integer, List<LoopInfo>> functions_with_loops, String daDir) {
		this.functions_with_loops = functions_with_loops;
		this.daDir = daDir;
	}
	
	public void doWork() {
		try {
			findTimeConsumingLoops();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public void findTimeConsumingLoops() throws IOException {
		findTimeConsumingLoopsForAFile(medianchain, FINALmedianchain);
		findTimeConsumingLoopsForAFile(simplechain, FINALsimplechain);
		findTimeConsumingLoopsForAFile(median, FINALmedian);
		findTimeConsumingLoopsForAFile(simple, FINALsimple);
	}

	public void findTimeConsumingLoopsForAFile(String infile, String outfile) throws IOException {
		List<String> loopClasses = new ArrayList<String>();
        List<String> loopMethods = new ArrayList<String>();
		List<String> loopLinenumbers = new ArrayList<String>();
		  
		String infilepath = Paths.get(daDir, infile).toString();
		String outfilepath = Paths.get(daDir, outfile).toString(); 
		BufferedReader bufreader = new BufferedReader( new FileReader( infilepath ) );
		BufferedWriter bufwriter = new BufferedWriter( new FileWriter( outfilepath ) );
		String tmpline;
		int total = Integer.parseInt( bufreader.readLine() ); // the 1st line is useless
		int count = 0;
		Set<String> tmpset = new HashSet<String>();
		
		while ( (tmpline = bufreader.readLine()) != null ) {
			String[] strs = tmpline.trim().split("\\s+");
			if ( tmpline.trim().length() > 0 ) {
				int cascadingLevel = Integer.parseInt( strs[0].substring(2, strs[0].indexOf(':')) ); 
				String codepoint = strs[1].substring(0, strs[1].indexOf(';'));   // looks like "xx.ClassName-MethodName-LineNumber"
				if ( isTimeConsumingLoop(codepoint) ) {
					bufwriter.write( tmpline + "\n" );
					count ++;
					tmpset.add( codepoint );
				}
			}
		}
		// summary -
		bufwriter.write( "summary - " + count + "(#static codepoints=" + tmpset.size() + ")" );
		bufreader.close();
		bufwriter.flush();
    	bufwriter.close();
		System.out.println("JX - successfully read " + total + " Suspected/Critical Bug Loops from " + infilepath);
    	System.out.println("JX - successfully write " + count + "(#static codepoints=" + tmpset.size() + ") time-consuming Bug Loops into " + outfilepath);
	}
	
	public boolean isTimeConsumingLoop(String codepoint) {
		String[] strs = codepoint.trim().split("-");
		String loopclass = strs[0];
		String loopmethod = strs[1];
		int loopline = Integer.parseInt(strs[2]);
		//System.out.println("****" + loopclass + "**" + loopmethod + "**" + loopline + "****");
		for (List<LoopInfo> loops: functions_with_loops.values() )
			for (LoopInfo loop: loops)
				if (loop.numOfTcOperations_recusively > 0) {
					String classname = MRrpc.format( loop.function.getMethod().getDeclaringClass().getName().toString() );
					String methodname = loop.function.getMethod().getName().toString();
					int linenumber = loop.line_number;
					if (classname.equals(loopclass) && methodname.equals(loopmethod) && Math.abs(linenumber-loopline)<=2 ) //!!!!
						return true;
				}
		return false;
	}
	
	
}
