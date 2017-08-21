package sa.lockloop;

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

import com.text.TextFileReader;
import com.text.TextFileWriter;

import sa.loop.LoopAnalyzer;
import sa.loop.LoopInfo;
import sa.wala.WalaUtil;


// time-consuming loop pruning/identifying
public class StaticPruning {
	
	LoopAnalyzer loopAnalyzer;
	
	String daDir;
	// input
	String medianchain = "output/medianchain_bugpool.txt";      //read
	String simplechain = "output/simplechain_bugpool.txt";      //read
	String median = "output/median_bugpool.txt";                //read
	String simple = "output/simple_bugpool.txt";                //read
	String staticx = "output/staticbugpool.txt";                //read
	// output
	String FINALmedianchain = "output/FINAL_medianchain_bugpool.txt";      //output
	String FINALsimplechain = "output/FINAL_simplechain_bugpool.txt";      //output
	String FINALmedian = "output/FINAL_median_bugpool.txt";                //output
	String FINALsimple = "output/FINAL_simple_bugpool.txt";                //output
	String FINALstaticx = "output/FINAL_staticbugpool.txt";                //output
    
	
	
    StaticPruning(LoopAnalyzer loopAnalyzer, String daDir) {
    	this.loopAnalyzer = loopAnalyzer;
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
		findTimeConsumingLoopsForAFile(staticx, FINALstaticx);
	}

	
	public void findTimeConsumingLoopsForAFile(String infile, String outfile) throws IOException {
		System.out.println("\nJX - INFO - findTimeConsumingLoopsForAFile:" + outfile);
		List<String> loopClasses = new ArrayList<String>();
        List<String> loopMethods = new ArrayList<String>();
		List<String> loopLinenumbers = new ArrayList<String>();
		  
		String infilepath = Paths.get(daDir, infile).toString();
		String outfilepath = Paths.get(daDir, outfile).toString(); 
		TextFileReader reader = new TextFileReader(infilepath);
		TextFileWriter writer = new TextFileWriter(outfilepath);
		
		String tmpline;
		Set<String> tmpset = new HashSet<String>();
		
		while ( (tmpline = reader.readLine()) != null ) {
			String[] strs = tmpline.split("\\s+");
			int cascadingLevel = Integer.parseInt( strs[0].substring(2, strs[0].indexOf(':')) );  //useless now
			String codepoint = strs[1].substring(0, strs[1].indexOf(';'));   // looks like "xx.ClassName-MethodName-LineNumber"
			if ( isTimeConsumingLoop(codepoint) ) {
				writer.writeLine( tmpline );
				tmpset.add( codepoint );
			}
		}
		writer.write( "//summary - " + writer.getValidNumberOfLines() + "(#static codepoints=" + tmpset.size() + ") from " + reader.getValidNumberOfLines());
		reader.close();
    	writer.close();
		System.out.println("JX - successfully read " + reader.getValidNumberOfLines() + " Suspected/Critical Bug Loops from " + infilepath);
    	System.out.println("JX - successfully write " + writer.getValidNumberOfLines() + " (#static codepoints=" + tmpset.size() + ") time-consuming Bug Loops into " + outfilepath);
	}
	
	
	public boolean isTimeConsumingLoop(String codepoint) {
		String[] strs = codepoint.trim().split("-");
		String loopclass = strs[0];
		String loopmethod = strs[1];
		int loopline = Integer.parseInt(strs[2]);
		//System.out.println("****" + loopclass + "**" + loopmethod + "**" + loopline + "****");
		
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes())
			for (LoopInfo loop: cgNodeInfo.getLoops()) {
				if (loop.numOfTcOperations_recusively > 0) {
					String classname = WalaUtil.formatClassName( loop.getCGNode().getMethod().getDeclaringClass().getName().toString() );
					String methodname = loop.getCGNode().getMethod().getName().toString();
					int linenumber = loop.getLineNumber();
					if (classname.equals(loopclass) && methodname.equals(loopmethod) && Math.abs(linenumber-loopline)<=2 ) { //!!!!
						System.out.println( loop.toString_detail() );
						return true;
					}
				}
			}
		
		return false;
	}
	
	
}
