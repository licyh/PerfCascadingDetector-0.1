package sa.loopsize;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;

public class ProcessUnit {
	//processUnit, it's a unit of processing, it contains CGNode and the SSAInstruction we concern 
	CGNode cgNode;  
	CallGraph cg;
	SSAInstruction inst;
	boolean isLoopCond; //is the loop condition variable we concern  
	boolean isBounded;
	boolean strongBounded;
	//after processing a CGNode,  this map stores other CGNode and SSAInstruction we need infer  
	Map<MyPair,CGNode> newlyCGN; // parameter related 
	int level; // how deep from starter 
	// this is for newlyCGN1 to record the parameter location 
	int inIndex = -1;
	
	public ProcessUnit(CallGraph cgR, CGNode cgN, SSAInstruction inst, boolean condBranch, int myLevel, int initialIndex) {
		// TODO Auto-generated constructor stub
		this.cg = cgR;
		this.cgNode = cgN;
		this.inst = inst;
		this.isLoopCond = condBranch;
		this.isBounded = true;
		this.newlyCGN = new HashMap<MyPair,CGNode>();
		this.level = myLevel;
		this.strongBounded = false;
		this.inIndex = initialIndex;
	}
	
	public void run(){
		int numOfUse = this.inst.getNumberOfUses();
		SSAInstruction tmp = null;
		if(this.isLoopCond){
			assert (this.inst instanceof SSAConditionalBranchInstruction);
			assert numOfUse == 2;
			//1. collection case TODO
			//2. the second parameter is constant or parameter 
			//3. normal case 
			int varNum = this.inst.getUse(1);
			//System.out.println(varNum);
			//System.out.println(this.cgNode.toString());
			if((tmp = SSAUtil.isCollection(this.cgNode,this.inst)) != null){ // collection 
				System.out.println("this loop variable relies on a collection variable");
				System.out.println(this.inst.toString());
				System.out.println(tmp.toString());
				String varName = ((SSAFieldAccessInstruction)tmp).getDeclaredField().getName().toString();
				String varType = ((SSAFieldAccessInstruction)tmp).getDeclaredFieldType().getName().toString();
				CollectionInst collObj = new CollectionInst(this.cg,this.cgNode,this.inst,varName,varType);
				collObj.findAllModifyLoc();
				collObj.printResult();
				/*FieldInst fieldObj = new FieldInst(tmp,this.cg,this.cgNode);
				fieldObj.getAllModifyLoc();
				fieldObj.printAllModifyLoc();
				if(fieldObj.modifyLoc.size() == 0){
					this.isBounded &= true;
				}else{
					Iterator<Map.Entry<SSAInstruction,CGNode>> field_entries = fieldObj.modifyLoc.entrySet().iterator();
					while(field_entries.hasNext()){
						Map.Entry<SSAInstruction,CGNode> field_entry = field_entries.next();
						SSAInstruction field_key = field_entry.getKey();
						CGNode field_value = field_entry.getValue();
						MyPair field_pair = new MyPair(field_key,-1);
						this.newlyCGN.put(field_pair,field_value);
					}
				}*/
			}else if(SSAUtil.isVnParameter(this.cgNode,varNum)){ // it's parameter 
				System.out.println("this loop variable relies on parameter");
				System.out.println(this.inst.toString());
				int paraLoc = SSAUtil.getParameterLoc(varNum,this.cgNode);
				ParameterInst paraObj = new ParameterInst(paraLoc,this.cg,this.cgNode);
				paraObj.getPossibleCaller();
				paraObj.printCalleeLoc();
				if(paraObj.calleeLoc.size() == 0)
					this.isBounded &= true;
				else{
					Iterator<Map.Entry<SSAInstruction,CGNode>> para_entries = paraObj.calleeLoc.entrySet().iterator();
					while(para_entries.hasNext()){
						Map.Entry<SSAInstruction,CGNode> para_entry = para_entries.next();
						SSAInstruction para_key = para_entry.getKey();
						CGNode para_value = para_entry.getValue();
						MyPair tmp_pair = new MyPair(para_key,SSAUtil.getParameterLoc(varNum,this.cgNode));
						this.newlyCGN.put(tmp_pair, para_value);
					}
				}
			}else if(SSAUtil.isConstant(this.cgNode,varNum)){
				System.out.println(this.inst.toString());
				System.out.println("this loop is bouned");
			}else{
				System.out.println("corner case??????????");
			}
		}else{
			System.out.println("not branch, normal case");
			System.out.println(this.inst.toString());
			this.isBounded &= normalCaseProcess(this.inst);
		}
	}
	
	public boolean normalCaseProcess(SSAInstruction inst){
		if(this.level > 20){
			 System.out.println("exceed the threshold");
			 return false;
		}
		//System.out.println(inst.toString());
		BackwardSlicing bws = new BackwardSlicing(inst,this.cgNode,this.inIndex);
		bws.getDataSlicing(inst);
		bws.printSlice();
		if(bws.isBoundedCondInThisCFG()){
			this.strongBounded = true;
			System.out.println("it's bounded in" + this.cgNode.getMethod().getSignature().toString());
		}else{
			if(bws.callRet.size() != 0){
				System.out.println("normal case : call return");
				for(SSAInstruction ssa_tmp : bws.callRet){
					RetInst retObj = new RetInst(ssa_tmp,this.cgNode,this.cg);
					retObj.getCalleeRetInst();
					retObj.pruneRetInsts();
					if(retObj.retDetail.size()!=0){
						Iterator<Map.Entry<SSAInstruction,CGNode>> ret_entries = retObj.retDetail.entrySet().iterator();
						while(ret_entries.hasNext()){
							Map.Entry<SSAInstruction,CGNode> ret_entry = ret_entries.next();
							SSAInstruction ret_key = ret_entry.getKey();
							CGNode ret_value = ret_entry.getValue();
							MyPair ret_pair = new MyPair(ret_key,-1);
							this.newlyCGN.put(ret_pair, ret_value);
						}
					}
				}
			}
			if(bws.paraList.size() != 0){
				System.out.println("normal case : parameter");
				for(Integer loc_tmp : bws.paraList){
					ParameterInst paraObj = new ParameterInst(loc_tmp.intValue(),this.cg,this.cgNode);
					paraObj.getPossibleCaller();
					paraObj.printCalleeLoc();
					if(paraObj.calleeLoc.size()!=0){
						Iterator<Map.Entry<SSAInstruction,CGNode>> para_entries = paraObj.calleeLoc.entrySet().iterator();
						while(para_entries.hasNext()){
							Map.Entry<SSAInstruction,CGNode> para_entry = para_entries.next();
							SSAInstruction para_key = para_entry.getKey();
							CGNode para_value = para_entry.getValue();
							MyPair tmp_pair = new MyPair(para_key,SSAUtil.getParameterLoc(loc_tmp.intValue(),this.cgNode));
							this.newlyCGN.put(tmp_pair, para_value);
						}
					}
				}
			}
			if(bws.thisList.size() != 0){
				System.out.println("normal : field");
				for(SSAInstruction fieldAcc : bws.thisList){
					FieldInst fieldObj = new FieldInst(fieldAcc,this.cg,this.cgNode);
					fieldObj.getAllModifyLoc();
					fieldObj.printAllModifyLoc();
					if(fieldObj.modifyLoc.size() != 0){
						Iterator<Map.Entry<SSAInstruction,CGNode>> field_entries = fieldObj.modifyLoc.entrySet().iterator();
						while(field_entries.hasNext()){
							Map.Entry<SSAInstruction,CGNode> field_entry = field_entries.next();
							SSAInstruction field_key = field_entry.getKey();
							CGNode field_value = field_entry.getValue();
							MyPair field_pair = new MyPair(field_key,-1);
							this.newlyCGN.put(field_pair,field_value);
						}
					}					
				}
			}
		}
		return true;
	}
	
	public void printNewlyCGN(){
		System.out.println("print newly added CGNode");
		Iterator<Map.Entry<MyPair,CGNode>> entries = this.newlyCGN.entrySet().iterator();
		while(entries.hasNext()){
			Map.Entry<MyPair,CGNode> entry = entries.next();
			SSAInstruction key = entry.getKey().getL();
			int tmp_int = entry.getKey().getR();
			CGNode value = entry.getValue();
			System.out.println("=============================================");
			System.out.println("class name" + ":" + value.getMethod().getDeclaringClass().getName().toString());
			System.out.println("method name" + ":" + value.getMethod().getName().toString());
			System.out.println("SSAInstruction" + ":" + key.toString());
			System.out.println("paraLoc" + ":" + tmp_int);
			System.out.println("=============================================");
		}
		System.out.println("finish print newly added CGNode");
	}
	
}
