package sa.loopsize;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;

public final class SSAUtil {
	
	public static void testDef(SSAInstruction ssaInst){
		int numOfDef = ssaInst.getNumberOfDefs();
		for(int i = 0; i < numOfDef; i++){
			System.out.println("getDef" + i +" : " +ssaInst.getDef(i));
		}
	}
	public static void testUse(SSAInstruction ssaInst){
		int numOfDef = ssaInst.getNumberOfUses();
		for(int i = 0; i < numOfDef; i++){
			System.out.println("getUse" + i + ":" +ssaInst.getUse(i));
		}
	}

	  // find data dependence of specific variable 
	  // op v1, v2 ---> v2 = xxx
	  public static SSAInstruction getSSAIndexByDefvn(SSAInstruction[] ssas, int defvn) {
	    int index = -1;
	    int count = 0;
	    SSAInstruction ssaInst = null;
	    for (int i = 0; i < ssas.length; i++)
	      if (ssas[i] != null) {
	    	assert count <= 1;
	    	if (ssas[i].hasDef() && ssas[i].getDef() == defvn){
	    		index = i;
	    		count++;
	    		ssaInst = ssas[i];
	    	}
	    }
	    return ssaInst;
	  }
	  
	  public static void printAllInsts(SSAInstruction[] ssas){
		  System.out.println("begin to print ssa instructions");
		  for(int i = 0; i < ssas.length; i++){
			  if(ssas[i] != null){
				  System.out.println(ssas[i].toString());
			  }
		  }
	  }
	 
	  // determine a variable is parameter 
	 public static boolean isVnParameter(CGNode function, int vn) {
		    IMethod im = function.getMethod();
		    IR ir = function.getIR();
		    if (!im.isStatic() && 2<= vn &&  vn <= ir.getNumberOfParameters()
		        || im.isStatic() && vn <= ir.getNumberOfParameters()){
		      return true;
		    }
		    return false;
	 } 
	 
	 // determine a variable is constant(including boolen, int, floag...) or not 
	 public static boolean isConstant(CGNode function, int vn){
		 	IMethod im = function.getMethod();
		 	IR ir = function.getIR();
		    SymbolTable symboltable = ir.getSymbolTable();                                                 //JX: seems same??
		    //System.out.println("ir.getSymbolTable: " + symboltable);
		    //System.out.println(symboltable.getNumberOfParameters());
		    //for (int i=0; i<symboltable.getNumberOfParameters(); i++)
		     // System.out.println(symboltable.getParameter(i)); 
		   // if(symboltable.isIntegerConstant(vn)){
		    //	System.out.println(symboltable.getIntValue(vn));
		   //return symboltable.isFloatConstant(vn);
			//}
			return symboltable.isConstant(vn);
	 }
	 
	 //return 1 this.filed
	 //return 2 parameter.field
	 public int complexType(SSAInstruction inst){
		 if(inst instanceof SSAFieldAccessInstruction)
			 return 1;
		 return -1;
	 }
	 
	 //for a field access, determine it's which class's field 
	 public static String fieldOfWhichClass(SSAFieldAccessInstruction finst){
		if (finst instanceof SSAGetInstruction){
			//System.out.println("getfield");
			return finst.getDeclaredField().getDeclaringClass().getName().toString();
			//System.out.println(finst.toString());
			//System.out.println(finst.getDeclaredField().getDeclaringClass().getName().toString()); // return class name 
			//System.out.println(finst.getDeclaredField().getName().toString()); // return this variable name 
			//System.out.println(finst.getDeclaredFieldType().toString());
			//System.out.println(finst.getUse(0));

		}
		if (finst instanceof SSAPutInstruction){
			//System.out.println("putfield");
			return finst.getDeclaredField().getDeclaringClass().getName().toString();
			//System.out.println(finst.getDeclaredField().getDeclaringClass().getName().toString()); // return class name 
			//System.out.println(finst.getDeclaredField().getName().toString()); // return this variable name\
		}
		return null;
	 }
	 
	 //for a field access, determine it's which class's field 
	 public static String fieldOfWhichVar(SSAFieldAccessInstruction finst){
		if (finst instanceof SSAGetInstruction){
			//System.out.println("getfield");
			return finst.getDeclaredField().getName().toString();
			//System.out.println(finst.toString());
			//System.out.println(finst.getDeclaredField().getDeclaringClass().getName().toString()); // return class name 
			//System.out.println(finst.getDeclaredField().getName().toString()); // return this variable name 
			//System.out.println(finst.getDeclaredFieldType().toString());
			//System.out.println(finst.getUse(0));

		}
		if (finst instanceof SSAPutInstruction){
			//System.out.println("putfield");
			return finst.getDeclaredField().getName().toString();
			//System.out.println(finst.getDeclaredField().getDeclaringClass().getName().toString()); // return class name 
			//System.out.println(finst.getDeclaredField().getName().toString()); // return this variable name\
		}
		return null;
	 }
	 
	 //get all return instructions inside one method 
	 public static HashSet<SSAInstruction> getAllRetInst(IR ir){
		 HashSet<SSAInstruction> retInsts = new HashSet<SSAInstruction>();
		 SSACFG cfg = ir.getControlFlowGraph();
	 	 for (Iterator<ISSABasicBlock> ibb = cfg.iterator(); ibb.hasNext(); ) {
	 		 ISSABasicBlock bb = ibb.next();
	 		 for (Iterator<SSAInstruction> issa = bb.iterator(); issa.hasNext(); ) {
	 			 SSAInstruction tmp_ssa = issa.next();
	 			 if(tmp_ssa instanceof SSAReturnInstruction){
	 				 retInsts.add(tmp_ssa);  
	 	    	 }
	 	     }
	 	 }
	 	 return retInsts;
	 }
	 
	 //for a parameter, get its location is parameter array 
	 public static int getParameterLoc(int num, CGNode node){
		 if(node.getMethod().isStatic())
			 return num;
		 else
			 return num-1;
	 }
	 
	 //print detailed information for a SSAInstruction 
	 public static void printLocation(CGNode cgN, SSAInstruction inst){
		 
	 }
	 
	 //determine a variable is local variable or not 
	 public static boolean isLocalVar(){
		 return true;
	 }
	 
	 //determine a conditional branch is Colleciton-like branch
	 public static SSAInstruction isCollection(CGNode cgNode, SSAInstruction ssaInst){
		 //*1*. have to successor basicblocks, one should be like 
		 //9 v10 = invokeinterface < Application, Ljava/util/Iterator, next()Ljava/lang/Object; > v5 @18 exception:v9
		 //the other one should be like return 
		 //*2*. have one predecessor basicblocks, it should be like
		 //5 v7 = invokeinterface < Application, Ljava/util/Iterator, hasNext()Z > v5 @9 exception:v6
		 // and this basicblocks should have predecessor 2 v5 = invokevirtual < Application, Ljava/util/HashSet, iterator()Ljava/util/Iterator; > v3 @4 exception:v4
		 // and its predecessor basicblocks, it should be like 
		 // 1 v3 = getfield < Application, LTestCase, mySet, <Application,Ljava/util/HashSet> > v1
		 SSAInstruction retInst = null;
		 SSAInstruction tmpInst = null;
		 IR ir = cgNode.getIR();
		 SSACFG cfg = ir.getControlFlowGraph();
		// System.out.println("xxxxxx" + ssaInst.toString());
		 if(isSecondUseZero(ssaInst.getUse(1),cgNode)){
		//	 System.out.println("sbbb1");
			 ISSABasicBlock ibb = ir.getBasicBlockForInstruction(ssaInst);
			 if(isCollectionSucc(ibb,cfg)){
				// System.out.println("sbbb2");
				 if( (tmpInst = isCollectionPred(ibb,cfg)) != null){
				//	 System.out.println("sbbbb3");
					 retInst = tmpInst;
				 }
			 }
		 }
		 return retInst;
	 }
	 
	 public static boolean isSecondUseZero(int num, CGNode cgN){
		 IMethod im = cgN.getMethod();
		 IR ir = cgN.getIR();
		 SymbolTable symboltable = ir.getSymbolTable();
		 if(isConstant(cgN,num)){
			 if(symboltable.getIntValue(num) == 0)
				 return true;
		 }
		 return false;
	 }
	 
	 public static boolean isCollectionSucc(ISSABasicBlock ibb, SSACFG cfg){
		 boolean retVal = false;
		 ISSABasicBlock[] mybb = new ISSABasicBlock[2];
		 int i = 0;
		 if(cfg.getSuccNodeCount(ibb) == 2){
			 for(ISSABasicBlock bbb : cfg.getNormalSuccessors(ibb)){
				 mybb[i++] = bbb;
			 }
			/* System.out.println(mybb[0].toString());
			 System.out.println(mybb[1].toString());
			 System.out.println(isCollectionRetNone(mybb[0]));
			 System.out.println(isCollectionNext(mybb[1])); 
			 System.out.println("xxxx"); 
			 System.out.println(isCollectionRetNone(mybb[1]));
			 System.out.println(isCollectionNext(mybb[0]));*/
			 if((isCollectionNext(mybb[0]) && isCollectionRetNone(mybb[0])) ||
					 (isCollectionNext(mybb[1]) && isCollectionRetNone(mybb[1])))
				 	// System.out.println("fuck");
					 retVal = true;
		 }
		 return retVal;
	 }
	 
	 public static boolean isCollectionNext(ISSABasicBlock ibb){
		 boolean retVal = false;
		 SSAInstruction ssa_tmp = null;
		 int i = 0;
		 for(SSAInstruction ssa: ibb){
			ssa_tmp = ssa;
			i++;
		 }
		 if(i == 1){
			 if(ssa_tmp instanceof SSAInvokeInstruction){
				 if(((SSAInvokeInstruction) ssa_tmp).getDeclaredTarget().getDeclaringClass().getName().toString().equals("Ljava/util/Iterator") &&
						 ((SSAInvokeInstruction) ssa_tmp).getDeclaredTarget().getName().toString().equals("next") &&
						 ((SSAInvokeInstruction) ssa_tmp).getDeclaredTarget().getReturnType().getName().toString().equals("Ljava/lang/Object") &&
						 ((SSAInvokeInstruction) ssa_tmp).isDispatch())
					 retVal = true;
				 //System.out.println("sbbbbbb" + ssa_tmp.toString());
				 //5 v7 = invokeinterface < Application, Ljava/util/Iterator, hasNext()Z > v5 @9 exception:v6
				 //System.out.println(((SSAInvokeInstruction) ssa_tmp).getDeclaredTarget().getName().toString());
				 //print hasNext
				 //System.out.println(((SSAInvokeInstruction) ssa_tmp).getDeclaredTarget().getReturnType().getName().toString());
				 //System.out.println(((SSAInvokeInstruction) ssa_tmp).getDeclaredTarget().getDeclaringClass().getName().toString());
				 //print Ljava/util/Iterator
				 //System.out.println(((SSAInvokeInstruction) ssa_tmp).getDeclaredTarget().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application));
				 //print Application
			 }
		 }
		 return retVal;
	 }
	 
	 public static boolean isCollectionRetNone(ISSABasicBlock ibb){
		 boolean retVal = false;
		 SSAInstruction ssa_tmp = null;
		 int i = 0;
		 for(SSAInstruction ssa: ibb){
			ssa_tmp = ssa;
			i++;
		 }
		 if(i == 1){
			 if(ssa_tmp instanceof SSAReturnInstruction){
				  System.out.println(ssa_tmp.toString());
				  if(((SSAReturnInstruction) ssa_tmp).returnsVoid())
					  retVal = true;
			 }
		 }
		 return retVal;
	 }
	 
	 public static SSAInstruction isCollectionPred(ISSABasicBlock ibb,SSACFG cfg){
		 SSAInstruction retInst = null;
		 boolean retVal1 = false;
		 boolean retVal2 = false;
		 boolean retVal3 = false;
		 int i1 = 0;
		 SSAInstruction ssa_tmp1 = null;
		 ISSABasicBlock ibb1 = null;
		 // BB4 5 v7 = invokeinterface < Application, Ljava/util/Iterator, hasNext()Z > v5 @9 exception:v
		 if(cfg.getPredNodeCount(ibb) == 1){
			 Collection<ISSABasicBlock> pred = cfg.getNormalPredecessors(ibb);
			 for(ISSABasicBlock iiib : pred){
				 ibb1 = iiib;
				 for(SSAInstruction inst: iiib){
					 i1++;
					 ssa_tmp1 = inst;
				 }
			 }
		 }
		 if(i1 == 1){
			 if(ssa_tmp1 instanceof SSAInvokeInstruction){
				 if(((SSAInvokeInstruction) ssa_tmp1).getDeclaredTarget().getDeclaringClass().getName().toString().equals("Ljava/util/Iterator") &&
						 ((SSAInvokeInstruction) ssa_tmp1).getDeclaredTarget().getName().toString().equals("hasNext") &&
						 ((SSAInvokeInstruction) ssa_tmp1).getDeclaredTarget().getReturnType().getName().toString().equals("Z") &&
						 ((SSAInvokeInstruction) ssa_tmp1).isDispatch()){
					// System.out.println("i am here 1");
					 retVal1 = true;
				}
			}
		 }
		 //System.out.println(ibb1.toString());
		 //System.out.println(cfg.getPredNodeCount(ibb1));
		 SSAInstruction ssa_tmp2 = null;
		 ISSABasicBlock ibb2 = null;
		 // BB3 empty
		 ibb2 = isCollectionImmediataPred(ibb1,cfg);
		 if(retVal1){
			// System.out.println("i am here 2");
			 retVal2 = true;
		 }
		 System.out.println(ibb2.toString());
		 // BB2 2 v5 = invokevirtual < Application, Ljava/util/HashSet, iterator()Ljava/util/Iterator; > v3 @4 exception:v4
		 int i3 = 0;
		 SSAInstruction ssa_tmp3 = null;
		 ISSABasicBlock ibb3 = null;
		 if(cfg.getPredNodeCount(ibb2) == 1){
			 Collection<ISSABasicBlock> pred2 = cfg.getNormalPredecessors(ibb2);
			 for(ISSABasicBlock iiib : pred2){
				 ibb3 = iiib;
				 for(SSAInstruction inst: iiib){
					 i3++;
					 ssa_tmp3 = inst;
				 }
			 }
		 }
		 if(retVal2 && i3 == 1){
			 if(ssa_tmp3 instanceof SSAInvokeInstruction){
				 if(((SSAInvokeInstruction) ssa_tmp3).getDeclaredTarget().getDeclaringClass().getName().toString().contains("Ljava/util") &&
						 ((SSAInvokeInstruction) ssa_tmp3).getDeclaredTarget().getName().toString().equals("iterator") &&
						 ((SSAInvokeInstruction) ssa_tmp3).getDeclaredTarget().getReturnType().getName().toString().equals("Ljava/util/Iterator") &&
						 ((SSAInvokeInstruction) ssa_tmp3).isDispatch()){
					// System.out.println("i am here 3");
					 retVal3 = true;
				}
			}
		 }
		 System.out.println(ibb3.toString());
		 // BB1 1 v3 = getfield < Application, LTestCase, mySet, <Application,Ljava/util/HashSet> > v1
		 int i4 = 0;
		 SSAInstruction ssa_tmp4 = null;
		 ISSABasicBlock ibb4;
		 if(cfg.getPredNodeCount(ibb3) == 1){
			 Collection<ISSABasicBlock> pred3 = cfg.getNormalPredecessors(ibb3);
			 for(ISSABasicBlock iiib : pred3){
				 ibb4 = iiib;
				 for(SSAInstruction inst: iiib){
					 i4++;
					 ssa_tmp4 = inst;
				 }
			 }
		 }
		 if(retVal3 && i4 == 1){
			 if(ssa_tmp4 instanceof SSAFieldAccessInstruction){
				 //System.out.println("i am here 4");
				// System.out.println(ssa_tmp4.toString());
				 retInst = ssa_tmp4;
			 }
		 }
		 return retInst;
	 }
	 
	 public static ISSABasicBlock isCollectionImmediataPred(ISSABasicBlock bb, SSACFG cfg){
		ISSABasicBlock retBB = null;
		ISSABasicBlock[] mybb = new ISSABasicBlock[2];
		int i = 0;
		if(cfg.getPredNodeCount(bb) == 2){
			for(ISSABasicBlock ibb : cfg.getNormalPredecessors(bb)){
				mybb[i++] = ibb;
			}
			/*System.out.println(bb.toString());
			System.out.println(mybb[0].toString());
			System.out.println(mybb[1].toString());
			System.out.println(isEmptyBB(mybb[0]));
			System.out.println(isEmptyBB(mybb[1]));*/
			if((isEmptyBB(mybb[0]) && isGotoBB(mybb[1])) || (isEmptyBB(mybb[1]) && isGotoBB(mybb[0])))
				retBB = isEmptyBB(mybb[0]) ? mybb[0] : mybb[1];
		} 
		return retBB;
		 
	 }

	 public static boolean isEmptyBB(ISSABasicBlock bb){
		 /*System.out.println(bb.getFirstInstructionIndex());
		 System.out.println(bb.getLastInstructionIndex());
		 System.out.println(bb.getLastInstruction() == null);*/
		 if(bb.getFirstInstructionIndex() == bb.getLastInstructionIndex()){
			 if(bb.getLastInstruction() == null)
				 return true;
		 }
		 return false;
	 }
	 
	 public static boolean isGotoBB(ISSABasicBlock bb){
		 if(bb.getFirstInstructionIndex() == bb.getLastInstructionIndex()){
			 if(bb.getLastInstruction() instanceof SSAGotoInstruction)
				 return true;
		 }
		 return false;
	 }
	 
	 public static boolean isBounded(ProcessUnit unit){
		 unit.run();
		 System.out.println("===============================================================");
		 if(unit.strongBounded){
			 System.out.println("it's strongly bounded");
			 return true;
		 }
		 if(unit.level > 20){
			 System.out.println("exceed the threshold");
			 return false;
		 }
		 LinkedList<ProcessUnit> work_set = new LinkedList<ProcessUnit>();
		 Map<MyPair,CGNode> newlyUnit = unit.newlyCGN;
		 unit.printNewlyCGN();
		 Iterator<Map.Entry<MyPair,CGNode>> entries = newlyUnit.entrySet().iterator();
		 int next_level = unit.level++;
		 while(entries.hasNext()){
			 boolean isBranch = false;
			 Map.Entry<MyPair,CGNode> entry = entries.next();
			 SSAInstruction ssa_tmp = entry.getKey().getL();
			 int int_tmp = entry.getKey().getR();
			 CGNode cgn_tmp = entry.getValue();
			 if(ssa_tmp instanceof SSAConditionalBranchInstruction)
				 isBranch = true;
			 ProcessUnit new_unit = new ProcessUnit(unit.cg,cgn_tmp,ssa_tmp,isBranch,next_level,int_tmp);
			 work_set.add(new_unit);
		}
		if(work_set.size() == 0)
			return unit.isBounded;
		else{
			boolean retVal = true;
			for(ProcessUnit x : work_set){
				retVal &= isBounded(x);
			}
			return retVal;
		}
	}
}
