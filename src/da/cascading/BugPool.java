package da.cascading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    String staticbugpoolFilename = "output/staticbugpool.txt";
    Set<String> simplebugpool = new TreeSet<String>();
	Set<String> medianbugpool = new TreeSet<String>();
	Set<String> simplechainbugpool = new TreeSet<String>();
	Set<String> medianchainbugpool = new TreeSet<String>();
	Set<String> staticbugpool = new HashSet<String>();
	
	
	public BugPool(String projectDir, HappensBeforeGraph hbg) {
		this.projectDir = projectDir;
		this.hbg = hbg;
	}
	
	
	// newly add
	public void addLoopBug( LoopBug loopbug ) {
		bugs.add( loopbug );
		mergeToResults( loopbug );
	}
	
	// for level = 1 for Queue 
    public void addLoopBug( int nodeIndex ) {
    	// add to bug pool
    	LoopBug loopbug = new LoopBug( nodeIndex, 1 );
    	loopbug.cascadingChain.add( nodeIndex );
    	bugs.add( loopbug );
    	mergeToResults( loopbug );
    }
	
	
    public int getBugInstanceNumber() {
    	return bugs.size();
    }
    
    public int getBugStaticNumber() {
    	return staticbugpool.size();
    }
    
    
    
    public void mergeToResults(LoopBug loopbug) {
		int nodeIndex = loopbug.nodeIndex;
		int cascadingLevel = loopbug.cascadingLevel;
		bugnodeset.add( nodeIndex );
		medianchainbugpool.add( "CL" + cascadingLevel + ": " + fullCallstacksOfCascadingChain(loopbug) );
		simplechainbugpool.add( "CL" + cascadingLevel + ": " + lastCallstacksOfCascadingChain(loopbug) );
		medianbugpool.add( "CL" + cascadingLevel + ": " + hbg.fullCallstack(nodeIndex) );
		simplebugpool.add( "CL" + cascadingLevel + ": " + hbg.lastCallstack(nodeIndex) );
		staticbugpool.add( "CL0: " + hbg.lastCallstack(nodeIndex) );
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
    
    
    /*
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
    		staticbugpool.add( hbg.lastCallstack(nodeIndex) );
    	}
    	System.out.println(", ie, representing " + bugnodeset.size() + "(real) loop nodes out of total " + hbg.getNodeList().size() + " happens-before nodes");
    }
    */
    
    public void printJobIdentity() {
    	JobTagger jobTagger = new JobTagger( this.hbg );
    	for (int index: bugnodeset) {
    		jobTagger.findJobID(index);
    		System.out.println("\n");
    	}
    }
    
    
    
    public void clearOutput() {
    	Path medianchainbugpoolFile = Paths.get(projectDir, packageDir, medianchainbugpoolFilename);
    	Path simplechainbugpoolFile = Paths.get(projectDir, packageDir, simplechainbugpoolFilename);
    	Path medianbugpoolFile = Paths.get(projectDir, packageDir, medianbugpoolFilename);
    	Path simplebugpoolFile = Paths.get(projectDir, packageDir, simplebugpoolFilename);
    	Path staticbugpoolFile = Paths.get(projectDir, packageDir, staticbugpoolFilename);
    	try {
			Files.deleteIfExists( medianchainbugpoolFile );
	    	Files.deleteIfExists( simplechainbugpoolFile );
	    	Files.deleteIfExists( medianbugpoolFile );
	    	Files.deleteIfExists( simplebugpoolFile );
	    	Files.deleteIfExists( staticbugpoolFile );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    
    public void printResults() {
    	printResults(false);
    }
    
    public void printResults(boolean appendToFile) {
    	System.out.println("\nJX - INFO - Results of bug pool");
    	
    	System.out.println("JX - INFO - #bugnodes = " + bugnodeset.size() + " (real) loop nodes out of total " + hbg.getNodeList().size() + " happens-before nodes");
    	//mergeResults();
    	//printJobIdentity();

    	// bug pools - 
        // write to file & print
    	Path medianchainbugpoolFile = Paths.get(projectDir, packageDir, medianchainbugpoolFilename);
    	Path simplechainbugpoolFile = Paths.get(projectDir, packageDir, simplechainbugpoolFilename);
    	Path medianbugpoolFile = Paths.get(projectDir, packageDir, medianbugpoolFilename);
    	Path simplebugpoolFile = Paths.get(projectDir, packageDir, simplebugpoolFilename);
    	Path staticbugpoolFile = Paths.get(projectDir, packageDir, staticbugpoolFilename);
    	System.out.println( "\nwrite to files - " );
    	System.out.println( "\t" + medianchainbugpoolFile );
    	System.out.println( "\t" + simplechainbugpoolFile );
    	System.out.println( "\t" + medianbugpoolFile );
    	System.out.println( "\t" + simplebugpoolFile );
    	System.out.println( "\t" + staticbugpoolFile );
		
    	TextFileWriter mcWriter = new TextFileWriter( medianchainbugpoolFile, appendToFile );
    	TextFileWriter scWriter = new TextFileWriter( simplechainbugpoolFile, appendToFile );
    	TextFileWriter mWriter = new TextFileWriter( medianbugpoolFile, appendToFile );
    	TextFileWriter sWriter = new TextFileWriter( simplebugpoolFile, appendToFile );
    	TextFileWriter staticWriter = new TextFileWriter( staticbugpoolFile, appendToFile );
    	
    	if (appendToFile) {
    		mcWriter.writeLine("\n\n\n//New Sink's Bugs\n");
    		scWriter.writeLine("\n\n\n//New Sink's Bugs\n");
    		mWriter.writeLine("\n\n\n//New Sink's Bugs\n");
    		sWriter.writeLine("\n\n\n//New Sink's Bugs\n");
    		staticWriter.writeLine("\n\n\n//New Sink's Bugs\n");
    	}
    	
    	System.out.println("\nmedianchainbugpool(whole chain's fullcallstacks) - " + "has " + medianchainbugpool.size() + " loops (#static codepoints=" + staticbugpool.size() + ")" );
    	mcWriter.writeLine( "//" + medianchainbugpool.size() + " (#static codepoints=" + staticbugpool.size() + ")" );
    	for (String chainfullcallstacks: medianchainbugpool) {
    		//System.out.println( chainfullcallstacks );
    		mcWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nsimplechainbugpool(whole chain's lastcallstacks) - " + "has " + simplechainbugpool.size() + " loops (#static codepoints=" + staticbugpool.size() + ")" );
    	scWriter.writeLine( "//" + simplechainbugpool.size() + " (#static codepoints=" + staticbugpool.size() + ")" );
    	for (String chainfullcallstacks: simplechainbugpool) {
    		System.out.println( chainfullcallstacks );
    		scWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nmedianbugpool(loop's fullcallstack) - " + "has " + medianbugpool.size() + " loops (#static codepoints=" + staticbugpool.size() + ")" );
    	mWriter.writeLine( "//" + medianbugpool.size() + " (#static codepoints=" + staticbugpool.size() + ")" );
    	for (String fullcallstack: medianbugpool) {
    		//System.out.println( fullcallstack );
    		mWriter.writeLine( fullcallstack );
    	}
    	
    	System.out.println("\nsimplebugpool(loop's lastcallstack) - " + "has " + simplebugpool.size() + " loops (#static codepoints=" + staticbugpool.size() + ")");
    	sWriter.writeLine( "//" + simplebugpool.size() + " (#static codepoints=" + staticbugpool.size() + ")" );
    	for (String lastcallstack: simplebugpool) {
    		System.out.println( lastcallstack );
    		sWriter.writeLine( lastcallstack );
    	}
    	
    	mcWriter.close();
    	scWriter.close();
    	mWriter.close();
    	sWriter.close();
    	
    	System.out.println("\nstaticbugpool(loop's lastcallstack)/#static codepoints = " + staticbugpool.size() + " loops");
    	staticWriter.writeLine( "//#static codepoints = " + staticbugpool.size() );
    	for (String lastcallstack: staticbugpool) {
    		//System.out.println( lastcallstack );
    		staticWriter.writeLine( lastcallstack );
    	}
    	
    	staticWriter.close();
    }
    

	
}
