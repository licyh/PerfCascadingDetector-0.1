package dt.spoon.processors;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCFlowBreak;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtLoop;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.ReturnOrThrowFilter;
import spoon.support.reflect.code.CtCommentImpl;
import spoon.support.reflect.code.CtLocalVariableImpl;
import spoon.Launcher;

public class LoopProcessor extends AbstractProcessor<CtLoop> {
	public void process(CtLoop element) {
		//CtCommentImpl comm = new CtCommentImpl();
		//comm.setContent("haha, i am JX");
		CtCodeSnippetStatement statementInConstructor 
			= getFactory().Code().createCodeSnippetStatement("int a = 1");
		
		//element.insertBefore( statementInConstructor );
    }
}





