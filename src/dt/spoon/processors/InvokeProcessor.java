package dt.spoon.processors;

import dt.spoon.checkers.Checker;
import dt.spoon.checkers.CommonChecker;
import dt.spoon.checkers.RPCChecker;
import dt.spoon.util.Util;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;


/**
 * Insert RPCs
 * For many kinds of invocations: ie, CtConstructorCall, CtInvocation, CtNewClass
 * @author xincafe
 */
public class InvokeProcessor extends AbstractProcessor<CtInvocation> {
	
	Checker scopeChecker = null;
	Checker checker = null;
	
	public InvokeProcessor() {
		this.scopeChecker = new CommonChecker( "src/dt/spoon/res/mr-4813/scope.txt" );
		this.checker = new RPCChecker( "src/dt/spoon/res/mr-4813/rpc.txt" );
	}
		
	
	public void process (CtInvocation invoke) {
		
		CtExecutableReference executable = invoke.getExecutable();
		String invokesig = executable.getDeclaringType().getQualifiedName() + "." + executable.getSimpleName();
		String invokeclass = executable.getDeclaringType().getQualifiedName();
		String invokemethod = executable.getSimpleName();
		
		
		if (checker != null && !checker.isTarget(invokesig)) return;


                CtMethod method = Util.getMethod(invoke);
                if (method != null) {
                        String methodsig = Util.getMethodSig(Util.getMethod(invoke));
                        if (scopeChecker != null && !scopeChecker.isTarget(methodsig)) 
                              return;
                }


		
		//if (invokeclass.equals("java.lang.Object")) return;
		
		//System.out.println("JX - INFO - checked RPC: " + invokesig);
        System.out.println("JX - INFO - checked RPC: " + invoke.getPosition().toString());
        
		// Main work
        CtElement element = (CtElement)invoke;
		//System.out.println("JX - CtElement: " + element);
		while ( !(element.getParent() instanceof CtBlock) ) {
			//System.out.println("JX - CtElement: " + element.getParent());
			element = element.getParent();
		}
		if (element instanceof CtStatement) {
			CtStatement statement = (CtStatement)element;
			statement.insertBefore( Util.getCodeSnippetStatement(this, codeStr(invokesig)) );
		}
	}
	
  	
  	public String codeStr(String sig) {
  		String codestr = "";
		//codestr = "_DM_Log.log_LoopPrint( \"loop_\" + " + loopindex + " + \"_\" + loop" + loopindex + ");";
		codestr = "LogClass._DM_Log.log_RPC("        // jx - looks like _DM_Log.log_LoopPrint("xx.xx.xx.yy_loop?_"+loop?); 
				+ "\"" + sig + "\""
				+ ");";
  		return codestr;
  	}
}


