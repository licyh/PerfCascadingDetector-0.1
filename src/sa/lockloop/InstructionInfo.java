package sa.lockloop;

import java.util.ArrayList;
import java.util.List;



public class InstructionInfo {
	boolean isTcOperation;
	
	public int numOfSurroundingLoops_in_current_function;         //only for current-level function
	public List<Integer> surroundingLoops_in_current_function;    //only for current-level function
	//List<String> variables;
  
	public int maxdepthOfLoops_in_call; //only save the longest loop chain
	public int call;               //CGNode id, if any 
	List<Integer> function_chain; //CGNode id
	List<Integer> instruction_chain; //instruction index

	
	
	public InstructionInfo() {
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