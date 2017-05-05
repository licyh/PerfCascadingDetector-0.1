package dt.spoon.processors;

import dt.spoon.checkers.Checker;
import dt.spoon.checkers.CommonChecker;
import dt.spoon.util.Util;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.reference.CtExecutableReference;


/**
 * Insert IOs
 * For many kinds of invocations: ie, CtConstructorCall, CtInvocation, CtNewClass
 * @author xincafe
 */
public class AbsInvokeProcessor extends AbstractProcessor<CtAbstractInvocation> {
	
	Checker checker = null;
	
	public AbsInvokeProcessor() {
		this.checker = new CommonChecker( "src/dt/spoon/res/mr-4813/io.txt" );
	}
		
	/**
	 * for I/Os
	 */
	public void process (CtAbstractInvocation absinvoke) {
		
		CtExecutableReference executable = absinvoke.getExecutable();
		//String sig = executable.getSignature();   //like JX - 1 - java.io.File#File(java.lang.String) java.io.PrintStream#println(java.lang.String)
		String invokesig = executable.getDeclaringType().getQualifiedName() + "." + executable.getSimpleName();
		
		if (checker != null && !checker.isTarget(invokesig))
			return;
		System.out.println("JX - INFO - checked IO: " + invokesig);
		
		// Main work
		CtStatement statement = (CtStatement)absinvoke;
		statement.insertBefore( Util.getCodeSnippetStatement(this, codeStr(invokesig)) );
			
	}
	
  	
  	public String codeStr(String sig) {
  		String codestr = "";
		//codestr = "_DM_Log.log_LoopPrint( \"loop_\" + " + loopindex + " + \"_\" + loop" + loopindex + ");";
		codestr = "LogClass._DM_Log.log_IO("        // jx - looks like _DM_Log.log_LoopPrint("xx.xx.xx.yy_loop?_"+loop?); 
				+ "\"" + sig + "\""
				+ ");";
  		return codestr;
  	}
}


