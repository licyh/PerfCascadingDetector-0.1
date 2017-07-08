package sa.loop;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import sa.lock.LockAnalyzer;
import sa.lockloop.CGNodeInfo;
import sa.lockloop.CGNodeList;
import sa.lockloop.InstructionInfo;
import sa.wala.CGNodeUtil;
import sa.wala.WalaAnalyzer;

public class NestedLoopAnalyzer {
	// wala
	WalaAnalyzer walaAnalyzer;
	CallGraph cg;
	Path outputDir;
	// loop
	LoopAnalyzer loopAnalyzer;
	// results
	CGNodeList cgNodeList = null;	
	
	
	
	public NestedLoopAnalyzer(WalaAnalyzer walaAnalyzer, LoopAnalyzer loopAnalyzer, CGNodeList cgNodeList) {
		this.walaAnalyzer = walaAnalyzer;
		this.cg = this.walaAnalyzer.getCallGraph();
		this.outputDir = this.walaAnalyzer.getTargetDirPath();
		this.loopAnalyzer = loopAnalyzer;
		this.cgNodeList = cgNodeList;
	}
	
	// Please call doWork() manually
	public void doWork() {
		System.out.println("\nJX - INFO - NestedLoopAnalyzer: doWork...");
		findNestedLoopsInLoops();
		printResultStatus();
	}
	
	
	
	/*********************************************************
	 * New added - JX - just find nested loops                 
	 ********************************************************/
	public void findNestedLoopsInLoops() {
		System.out.println("\nJX-findNestedLoops");
  
		// Initialize nested loop information by DFS for all looping functions
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes()) {
			dfsToGetFunctionInfos(cgNodeInfo.getCGNode(), 0);
		}
  
		// deal with outermost loops 
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes()) {
			findNested(cgNodeInfo.getCGNode());
		}
	}
	  
	
	
	
	/**
	 * copy from "LoopinglockAnalyzer.dfsToGetFunctionInfos"
	 */
	int MAXN_DEPTH = 100;   //default: 1000    for ha-1.0.0, using 100 get the same results
	  
	public void dfsToGetFunctionInfos(CGNode cgNode, int depth) {
    
		//CGNodeInfo cgNodeInfo = new CGNodeInfo(cgNode);
		CGNodeInfo cgNodeInfo = cgNodeList.forceGet(cgNode);
		if ( cgNodeInfo.doneNestedLoopComputation() ) return ;
		cgNodeInfo.setDoneNestedLoopComputation(true);
		
		cgNodeInfo.max_depthOfLoops = 0;
		int id = cgNode.getGraphNodeId();
		
		if (depth > MAXN_DEPTH)
			return ;
    
		if ( !walaAnalyzer.isInPackageScope(cgNode) )
			return ;
    
		if (LockAnalyzer.locktypes.contains(CGNodeUtil.getMethodShortName(cgNode)) || LockAnalyzer.unlocktypes.contains(CGNodeUtil.getMethodShortName(cgNode)))
			return ;
    
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();
		SSAInstruction[] instructions = ir.getInstructions();
		//List<LoopInfo> loops = loopAnalyzer.getResults().get(id);
		List<LoopInfo> loops = cgNodeList.get(id).getLoops();
		
		for (int i = 0; i < instructions.length; i++) {
			SSAInstruction ssa = instructions[i];
			if (ssa == null) continue;
			
			int bb = cfg.getBlockForInstruction(i).getNumber();
			InstructionInfo instruction = new InstructionInfo();
			
			// Current function level
			if (loops != null) {
				instruction.numOfSurroundingLoops_in_current_function = 0;
				for (int j = 0; j < loops.size(); j++)
					if (loops.get(j).getBasicBlockNumbers().contains(bb)) {
						instruction.numOfSurroundingLoops_in_current_function ++;
						instruction.surroundingLoops_in_current_function.add(j);
					}
				//test
				//if (instruction.numOfLoops_in_current_function > 0) System.err.println(instruction.numOfLoops_in_current_function);
			}
			else {
				instruction.numOfSurroundingLoops_in_current_function = 0;
			}
			
			// Go into calls
			if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
				java.util.Set<CGNode> set = cg.getPossibleTargets(cgNode, ((SSAInvokeInstruction) ssa).getCallSite());
				//if (set.size() > 1) System.err.println("CallGraph#getPossibleTargets's size > 1"); // for Test, how to solve the problem??
				if (set.size() > 0) {         //JX: because I haven't yet added "hadoop-common"
					CGNode n = set.iterator().next(); 
					dfsToGetFunctionInfos(n, depth+1); //layer+1?
					/*
					//if (!functions.containsKey(n.getGraphNodeId())) {
					if ( ! cgNodeList.forceGet( n.getGraphNodeId() ).doneNestedLoopComputation() ) {
						//function.max_depthOfLoops = 1;  //how many???????
						//functions.put(n.getGraphNodeId(), function);
						dfsToGetFunctionInfos(n, depth+1); //layer+1?
					} else {  //especial case: recursive function.    //TODO - maybe something wrong
						
              			//if (id == n.getGraphNodeId()) {
                		//function.max_depthOfLoops = 15;
                		//functions.put(id, function);
                		//System.err.println("asdafasfd!!!");
                	
              			}
					}
					*/
					if ( cgNode.equals(n) )
						instruction.maxdepthOfLoops_in_call = Math.max( cgNodeList.get(n.getGraphNodeId()).max_depthOfLoops, 100 );
					else 
						instruction.maxdepthOfLoops_in_call = cgNodeList.get(n.getGraphNodeId()).max_depthOfLoops;
					instruction.call = n.getGraphNodeId();
				} else {                     //if we can't find the called CGNode.
					//TODO
					instruction.maxdepthOfLoops_in_call = 0;
				}  
			} else {
				//TODO
				instruction.maxdepthOfLoops_in_call = 0;
			}
			// Put into CGNodeInfo.Map<Integer, InstructionInfo>
			cgNodeInfo.instructions.put(i, instruction);
		}
    
		// find the instruction with maximal loops && save the function path
		InstructionInfo max_instruction = null;
		for (Iterator<Integer> it = cgNodeInfo.instructions.keySet().iterator(); it.hasNext(); ) {
			int index = it.next();
			InstructionInfo instruction = cgNodeInfo.instructions.get(index);
			if (instruction.numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call > cgNodeInfo.max_depthOfLoops) {
				max_instruction = instruction;
				cgNodeInfo.max_depthOfLoops = instruction.numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call;
			}
		}
		if (max_instruction != null && max_instruction.call >= 0) {
			cgNodeInfo.function_chain_for_max_depthOfLoops.addAll(cgNodeList.get(max_instruction.call).function_chain_for_max_depthOfLoops);
			cgNodeInfo.hasLoops_in_current_function_for_max_depthOfLoops.addAll(cgNodeList.get(max_instruction.call).hasLoops_in_current_function_for_max_depthOfLoops);
		}
		cgNodeInfo.function_chain_for_max_depthOfLoops.add(id);
		if (max_instruction != null && max_instruction.numOfSurroundingLoops_in_current_function > 0)
			cgNodeInfo.hasLoops_in_current_function_for_max_depthOfLoops.add(max_instruction.numOfSurroundingLoops_in_current_function);
		else
			cgNodeInfo.hasLoops_in_current_function_for_max_depthOfLoops.add(0);
    
	}
	
	
	  
	public void findNested(CGNode cgNode) {
		  
	    int id = cgNode.getGraphNodeId();
	    IR ir = cgNode.getIR();
	    SSACFG cfg = ir.getControlFlowGraph();
	    List<LoopInfo> loops = cgNodeList.get(id).getLoops();
	    
	    CGNodeInfo function = cgNodeList.get(id); 
	      
	    //System.out.print("function " + id + ": ");
	    for (int i = 0; i < loops.size(); i++) {
	    	LoopInfo loop = loops.get(i);
	    	// tmp vars
	    	InstructionInfo max_instruction = null;
	    	int max_depthOfLoops_in_current_function = 0;
	    	// end-tmp
	    	for (Iterator<Integer> it = loop.getBasicBlockNumbers().iterator(); it.hasNext(); ) {
	    		int bbnum = it.next();
	    		int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
	    		int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
	    		for (int index = first_index; index <= last_index; index++) {
	    			InstructionInfo instruction = function.instructions.get(index);
	    			if (instruction == null)
	    				continue;
	    			// Re-compute the numOfLoops in current/first-level function
	    			int numOfSurroundingLoops_in_current_function = 0;
	    			if (instruction.numOfSurroundingLoops_in_current_function > 0) {
	    				for (int j = 0; j < instruction.surroundingLoops_in_current_function.size(); j ++) {
	    					LoopInfo loop2 = loops.get( instruction.surroundingLoops_in_current_function.get(j) );
	    					if (loop.getBasicBlockNumbers().containsAll(loop2.getBasicBlockNumbers()))
	    						numOfSurroundingLoops_in_current_function ++;
	    				}
	    			}
	    			// Then
	    			int depthOfLoops = numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call;
	    			//if (instruction.numOfLoops_in_call >= 7 && instruction.numOfLoops_in_call <= 15) System.err.println("!!:" + instruction.numOfLoops_in_call);
	    			if (depthOfLoops <= 0)
	    				continue;
          
	    			if (depthOfLoops > loop.max_depthOfLoops) {
	    				loop.max_depthOfLoops = depthOfLoops;
	    				max_instruction = instruction;
	    				max_depthOfLoops_in_current_function = numOfSurroundingLoops_in_current_function;
	    			}
	    		}
	    	} //for-it
	    	//save others, ie function path, for the Loop             haven't yet verified
	    	if (max_instruction != null && max_instruction.call >= 0) {
	    		loop.function_chain_for_max_depthOfLoops.addAll(cgNodeList.get(max_instruction.call).function_chain_for_max_depthOfLoops);
	    		loop.hasLoops_in_current_function_for_max_depthOfLoops.addAll(cgNodeList.get(max_instruction.call).hasLoops_in_current_function_for_max_depthOfLoops);
	    	}
	    	loop.function_chain_for_max_depthOfLoops.add(id);
	    	loop.hasLoops_in_current_function_for_max_depthOfLoops.add(max_depthOfLoops_in_current_function);
      
     
	    	// For Test
	    	if (cgNode.getMethod().getSignature().indexOf("BlockManager.processReport(") >=0 || loop.max_depthOfLoops == 15 && loop.max_depthOfLoops < 15) {
	    		System.err.println(loop.max_depthOfLoops + " : " + cgNode.getMethod().getSignature() );
	    		// print the function chain
	    		for (int k = loop.function_chain_for_max_depthOfLoops.size()-1; k >= 0; k--)
	    			System.err.print(cg.getNode( loop.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getName() + "#" + loop.hasLoops_in_current_function_for_max_depthOfLoops.get(k) + "#" + "->");
	    		System.err.println("End");
	    	}
	    }//for-outermost
	}
  
  
	public void printResultStatus() {
		// Print the status
		int N_NestedLOOPS = 20;
		int[] count = new int[N_NestedLOOPS]; int othercount = 0;
		
		for (CGNodeInfo cgNodeInfo: loopAnalyzer.getLoopCGNodes()) {
			for (LoopInfo loop: cgNodeInfo.getLoops()) {
				int depthOfLoops = loop.max_depthOfLoops;
				if (depthOfLoops < N_NestedLOOPS) count[depthOfLoops]++;
				else othercount = 0;
			}
		}
		System.out.println("The Status of Loops in All Functions:\n" 
				+ "#scanned functions: " + walaAnalyzer.getNPackageFuncs() 
				+ " out of #Total:" + walaAnalyzer.getNTotalFuncs() + "(#AppFuncs:" + walaAnalyzer.getNApplicationFuncs() + "+#PremFuncs:" + walaAnalyzer.getNPremordialFuncs() +")");    
		System.out.println("#loops: see LoopAnalyzer"+ " (#functions with loops: see LoopAnalyzer" + ")");
		System.out.println("//distribution of #nestedloops");
		for (int i = 0; i < N_NestedLOOPS; i++)
			System.out.print("#" + i + ":" + count[i] + ", ");
		System.out.println("#>=" + N_NestedLOOPS + ":" + othercount);
		System.out.println("jx - cgNodeList.size() = " + cgNodeList.size() );
		System.out.println();
	}
	
	
}
