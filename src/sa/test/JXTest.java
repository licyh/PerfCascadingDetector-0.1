package sa.test;

import com.ibm.wala.util.WalaException;

import sa.wala.WalaAnalyzer;

public class JXTest {

	public static void main(String[] args) {
		try {
			doWork();
		} catch (WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void doWork() throws WalaException {
		WalaAnalyzer wala = new WalaAnalyzer("bin/sa/test/testsrc/");
		wala.testIR();
	}
	
}
