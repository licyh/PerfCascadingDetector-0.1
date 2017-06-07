package sa.lockloop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;

class LockInfo {
  CGNode func;
  int func_id;       //ie, func.getGraphNodeId()  
  String func_name;  //ie, func.getMethod().getSignature();
  
  String lock_identity;   //only be used in & after the phase of "analyzeAllLocks"
  
  String lock_type;  //for now: "synchronized_method", "synchronized_lock" + "lock", "readLock", "writeLock", "tryLock", "writeLockInterruptibly", "readLockInterruptibly", "lockInterruptibly"
  int lock_name_vn;  //only for "synchronized (vn)"
  String lock_class; //only for "class.lock()/readLock()/writeLock()"    #will not just like this
  String lock_name;  //useless now
  
  int begin_bb;         // bb - basic block in WALA
  int end_bb;  
  Set<Integer> bbs;
  Set<Integer> succbbs; //used as a temporary variable
  Set<Integer> predbbs; //used as a temporary variable
  
  LockInfo() {  
    this.func_id = -1;
    this.func_name = "";
    
    this.lock_type = "";
    this.lock_name_vn = -1;
    this.lock_name = "";
    this.lock_class = "";

    this.begin_bb = -1;
    this.end_bb = -1;
    this.bbs = new TreeSet<Integer>();
    this.succbbs = new HashSet<Integer>();
    this.predbbs = new HashSet<Integer>();
  }
 
  /*
  public void dfsFromEnter(ISSABasicBlock bb, SSACFG cfg) {
    if (isMatched(bb))
      return;
    for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext(); ) {
      ISSABasicBlock succ = it.next();
      int succnum = succ.getNumber();
      //if (succ.isExitBlock()) System.err.println("succ.isExitBlock() = " + bb.getNumber() + ":" + succnum);
      if (!succ.isExitBlock() && !this.bbs.contains(succnum)) {  //add "!succ.isExitBlock()" because there are many edges to Exit Block, but the PDF IR never show that but IR.toString do
        this.bbs.add(succnum);
        dfsFromEnter(succ, cfg);
      }
    } 
  }
  */
  
  public void dfsFromEnter(ISSABasicBlock bb, SSACFG cfg) {
    for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext(); ) {
      ISSABasicBlock succ = it.next();
      int succnum = succ.getNumber();
      if (!this.succbbs.contains(succnum)) {
        this.succbbs.add(succnum);
        dfsFromEnter(succ, cfg);
      }
    } 
  }
  
  public void dfsFromExit(ISSABasicBlock bb, SSACFG cfg) {
    if (this.isMatched(bb))
      return;
    for (Iterator<ISSABasicBlock> it = cfg.getPredNodes(bb); it.hasNext(); ) {
      ISSABasicBlock pred = it.next();
      int prednum = pred.getNumber();
      if (!this.predbbs.contains(prednum)) {
        this.predbbs.add(prednum);
        dfsFromExit(pred, cfg);
      }
    } 
  }
  
  public void mergeLoop() {
    this.predbbs.retainAll(this.succbbs);   
    this.bbs.addAll(this.predbbs);
  }
  
  public boolean isMatched(ISSABasicBlock bb) {
    if (this.lock_type.equals(JXLocks.synchronizedtypes.get(1))) {
      for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext(); ) {
        SSAInstruction ssa = it.next();
        if (ssa instanceof SSAMonitorInstruction)
          if (((SSAMonitorInstruction) ssa).isMonitorEnter()) { //enter
            int vn = ((SSAMonitorInstruction) ssa).getRef();
            if (this.lock_name_vn == vn)
              return true;
          }
      } //for-it
    }
    else { //this.lock_type.equals.("lock/readLock/WriteLock/...")
      for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext(); ) {
        SSAInstruction ssa = it.next();
        if (ssa instanceof SSAInvokeInstruction) {
          String short_funcname = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getName().toString();
          if (JXLocks.locktypes.contains(short_funcname)) { //lock
            String lock_class = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getDeclaringClass().toString();
            if (this.lock_class.equals(lock_class) && this.lock_type.equals(short_funcname))
              return true;
          }
        }
      } //for-it
    }
    return false;
  }
  
  @Override
  public String toString() {
    return "{function:" + func_name + "\t" 
      + " lock_class:" + lock_class + "\t" + " lock_type:" + lock_type + "\t" 
      + " lock_name:" + lock_name + "\t" + " lock_name_vn:" + lock_name_vn + "\t" 
      + " begin:" + begin_bb + " end:" + end_bb + " bbs:" + bbs + "}";
  }
}


public class LoopInfo {
  // where
  CGNode function;
  int line_number;
  
  // its Basic Block info
  Set<Integer> bbs;
  int begin_bb;         // bb - basic block in WALA
  int end_bb;
  Set<Integer> succbbs; //used as a temporary variable
  Set<Integer> predbbs; //used as a temporary variable
  
  // included time-consuming operation info
  int numOfTcOperations_recusively;
  List<SSAInstruction> tcOperations_recusively;
  List<TcOperationInfo> tcOperations_recusively_info;
  
  // nested loop info
  int max_depthOfLoops;  //ie, nested loops inside the loop
  List<Integer> function_chain_for_max_depthOfLoops;
  List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
  
  String var_name;
  
  LoopInfo() {
	this.function = null;
	this.line_number = 0;
	  
    this.bbs = new TreeSet<Integer>();
    this.succbbs = new HashSet<Integer>();
    this.predbbs = new HashSet<Integer>();
    
    this.numOfTcOperations_recusively = 0;    //jx: yes
    this.tcOperations_recusively = new ArrayList<SSAInstruction>();
    this.tcOperations_recusively_info = new ArrayList<TcOperationInfo>();
    
    this.max_depthOfLoops = 0;
    this.function_chain_for_max_depthOfLoops = new ArrayList<Integer>();
    this.hasLoops_in_current_function_for_max_depthOfLoops = new ArrayList<Integer>();
  }
  
  public void dfsFromEnter(ISSABasicBlock bb, SSACFG cfg) {
    for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext(); ) {
      ISSABasicBlock succ = it.next();
      int succnum = succ.getNumber();
      if (!this.succbbs.contains(succnum)) {
        this.succbbs.add(succnum);
        dfsFromEnter(succ, cfg);
      }
    } 
  }
  
  public void dfsFromExit(ISSABasicBlock bb, SSACFG cfg) {
    if (bb.equals(cfg.getNode(this.begin_bb)))
      return;
    for (Iterator<ISSABasicBlock> it = cfg.getPredNodes(bb); it.hasNext(); ) {
      ISSABasicBlock pred = it.next();
      int prednum = pred.getNumber();
      if (!this.predbbs.contains(prednum)) {
        this.predbbs.add(prednum);
        dfsFromExit(pred, cfg);
      }
    } 
  }
  
  public void mergeLoop() {
    this.predbbs.retainAll(this.succbbs);
    this.bbs.addAll(this.predbbs);
  }
  
  @Override
  public String toString() {
    return "LOOP - " + function.getMethod().getSignature() + ":" + line_number + ", "
    		+ "Time-consumingOps(" + numOfTcOperations_recusively + "):" + tcOperations_recusively + ", " 
    		+ "{begin:" + begin_bb + " end:" + end_bb + " var_name:" + var_name + " bbs:" + bbs + "}";
  }
  
  //tmp for loop's Time-consuming operations
  public String toString_detail() {
	    return "LOOP - " + function.getMethod().getSignature() + ":" + line_number + ", "
	    		+ "Time-consumingOps(" + numOfTcOperations_recusively + "):" + tcOperations_recusively_info;
	  }
  
}



class FunctionInfo {
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


class TcOperationInfo {
	CGNode function;
		String callpath;
	int line_number;
	
	SSAInstruction ssa;   //this is the core, the tc operation
	
	TcOperationInfo() {
		this.function = null;
			this.callpath = "";
		this.line_number = 0;
		this.ssa = null;
	}
	
    @Override
    public String toString() { 
        //return function.getMethod().getSignature().substring(0, function.getMethod().getSignature().indexOf('('))
        	//	+ ":" + line_number + ":" + ((SSAInvokeInstruction)ssa).getDeclaredTarget(); 
        return line_number + ":" + callpath + "@" + ((SSAInvokeInstruction)ssa).getDeclaredTarget(); 
    }
}

