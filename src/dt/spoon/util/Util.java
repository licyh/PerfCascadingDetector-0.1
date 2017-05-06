package dt.spoon.util;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

public class Util {

	
	public static String getMethodSig(CtMethod method) {
		String methodsig = method.getDeclaringType().getQualifiedName() + "." + method.getSimpleName();
		return methodsig;
	}
	
	public static CtMethod getMethod(CtElement ele) {
	    while ( !(ele instanceof CtMethod) ) 
	    	ele = ele.getParent();
	    CtMethod method = (CtMethod)ele;
		return method;
	}
	
  	public static CtCodeSnippetStatement getCodeSnippetStatement(AbstractProcessor processor, String codesnippet) {
  		if ( codesnippet.endsWith(";") )
  			codesnippet = codesnippet.substring(0, codesnippet.length()-1);
		CtCodeSnippetStatement statement
			= processor.getFactory().Code().createCodeSnippetStatement( codesnippet );
		return statement;
	}
}
