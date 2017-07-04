package sa.loopsize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;

public class ParameterInst {
	int paraLoc; //parameter location 
	Map<SSAInstruction,CGNode> calleeLoc;// record all location which this function is called 
	CallGraph cg;
	CGNode cgNode; // CGNode of whichFun
	recorderHelper rHelper;

	public ParameterInst(int anchor, CallGraph myCg, CGNode cgN){
		this.cg = myCg;
		this.paraLoc = anchor;
		this.cgNode = cgN;
		this.calleeLoc = new HashMap<SSAInstruction,CGNode>();
	}
	
	// get the caller of the method where ssaInst is in 
	public void getPossibleCaller(){
		if(this.cgNode.getMethod().getName().toString().toString().equals("main"))
			return;
		for(Iterator<CGNode> it = this.cg.getPredNodes(this.cgNode);it.hasNext();){
			CGNode tmp_cg = it.next();
			if(tmp_cg.getMethod().getName().toString().equals("fakeRootMethod"))
				continue;
			System.out.println("xxxxxxxxxxxxxxxxxx" + tmp_cg.getMethod().getName().toString());
			if(isCallRelation(tmp_cg,this.cgNode)){
				System.out.println("???????????????????" + tmp_cg.getMethod().getSignature().toString());
				for(SSAInstruction ret : this.rHelper.whichSSA){
					System.out.println("wtf???????");
					System.out.println(ret.toString());
					this.calleeLoc.put(ret,this.rHelper.origCg);
				}
			}
		}
	}
	
	// does CGNode from have call instruction calling to? 
	public boolean isCallRelation (CGNode from, CGNode to){
		Map<SSAInstruction,Set<CGNode>> recorder = new HashMap<SSAInstruction,Set<CGNode>>();
		this.rHelper = new recorderHelper(from);
		SSAInstruction[] searchSpace = from.getIR().getInstructions();
		for(SSAInstruction inst : searchSpace){
			if(inst instanceof SSAInvokeInstruction){
				//if(from.getMethod().getSignature().toString().contains("main"))
				//	System.out.println(inst.toString());
				Set<CGNode> tmp = this.cg.getPossibleTargets(from, ((SSAInvokeInstruction) inst).getCallSite());
				for(CGNode x: tmp){
					System.out.println("getPossibleTargets" + ":" + x.getMethod().getSignature().toString());
				}
				this.rHelper.addMap(inst, tmp);
			}
		}
		return this.rHelper.isContained(to);
	}
	
	public void printCalleeLoc(){
		System.out.println("print callsite information for" + ":" + this.cgNode.getMethod().getSignature().toString());
		Iterator<Map.Entry<SSAInstruction,CGNode>> entries = this.calleeLoc.entrySet().iterator();
		while(entries.hasNext()){
			Map.Entry<SSAInstruction,CGNode> entry = entries.next();
			SSAInstruction key = entry.getKey();
			CGNode value = entry.getValue();
			System.out.println("********************************");
			System.out.println("call instruction is " + " : " + key.toString());
			System.out.println("in which class" + " : " + value.getMethod().getDeclaringClass().getName().toString());
			System.out.println("in which method" + " : " + value.getMethod().getName().toString());
			System.out.println("********************************");
		}
	}
}


//record helper class 
class recorderHelper{
	Map<SSAInstruction, Set<CGNode> >recorder;
	CGNode origCg;
	Set<SSAInstruction> whichSSA = new HashSet<SSAInstruction>();
	public recorderHelper(CGNode cgN){
		this.recorder = new HashMap<SSAInstruction,Set<CGNode>>();
		this.origCg = cgN;
	}
	
	public void addMap(SSAInstruction inst, Set<CGNode> cgSet){
		this.recorder.put(inst,cgSet);
	}
	
	public boolean isContained(CGNode targetCGN){
		boolean retVal = false;
		Iterator<Map.Entry<SSAInstruction,Set<CGNode>>> entries = this.recorder.entrySet().iterator();
		while(entries.hasNext()){
			Map.Entry<SSAInstruction,Set<CGNode>> entry = entries.next();
			SSAInstruction key = (SSAInstruction)entry.getKey();
			Set<CGNode> value = (Set<CGNode>)entry.getValue();
			if(value.contains(targetCGN)){
				this.whichSSA.add(key);
				retVal = true;
			}
		}
		return retVal;
	}
}
