package sa;

import java.nio.file.Paths;

import com.system.Timer;

import sa.lockloop.LLAnalysis;
import sa.wala.WalaAnalyzer;

public class StaticAnalysis {

	//WalaAnalyzer walaAnalyzer;
	String projectDir;
	String jarsDir;
	
	
	public static void main(String[] args) {
		new StaticAnalysis(args);
	}
	
	/**
	 * @param args
	 * 	args[0] - projectDir, eg, ${workspace_loc}/JXCascading-detector
	 * 	args[1] - jarsDir, eg, ${workspace_loc}/JXCascading-detector/src/sa/res/ha-4584
	 */
	public StaticAnalysis(String[] args) {
		projectDir = args[0];
		jarsDir = args[1];
		doWork();
	}
	
	
	public void doWork() {
		System.out.println("JX - INFO - StaticAnalysis.doWork");
		
    	Timer timer = new Timer( Paths.get(projectDir, "src/da/output/wala-timer.txt") );
    	timer.tic("WalaAnalyzer begin");
		WalaAnalyzer walaAnalyzer = new WalaAnalyzer(jarsDir);
		timer.toc("WalaAnalyzer end");
		
		LLAnalysis jxLocks = new LLAnalysis(walaAnalyzer, projectDir);
	}
	
	
}
