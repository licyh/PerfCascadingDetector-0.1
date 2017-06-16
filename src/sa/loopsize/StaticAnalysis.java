package sa.loopsize;

import sa.wala.WalaAnalyzer;

public class StaticAnalysis {
	
	WalaAnalyzer walaAnalyzer;
	
	public StaticAnalysis() {
		this.walaAnalyzer = new WalaAnalyzer("src/sa/res/ha-4584");
		
	}
	
	
	public static void main(String[] args) {
		new StaticAnalysis();
	}
	
}



