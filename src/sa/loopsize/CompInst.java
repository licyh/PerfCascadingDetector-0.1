package sa.loopsize;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;

public class CompInst {
	int valNum; // in condition branch, what's the int value for this variable 
	BackwardSlicing bHelper;
	CGNode cgNode;
	SSAInstruction ssaInst; //get immediate ssa 
	HashSet<SSAInstruction> analyzedSSA; 
	
	public CompInst(int x, CGNode cgN){
		this.valNum = x;
		this.cgNode = cgN;
		this.analyzedSSA = new HashSet<SSAInstruction>();
		System.out.println("xxxxxxxx");
	}
	
	
	//		10 v7 = binaryop(mul) v2 , v5:#100
	//		11 v8 = binaryop(add) v4:#1 , v7
	//		12 conditional branch(ge, to iindex=21) v13,v8
	//get 11 v8 = binaryop(add) v4:#1 , v7 for this conditional branch 
	// v8 should have def 
/*	public SSAInstruction getImmediateSSA(){
		return SSAUtil.getSSAIndexByDefvn(this.cgNode.getIR().getInstructions(), this.valNum);
	}
	
	public void initialBWS(){
		this.ssaInst = getImmediateSSA();
		System.out.println(this.ssaInst.toString());
		this.bHelper = new BackwardSlicing(this.ssaInst,this.cgNode);
		System.out.println("finish initializing bws1");
		this.analyzedSSA = this.bHelper.getDataSlicing(this.ssaInst);
		System.out.println("finish initializing bws2");
	}
	
	public void printAnalyzedSSA(){
		System.out.println("print analyzed ssa");
		this.bHelper.printSlice(this.analyzedSSA);
	}*/
	
}
