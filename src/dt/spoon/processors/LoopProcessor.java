package dt.spoon.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCFlowBreak;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.filter.ReturnOrThrowFilter;
import spoon.support.reflect.code.CtCommentImpl;
import spoon.support.reflect.code.CtLocalVariableImpl;
import dt.spoon.MySpoon;
import dt.spoon.checkers.Checker;
import dt.spoon.checkers.CommonChecker;
import dt.spoon.util.Util;
import spoon.Launcher;

public class LoopProcessor extends AbstractProcessor<CtLoop> {
	
	Checker scopeChecker = null;
	
	public LoopProcessor() {
		this.scopeChecker = new CommonChecker( "src/dt/spoon/res/mr-4813/scope.txt" );
	}
	
	public void process(CtLoop loop) {
		
        CtMethod method = Util.getMethod(loop);
        if (method == null)
        	return;
        
        String methodsig = Util.getMethodSig(method);
        //if (scopeChecker != null && !scopeChecker.isTarget(methodsig)) 
        //	return;
		
		// Check if a normal loop that has loop body
		CtBlock<?> bodyblock = (CtBlock<?>) loop.getBody();
		if (bodyblock == null) return;   	 //like "while (x<= 0 && next());" or "for (xx);"
		
		// tmp filters - for "while (true) { return; } xxxxlog_loop_endxxxxx", couldn't insert after
		if (loop instanceof CtWhile || loop instanceof CtDo) {
			CtExpression<Boolean> expr = null;
			if (loop instanceof CtWhile)
				expr = ((CtWhile)loop).getLoopingExpression();
			else if (loop instanceof CtDo)
				expr = ((CtDo)loop).getLoopingExpression();
			if (expr instanceof CtLiteral) {
				if ( ((CtLiteral) expr).getValue().toString().equals("true") )
					return;
			}
		}
		
		
		String pos = loop.getPosition().toString();
		System.out.println( "JX - INFO - checked loop - " + loop.getPosition().toString() );
		
		// Before Loop
		loop.insertBefore( Util.getCodeSnippetStatement(this, codeStr(1,methodsig,MySpoon.loopcount,pos)) );

		// Inside Loop
		bodyblock.insertBegin( Util.getCodeSnippetStatement(this, codeStr(2,methodsig,MySpoon.loopcount,pos)) );
		for ( CtCFlowBreak cflowbreak: loop.getElements(new ReturnOrThrowFilter()) ) {   //insert before "Return" and "Throw"
			if (method == Util.getMethod(cflowbreak))  // for removing "method{Loop1 {  new Class(){method{ return;}}  }}"
				cflowbreak.insertBefore( Util.getCodeSnippetStatement(this, codeStr(3,methodsig,MySpoon.loopcount,pos)) );
		}
		
		// After Loop			
		loop.insertAfter( Util.getCodeSnippetStatement(this, codeStr(3,methodsig,MySpoon.loopcount,pos)) );
		
		++ MySpoon.loopcount;
    }
	
	
  	/**
  	 * Insert Flags:
  	 * 		1 - insert at method BEGIN - method.insertBefore
  	 * 		2 - insert at loop BEGIN - ie the 1st place inside loop body, NOT outside
  	 * 		3 - insert at method END - method.insertAfter
  	 * Arguments:
  	 * 		int flag - the location for the method 
  	 * 		int loopindex - loop index in the method
  	 * Note: 
  	 * 		the best way to write the below codes for insertion is to Print Out(eg, in JXTest) to see.
  	 */
  	public String codeStr(int flag, String methodsig, int loopindex, String pos) {
  		String codestr = "";
  		if (flag == 1) {
  			codestr = "int " + "loop" + loopindex + " = 0;";
  			codestr += "LogClass._DM_Log.log_LoopBegin("     // jx - looks like _DM_Log.log_LoopBegin("xx.xx.xx.yy_loop?"); 
 					 + "\"" + methodsig+"_loop"+loopindex +"_"+pos+ "\""
 					 + ");"; 
  		}
  		else if (flag == 2) {
  			codestr = "loop" + loopindex + "++;";  			
  			codestr += "LogClass._DM_Log.log_LoopCenter("     // jx - looks like _DM_Log.log_LoopPrint("xx.xx.xx.yy_loop?"); 
  					 + "\"" + methodsig+"_loop"+loopindex +"_"+pos+ "\""
  					 + ");";
  		}
  		else if (flag == 3) {
  			//codestr = "_DM_Log.log_LoopPrint( \"loop_\" + " + loopindex + " + \"_\" + loop" + loopindex + ");";
  			codestr = "LogClass._DM_Log.log_LoopEnd("        // jx - looks like _DM_Log.log_LoopPrint("xx.xx.xx.yy_loop?_"+loop?); 
  					+ "\"" + methodsig+"_loop"+loopindex +"_"+pos +"_" + "\"" + "+" + "loop" + loopindex
  					+ ");";
  		}
  		return codestr;
  	}
}


/*
class LoopFilter implements Filter<CtLoop> {
	public boolean matches(CtLoop loop) {
		return ((loop instanceof CtFor) 
				|| (loop instanceof CtWhile)
				|| (loop instanceof CtDo)
				|| (loop instanceof CtForEach)
				);
	}
}
*/

