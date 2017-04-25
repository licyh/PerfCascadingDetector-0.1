package dt.spoon.processors;

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

public class MethodProcessor extends AbstractProcessor<CtMethod> {
	public void process(CtMethod method) {
		String methodsig = method.getDeclaringType().getQualifiedName()
				+ "." + method.getSimpleName()
				+ method.getSignature(); 
		
		// insert for all loops inside the method
		int count = -1;
		for ( CtLoop loop: method.getElements(new LoopFilter()) ) {
			CtBlock<?> bodyblock = (CtBlock<?>) loop.getBody();
			if (bodyblock == null) continue;   	 //like "while (x<= 0 && next());" or "for (xx);"
				
			++count;
			System.out.println( loop.getPosition().toString() );
			
			// before loop
			loop.insertBefore( getCodeSnippetStatement( codeStr(1,methodsig,count) ) );

			// inside loop
			bodyblock.insertBegin( getCodeSnippetStatement( codeStr(2,methodsig,count) ) );
			for ( CtCFlowBreak cflowbreak: loop.getElements(new ReturnOrThrowFilter()) ) {   //insert before "Return" and "Throw"
				cflowbreak.insertBefore( getCodeSnippetStatement( codeStr(3,methodsig,count) ) );
			}
			
			// after loop			
			loop.insertAfter( getCodeSnippetStatement( codeStr(3,methodsig,count) ) );
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
  	
  	
  	public CtCodeSnippetStatement getCodeSnippetStatement(String codesnippet) {
  		if ( codesnippet.endsWith(";") )
  			codesnippet = codesnippet.substring(0, codesnippet.length()-1);
		CtCodeSnippetStatement statement
			= getFactory().Code().createCodeSnippetStatement( codesnippet );
		return statement;
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