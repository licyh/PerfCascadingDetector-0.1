package com;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class TextFileReader {

	Path filepath;
	BufferedReader bufreader;
	
	// line by line
	public List<String> strs = new ArrayList<String>();
	// split line by split line 
	public List<String[]> splitstrs = new ArrayList<String[]>();
	
	
	/**
	 * For a common file
	 */
	public TextFileReader(String filestr) {
		this( Paths.get(filestr) );
	}

	public TextFileReader(Path filepath) {
		this.filepath = filepath;
		if ( !Files.exists(filepath) )
			System.out.println("JX - ERROR - !Files.exists @ " + filepath);
		
		try {
			this.bufreader = new BufferedReader( new FileReader( filepath.toString() ) );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * For a file inside a jar package 
	 * @param class - like "MapReduceTransformer.class"
	 * @param filestr - like "resource/looplocations"
	 */
	public TextFileReader(Class clazz, String filestr) {
		this( clazz, Paths.get(filestr) );
	}
	
	public TextFileReader(Class clazz, Path filepath) {
		this.filepath = filepath;
		// I don't know how to check if the file exists here?
		/*
		if ( !Files.exists(filepath) )
			System.out.println("JX - ERROR - !Files.exists @ " + filepath);
		*/
		InputStream ins = clazz.getClassLoader().getResourceAsStream( filepath.toString() );
		try {
	    	this.bufreader = new BufferedReader( new InputStreamReader(ins) );
    	} catch (Exception e) {
    		System.out.println("JX - ERROR - TextFileReader: when reading " + filepath.toString() + " inside a classpath");
    		e.printStackTrace();
    	}
	}
		
	
	public String readLine() {
	    String tmpline = "";
	    do {
	    	try {
				tmpline = bufreader.readLine();   // Note: can't ".trim()" directly, because it may return null, so null.trim() will except xx
			} catch (IOException e) {
				e.printStackTrace();
			} 
	    } while( tmpline != null && 
	    		(tmpline.startsWith("//")||tmpline.startsWith("#")||tmpline.trim().length()==0)
	    	   );
	    return tmpline;
	}
	
	/**
	 * For reading all and saving
	 */
	public void readFile() {		
		String tmpline;
		while ( (tmpline = readLine()) != null ) {
			strs.add( tmpline.trim() );
			String[] tmpstrs = tmpline.trim().split("\\s+");
			splitstrs.add( tmpstrs );
		}
		try {
			bufreader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("JX - INFO - successfully read " + strs.size() + " lines in " + filepath.toString());
	}
			
}
