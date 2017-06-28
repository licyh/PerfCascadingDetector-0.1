package sa.loop;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

public class TcOperationInfo {
	public CGNode function;
	public String callpath;
	public int line_number;

	public SSAInstruction ssa;   //this is the core, the tc operation

	
	public TcOperationInfo() {
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