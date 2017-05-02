package dt.spoon.util;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCodeSnippetStatement;

public class Util {

  	public static CtCodeSnippetStatement getCodeSnippetStatement(AbstractProcessor processor, String codesnippet) {
  		if ( codesnippet.endsWith(";") )
  			codesnippet = codesnippet.substring(0, codesnippet.length()-1);
		CtCodeSnippetStatement statement
			= processor.getFactory().Code().createCodeSnippetStatement( codesnippet );
		return statement;
	}
}
