package dt.spoon.processors;

import com.TextFileReader;

import dt.spoon.checkers.Checker;
import dt.spoon.checkers.CommonChecker;
import dt.spoon.util.Util;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCFlowBreak;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.filter.ReturnOrThrowFilter;


/**
 * Insert Loops
 * @author xincafe
 */
public class MethodProcessor extends AbstractProcessor<CtMethod> {
	
	Checker checker = null;
	
	public MethodProcessor() {
		this.checker = new CommonChecker( "src/dt/spoon/res/mr-4813/scope.txt" );
	}
	
	
	public void process(CtMethod method) {
		String methodsig = method.getDeclaringType().getQualifiedName()
							+ "." + method.getSimpleName();
		//String methodsig = method.getSignature();   //looks like "java.lang.String[] getFileVisibilities(org.apache.hadoop.conf.Configuration)" 
		
		//System.out.println("JX - DEBUG - methodsig: " + methodsig);
		if (checker != null && !checker.isTarget(methodsig))
			return;
		//System.out.println("JX - DEBUG - methodsig: " + methodsig);
		
		// Insert for all loops inside the method
		int count = -1;
		for ( CtLoop loop: method.getElements(new LoopFilter()) ) {
			// Check if a normal loop that has loop body
			CtBlock<?> bodyblock = (CtBlock<?>) loop.getBody();
			if (bodyblock == null) continue;   	 //like "while (x<= 0 && next());" or "for (xx);"
			++count;
			System.out.println( "JX - INFO - checked loop - " + loop.getPosition().toString() );
			
			// Before Loop
			loop.insertBefore( Util.getCodeSnippetStatement(this, codeStr(1,methodsig,count)) );

			// Inside Loop
			bodyblock.insertBegin( Util.getCodeSnippetStatement(this, codeStr(2,methodsig,count)) );
			for ( CtCFlowBreak cflowbreak: loop.getElements(new ReturnOrThrowFilter()) ) {   //insert before "Return" and "Throw"
				cflowbreak.insertBefore( Util.getCodeSnippetStatement(this, codeStr(3,methodsig,count)) );
			}
			
			// After Loop			
			loop.insertAfter( Util.getCodeSnippetStatement(this, codeStr(3,methodsig,count)) );
		}
		
		// insert at the end of method. jx - no need for now
		/*
		CtBlock methodblock = method.getBody();
		methodblock.insertEnd( getCodeSnippetStatement("xxxx")  );
		*/
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
  	public String codeStr(int flag, String methodsig, int loopindex) {
  		String codestr = "";
  		if (flag == 1) {
  			codestr = "int " + "loop" + loopindex + " = 0;";
  			codestr += "LogClass._DM_Log.log_LoopBegin("     // jx - looks like _DM_Log.log_LoopBegin("xx.xx.xx.yy_loop?"); 
 					 + "\"" + methodsig+"_loop"+loopindex + "\""
 					 + ");"; 
  		}
  		else if (flag == 2) {
  			codestr = "loop" + loopindex + "++;";  			
  			codestr += "LogClass._DM_Log.log_LoopCenter("     // jx - looks like _DM_Log.log_LoopPrint("xx.xx.xx.yy_loop?"); 
  					 + "\"" + methodsig+"_loop"+loopindex + "\""
  					 + ");";
  		}
  		else if (flag == 3) {
  			//codestr = "_DM_Log.log_LoopPrint( \"loop_\" + " + loopindex + " + \"_\" + loop" + loopindex + ");";
  			codestr = "LogClass._DM_Log.log_LoopEnd("        // jx - looks like _DM_Log.log_LoopPrint("xx.xx.xx.yy_loop?_"+loop?); 
  					+ "\"" + methodsig+"_loop"+loopindex+"_" + "\"" + "+" + "loop" + loopindex
  					+ ");";
  		}
  		return codestr;
  	}
  	
}



class LoopFilter implements Filter<CtLoop> {
	public boolean matches(CtLoop loop) {
		return ((loop instanceof CtFor) 
				|| (loop instanceof CtWhile)
				|| (loop instanceof CtDo)
				|| (loop instanceof CtForEach)
				);
	}
}


