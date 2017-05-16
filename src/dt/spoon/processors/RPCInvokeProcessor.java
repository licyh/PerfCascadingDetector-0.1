package dt.spoon.processors;

import java.nio.file.Path;
import java.nio.file.Paths;

import dt.spoon.MySpoon;
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
public class RPCInvokeProcessor extends AbstractProcessor<CtInvocation> {
	
	Path bugConfigDirPath;
	Checker scopeChecker = null;
	Checker checker = null;
	
	
	public RPCInvokeProcessor(String bugConfigDir) {
		this( Paths.get(bugConfigDir) );
	}
	
	public RPCInvokeProcessor(Path bugConfigDirPath) {
		this.bugConfigDirPath = bugConfigDirPath;
		this.scopeChecker = new CommonChecker( this.bugConfigDirPath.resolve("scope.txt").toString() );
		this.checker = new RPCChecker( this.bugConfigDirPath.resolve("rpc.txt").toString() );
	}
		
	
	public void process (CtInvocation invoke) {
		
		CtExecutableReference executable = invoke.getExecutable();
		String invokesig = executable.getDeclaringType().getQualifiedName() + "." + executable.getSimpleName();
		String invokeclass = executable.getDeclaringType().getQualifiedName();
		String invokemethod = executable.getSimpleName();
		
		
		if (checker != null && !checker.isTarget(invokesig)) return;


        CtMethod method = Util.getMethod(invoke);
        if (method != null) {
        	String methodsig = Util.getMethodSig(method);
        	if (scopeChecker != null && !scopeChecker.isTarget(methodsig)) 
        		return;
        }
        
		//if (invokeclass.equals("java.lang.Object")) return;
        
        CtElement element = (CtElement)invoke;
        if ( !Util.isInBlock(element) )     //maybe inside a Field
        	return;
		
		
        String pos = invoke.getPosition().toString();
        System.out.println("JX - INFO - checked RPC: " + invokesig + "_" + pos);
        
		// Main work
		while ( !(element.getParent() instanceof CtBlock) ) {
			//System.out.println("JX - CtElement: " + element.getParent());
			element = element.getParent();
		}
		CtStatement statement = (CtStatement)element;
		if ( Util.canInsertBefore(statement) ) {
			statement.insertBefore( Util.getCodeSnippetStatement(this, codeStr(invokesig,pos)) );
		}
		else {
			statement.insertAfter( Util.getCodeSnippetStatement(this, codeStr(invokesig,pos)) );
		}
		
		++ MySpoon.rpccount;
	}
	
  	
  	public String codeStr(String invokesig, String pos) {
  		String codestr = "";
		//codestr = "_DM_Log.log_LoopPrint( \"loop_\" + " + loopindex + " + \"_\" + loop" + loopindex + ");";
		codestr = "LogClass._DM_Log.log_RPC("        // jx - looks like _DM_Log.log_LoopPrint("xx.xx.xx.yy_loop?_"+loop?); 
				+ "\"" + invokesig+"_"+pos + "\""
				+ ");";
  		return codestr;
  	}
}


