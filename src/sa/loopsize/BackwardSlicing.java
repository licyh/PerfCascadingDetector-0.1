package sa.loopsize;

import java.util.HashSet;
import java.util.LinkedList;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.graph.dominators.Dominators;

public class BackwardSlicing {
	SSAInstruction ssaInst;
	CGNode cgNode;
	SSAInstruction[] allInsts;
	IR ir;
	HashSet<SSAInstruction> outerInst; // outer instruction, which doesn't have backward slicing 
	HashSet<SSAInstruction> slicer;//slicing result
	HashSet<SSAInstruction> callRet; //function call instruction 
	HashSet<Integer> paraList; //we need analyze parameter 
	HashSet<SSAInstruction> thisList; // we need analyze class field 
	boolean inStatic = false;
	int paraIndex = -1;
	
	public BackwardSlicing(SSAInstruction inst, CGNode cgN, int para_index){
		this.ssaInst = inst;
		this.cgNode = cgN;
		this.outerInst = new HashSet<SSAInstruction>();
		this.slicer = new HashSet<SSAInstruction>();
		this.callRet = new HashSet<SSAInstruction>();
		this.paraList = new HashSet<Integer>();
		this.thisList = new HashSet<SSAInstruction>();
		this.allInsts = cgN.getIR().getInstructions();
		this.ir = cgN.getIR();
		this.inStatic = cgN.getMethod().isStatic();
		this.paraIndex = para_index;
	}
	
	// when paraIndex = -1, normal case 
	// when paraIndex = others, dInst is a call instruction, and it's from parameter case 
	public HashSet<SSAInstruction> getDataDependences(SSAInstruction dInst){
		HashSet<SSAInstruction> dataSet = new HashSet<SSAInstruction>();
		if(dInst instanceof SSAInvokeInstruction){
			if(!((SSAInvokeInstruction)dInst).getDeclaredTarget().getReturnType().getName().toString().equals("V") && (this.paraIndex == -1)){
				// normal dInst is a call instruction, but it's not from parameter case 
				if(!((SSAInvokeInstruction)dInst).getDeclaredTarget().getReturnType().getName().toString().equals("Z")){
					//exclude the case, which return value is boolean 
					this.callRet.add(dInst);
					this.outerInst.add(dInst);
				}
				return dataSet;
			}else{
				int paraIndex = dInst.getUse(this.paraIndex);
				if(SSAUtil.isConstant(this.cgNode,paraIndex)){
					this.outerInst.add(dInst);
					// do nothing 
				}
				if(SSAUtil.isVnParameter(this.cgNode,paraIndex)){
					this.outerInst.add(dInst);
					this.paraList.add(SSAUtil.getParameterLoc(paraIndex,this.cgNode));
				}
				return dataSet;
			}
		}
		int numOfUse = dInst.getNumberOfUses();
		if(numOfUse == 0)
			System.out.println(dInst.toString() + "doesn't have use!!!");
		for(int i = 0; i < numOfUse; i++){
			int index = dInst.getUse(i);
			SSAInstruction ssa_tmp = SSAUtil.getSSAIndexByDefvn(this.allInsts,index);
			if(ssa_tmp != null){
				dataSet.add(ssa_tmp);
			}
			if(ssa_tmp == null){
				this.outerInst.add(dInst);
				if(SSAUtil.isVnParameter(this.cgNode, index)){
					System.out.println(this.cgNode.toString());
					this.paraList.add(SSAUtil.getParameterLoc(index,this.cgNode));
				}
				if(!this.inStatic && index == 1 && (dInst instanceof SSAGetInstruction))
					this.thisList.add(dInst);
			}
		}
		return dataSet;
	}
	
	//include dsInst itself 
	public void getDataSlicing(SSAInstruction dsInst){
		LinkedList<SSAInstruction> work_set = new LinkedList<SSAInstruction>();
		HashSet<SSAInstruction> visited_set = new HashSet<SSAInstruction>();
		work_set.add(dsInst);
		visited_set.add(dsInst);
		this.slicer.add(dsInst);
		while(!work_set.isEmpty()){
			SSAInstruction tmpInst = work_set.poll();
			HashSet<SSAInstruction> dSet = getDataDependences(tmpInst);
			for(SSAInstruction newInst : dSet){
				if(!visited_set.contains(newInst)){
					this.slicer.add(newInst);
					work_set.add(newInst);
					visited_set.add(newInst);
				}
			}
			System.out.println("working....");
		}
	}
	
	public void printSlice(){
		System.out.println("print slicing result");
		System.out.println("====================");
		for(SSAInstruction tmpInst:this.slicer)
			System.out.println(tmpInst.toString());
		System.out.println("====================");
		System.out.println("print out slicing result");
		System.out.println("$$$$$$$$$$$$$$$$$$$$$");
		for(SSAInstruction aInst:this.outerInst)
			System.out.println(aInst.toString());
		System.out.println("$$$$$$$$$$$$$$$$$$$$$");
	}
	
	public boolean isBoundedCondInThisCFG(){
		//no need to analyze function call, parameter, and class field
		// and for every instruction in outerInst, all its uses are constant 
		// then it's bounded 
		// otherwise, we need analyze function call, parameter, and class field 
		if(this.callRet.size() == 0 && this.paraList.size() == 0 && this.thisList.size() == 0){
			for(SSAInstruction ssa_tmp : this.outerInst){
				for(int i = 0; i < ssa_tmp.getNumberOfUses();i++){
					if(SSAUtil.getSSAIndexByDefvn(this.ir.getInstructions(),ssa_tmp.getUse(i)) == null){
						if(SSAUtil.isConstant(this.cgNode,ssa_tmp.getUse(i)))
							continue;
						else
							return false;
					}
				}
			}
			return true;
		}
		return false;
	}
}
