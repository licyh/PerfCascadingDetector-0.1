package da.cascading;

import da.cascading.core.Sink;
import da.cascading.core.QueueCase;
import da.graph.AccidentalHBGraph;
import da.graph.HappensBeforeGraph;
import da.graph.LogInfoExtractor;
import da.tagging.JobTagger;

public class CascadingAnalyzer {

	String projectDir;
	HappensBeforeGraph hbg;
	AccidentalHBGraph ag;
	
	JobTagger jt;
	
	
	public CascadingAnalyzer(String projectDir, HappensBeforeGraph hbg, AccidentalHBGraph ag) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		this.ag = ag;
		//more
		this.jt = new JobTagger(this.hbg);
	}
	
	public void doWork() {
		handleSinks();
	}
	
	
	
	/*********************************************************************************
	 * Core
	 *********************************************************************************/
	
	public void handleSinks() {
		new BugPool(this.projectDir, this.hbg).clearOutput();
		
		for (Sink sink: this.ag.getLogInfoExtractor().getSinks()) {
			//for DEBUG
			if (!sink.getID().equals(2)) continue;
			
			sink.setEnv(this.projectDir, this.hbg, this.ag, this.ag.getLogInfoExtractor(), this.jt);
			sink.doWork();
			//for DEBUG
			//break;
		}
	}
	
}
