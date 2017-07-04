package sa.loopsize;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;

public class LabelledSSA {
	SSAInstruction ssaInst;
	CGNode cgNode;
	public LabelledSSA(SSAInstruction inst, CGNode cgN) {
		// TODO Auto-generated constructor stub
		this.ssaInst = inst;
		this.cgNode = cgN;
	}
	
	public SSAInstruction getSSA(){
		return this.ssaInst;
	}
	
	public CGNode getCGNode(){
		return this.cgNode;
	}
}
