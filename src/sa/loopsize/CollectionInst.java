package sa.loopsize;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;

public class CollectionInst {
	SSAInstruction inst;
	CallGraph cg;
	CGNode cgNode;
	String varName;
	String varType;
	HashSet<SSAInstruction> result;
	
	public CollectionInst(CallGraph cgIn, CGNode cgNIn, SSAInstruction ssa, String varName, String varType) {
		// TODO Auto-generated constructor stub
		this.inst = ssa;
		this.cg = cgIn;
		this.cgNode = cgNIn;
		this.varName = varName;
		this.varType = varType;
		this.result = new HashSet<SSAInstruction>();
	}

	
	//1. get all locations which invoke getfield for this collection 
	//2. determine which actually does add/addAll/put/putAll
	public void findAllModifyLoc(){
		for (Iterator<? extends CGNode> it = this.cg.iterator(); it.hasNext();) {
	    	CGNode tmp_n = it.next();
	    	if(tmp_n == null)
	    		continue;
	    	if (!LoopVarUtil.isApplicationMethod(tmp_n))
	    		continue;
	      	if (LoopVarUtil.isNativeMethod(tmp_n))
	      		continue;
	    	//System.out.println(tmp_n.toString());
	    	IMethod tmp_meth = tmp_n.getMethod();
	    	if(tmp_meth == null)
	    		continue;
	    	//System.out.println(tmp_meth.toString());
	    	IR tmp_ir = tmp_n.getIR();
	    	if(tmp_ir == null)
	    		continue;
	    	//System.out.println(tmp_ir.toString());
	    	SSACFG cfg = tmp_ir.getControlFlowGraph();
	 	    for (Iterator<ISSABasicBlock> ibb = cfg.iterator(); ibb.hasNext(); ) {
	 	      ISSABasicBlock bb = ibb.next();
	 	      for (Iterator<SSAInstruction> issa = bb.iterator(); issa.hasNext(); ) {
	 	    	  SSAInstruction tmp_ssa = issa.next();
	 	    	  if(tmp_ssa instanceof SSAFieldAccessInstruction){
	 	    			SSAFieldAccessInstruction finst = (SSAFieldAccessInstruction)tmp_ssa;
	 	    			if(tmp_ssa instanceof SSAGetInstruction){
	 	    				//TODO derived class?
	 	    				if(((SSAFieldAccessInstruction) tmp_ssa).getDeclaredField().getName().toString().equals(this.varName) &&
	 	    						((SSAFieldAccessInstruction) tmp_ssa).getDeclaredFieldType().getName().toString().equals(this.varType)){
	 	    					if(isAddOrPut(cfg,tmp_ssa,this.varType)){
	 	    						this.result.add(tmp_ssa);
	 	    					}
	 	    				}
	 	    			}  
	 	    	  }
	 	      }
	 	    }
	    }
	}
	
	public boolean isAddOrPut(SSACFG cfg,SSAInstruction ssa, String type){
		boolean retflag = false;
		SSAInstruction tmp = null;
 	    for (Iterator<ISSABasicBlock> ibb = cfg.iterator(); ibb.hasNext(); ) {
	 	      ISSABasicBlock abb = ibb.next();
	 	      for (Iterator<SSAInstruction> issa = abb.iterator(); issa.hasNext(); ) {
	 	    	  SSAInstruction tmp_ssa = issa.next();
	 	    	  if(tmp_ssa instanceof SSAInvokeInstruction){
	 	    		  //System.out.println(ssa.toString());
	 	    		  //System.out.println(tmp_ssa.toString());
	 	    		  //System.out.println(tmp_ssa.getUse(0));
	 	    		  tmp = SSAUtil.getSSAIndexByDefvn(cfg.getInstructions(),tmp_ssa.getUse(0));
	 	    		  if(tmp != null && tmp.equals(ssa)){
	 	    			  if(((SSAInvokeInstruction) tmp_ssa).getDeclaredTarget().getDeclaringClass().getName().toString().equals(type)){
	 	    				  if(((SSAInvokeInstruction) tmp_ssa).getDeclaredTarget().getName().toString().equals("add")){
	 	    					  retflag = true;
	 	    					  break;
	 	    				  }
	 	    			  }
	 	    		  }
	 	    	  }
	 	     }
 	    }
 	    return retflag;
	}
	
	public void printResult(){
		System.out.println("beging print result......");
		for(SSAInstruction tmp : this.result){
			System.out.println(tmp.toString());
		}
		System.out.println("end print result");
	}
}
