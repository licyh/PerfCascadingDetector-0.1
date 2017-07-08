package sa.lockloop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import sa.lock.LockInfo;
import sa.lock.LoopingLockInfo;
import sa.loop.LoopInfo;



public class CGNodeInfo {
	CGNode cgNode;
	  
	//Newest! just added
	List<LockInfo> locks = null;                 //Type - ArrayList<LoopInfo>
	List<LoopInfo> loops = null;                 //Type - ArrayList<LoopInfo>
	
	// loop-containing locks
	public boolean hasLoopingLocks;                      //only for first-level functions that have locks
	public Map<Integer, LoopingLockInfo> looping_locks;  //only for first-level functions that have locks, map: lock-id -> max_depthOfLoops
	  
	
	boolean doneNestedLoopComputation = false;
	public int max_depthOfLoops;
	public List<Integer> function_chain_for_max_depthOfLoops;
	public List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
	  
	public Map<Integer, InstructionInfo> instructions;
	
	
	
	// newly added
	public int numOfTcOperations;
	public int numOfTcOperations_recusively;
	public List<SSAInstruction> tcOperations;            // time-consuming operation locations
	public List<SSAInstruction> tcOperations_recusively; // won't be used&inited. if want to
		
	  
	  
	
	
	/*
	 * better not use CGNodeInfo(int cgNodeId)
	 */
	public CGNodeInfo(CGNode cgNode) {
		this.cgNode = cgNode;
		// others
		this.numOfTcOperations = 0;           
		this.numOfTcOperations_recusively = 0;
		this.tcOperations = new ArrayList<SSAInstruction>();
		this.tcOperations_recusively = new ArrayList<SSAInstruction>();       //just put here, won't be used
		  
	    this.hasLoopingLocks = false;                           //only for first-level functions that have locks
	    this.looping_locks = new TreeMap<Integer, LoopingLockInfo>(); //only for first-level functions that have locks
	    
	    this.max_depthOfLoops = 0;
	    this.function_chain_for_max_depthOfLoops = new ArrayList<Integer>();
	    this.hasLoops_in_current_function_for_max_depthOfLoops = new ArrayList<Integer>();
	    
	    this.instructions = new TreeMap<Integer, InstructionInfo>();
	}
	
	
	public CGNode getCGNode() {
		return this.cgNode;
	}
	

	
	
	public boolean hasLocks() {
		return this.locks != null && !this.locks.isEmpty();
	}
	
	public List<LockInfo> getLocks() {
		return this.locks;
	}
	
	public void setLocks(List<LockInfo> locks) {
		this.locks = locks;
	}
	
	
	public boolean hasLoops() {
		return this.loops != null && !this.loops.isEmpty();
	}
	
	public List<LoopInfo> getLoops() {
		return this.loops;
	}
	
	public void setLoops(List<LoopInfo> loops) {
		this.loops = loops;
	}
	
	
	public boolean doneNestedLoopComputation() {
		return this.doneNestedLoopComputation;
	}
	
	public void setDoneNestedLoopComputation(boolean bool) {
		this.doneNestedLoopComputation = bool;
	}
	
	
	
	
	@Override
	public String toString() {
	    String result = "CGNodeInfo{ ";
	    for (int i = 0; i < instructions.size(); i++) {
	      result.concat(instructions.get(i).toString() + " ");
	    }
	    result.concat("}");
	    return result;
	}
}



