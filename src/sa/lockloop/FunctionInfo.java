package sa.lockloop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

import sa.lock.LockInfo;
import sa.loop.LoopInfo;



public class FunctionInfo {
	  // newly added
	  int numOfTcOperations;
	  int numOfTcOperations_recusively;
	  List<SSAInstruction> tcOperations;            // time-consuming operation locations
	  List<SSAInstruction> tcOperations_recusively; // won't be used&inited. if want to
		
	  boolean hasLoopingLocks;                      //only for first-level functions that have locks
	  Map<Integer, LoopingLockInfo> looping_locks;  //only for first-level functions that have locks, map: lock-id -> max_depthOfLoops
	  
	  //Newest! just added
	  List<LockInfo> locks = null;                 //Type - ArrayList<LoopInfo>
	  List<LoopInfo> loops = null;                 //Type - ArrayList<LoopInfo>
	  
	  int max_depthOfLoops;
	  List<Integer> function_chain_for_max_depthOfLoops;
	  List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
	  
	  Map<Integer, InstructionInfo> instructions;
	  
	  
	  FunctionInfo() {
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
	  
	  @Override
	  public String toString() {
	    String result = "FunctionInfo{ ";
	    for (int i = 0; i < instructions.size(); i++) {
	      result.concat(instructions.get(i).toString() + " ");
	    }
	    result.concat("}");
	    return result;
	  }
	}

	class LoopingLockInfo {
	  CGNode function;
	  LockInfo lock;
	  
	  int max_depthOfLoops;
	  List<Integer> function_chain_for_max_depthOfLoops;
	  List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
	  
	  LoopingLockInfo() {
	    this.function = null;
	    this.lock = null;
	    
	    this.max_depthOfLoops = 0;
	    this.function_chain_for_max_depthOfLoops = new ArrayList<Integer>();
	    this.hasLoops_in_current_function_for_max_depthOfLoops = new ArrayList<Integer>();
	  }
	}


	class InstructionInfo {
	  boolean isTcOperation;
		
	  int numOfSurroundingLoops_in_current_function;         //only for current-level function
	  List<Integer> surroundingLoops_in_current_function;    //only for current-level function
	  //List<String> variables;
	  
	  int maxdepthOfLoops_in_call; //only save the longest loop chain
	  int call;               //CGNode id, if any 
	  List<Integer> function_chain; //CGNode id
	  List<Integer> instruction_chain; //instruction index
	  
	  InstructionInfo() {
		this.isTcOperation = false;  
		  
	    this.numOfSurroundingLoops_in_current_function = 0;                    //only for current-level function
	    this.surroundingLoops_in_current_function = new ArrayList<Integer>();  //only for current-level function
	    
	    this.maxdepthOfLoops_in_call = 0;
	    this.call = -1;
	    this.function_chain = new ArrayList<Integer>();
	    this.instruction_chain = new ArrayList<Integer>();
	  }
	  
	  @Override
	  public String toString() {
	    return "InstructionInfo{numOfLoops_in_current_function:" + numOfSurroundingLoops_in_current_function + ",numOfLoops_in_call:" + maxdepthOfLoops_in_call + ",function_chain:" + function_chain + "}";
	  }
	}

