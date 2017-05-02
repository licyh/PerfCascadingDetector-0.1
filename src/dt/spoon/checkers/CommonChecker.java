package dt.spoon.checkers;

import com.TextFileReader;


public class CommonChecker implements Checker {

	TextFileReader reader;
	
	public CommonChecker(String filename) {
		this.reader = new TextFileReader( filename );
		this.reader.readFile();
	}
	
	@Override
	public boolean isTarget(String sig) {
		for (String str: reader.strs) {
			if (sig.startsWith(str))
				return true;
		}
		return false;
	}
	
}