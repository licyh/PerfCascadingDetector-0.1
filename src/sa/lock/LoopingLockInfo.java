package sa.lock;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.ipa.callgraph.CGNode;



public class LoopingLockInfo {

	CGNode function;
	LockInfo lock;
  
	int max_depthOfLoops;
	List<Integer> function_chain_for_max_depthOfLoops;
	List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
  
	public LoopingLockInfo() {
		this.function = null;
		this.lock = null;
    
		this.max_depthOfLoops = 0;
		this.function_chain_for_max_depthOfLoops = new ArrayList<Integer>();
		this.hasLoops_in_current_function_for_max_depthOfLoops = new ArrayList<Integer>();
	}
	
}