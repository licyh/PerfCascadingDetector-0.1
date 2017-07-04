package sa.loopsize;

import sa.wala.WalaAnalyzer;
import sun.misc.Queue;

import java.beans.Statement;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.shrikeBT.*;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.text.TextFileWriter;

public class StaticAnalysis {
	
	WalaAnalyzer walaAnalyzer;
    ClassHierarchy cha;
    CallGraph cg;
    CGNode cNode;
    SDG sdg;
    PDG pdg;
    
	public StaticAnalysis() {
		this.walaAnalyzer = new WalaAnalyzer("src/cyx");
		this.cha = this.walaAnalyzer.getClassHierarchy();
		this.cg = this.walaAnalyzer.getCallGraph();
		this.sdg = this.walaAnalyzer.getSDG();
	}
	
	// input: loop path 
	// path info should be like org.apache.hadoop.hdfs.server.datanode.FSDataset$FSVolumeSet.getBlockInfo-679
	// return List<SSAInstruction>
	public List<SSAInstruction> getLoopVariable(String loopPath){
		String[] splitPath = loopPath.split("-");
		assert splitPath.length == 2;
		/*for(String partStr: splitPath)
			System.out.println(partStr);*/
		String methPath = splitPath[0];
		int loopLine = Integer.parseInt(splitPath[1]);
		CallGraph cg = this.walaAnalyzer.getCallGraph();
		IR ir = null;
		IMethod im = null;
		List<SSAInstruction> loopSSA = new ArrayList<SSAInstruction>();
    	for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
    		CGNode f = it.next();
	      	if ( LoopVarUtil.isApplicationMethod(f) ) {
	      		if ( !LoopVarUtil.isNativeMethod(f)){
	      			im = f.getMethod();
	      			if(im.getSignature().indexOf(methPath)>=0){
	      				ir = f.getIR();
	      				//For test, generate CFG 
	      				//System.out.println(f.getIR().toString());
	      				try{
	      					LoopVarUtil.generateViewIR(f.getIR(), cha);
	      				}catch(WalaException e){
	      					e.printStackTrace();
	      				}
	      			    SSACFG cfg = ir.getControlFlowGraph();
	      			    for (Iterator<ISSABasicBlock> cfg_it = cfg.iterator(); cfg_it.hasNext(); ) {
	      			    	ISSABasicBlock bb = cfg_it.next();
	      			    	for(Iterator<SSAInstruction> bb_it = bb.iterator(); bb_it.hasNext();){
	      			    		SSAInstruction ssaInst = bb_it.next();
	      			    		int lineOfSSA = 0;
	      			    		lineOfSSA = LoopVarUtil.getSourceLineNumberFromSSA(ssaInst, ir);
	      			    		//assert lineOfSSA != -1;
	      			    		if(lineOfSSA == loopLine){
	      			    			//System.out.println("bingo:" + ssaInst.toString());
	      			    			//System.out.println(SSAUtil.fieldOfWhichClass((SSAFieldAccessInstruction)ssaInst));	      			    			SSAUtil.fieldOfWhichClass((SSAFieldAccessInstruction)ssaInst);
	      			    			//System.out.println(SSAUtil.fieldOfWhichVar((SSAFieldAccessInstruction)ssaInst));
	      			    			loopSSA.add(ssaInst);
	      			    			if(this.cNode == null)
	      			    				this.cNode = f;
	      			    		}
	      			    	}
	      			    }
	      			}
	      		}
	      	}
    	}
    	if(loopSSA.size() == 0)
    		throw new RuntimeException("please check input for locating loop");
		return loopSSA;
	}
	
	public void printAllIR(){
		CallGraph cg = this.walaAnalyzer.getCallGraph();
		TextFileWriter x = new TextFileWriter("/home/nemo/outFig/ir.txt");
		IR ir = null;
		IMethod im = null;
		List<SSAInstruction> loopSSA = new ArrayList<SSAInstruction>();
    	for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
    		CGNode f = it.next();
	      	if ( LoopVarUtil.isApplicationMethod(f) ) {
	      		if ( !LoopVarUtil.isNativeMethod(f)){
	      			im = f.getMethod();
	      			ir = f.getIR();
	      			x.writeLine(ir.toString());
	      		}
	      	}
    	}
    	x.close();
	}
	
	public SSAInstruction getCondVar(List<SSAInstruction> loopSSA){
		int condNum = 0;
		SSAInstruction myInst = null;
		for(SSAInstruction ssaInst:loopSSA){
			if (ssaInst instanceof  SSAConditionalBranchInstruction ){
				if(((SSAConditionalBranchInstruction) ssaInst).getOperator().toString().equals("ge"))
					System.out.println("the operator is: !");
				System.out.println(((SSAConditionalBranchInstruction) ssaInst).getType().toString());
				//System.out.println(ssaInst.toString());
				//System.out.println(((SSAConditionalBranchInstruction) ssaInst).getOperator());
				//SSAUtil.testDef(ssaInst);
				//SSAUtil.testUse(ssaInst);
				condNum++;
				if(myInst == null)
					myInst = ssaInst;
			}
		}
		if(condNum == 1){
			System.out.println("potential cond var:" + myInst.toString());
			return myInst;
		}
		return myInst;
	}
		
	public static void main(String[] args) {
		List<SSAInstruction> loopSSAInst = new ArrayList<SSAInstruction>();
		StaticAnalysis sHelper = new StaticAnalysis();
		sHelper.printAllIR();
		loopSSAInst = sHelper.getLoopVariable("testFor6-95");
		SSAInstruction ssa = sHelper.getCondVar(loopSSAInst);
		assert ssa != null;
		int level = 0;
		LinkedList<ProcessUnit> processList = new LinkedList<ProcessUnit>();
		ProcessUnit unit = new ProcessUnit(sHelper.cg,sHelper.cNode,ssa,true,level,-1);
		System.out.println(SSAUtil.isBounded(unit));
	}
}



