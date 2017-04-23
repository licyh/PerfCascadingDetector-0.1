package dt.spoon.processors;

import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtLoop;
import spoon.support.reflect.code.CtCommentImpl;
import spoon.processing.AbstractProcessor;


public class CatchProcessor extends AbstractProcessor<CtCatch> {
	public void process(CtCatch element) {
        if ((element.getBody().getStatements().size()) == 0) {
            getFactory().getEnvironment().report(this, org.apache.log4j.Level.WARN, element, "empty catch clause");
            
            //element.
        }
    }
}







