package sa.loop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.text.TextFileWriter;

import sa.lockloop.CGNodeInfo;
import sa.lockloop.CGNodeList;
import sa.loop.LoopInfo;
import sa.wala.IRUtil;
import sa.wala.WalaAnalyzer;




public class LoopAnalyzer {

	// wala
	WalaAnalyzer walaAnalyzer;
	CallGraph cg;
	Path outputDir;	  
	// database
	CGNodeList cgNodeList = null;   // from outside
	
	// resulsts
	ArrayList<CGNodeInfo> loopCGNodes = new ArrayList<CGNodeInfo>();   //entry in CGNodeList
	int nLoops = 0;                 //the total number of loops
	int nLoopingCGNodes = 0;          //how many functions that contain loops
	
	


	public LoopAnalyzer(WalaAnalyzer walaAnalyzer, CGNodeList cgNodeList) {
		this.walaAnalyzer = walaAnalyzer;
		this.cg = this.walaAnalyzer.getCallGraph();
		this.outputDir = this.walaAnalyzer.getTargetDirPath();
		this.cgNodeList = cgNodeList;
	}
	
	/** ONLY used for independently calling 'findLoopsForCGNode(xx)' */
	public LoopAnalyzer(WalaAnalyzer walaAnalyzer) {
		this(walaAnalyzer, null);
	}
	
	// Please call doWork() manually
	public void doWork() {
		System.out.println("\nJX - INFO - LoopAnalyzer: doWork...");
		if (this.cgNodeList == null) {
			System.out.println("\nJX - ERROR - LoopAnalyzer: doWork - " + "this.cgNodeList == null");
			return;
		}
		
		try {
			findLoopsForAllCGNodes();
			printResultStatus();
			writeResultsToFile();   //JX - write to local files. NO necessary, can be commented
		} catch (IOException e) {
			e.printStackTrace();
		}    
	}
	
	
	public ArrayList<CGNodeInfo> getLoopCGNodes() { 
		return this.loopCGNodes;
	}
	
	
	public int getNLoops() {
		return this.nLoops;
	}
	
	public int getNLoopingCGNodes() {
		return this.nLoopingCGNodes;
	}

	public Path getOutputDir(Path outputDir) {
		return this.outputDir;
	}
	
	public void setOutputDir(String outputDirStr) {
		setOutputDir( Paths.get(outputDirStr) );
	}
	public void setOutputDir(Path outputDir) {
		this.outputDir = outputDir;
	}
	
	
	
	/****************************************************************************************************
	 * Find functions with loops
	 * Note: we just focus on "Application" functions, without "Primordial" functions
	 * @throws IOException 
	 **************************************************************************************************
	 */
	private void findLoopsForAllCGNodes() throws IOException {
	    System.out.println("JX - INFO - LoopAnalyzer: findLoopsForAllCGNodes");

	    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
	        CGNode f = it.next();
	        if ( !walaAnalyzer.isInPackageScope(f) ) continue;
	        
	        // Find loops for each function
	        List<LoopInfo> loops = findLoopsForCGNode(f);
	        if (loops.size() > 0) {
	        	int id = f.getGraphNodeId();
	        	cgNodeList.forceGet(id).setLoops(loops);
	        }
	    }
	    
		for (CGNodeInfo cgNodeInfo: cgNodeList.values() ) {
	    	if ( !cgNodeInfo.hasLoops() ) continue;
	    	loopCGNodes.add( cgNodeInfo );
	    	nLoops += cgNodeInfo.getLoops().size();
	    	nLoopingCGNodes ++;
		}
	}
	  
	  
	/**
	 * Find loops for a CGNode/Method/Function
	 */
	public List<LoopInfo> findLoopsForCGNode(CGNode cgNode) {
	    IR ir = cgNode.getIR();
	    SSACFG cfg = ir.getControlFlowGraph();
	    
	    List<LoopInfo> loops = new ArrayList<LoopInfo>();
	    int n_backedges = 0; //for Test
	    int num = -1; //for Test
	    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
		    ISSABasicBlock bb = it.next();
		    int bbnum = bb.getNumber();
		    if (bbnum != ++num) System.err.println("bbnum != ++num");  //for Test
		    for (IntIterator it_2 = cfg.getSuccNodeNumbers(bb).intIterator(); it_2.hasNext(); ) {
		        int succ = it_2.next();
		        if (succ < bbnum) {    //something like "catch" basic blocks have self-loops, so using "<". yes!!
		        	n_backedges ++;  //for Test
		        	//if (cfg.getSuccNodeCount(bb) != 1) System.err.println("for-exit basic block: cfg.getSuccNodeCount(bb) != 1" + "  how many:" + cfg.getSuccNodeCount(bb));  //for Test
		        	int existed = -1;
		        	for (int i = 0; i < loops.size(); i++)
		        		if (loops.get(i).getBeginBasicBlockNumber() == succ) {
		        			existed = i;
		        			break;
		        		}
		        	if (existed == -1) { //the for hasn't yet been recorded  
		        		int begin_bb = succ;
		        		int end_bb = bbnum;
		        		LoopInfo loop = new LoopInfo(cgNode, begin_bb, end_bb);   
		        		//loop.var_name = ???
		        		loops.add(loop);
		        	} else {            //the for has been recorded 
		        		LoopInfo loop = loops.get(existed);
		        		if (bbnum > loop.getEndBasicBlockNumber())
		        			loop.setEndBasicBlock( bbnum );  //is it right? yes for now
		        	}
		        }
		    } //for-it_2
	    } //for-it
	    
	    // for Test: #backedges by computeBackEdges - #self-loop = what I find by myself
	    IBinaryNaturalRelation backedges = Acyclic.computeBackEdges(cfg, cfg.entry());
	    int total = 0;
	    for (Iterator<IntPair> it = backedges.iterator(); it.hasNext(); ) {
	    	IntPair ip = it.next();
	    	if (ip.getX() != ip.getY()) total ++;
	    } 
	    if (total != n_backedges) {  //for Test
	    	System.err.println("total != n_backedges  #backedges:" + total + " #real backedges:" + n_backedges + " Method:" + cgNode.getMethod().getSignature());
	    }
	   
	    //printLoops(loops);
	    
	    return loops;
	}
	 
	
	public void printResultStatus() {
	    // Print the status
		System.out.println("JX - INFO - LoopAnalyzer: The status of results");
		int nPackageFuncs = walaAnalyzer.getNPackageFuncs();
		int nTotalFuncs = walaAnalyzer.getNTotalFuncs();
		int nApplicationFuncs = walaAnalyzer.getNApplicationFuncs();
		int nPremordialFuncs = walaAnalyzer.getNPremordialFuncs();
		int nOtherFuncs = walaAnalyzer.getNOtherFuncs();
	    
	    int N_LOOPS = 20;
	    int[] count = new int[N_LOOPS];
	    count[0] = nPackageFuncs - nLoopingCGNodes;
	    for (CGNodeInfo cgNodeInfo: cgNodeList.values()) {
	    	if ( !cgNodeInfo.hasLoops() ) continue;
	    	List<LoopInfo> loops = cgNodeInfo.getLoops();
	    	int size = loops.size();
	    	if (size < N_LOOPS) count[size]++;
	    }
	    System.out.println("The Status of Loops in All Functions:\n" 
	        + "#scanned functions: " + nPackageFuncs 
	        + " out of #Total:" + nTotalFuncs + "(#AppFuncs:" + nApplicationFuncs + "+#PremFuncs:" + nPremordialFuncs +")");    
	    System.out.println("#functions with loops: " + nLoopingCGNodes + " (#loops:" + nLoops + ")");
	    System.out.println("//distribution of #loops");
	    for (int i = 0; i < N_LOOPS; i++)
	      System.out.print("#" + i + ":" + count[i] + ", ");
	    
	    // Print all result loops for test
	    // Print all loops - for test
	  	/*
	  	bufwriter.write( nLoopingCGNodes + " " + nLoops + "\n" );
	    for (List<LoopInfo> loops: functions_with_loops.values()) {
	      //System.err.println(loops.get(0).function.getMethod().getSignature());
	      bufwriter.write( loops.get(0).function.getMethod().getSignature() + " " );
	      printLoops(loops, bufwriter);
	    }
	    */
	    // Print the function's loops
	    //System.err.print("#loops=" + loops.size() + " - ");
	    //for (LoopInfo loop: loops) {
		      // print normal	
		      //System.err.print(loop + ", ");
		      // print source line number
		      //System.err.print(loop.line_number + ", ");
	    //}
	    //System.err.println();
	}
	
	
	/**
	 * write the results of loop analysis into file
	 */
	public void writeResultsToFile() throws IOException {
		System.out.println("JX - INFO - LoopAnalyzer: writeResultsToFile..");
	  	
	  	Path loopfile = this.outputDir.resolve("looplocations");
	  	TextFileWriter writer = new TextFileWriter(loopfile);
	  	
	    // Print all the loops that are in the "package-scope.txt"
	    // ps: it will print ALL if without 'package-scope.txt'
	    int nfuncsInScope = 0;
	    int nloopsInScope = 0;
	    for (CGNodeInfo cgNodeInfo: cgNodeList.values()) {
	    	if ( !cgNodeInfo.hasLoops() ) continue;
	    	List<LoopInfo> loops = cgNodeInfo.getLoops();
	    	if ( !walaAnalyzer.isInPackageScope(loops.get(0).cgNode) ) continue;
	    	nfuncsInScope ++;
	    	nloopsInScope += loops.size();
    	}
	    writer.writeLine("//#functions containing loops In Scope     #loops totally");
	    writer.writeLine( nfuncsInScope + " " + nloopsInScope );
	    
	    for (CGNodeInfo cgNodeInfo: cgNodeList.values()) {
	    	if ( !cgNodeInfo.hasLoops() ) continue;
	    	List<LoopInfo> loops = cgNodeInfo.getLoops();
	    	if ( !walaAnalyzer.isInPackageScope(loops.get(0).cgNode) ) continue;
	    	
    		writer.write( loops.get(0).cgNode.getMethod().getSignature() + " " );
    		writer.write( loops.size() + " " );
    	    for (LoopInfo loop: loops)
    	    	writer.write( loop.line_number + " " );
    	    writer.write( "\n" );
    	}
	    writer.close();
	    
        System.out.println( "JX - INFO - Successfully write " + nLoops + " loops into " + loopfile.toString() );
	}
	
	  
	
}
