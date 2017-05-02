package dt.spoon.test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;



public class Outer {
	int a = 0;
	int b = 0;
	
	public void methodA() {
		File file = new File("xx");
		Path path = Paths.get("yy", "fffff");
		
	}
	
	public void methodB() {
		int k = 0;
	}
}
