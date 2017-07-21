package sa.lock;

import java.nio.file.Path;
import java.util.BitSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;

import sa.lockloop.CGNodeInfo;
import sa.lockloop.CGNodeList;
import sa.loop.TCOpUtil;
import sa.loop.TcOperationInfo;
import sa.loop.LoopAnalyzer;
import sa.wala.IRUtil;
import sa.wala.WalaAnalyzer;

public class RPCLockAnalyzer {

	// wala
	WalaAnalyzer walaAnalyzer;
	CallGraph cg;
	Path outputDir;
	// database
	CGNodeList cgNodeList = null;
	// lock & loop
	LockAnalyzer lockAnalyzer;
	TCOpUtil iolooputil;
	
	
	
	public static void main(String[] args) {
		WalaAnalyzer walaAnalyzer = new WalaAnalyzer("src/sa/res/mr-4813");
		CGNodeList cgNodeList = new CGNodeList(walaAnalyzer.getCallGraph());
		LockAnalyzer lockAnalyzer = new LockAnalyzer(walaAnalyzer, cgNodeList);
		lockAnalyzer.doWork();
		
		new RPCLockAnalyzer(walaAnalyzer, lockAnalyzer, cgNodeList).doWork();
	}
	
	public RPCLockAnalyzer(WalaAnalyzer walaAnalyzer, LockAnalyzer lockAnalyzer, CGNodeList cgNodeList) {
		this.walaAnalyzer = walaAnalyzer;
		this.cg = this.walaAnalyzer.getCallGraph();
		this.outputDir = this.walaAnalyzer.getTargetDirPath();
		this.lockAnalyzer = lockAnalyzer;
		this.cgNodeList = cgNodeList;
		//others
		this.iolooputil = new TCOpUtil( this.walaAnalyzer.getTargetDirPath() );
	}
	
	// Please call doWork() manually
	public void doWork() {
		System.out.println("\nJX - INFO - LoopingLockAnalyzer: doWork...");
		findRPCsForAllLocks();
		printResultStatus();
		
	}
	
	
	public void findRPCsForAllLocks() {
		
		for (CGNodeInfo cgNodeInfo: lockAnalyzer.getLockCGNodes())
			for (LockInfo lock: cgNodeInfo.getLocks()) {
				findRPCsForLock(lock, cgNodeInfo.getCGNode());
			}
		
	}
	
	
	public void findRPCsForLock(LockInfo lock, CGNode cgNode) {
		System.out.println("JX - DEBUG - RPCs in lock body: " + lock );
		int id = cgNode.getGraphNodeId();
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();
		SSAInstruction[] instructions = ir.getInstructions();
   
		BitSet traversednodes = new BitSet();
		traversednodes.clear();
		traversednodes.set( id );
		
		for (int bbnum: lock.getBasicBlockNumbers()) {
			int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
			int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
			for (int i = first_index; i <= last_index; i++) {
				SSAInstruction ssa = instructions[i];
				if (ssa == null) continue;
				
				if (iolooputil.isRPCSSA(ssa))
					System.out.println("JX - DEBUG - ssa: " + ssa);
				
				if (ssa instanceof SSAInvokeInstruction) {  
					SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
					java.util.Set<CGNode> set = cg.getPossibleTargets(cgNode, invokessa.getCallSite());
					for (CGNode node: set) {
						dfsToGetRPCsForCGNode(node, traversednodes);
					}
				}
			}
		} //for-bbnum
	}
	
	
	public void dfsToGetRPCsForCGNode(CGNode cgNode, BitSet traversednodes) {
		
		if ( !walaAnalyzer.isInPackageScope(cgNode) ) return ;

		int id = cgNode.getGraphNodeId();
		if ( traversednodes.get(id) ) return ;
		traversednodes.set(id);
		
		IR ir = cgNode.getIR();  //if (ir == null) return;
		SSAInstruction[] instructions = ir.getInstructions();
    
		for (int i = 0; i < instructions.length; i++) {
			SSAInstruction ssa = instructions[i];
			if (ssa == null) continue;
			
			if (iolooputil.isRPCSSA(ssa))
				System.out.println("JX - DEBUG - ssa: " + ssa);
			
			if (ssa instanceof SSAInvokeInstruction) {  
				SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
				java.util.Set<CGNode> set = cg.getPossibleTargets(cgNode, invokessa.getCallSite());
				for (CGNode cgnode: set) {
					dfsToGetRPCsForCGNode(cgnode, traversednodes);
				}
			}
		}//for
		
	}
	
	
	public void printResultStatus() {
		
	}
	
}
