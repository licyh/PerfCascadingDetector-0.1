package com.benchmark;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.TextFileReader;


public class Checker {

	// line by line
	List<String> targetStrs = new ArrayList<String>();
	// split line by split line 
	List<String[]> splitTargetStrs = new ArrayList<String[]>();
	
	
	public Checker() {
	}
	
	
	public void addCheckLine(String line) {
		if (line.trim().length() == 0) return;
		targetStrs.add( line.trim() );
		String[] tmpstrs = line.trim().split("\\s+");
		splitTargetStrs.add( tmpstrs );
	}
	
	
	public void addCheckFile(String filename) {
		addCheckFile( Paths.get(filename) );
	}
	
	public void addCheckFile(Path filepath) {
		TextFileReader reader = new TextFileReader( filepath );
		reader.readFile();
		targetStrs.addAll( reader.strs );
		splitTargetStrs.addAll( reader.splitstrs );
	}

	
	/**
	 * Way of checking - startsWith
	 */
	public boolean isTarget(String sig) {
		for (String str: targetStrs) {
			if (sig.startsWith(str))
				return true;
		}
		return false;
	}
	

	/**
	 * Way of checking - equals
	 */
	public boolean isSplitTarget(String sig1) {
		for (String[] strs: splitTargetStrs) {
			if (sig1.equals(strs[0]))
				return true;
		}
		return false;
	}
	
	public boolean isSplitTarget(String sig1, String sig2) {
		for (String[] strs: splitTargetStrs) {
			if (sig1.equals(strs[0]) && sig2.equals(strs[1]))
				return true;
		}
		return false;
	}
	
	public boolean isSplitTarget(String sig1, String sig2, String sig3) {
		for (String[] strs: splitTargetStrs) {
			if (sig1.equals(strs[0]) && sig2.equals(strs[1]) && sig3.equals(strs[2]))
				return true;
		}
		return false;
	}
	
	// isSplitTarget_n(str)
	public boolean isSplitTarget_0(String sig) {  // same as isSplitTarget(argu1)
		return isSplitTarget(sig);
	}
	
	public boolean isSplitTarget_1(String sig) {
		for (String[] strs: splitTargetStrs) {
			if (sig.equals(strs[1]))
				return true;
		}
		return false;
	}
	
	public boolean isSplitTarget_2(String sig) {
		for (String[] strs: splitTargetStrs) {
			if (sig.equals(strs[2]))
				return true;
		}
		return false;
	}
	
	// Note: int f1, int f2, int f3 should be 0 or 1 or 2 or n-1, NOT 1, 2, .. n-1
	public boolean isSplitTarget(int f1, String sig1, int f2, String sig2) {
		for (String[] strs: splitTargetStrs) {
			if (sig1.equals(strs[f1]) && sig2.equals(strs[f2]))
				return true;
		}
		return false;
	}
	
	public boolean isSplitTarget(int f1, String sig1, int f2, String sig2, int f3, String sig3) {
		for (String[] strs: splitTargetStrs) {
			if (sig1.equals(strs[f1]) && sig2.equals(strs[f2]) && sig2.equals(strs[f3]))
				return true;
		}
		return false;
	}
	
}