package dt.spoon;


public class CatchProcessor extends spoon.processing.AbstractProcessor<spoon.reflect.code.CtCatch> {

	
	public void process(spoon.reflect.code.CtCatch element) {
        if ((element.getBody().getStatements().size()) == 0) {
            getFactory().getEnvironment().report(this, org.apache.log4j.Level.WARN, element, "empty catch clause");
        }
    }
	
	
	
}

