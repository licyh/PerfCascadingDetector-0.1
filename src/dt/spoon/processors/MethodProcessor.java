package dt.spoon.processors;


import dt.spoon.checkers.Checker;
import dt.spoon.checkers.CommonChecker;

import spoon.processing.AbstractProcessor;

import spoon.reflect.declaration.CtMethod;



public class MethodProcessor extends AbstractProcessor<CtMethod> {
	
	Checker checker = null;
	
	public MethodProcessor() {
		this.checker = new CommonChecker( "src/dt/spoon/res/mr-4813/scope.txt" );
	}
	
	
	public void process(CtMethod method) {
		String methodsig = method.getDeclaringType().getQualifiedName()
							+ "." + method.getSimpleName();
		//String methodsig = method.getSignature();   //looks like "java.lang.String[] getFileVisibilities(org.apache.hadoop.conf.Configuration)" 
		
		if (checker != null && !checker.isTarget(methodsig))
			return;
		//System.out.println("JX - DEBUG - methodsig: " + methodsig);
		
		// insert at the end of method. 
		/*
		CtBlock methodblock = method.getBody();
		methodblock.insertEnd( getCodeSnippetStatement("xxxx")  );
		*/
    }

  
  	
}


