package da.cascading;

import da.cascading.core.Sink;
import da.cascading.core.QueueCase;
import da.graph.AccidentalHBGraph;
import da.graph.HappensBeforeGraph;
import da.graph.LogInfoExtractor;

public class CascadingAnalyzer {

	String projectDir;
	HappensBeforeGraph hbg;
	AccidentalHBGraph ag;
	
	
	public CascadingAnalyzer(String projectDir, HappensBeforeGraph hbg, AccidentalHBGraph ag) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		this.ag = ag;
	}
	
	public void doWork() {
		xxx();
	}
	
	
	
	/*********************************************************************************
	 * Core
	 *********************************************************************************/
	
	public void xxx() {
		new BugPool(this.projectDir, this.hbg).clearOutput();
		
		for (Sink sink: this.ag.getLogInfoExtractor().getSinks()) {
			sink.setEnv(this.projectDir, this.hbg, this.ag, this.ag.getLogInfoExtractor());
			sink.doWork();
			//for DEBUG
			break;
		}
		
	    //Queue
	    //QueueCase queueCase = new QueueCase(this.projectDir, this.hbg, this.ag.getLogInfoExtractor());
	    //queueCase.doWork();
	}
	
}
