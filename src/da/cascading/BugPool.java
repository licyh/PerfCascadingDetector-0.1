package da.cascading;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.text.TextFileWriter;

import da.graph.HappensBeforeGraph;

public class BugPool {
	String projectDir;
	String packageDir = "src/da";
	
    Set<LoopBug> bugs = new HashSet<LoopBug>();          //dynamic loop instances, only one bug pool for whole code snippets
    Set<Integer> bugnodeset = new HashSet<Integer>();       //HappensBeforeGraph's node index set for bugs 
    
    int CASCADING_LEVEL = 10;                               //minimum:2; default:3;
    @SuppressWarnings("unchecked")
	HashMap<Integer, Integer>[] predNodes = new HashMap[ CASCADING_LEVEL + 1 ];  //record cascading paths, for different threads
    @SuppressWarnings("unchecked")
	HashMap<Integer, Integer>[] upNodes   = new HashMap[ CASCADING_LEVEL + 1 ];  //record cascading paths, for the same thread

    
    
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
	
	
	public BugPool(String projectDir, HappensBeforeGraph hbg) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		
        for (int i = 1; i <= CASCADING_LEVEL; i++) {
        	this.predNodes[i] = new HashMap<Integer, Integer>();
        	this.upNodes[i]   = new HashMap<Integer, Integer>();
        }
	}
	
	
    public void addLoopBug( int nodeIndex, int cascadingLevel ) {
    	// add to bug pool
    	LoopBug loopbug = new LoopBug( nodeIndex, cascadingLevel );
    	bugs.add( loopbug );	
    	// get cascading lock chain
    	if ( cascadingLevel == 1 ) { // Immediate loop bug
    		loopbug.cascadingChain.add( nodeIndex );
    	}
    	else if ( cascadingLevel >= 2 ) { // Lock-related loop bug
        	loopbug.cascadingChain.add( nodeIndex );
        	int tmp = nodeIndex;
    		for (int i=cascadingLevel; i>=2; i--) {
    			tmp = upNodes[i].get(tmp);
    			loopbug.cascadingChain.add( tmp );
    			tmp = predNodes[i].get(tmp);
    			loopbug.cascadingChain.add( tmp );
    		}
    	}
    	//jx: had better commented this when #targetcode is large or #loopbug is large
    	//System.out.println( loopbug );
    }
    
	
    public void printResultsOfTraverseTargetCodes() throws IOException {
    	System.out.println("\nJX - Results of traverseTargetCodes");
    	
    	// real bug pool
    	System.out.print("\nbugpool - " + "has " + bugs.size() + " dynamic loop instances");
    	Set<String> tmpset = new HashSet<String>();
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
    	System.out.println(", ie, representing " + bugnodeset.size() + " nodes out of total " + hbg.getNodeList().size() + " nodes");

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
		
    	TextFileWriter mcWriter = new TextFileWriter( medianchainbugpoolFilename );
    	TextFileWriter scWriter = new TextFileWriter( simplechainbugpoolFilename );
    	TextFileWriter mWriter = new TextFileWriter( medianbugpoolFilename );
    	TextFileWriter sWriter = new TextFileWriter( simplebugpoolFilename );

    	System.out.println("\nmedianchainbugpool(whole chain's fullcallstacks) - " + "has " + medianchainbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	mcWriter.writeLine( medianchainbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String chainfullcallstacks: medianchainbugpool) {
    		//System.out.println( chainfullcallstacks );
    		mcWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nsimplechainbugpool(whole chain's lastcallstacks) - " + "has " + simplechainbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	scWriter.writeLine( simplechainbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String chainfullcallstacks: simplechainbugpool) {
    		System.out.println( chainfullcallstacks );
    		scWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nmedianbugpool(loop's fullcallstack) - " + "has " + medianbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	mWriter.writeLine( medianbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String fullcallstack: medianbugpool) {
    		//System.out.println( fullcallstack );
    		mWriter.writeLine( fullcallstack );
    	}
    	
    	System.out.println("\nsimplebugpool(loop's lastcallstack) - " + "has " + simplebugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")");
    	sWriter.writeLine( simplebugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String lastcallstack: simplebugpool) {
    		System.out.println( lastcallstack );
    		sWriter.writeLine( lastcallstack );
    	}
    	
    	mcWriter.close();
    	scWriter.close();
    	mWriter.close();
    	sWriter.close();
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
	
}
