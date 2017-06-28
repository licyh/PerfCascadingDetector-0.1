package sa.loop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;



public class LoopInfo {
	// where
	public CGNode cgNode;
	public int line_number;
  
	// its Basic Block info
	public Set<Integer> bbs;
	int begin_bb;         // bb - basic block in WALA
	int end_bb;
	Set<Integer> succbbs; //used as a temporary variable
	Set<Integer> predbbs; //used as a temporary variable
  
	// included time-consuming operation info
	public int numOfTcOperations_recusively;
	public List<SSAInstruction> tcOperations_recusively;
	public List<TcOperationInfo> tcOperations_recusively_info;
  
	// nested loop info
	public int max_depthOfLoops;  //ie, nested loops inside the loop
	public List<Integer> function_chain_for_max_depthOfLoops;
	public List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
  
	String var_name;
  
	
	
	public LoopInfo() {
		this.cgNode = null;
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
	
	
	
	// newly added, haven't been tested
	public boolean containsSSA(SSAInstruction targetSSA) {
	    IR ir = cgNode.getIR();
	    SSACFG cfg = ir.getControlFlowGraph();
	    
		for (int bbnum: bbs) {
			ISSABasicBlock bb = cfg.getNode(bbnum);
			for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext(); ) {
	    		SSAInstruction ssa = it.next();
	    		if ( ssa.equals(targetSSA) )
	    			return true;
			}
		}
		return false;
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
	    return "LOOP - " + cgNode.getMethod().getSignature() + ":" + line_number + ", "
	    		+ "Time-consumingOps(" + numOfTcOperations_recusively + "):" + tcOperations_recusively + ", " 
	    		+ "{begin:" + begin_bb + " end:" + end_bb + " var_name:" + var_name + " bbs:" + bbs + "}";
	}
  
	
	//tmp for loop's Time-consuming operations
	public String toString_detail() {
	    return "LOOP - " + cgNode.getMethod().getSignature() + ":" + line_number + ", "
	    		+ "Time-consumingOps(" + numOfTcOperations_recusively + "):" + tcOperations_recusively_info;
	}
  
}


