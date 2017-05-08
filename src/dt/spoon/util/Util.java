package dt.spoon.util;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

public class Util {

	
	
	
	public static boolean isInBlock(CtElement element) {
		while ( (element != null) && !(element instanceof CtBlock) ) {
			element = element.getParent();
		}
		if (element != null)
			return true;
		return false;
	}
	
	
	public static String getMethodSig(CtMethod method) {
		String methodsig = method.getDeclaringType().getQualifiedName() + "." + method.getSimpleName();
		return methodsig;
	}
	
	public static CtMethod getMethod(CtElement element) {
	    while ( (element != null) && !(element instanceof CtMethod) ) 
	    	element = element.getParent();
        if ( element != null ) {
	        CtMethod method = (CtMethod)element;
		    return method;
        }
        return null;
	}
	
  	public static CtCodeSnippetStatement getCodeSnippetStatement(AbstractProcessor processor, String codesnippet) {
  		if ( codesnippet.endsWith(";") )
  			codesnippet = codesnippet.substring(0, codesnippet.length()-1);
		CtCodeSnippetStatement statement
			= processor.getFactory().Code().createCodeSnippetStatement( codesnippet );
		return statement;
	}
}
