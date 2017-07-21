package sa.loop;

import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;

import sa.lock.LockAnalyzer;
import sa.lockloop.CGNodeInfo;
import sa.lockloop.CGNodeList;
import sa.wala.IRUtil;
import sa.wala.WalaAnalyzer;

public class TCLoopAnalyzer {

	// wala
	WalaAnalyzer walaAnalyzer;
	CallGraph cg;
	Path outputDir;
	// database
	CGNodeList cgNodeList = null;	
	// loop
	LoopAnalyzer loopAnalyzer;
	TCOpUtil iolooputil;
	
	// results
	int nTCLoops = 0;
	
	
	
	
	public TCLoopAnalyzer(WalaAnalyzer walaAnalyzer, LoopAnalyzer loopAnalyzer, CGNodeList cgNodeList) {
		this.walaAnalyzer = walaAnalyzer;
		this.cg = this.walaAnalyzer.getCallGraph();
		this.outputDir = this.walaAnalyzer.getTargetDirPath();
		this.loopAnalyzer = loopAnalyzer;
		this.cgNodeList = cgNodeList;
		//others
		this.iolooputil = new TCOpUtil( this.walaAnalyzer.getTargetDirPath() );
	}
	
	// Please call doWork() manually
	public void doWork() {
		System.out.println("\nJX - INFO - TCLoopAnalyzer: doWork...");
		
		findTCOperationsForAllLoops();
		printResultStatus();
		
		iolooputil.printTcOperationTypes();                //for test
	      
	}
	
	
	public int getNTCLoops() {
		return this.nTCLoops;
	}
	
	
	
	
	/**************************************************************************
	 * JX - find time-consuming operations in all loops    
	 **************************************************************************/
	
	public void findTCOperationsForAllLoops() {
		System.out.println("JX - INFO - findTCOperationsForAllLoops");
		// Initialize Time-consuming operation information by DFS for all looping functions
		BitSet traversednodes = new BitSet();
		traversednodes.clear();
		
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes()) {
			dfsToGetTCOperations(cgNodeInfo.getCGNode(), 0, traversednodes);
		}
		
		// Deal with the outermost loops
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes()) {
			for (LoopInfo loop: cgNodeInfo.getLoops())
				findTCOperationsForLoop( loop );  
		}
		
		// summary
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes())
			for (LoopInfo loop: cgNodeInfo.getLoops()) {
				if (loop.numOfTcOperations_recusively > 0) {
					nTCLoops ++;
					//System.out.println( loop );
				}
			}
	}
	  
	  
	public int dfsToGetTCOperations(CGNode cgNode, int depth, BitSet traversednodes) {
    
		// for test - the depth can reach 58
		/*
		if (depth > 50) {
			System.err.println("JX - WARN - depth > " + depth);
		}
		 */

		if ( !walaAnalyzer.isInPackageScope(cgNode) ) { 
			return 0;
		}

		int id = cgNode.getGraphNodeId();
		CGNodeInfo cgNodeInfo = cgNodeList.forceGet(id);
    
		if ( traversednodes.get( id ) )            // if has already been traversed, then return
			return cgNodeInfo.numOfTcOperations_recusively; //maybe 0(unfinished) or realValue(finished)
       
		traversednodes.set( id );                  // if hasn't been traversed
		cgNodeInfo.numOfTcOperations = 0;            // "0" RIGHT NOW to prevent this scenario: func1 -call-> func2 -> func1(NOW func1 should be forbidden)
		cgNodeInfo.numOfTcOperations_recusively = 0; // this value should keep 0 until go through the function, because it may be return in the process 

		IR ir = cgNode.getIR();  //if (ir == null) return;
		SSAInstruction[] instructions = ir.getInstructions();
		int numOfTcOperations = 0;              //tmp var
		int numOfTcOperations_recusively = 0;  
 
		for (int i = 0; i < instructions.length; i++) {
			SSAInstruction ssa = instructions[i];
			if (ssa == null) continue;

			if ( iolooputil.isTimeConsumingSSA(ssa) ) {
				cgNodeInfo.tcOperations.add( ssa );
				numOfTcOperations ++;
				numOfTcOperations_recusively ++;
				continue;
			}
			// filter the rest I/Os
			if ( iolooputil.isJavaIOSSA(ssa) )
				continue;
      
			// if meeting a normal call(NOT RPC and I/O), Go into the call targets
			if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
				SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;   
				// get all possible targets
				java.util.Set<CGNode> set = cg.getPossibleTargets(cgNode, invokessa.getCallSite());
				// traverse all possible targets
				for (CGNode cgnode: set) {
					numOfTcOperations_recusively += dfsToGetTCOperations(cgnode, depth+1, traversednodes);
				}
			}
			else {
				// TODO - if need be
			}      
		}//for	 
    
		cgNodeInfo.numOfTcOperations = numOfTcOperations;
		cgNodeInfo.numOfTcOperations_recusively = numOfTcOperations_recusively;
		return cgNodeInfo.numOfTcOperations_recusively;
	}
  
	  	  
	
	/**
	 * for single loop
	 */
	public void findTCOperationsForLoop(LoopInfo loop) {
	  
		CGNode cgNode = loop.getCGNode();
		int id = cgNode.getGraphNodeId();
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();
		SSAInstruction[] instructions = ir.getInstructions();
    
		CGNodeInfo cgNodeInfo = cgNodeList.get(id); 
      
		/* already done at LoopInfo initializtion
	  	loop.numOfTcOperations_recusively = 0;
	  	loop.tcOperations_recusively = new ArrayList<SSAInstruction>();
		 */
		BitSet traversednodes = new BitSet();
		traversednodes.clear();
		traversednodes.set( id );
	  
		//for debug
		String callpath = cgNode.getMethod().getSignature().substring(0, cgNode.getMethod().getSignature().indexOf('('));
	  
		for (int bbnum: loop.getBasicBlockNumbers()) {
			int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
			int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
			for (int i = first_index; i <= last_index; i++) {
				SSAInstruction ssa = instructions[i];
				if (ssa == null) continue;
				
				if ( cgNodeInfo.tcOperations.contains( ssa ) ) {
					loop.numOfTcOperations_recusively ++;
					loop.tcOperations_recusively.add( ssa );
					//added tc_info
					TcOperationInfo tcOperation = new TcOperationInfo();
					tcOperation.ssa = ssa;
					tcOperation.function = cgNode;
					tcOperation.callpath = callpath;
					tcOperation.line_number = IRUtil.getSourceLineNumberFromSSA(cgNode, ssa);
					loop.tcOperations_recusively_info.add( tcOperation );
					//end
					continue;
				}
				// filter the rest I/Os
				if ( iolooputil.isJavaIOSSA(ssa) )
					continue;
				if (ssa instanceof SSAInvokeInstruction) {  
					SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
					java.util.Set<CGNode> set = cg.getPossibleTargets(cgNode, invokessa.getCallSite());
					for (CGNode cgnode: set) {
						dfsToGetTCOperationsForSSA(cgnode, 0, traversednodes, loop,  callpath);
					}
				}
			}
		} //for-bbnum
	}
	  

	  
	public void dfsToGetTCOperationsForSSA(CGNode cgNode, int depth, BitSet traversednodes, LoopInfo loop, String callpath) {
		//jx: if want to add this, then for MapReduce we need to more hadoop-common&hdfs like "org.apache.hadoop.conf" not juse 'fs/security' for it
		/*
		if ( !walaAnalyzer.isInPackageScope(cgNode) )
			return ;
		*/ 
		 
		if ( !cgNode.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) 
				|| cgNode.getMethod().isNative()) { // IMPO - native - must be
			return ;
		}
    
		int id = cgNode.getGraphNodeId();
		CGNodeInfo cgNodeInfo = cgNodeList.get(id);
    
		if ( traversednodes.get(id) )
			return ;
    
		//test
		if (cgNodeInfo == null) 
			System.out.println("jx - error - function == null");
   
		traversednodes.set(id);
		loop.numOfTcOperations_recusively += cgNodeInfo.tcOperations.size();
		loop.tcOperations_recusively.addAll( cgNodeInfo.tcOperations );
    
		IR ir = cgNode.getIR();  //if (ir == null) return;
		SSAInstruction[] instructions = ir.getInstructions();
    
		//for debug
		String curCallpath = callpath + "-" + cgNode.getMethod().getSignature().substring(0, cgNode.getMethod().getSignature().indexOf('('));
    
		//added tc_info
		for (SSAInstruction ssa: cgNodeInfo.tcOperations) {
			TcOperationInfo tcOperation = new TcOperationInfo();
			tcOperation.ssa = ssa;
			tcOperation.function = cgNode;
			tcOperation.callpath = curCallpath;
			tcOperation.line_number = IRUtil.getSourceLineNumberFromSSA(cgNode, ssa);
			loop.tcOperations_recusively_info.add( tcOperation );
		}
		//end
    
		for (int i = 0; i < instructions.length; i++) {
			SSAInstruction ssa = instructions[i];
			if (ssa == null)
				continue;
			if ( cgNodeInfo.tcOperations.contains( ssa ) )
				continue;
			// filter the rest I/Os
			if ( iolooputil.isJavaIOSSA(ssa) )
				continue;
			if (ssa instanceof SSAInvokeInstruction) {  
				SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
				java.util.Set<CGNode> set = cg.getPossibleTargets(cgNode, invokessa.getCallSite());
				for (CGNode cgnode: set) {
					dfsToGetTCOperationsForSSA(cgnode, depth+1, traversednodes, loop, curCallpath);
				}
			}
		}//for
	}
	  
	
	  
    public void printResultStatus() {
    	System.out.println("JX - INFO - TCLoopAnalyzer: printResultStatus");

    	System.out.println("#TCLoops = " + nTCLoops + " out of total " + loopAnalyzer.getNLoops() + " loops");
    	
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes())
			for (LoopInfo loop: cgNodeInfo.getLoops()) {
				if (loop.numOfTcOperations_recusively > 0) {
					//System.out.println( loop );
				}
			}
    }
    
    
}
