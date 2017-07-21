package sa.lock;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;




public class LockInfo {
	CGNode cgNode;
	int cgNode_id;       //ie, cgNode.getGraphNodeId()  
	String cgNode_name;  //ie, cgNode.getMethod().getSignature();

	public Set<Integer> bbs;
	int begin_bb;         // bb - basic block in WALA
	int end_bb;
	//tmp
	Set<Integer> succbbs; //used as a temporary variable
	Set<Integer> predbbs; //used as a temporary variable
	  
	
	public String lock_identity;   //only be used in & after the phase of "analyzeAllLocks"
	  
	public String lock_type;  //for now: "synchronized_method", "synchronized_lock" + "lock", "readLock", "writeLock", "tryLock", "writeLockInterruptibly", "readLockInterruptibly", "lockInterruptibly"
	int lock_name_vn;  //only for "synchronized (vn)"
	public String lock_class; //only for "class.lock()/readLock()/writeLock()"    #will not just like this
	public String lock_name;  //useless now
	  

	
	
	
	public LockInfo() {  
	    this.cgNode_id = -1;
	    this.cgNode_name = "";
	    
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
	 
	
	//APIs
	public CGNode getCGNode() {
		return this.cgNode;
	}
	
	/*
	public int getLineNumber() {
		return this.line_number;
	}
	*/
	
	public Set<Integer> getBasicBlockNumbers() {
		return this.bbs;
	}
	
	public int getBeginBasicBlockNumber() {
		return this.begin_bb;
	}
	
	public int getEndBasicBlockNumber() {
		return this.end_bb;
	}
	
	/*
	public void setEndBasicBlock(int end_bb) {
		this.end_bb = end_bb;
		computeBasicBlocks();
	}
	*/
	
	
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
	    if (this.lock_type.equals(LockAnalyzer.synchronizedtypes.get(1))) {
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
	          if (LockAnalyzer.locktypes.contains(short_funcname)) { //lock
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
	    return "{function:" + cgNode_name + "\t" 
	      + " lock_class:" + lock_class + "\t" + " lock_type:" + lock_type + "\t" 
	      + " lock_name:" + lock_name + "\t" + " lock_name_vn:" + lock_name_vn + "\t" 
	      + " begin:" + begin_bb + " end:" + end_bb + " bbs:" + bbs + "}";
	  }
	}