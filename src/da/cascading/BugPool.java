package da.cascading;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.text.TextFileWriter;

import da.graph.HappensBeforeGraph;
import da.tagging.JobTagger;

public class BugPool {
	String projectDir;
	String packageDir = "src/da";
	
    Set<LoopBug> bugs = new HashSet<LoopBug>();          	//dynamic loop instances, only one bug pool for whole code snippets
    Set<Integer> bugnodeset = new HashSet<Integer>();       //HappensBeforeGraph's node index set for bugs 
    

    
    
    HappensBeforeGraph hbg;
    
    // for output
    String simplebugpoolFilename = "output/simple_bugpool.txt";
    String medianbugpoolFilename = "output/median_bugpool.txt";
    String simplechainbugpoolFilename = "output/simplechain_bugpool.txt";
    String medianchainbugpoolFilename = "output/medianchain_bugpool.txt";
    Set<String> simplebugpool = new TreeSet<String>();
	Set<String> medianbugpool = new TreeSet<String>();
	Set<String> simplechainbugpool = new TreeSet<String>();
	Set<String> medianchainbugpool = new TreeSet<String>();
	Set<String> tmpset = new HashSet<String>();
	
	
	public BugPool(String projectDir, HappensBeforeGraph hbg) {
		this.projectDir = projectDir;
		this.hbg = hbg;
	}
	
	
	// newly add, useless now
	public void addLoopBug( LoopBug loopbug ) {
		bugs.add( loopbug );
	}
	
	// for level = 1 for Queue 
    public void addLoopBug( int nodeIndex ) {
    	// add to bug pool
    	LoopBug loopbug = new LoopBug( nodeIndex, 1 );
    	loopbug.cascadingChain.add( nodeIndex );
    	bugs.add( loopbug );
    }
	
	
    public void mergeResults() {
    	// real bug pool
    	System.out.print("\nbugpool - " + "has " + bugs.size() + "(fake) dynamic loop instances");
    	for (LoopBug loopbug: bugs) {
    		int nodeIndex = loopbug.nodeIndex;
    		int cascadingLevel = loopbug.cascadingLevel;
    		bugnodeset.add( nodeIndex );
    		medianchainbugpool.add( "CL" + cascadingLevel + ": " + fullCallstacksOfCascadingChain(loopbug) );
    		simplechainbugpool.add( "CL" + cascadingLevel + ": " + lastCallstacksOfCascadingChain(loopbug) );
    		medianbugpool.add( "CL" + cascadingLevel + ": " + hbg.fullCallstack(nodeIndex) );
    		simplebugpool.add( "CL" + cascadingLevel + ": " + hbg.lastCallstack(nodeIndex) );
    		tmpset.add( hbg.lastCallstack(nodeIndex) );
    	}
    	System.out.println(", ie, representing " + bugnodeset.size() + "(real) loop nodes out of total " + hbg.getNodeList().size() + " happens-before nodes");
    }
    
    
    public String fullCallstacksOfCascadingChain(LoopBug loopbug) {
    	String result = "";
    	for (int nodeindex: loopbug.cascadingChain) {
    		result += hbg.fullCallstack(nodeindex) + "|";
    		// for DEBUG
    		//result += hbg.getNodePIDTID(nodeindex) + ":" + hbg.fullCallstack(nodeindex) + "|";
	        //result += hbg.getNodePIDTID(nodeindex)+":"+nodeindex + ":" + hbg.fullCallstack(nodeindex) + "|";
    	}
    	return result;
    }
    
    
    public String lastCallstacksOfCascadingChain(LoopBug loopbug) {
    	String result = "";
    	for (int nodeindex: loopbug.cascadingChain) {
    		result += hbg.lastCallstack(nodeindex) + "|";
    		// for DEBUG
    		//result += hbg.getNodePIDTID(nodeindex) + ":" + hbg.lastCallstack(nodeindex) + "|";
    	}
    	return result;
    }
    
    
    
    
    public void printJobIdentity() {
    	JobTagger jobTagger = new JobTagger( this.hbg );
    	for (int index: bugnodeset) {
    		jobTagger.findJobIdentity(index);
    		System.out.println("\n");
    	}
    }
    
    
    public void printResults() {
    	printResults(false);
    }
    
    public void printResults(boolean appendToFile) {
    	System.out.println("\nJX - INFO - Results of bug pool");
    	
    	mergeResults();
    	printJobIdentity();

    	// bug pools - 
        // write to file & print
    	medianchainbugpoolFilename = Paths.get(projectDir, packageDir, medianchainbugpoolFilename).toString();
    	simplechainbugpoolFilename = Paths.get(projectDir, packageDir, simplechainbugpoolFilename).toString();
    	medianbugpoolFilename = Paths.get(projectDir, packageDir, medianbugpoolFilename).toString();
    	simplebugpoolFilename = Paths.get(projectDir, packageDir, simplebugpoolFilename).toString();
    	System.out.println( "\nwrite to files - " );
    	System.out.println( "\t" + medianchainbugpoolFilename );
    	System.out.println( "\t" + simplechainbugpoolFilename );
    	System.out.println( "\t" + medianbugpoolFilename );
    	System.out.println( "\t" + simplebugpoolFilename );
		
    	TextFileWriter mcWriter = new TextFileWriter( medianchainbugpoolFilename, appendToFile );
    	if (appendToFile)
    		mcWriter.writeLine("\n\n\n//Queue-related Bugs\n");
    	TextFileWriter scWriter = new TextFileWriter( simplechainbugpoolFilename, appendToFile );
    	if (appendToFile)
    		scWriter.writeLine("\n\n\n//Queue-related Bugs\n");
    	TextFileWriter mWriter = new TextFileWriter( medianbugpoolFilename, appendToFile );
    	if (appendToFile)
    		mWriter.writeLine("\n\n\n//Queue-related Bugs\n");
    	TextFileWriter sWriter = new TextFileWriter( simplebugpoolFilename, appendToFile );
    	if (appendToFile)
    		sWriter.writeLine("\n\n\n//Queue-related Bugs\n");

    	System.out.println("\nmedianchainbugpool(whole chain's fullcallstacks) - " + "has " + medianchainbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	mcWriter.writeLine( "//" + medianchainbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String chainfullcallstacks: medianchainbugpool) {
    		//System.out.println( chainfullcallstacks );
    		mcWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nsimplechainbugpool(whole chain's lastcallstacks) - " + "has " + simplechainbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	scWriter.writeLine( "//" + simplechainbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String chainfullcallstacks: simplechainbugpool) {
    		System.out.println( chainfullcallstacks );
    		scWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nmedianbugpool(loop's fullcallstack) - " + "has " + medianbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	mWriter.writeLine( "//" + medianbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String fullcallstack: medianbugpool) {
    		//System.out.println( fullcallstack );
    		mWriter.writeLine( fullcallstack );
    	}
    	
    	System.out.println("\nsimplebugpool(loop's lastcallstack) - " + "has " + simplebugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")");
    	sWriter.writeLine( "//" + simplebugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String lastcallstack: simplebugpool) {
    		System.out.println( lastcallstack );
    		sWriter.writeLine( lastcallstack );
    	}
    	
    	mcWriter.close();
    	scWriter.close();
    	mWriter.close();
    	sWriter.close();
    }
    

	
}
