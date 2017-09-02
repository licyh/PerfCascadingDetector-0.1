package com.text;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Checker {

	List<String> targetLines = new ArrayList<String>();						// line by line
	List<String[]> splitTargetLines = new ArrayList<String[]>();			// split line by split line 
	
	/**
	 * Functionality: 
	 * 		false - default. Just a checker.
	 * 		true  - should call "disableSuccessfullyCheckedLineFlag" first, means each target line could be successfully checked just once
	 * Note: 
	 * 		This is automatic and unrelated to Method "disableCurrentLine/disableLine". 
	 *      Please use "disableLine" if need customized disablement.
	 */
	boolean disableSuccessfullyCheckedLine = false;							// default: false; 
	Set<Integer> successfullyCheckedLines = new HashSet<Integer>();			// for targetLines/splitTargetLines. It works when true
	
	
	
	public Checker() {
	}
	
	
	public void addCheckLine(String line) {
		if (line.trim().length() == 0) return;
		targetLines.add( line.trim() );
		String[] tmpstrs = line.trim().split("\\s+");
		splitTargetLines.add( tmpstrs );
	}
	
	
	
	public void addCheckFile(String filename) {
		addCheckFile( Paths.get(filename) );
	}
	
	public void addCheckFile(Path filepath) {
		addCheckFile( filepath, false );
	}
	
	public void addCheckFile(String filename, boolean inJar) {
		addCheckFile( Paths.get(filename), inJar );
	}
	
	public void addCheckFile(Path filepath, boolean inJar) {
		TextFileReader reader = new TextFileReader( filepath, inJar );
		reader.readFile();
		targetLines.addAll( reader.strs );
		splitTargetLines.addAll( reader.splitstrs );
	}
	

	/**
	 *  if true, then each line will be checked just once
	 */
	public void disableSuccessfullyCheckedLineFlag() {
		disableSuccessfullyCheckedLine = true;
	}
	
	private boolean checkIfBeDisabled(int lineNumber) {
		return successfullyCheckedLines.contains( lineNumber );
	}
	
	private void checkIfNeedDisable(int lineNumber) {
		if (disableSuccessfullyCheckedLine)
			successfullyCheckedLines.add( lineNumber );
		currentSuccessfullyCheckedLineNumber = lineNumber;
	}
	
	

	/**
	 * Note: This is Non thread-safe. Only used when return true when call "isXXXTargetXXX"
	 * NOTE: users can disable specified lines which they would not check again. I.e., customized disable SuccessfullyCheckedLine
	 * Clue:
	 * 		call "disableCurrentLine" - when return true when call "isXXXTargetXXX" and should used right after "isXXXTargetXXX"
	 * 		call "disableLine" - useless now
	 */
	int currentSuccessfullyCheckedLineNumber;
	
	public void disableCurrentLine() {
		successfullyCheckedLines.add( currentSuccessfullyCheckedLineNumber );
	}
	
	// useless now
	public void disableLine(int lineNumber) {
		successfullyCheckedLines.add(lineNumber);
	}
	
	
	/********************************************************************************
	 * Checking methods - check if something belongs to a target line in the checker
	 ********************************************************************************/
	
	/**
	 * Way of checking - startsWith
	 */
	public boolean isTarget(String sig) {
		String str;
		for (int i = 0; i < targetLines.size(); i++) {
			str = targetLines.get(i);
			if (sig.startsWith(str) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	//useless for now
	public boolean isTarget_equals(String sig) {
		String str;
		for (int i = 0; i < targetLines.size(); i++) {
			str = targetLines.get(i);
			if (sig.equals(str) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	public boolean isTarget_contains(String sig) {
		String str;
		for (int i = 0; i < targetLines.size(); i++) {
			str = targetLines.get(i);
			if (sig.contains(str) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	//useless for now
	public boolean isTarget_contained(String sig) {
		String str;
		for (int i = 0; i < targetLines.size(); i++) {
			str = targetLines.get(i);
			if (str.contains(sig) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	
	
	
	/**
	 * Way of checking - equals
	 */
	public boolean isSplitTarget(String sig1) {
		String[] strs;
		for (int i = 0; i < splitTargetLines.size(); i++) {
			strs = splitTargetLines.get(i);
			if (sig1.equals(strs[0]) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	// isSplitTarget_n(str)
	public boolean isSplitTarget_0(String sig) {  // same as isSplitTarget(argu1)
		return isSplitTarget(sig);
	}
	
	public boolean isSplitTarget_1(String sig) {  // Note: couldn't use like "isSplitTarget_1(xx) && isSplitTarget_2(yy)"
		String[] strs;
		for (int i = 0; i < splitTargetLines.size(); i++) {
			strs = splitTargetLines.get(i);
			if (sig.equals(strs[1]) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	public boolean isSplitTarget_2(String sig) {
		String[] strs;
		for (int i = 0; i < splitTargetLines.size(); i++) {
			strs = splitTargetLines.get(i);
			if (sig.equals(strs[2]) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	public boolean isSplitTarget(String sig1, String sig2) {
		String[] strs;
		for (int i = 0; i < splitTargetLines.size(); i++) {
			strs = splitTargetLines.get(i);
			if (sig1.equals(strs[0]) && sig2.equals(strs[1]) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	public boolean isSplitTarget(String sig1, String sig2, String sig3) {
		String[] strs;
		for (int i = 0; i < splitTargetLines.size(); i++) {
			strs = splitTargetLines.get(i);
			if (sig1.equals(strs[0]) && sig2.equals(strs[1]) && sig3.equals(strs[2]) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	// Note: int f1, int f2, int f3 should be 0 or 1 or 2 or n-1, NOT 1, 2, .. n-1
	public boolean isSplitTarget(int f1, String sig1, int f2, String sig2) {
		String[] strs;
		for (int i = 0; i < splitTargetLines.size(); i++) {
			strs = splitTargetLines.get(i);
			if (sig1.equals(strs[f1]) && sig2.equals(strs[f2]) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	public boolean isSplitTarget(int f1, String sig1, int f2, String sig2, int f3, String sig3) {
		String[] strs;
		for (int i = 0; i < splitTargetLines.size(); i++) {
			strs = splitTargetLines.get(i);
			if (sig1.equals(strs[f1]) && sig2.equals(strs[f2]) && sig2.equals(strs[f3]) && !checkIfBeDisabled(i)) {
				checkIfNeedDisable( i );
				return true;
			}
		}
		return false;
	}
	
	

	
	
	
	
}