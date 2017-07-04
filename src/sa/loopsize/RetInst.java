package sa.loopsize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;

public class RetInst {
	SSAInstruction retCall; // this method return instruction we need analyze 
	HashSet<LabelledSSA> calleeRet; //= new HashSet<LabelledSSA>();//get return instructions for called method which retCall calls
	Map<SSAInstruction,CGNode> retDetail;
	CallGraph cg;
	String calledMeth; //name of method which retCall is in
	String calledClass;//name of class which retCall is in 
	CGNode callMethodCg; //CGNode which retCall is in 
	
	public RetInst(SSAInstruction inst,CGNode cgN, CallGraph iCG){
		this.retCall = inst;
		this.callMethodCg = cgN;
		this.cg = iCG;
		this.calleeRet = new HashSet<LabelledSSA>();
		this.retDetail = new HashMap<SSAInstruction,CGNode>();
	}
	
	public void getCalledMeth(){
		if(this.retCall instanceof SSAInvokeInstruction){
			this.calledMeth = ((SSAInvokeInstruction) this.retCall).getDeclaredTarget().getName().toString();
			this.calledClass = ((SSAInvokeInstruction) this.retCall).getDeclaredTarget().getDeclaringClass().getName().toString();
			System.out.println(this.calledMeth);
			System.out.println(this.calledClass);
			//System.out.println(((SSAInvokeInstruction) this.retCall).getDeclaredTarget().getName());
			//System.out.println(((SSAInvokeInstruction) this.retCall).getDeclaredTarget().getDeclaringClass().getName());

			//System.out.println(((SSAInvokeInstruction) this.retCall).getCallSite().toString());
		}else{
			System.out.print("this is not a call instruction, please check");
			this.calledClass = null;
			this.calledMeth = null;
		}
	}

	public void getCalleeRetInst(){
		if(this.retCall instanceof SSAInvokeInstruction){
			java.util.Set<CGNode> mySet = this.cg.getPossibleTargets(this.callMethodCg, ((SSAInvokeInstruction) this.retCall).getCallSite());
			for(CGNode tmp: mySet){
				IR ir = tmp.getIR();
				HashSet<SSAInstruction> retIR = SSAUtil.getAllRetInst(ir);
				for(SSAInstruction myInst : retIR){
					this.calleeRet.add(new LabelledSSA(myInst,tmp));
				}
			}
		}
	}
	
	/*public void getCalleeRetInst(){
		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
	    	CGNode tmp_n = it.next();
	    	if(tmp_n == null)
	    		continue;
	    	//System.out.println(tmp_n.toString());
	    	IMethod tmp_meth = tmp_n.getMethod();
	    	if(tmp_meth == null)
	    		continue;
	    	//System.out.println(tmp_meth.toString());
	    	IR tmp_ir = tmp_n.getIR();
	    	if(tmp_ir == null)
	    		continue;
	    	if(!tmp_meth.getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application))
	    		continue;
	    	System.out.println(tmp_meth.getDeclaringClass().getName().toString());
	    	System.out.println(this.calledClass);
	    	System.out.println(tmp_meth.getName().toString());
	    	System.out.println(this.calledMeth);
	    	System.out.println("xxxxxxx");
	    	if(tmp_meth.getDeclaringClass().getName().toString().equals(this.calledClass)){
	    		if(tmp_meth.getName().toString().equals(this.calledMeth)){
	    			//System.out.println("fxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxk");
	    			this.calleeRet = SSAUtil.getAllRetInst(tmp_ir);
	    			this.calledMethod = tmp_meth;
	    			this.calledMethodCg = tmp_n;
	    			return;
	    		}
	    	}
	    	
	    }
	}*/
	
	public void printAllRetInsts(){
		System.out.println("print all return instructions");
		for(LabelledSSA retInst: this.calleeRet){
			System.out.println(retInst.getSSA().toString());
			System.out.println(retInst.getSSA().getUse(0));
		}
	}
	
	
	public void pruneRetInsts(){
		HashSet<LabelledSSA> afterInsts = new HashSet<LabelledSSA>();
		for(LabelledSSA retInst: this.calleeRet){
			System.out.println(retInst.getSSA());
			if(!SSAUtil.isConstant(retInst.getCGNode(),retInst.getSSA().getUse(0))){
				afterInsts.add(retInst);
			}
		}
		this.calleeRet.clear();
		this.calleeRet = afterInsts;
		for(LabelledSSA retInstInfo: this.calleeRet){
			SSAInstruction tmp = retInstInfo.getSSA();
			CGNode tmp_n = retInstInfo.getCGNode();
			this.retDetail.put(tmp,tmp_n);
		}
	}
	
	public void printRetInfo(){
		System.out.println("print detailed return information");
		Iterator<Map.Entry<SSAInstruction,CGNode>> entries = this.retDetail.entrySet().iterator();
		while(entries.hasNext()){
			Map.Entry<SSAInstruction, CGNode> entry = entries.next();
			SSAInstruction key = entry.getKey();
			CGNode value = entry.getValue();
			System.out.println("********************************");
			System.out.println("call instruction is " + ":" + key.toString());
			System.out.println("in which class" + ":" + value.getMethod().getDeclaringClass().getName().toString());
			System.out.println("in which method" + ":" + value.getMethod().getName().toString());
			System.out.println("********************************");
		}
	}
}
