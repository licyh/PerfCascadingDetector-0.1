package com.benchmark;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.text.TextFileReader;


public class BugConfig {

	String bugId = "";
	
	public BugConfig() {
	}
	
	public BugConfig(String filestr) {
		this( Paths.get(filestr) );
	}

	public BugConfig(Path filepath) {
		this( filepath, false );
	}
	
	public BugConfig(String filestr, boolean inJar) {
		this( Paths.get(filestr), inJar );
	}

	public BugConfig(Path filepath, boolean inJar) {
		TextFileReader reader = new TextFileReader(filepath, inJar);
		this.bugId = reader.readLine();
	}
		
	/** for property format's config file
	public void setConfig() {
		
	}
	*/
	
	public String getBugId() {
		return bugId;
	}
	
}
