package dt.spoon;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import dt.spoon.processors.AbsInvokeProcessor;
import dt.spoon.processors.InvokeProcessor;
import dt.spoon.processors.MethodProcessor;
import spoon.Launcher;

public class SpoonTest {

	public static void main(String[] args) throws Exception {
		
		System.out.println("JX - INFO - Under Spoon Testing !!!");
		SpoonUtil.guiSpoon( "src/dt/spoon/test/JXTest.java" );
		
	}

	
}
