package sa.lock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import sa.lockloop.CGNodeInfo;
import sa.lockloop.CGNodeList;
import sa.lockloop.InstructionInfo;
import sa.loop.LoopAnalyzer;
import sa.loop.LoopInfo;
import sa.wala.CGNodeUtil;
import sa.wala.WalaAnalyzer;

public class LoopingLockAnalyzer {
	
	// wala
	WalaAnalyzer walaAnalyzer;
	CallGraph cg;
	Path outputDir;
	// database
	CGNodeList cgNodeList = null;
	// lock & loop
	LockAnalyzer lockAnalyzer;
	LoopAnalyzer loopAnalyzer;
	
	// results
	int nLoopingLocks = 0;
	int nLoopingLockingCGNodes = 0;
	
	
	// Statistics for "analyzeLoopingLocks()"
	int nHeavyLocks = 0;             // The number of time-consuming looping locks
	int nHeavyLockGroups = 0;
	/*
	int nHeartbeatLocks = 0;
	int nHeartbeatLockGroups = 0;
	int nSuspectedHeavyLocks = 0; 
	*/
	
	//tmp - for test
	String functionname_for_test = "method signature";
	
	
	
	
	public LoopingLockAnalyzer(WalaAnalyzer walaAnalyzer, LockAnalyzer lockAnalyzer, LoopAnalyzer loopAnalyzer, CGNodeList cgNodeList) {
		this.walaAnalyzer = walaAnalyzer;
		this.cg = this.walaAnalyzer.getCallGraph();
		this.outputDir = this.walaAnalyzer.getTargetDirPath();
		this.lockAnalyzer = lockAnalyzer;
		this.loopAnalyzer = loopAnalyzer;
		this.cgNodeList = cgNodeList;
	}
	
	// Please call doWork() manually
	public void doWork() {
		System.out.println("\nJX - INFO - LoopingLockAnalyzer: doWork...");
		findLoopsForAllLocks();
		printResultStatus();
		//analyzeLoopingLocks();
	}
	
	
	public int getNLoopingLocks() {
		return this.nLoopingLocks;
	}
	
	public int getNLoopingLockingCGNodes() {
		return this.nLoopingLockingCGNodes;
	}
	
	
	
	
	
	/*****************************************************************************************************
	 * Find inside loops for every Lock
	 *****************************************************************************************************/
	
	private void findLoopsForAllLocks() {
		System.out.println("JX - INFO - LoopingLockAnalyzer: findLoopsForAllLocks");
		
		// Initialization by DFS for lock-containing CGNodes
		for (CGNodeInfo cgNodeInfo: lockAnalyzer.getLockCGNodes()) {
			//System.err.println(cg.getNode(id).getMethod().getSignature());
			dfsToGetFunctionInfos( cgNodeInfo.getCGNode(), 0 );
		}
		
		// Find loops for every lock for lock-containing CGNodes. For safety, can't combine with the above, because this may modify value in CGNodeInfo for eventual usage.
		for (CGNodeInfo cgNodeInfo: lockAnalyzer.getLockCGNodes()) {
			findForLockingFunction( cgNodeInfo.getCGNode() );
		}
		
		// summary
		for (CGNodeInfo cgNodeInfo: lockAnalyzer.getLockCGNodes()) {
			if (cgNodeInfo.hasLoopingLocks) {
				nLoopingLockingCGNodes ++;
				for (Iterator<LoopingLockInfo> it_2 = cgNodeInfo.looping_locks.values().iterator(); it_2.hasNext(); ) {
					int num = it_2.next().max_depthOfLoops;
					if (num > 0) {
						nLoopingLocks ++;
					}
				}
			}
		}
	}
	  
	
	  
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
    
		//test - specified function's loop status
		if (cgNode.getMethod().getSignature().indexOf(functionname_for_test) >= 0) {
			System.err.println("aa " + cgNode.getMethod().getSignature());
			System.err.println("bb " + cgNodeInfo.max_depthOfLoops);
			System.err.println(cgNodeInfo.function_chain_for_max_depthOfLoops);
			System.err.println("cc " + cg.getNode(549).getMethod().getSignature());
			System.err.println("cc " + cg.getNode(280).getMethod().getSignature());
			// print the function chain
			for (int k = cgNodeInfo.function_chain_for_max_depthOfLoops.size()-1; k >= 0; k--)
				System.out.print(cg.getNode( cgNodeInfo.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getName() + "#" + cgNodeInfo.hasLoops_in_current_function_for_max_depthOfLoops.get(k) + "#" + "->");
			System.out.println("End");
		}
    
		//if (!functions.containsKey(id))
		//  functions.put(id, function);
		//else if (function.max_depthOfLoops > functions.get(id).max_depthOfLoops)
		//functions.put(id, cgNodeInfo);
	}
  
  
	public void findForLockingFunction(CGNode cgNode) {
    
		int id = cgNode.getGraphNodeId();
		IR ir = cgNode.getIR();
		SSACFG cfg = ir.getControlFlowGraph();
		List<LockInfo> locks = cgNodeList.get(id).getLocks();
		List<LoopInfo> loops = cgNodeList.get(id).getLoops();
    
		CGNodeInfo cgNodeInfo = cgNodeList.get(id); 
		cgNodeInfo.hasLoopingLocks = false;
    
		//System.out.print("function " + id + ": ");
		for (int i = 0; i < locks.size(); i++) {
			LockInfo lock = locks.get(i);
			LoopingLockInfo loopinglock = null;
			InstructionInfo max_instruction = null;
			int max_depthOfLoops_in_current_function = 0;
			for (Iterator<Integer> it = lock.bbs.iterator(); it.hasNext(); ) {
				int bbnum = it.next();
				int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
				int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
				for (int index = first_index; index <= last_index; index++) {
					InstructionInfo instruction = cgNodeInfo.instructions.get(index);
					if (instruction == null)
						continue;
					// Re-compute the numOfLoops in current/first-level function
					int numOfSurroundingLoops_in_current_function = 0;
					if (instruction.numOfSurroundingLoops_in_current_function > 0) {
						for (int j = 0; j < instruction.surroundingLoops_in_current_function.size(); j ++) {
							LoopInfo loop = loops.get( instruction.surroundingLoops_in_current_function.get(j) );
							if (lock.bbs.containsAll(loop.getBasicBlockNumbers()))
								numOfSurroundingLoops_in_current_function ++;
						}
					}
					// Then
					int depthOfLoops = numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call;
					//if (instruction.numOfLoops_in_call >= 7 && instruction.numOfLoops_in_call <= 15) System.err.println("!!:" + instruction.numOfLoops_in_call);
					if (depthOfLoops <= 0)
						continue;
					if (loopinglock == null) {
						loopinglock = new LoopingLockInfo();
						loopinglock.max_depthOfLoops = 0;
					}
					if (depthOfLoops > loopinglock.max_depthOfLoops) {
						loopinglock.max_depthOfLoops = depthOfLoops;
						max_instruction = instruction;
						max_depthOfLoops_in_current_function = numOfSurroundingLoops_in_current_function;
					}
				}
			}//for-it
			//save others, ie function path, for the loopinglock
			if (loopinglock != null) {
				if (max_instruction != null && max_instruction.call >= 0) {
					loopinglock.function_chain_for_max_depthOfLoops.addAll(cgNodeList.get(max_instruction.call).function_chain_for_max_depthOfLoops);
					loopinglock.hasLoops_in_current_function_for_max_depthOfLoops.addAll(cgNodeList.get(max_instruction.call).hasLoops_in_current_function_for_max_depthOfLoops);
				}
				loopinglock.function_chain_for_max_depthOfLoops.add(id);
				loopinglock.hasLoops_in_current_function_for_max_depthOfLoops.add(max_depthOfLoops_in_current_function);
				loopinglock.function = cgNode;  //for the future
				loopinglock.lock = lock;   //for the future
				cgNodeInfo.looping_locks.put(i, loopinglock);
			}
     
			// For Test
			//if (f.getMethod().getSignature().indexOf("FSNamesystem.processReport(") >=0)
			//  System.out.println("I do");
			if (loopinglock != null) {
				if (cgNode.getMethod().getSignature().indexOf("BlockManager.processReport(") >=0 || loopinglock.max_depthOfLoops == 15 && loopinglock.max_depthOfLoops < 15) {
					System.err.println(loopinglock.max_depthOfLoops + " : " + cgNode.getMethod().getSignature() + " : " + locks.get(i).lock_type);
					// print the function chain
					for (int k = loopinglock.function_chain_for_max_depthOfLoops.size()-1; k >= 0; k--)
						System.err.print(cg.getNode( loopinglock.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getName() + "#" + loopinglock.hasLoops_in_current_function_for_max_depthOfLoops.get(k) + "#" + "->");
					System.err.println("End");
				}
			}
      
		}//for-outermost
    
		if (cgNodeInfo.looping_locks.size() > 0)
			cgNodeInfo.hasLoopingLocks = true;
		//System.out.println(" PS - " + f.getMethod().getSignature());
    
	}
	  
	  
	  
    public void printResultStatus() {
		// Print the status    
		int MAXN_LOOPS_FOR_A_LOCK = 20;
		int[] count = new int[MAXN_LOOPS_FOR_A_LOCK]; int rest = 0;
		
		for (CGNodeInfo cgNodeInfo: lockAnalyzer.getLockCGNodes()) {
			if ( !cgNodeInfo.hasLoopingLocks ) continue;
			
			for (Iterator<LoopingLockInfo> it_2 = cgNodeInfo.looping_locks.values().iterator(); it_2.hasNext(); ) {
				int num = it_2.next().max_depthOfLoops;
				if (num > 0) {
					if (num < MAXN_LOOPS_FOR_A_LOCK) count[num] ++;
					else rest ++;
				}
			}
		}
      
		int nLocks = lockAnalyzer.getNLocks();
		int nLockingFuncs = lockAnalyzer.getNLockingCGNodes();
      
		count[0] = nLocks - nLoopingLocks;
		System.out.println("The Status of Critical Sections:");
		System.out.println("#functions that their critical sections involve loops: " + nLoopingLockingCGNodes + "(" + nLoopingLocks + "critical sections)" 
				+ " out of " + nLockingFuncs + "(" + nLocks + "critical sections)" + " functions with locks");
		System.out.println("//distribution of loop depth in " + nLocks + "(#>=1:" + nLoopingLocks + ")" + " critical sections");
		for (int i = 0; i < MAXN_LOOPS_FOR_A_LOCK; i++) {
			System.out.print("#" + i + ":" + count[i] + ", ");
		}
		System.out.println("#>=" + MAXN_LOOPS_FOR_A_LOCK + ":" + rest);
		// Print - distribution of loop depth in locking functions
		System.out.println("//PS: distribution of loop depth in " + nLockingFuncs + "(#>=1:" + nLoopingLockingCGNodes + ") locking functions");
		int MAXN_LOOPS_FOR_A_FUNCTION = 20;
		int[] count2 = new int[MAXN_LOOPS_FOR_A_FUNCTION];
		rest = 0;
		for (CGNodeInfo cgNodeInfo: lockAnalyzer.getLockCGNodes()) {
			// lock-containing cgNode
			int max_loops = 0;
			for (Iterator<LoopingLockInfo> it_2 = cgNodeInfo.looping_locks.values().iterator(); it_2.hasNext(); ) {
				int num = it_2.next().max_depthOfLoops;
				max_loops = num > max_loops ? num : max_loops;
			}
			if (max_loops < MAXN_LOOPS_FOR_A_FUNCTION)
				count2[max_loops] ++;
			else rest ++;
		}
		for (int i = 0; i < MAXN_LOOPS_FOR_A_FUNCTION; i++) {
			System.out.print("#" + i + ":" + count2[i] + ", ");
		}
		System.out.println("#>=" + MAXN_LOOPS_FOR_A_FUNCTION + ":" + rest);
		System.out.println("jx - functions.size() = " + cgNodeList.size()/*functions.size()*/ );
		System.out.println(); 
    }
	  
	  
	  
	    
    List<LoopingLockInfo> heavylocks = new ArrayList<LoopingLockInfo>();  // ie, Time-consuming Looping Locks
    Set<String> set_of_heavylocks = new TreeSet<String>();   
  
    public void analyzeLoopingLocks() {
    	System.out.println("\nJX - INFO - analyzeLoopingLocks");
 
    	int requiredDepth = 2;
		for (CGNodeInfo cgNodeInfo: lockAnalyzer.getLockCGNodes()) {
			if ( !cgNodeInfo.hasLoopingLocks ) continue;
    		for (Map.Entry<Integer, LoopingLockInfo> entry: cgNodeInfo.looping_locks.entrySet()) {
    			LoopingLockInfo loopinglock = entry.getValue();
    			if (loopinglock.max_depthOfLoops >= requiredDepth && loopinglock.max_depthOfLoops < 3) {
    				heavylocks.add( loopinglock );
    				set_of_heavylocks.add( loopinglock.lock.lock_identity );
    			}
    		}//for
    	}//for-outermost
    	nHeavyLocks = heavylocks.size();
    	nHeavyLockGroups = set_of_heavylocks.size();
    
    	// Print the status
    	System.out.println("#HeavyLocks (ie, time-consuming looping locks): " + nHeavyLocks + " out of " + nLoopingLocks + " looping locks" + " out of total " + lockAnalyzer.getNLocks()  + " locks");
    	System.out.println("#Groups of HeavyLocks (ie, real number): " + nHeavyLockGroups + " out of total " + lockAnalyzer.getNLockGroups() + " lock groups");
    	for (String str: set_of_heavylocks)
    		System.err.println( str );
    	System.out.println();
    }
}
