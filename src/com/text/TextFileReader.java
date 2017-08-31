package com.text;

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
	
	public List<String> strs = new ArrayList<String>();				// database: line by line
	public List<String[]> splitstrs = new ArrayList<String[]>();    // database: split line by split line 
	int validNumberOfLines = 0;  									// equals "this.strs/splitstrs.size()" when reading by "readFile()"
	
	
	
	/**
	 * For reading from a common File, NOT in jar
	 */
	public TextFileReader(String filestr) {
		this( Paths.get(filestr) );
	}

	public TextFileReader(Path filepath) {
		this( filepath, false );
	}
	
	/**
	 * For reading from a common file OR a jar-ed file (ie, a file in a jar)
	 * @param inJar - false:a common file; true: a jar-ed file
	 */
	public TextFileReader(String filestr, boolean inJar) {
		this( Paths.get(filestr), inJar );
	}

	public TextFileReader(Path filepath, boolean inJar) {
		if (!inJar)
			initialize(filepath);
		else 
			initializeInJar( filepath );
	}
	

	private void initialize(Path filepath) {
		this.filepath = filepath;
		if ( !Files.exists(filepath) ) {
			System.out.println("JX - ERROR - !Files.exists @ " + filepath);
			return;
		}
		try {
			this.bufreader = new BufferedReader( new FileReader( filepath.toString() ) );
		} catch (FileNotFoundException e) {
			System.out.println("JX - ERROR - TextFileReader: when reading " + filepath.toString() );
			e.printStackTrace();
		}
	}
	
	private void initializeInJar(Path filepath) {
		this.filepath = filepath;
		// I don't know how to check if the file exists here?
		/*
		if ( !Files.exists(filepath) )
			System.out.println("JX - ERROR - !Files.exists @ " + filepath);
		*/
		InputStream ins = TextFileReader.class.getClassLoader().getResourceAsStream( filepath.toString() );
		try {
	    	this.bufreader = new BufferedReader( new InputStreamReader(ins) );
    	} catch (Exception e) {
    		System.out.println("JX - ERROR - TextFileReader: when reading " + filepath.toString() + " inside a classpath");
    		e.printStackTrace();
    	}	
	}
	
	
	/**
	 * For a file inside a jar package 
	 * @param class - like "MapReduceTransformer.class"
	 * @param filestr - like "resource/looplocations"
	 */
	@Deprecated
	public TextFileReader(Class clazz, String filestr) {
		this( clazz, Paths.get(filestr) );
	}
	
	@Deprecated
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
		
	
	
	
	/**************************************************************************
	 * Core
	 **************************************************************************/
		
	/**
	 * For reading all and saving
	 */
	public void readFile() {		
		String tmpline;
		while ( (tmpline = readLine()) != null ) {
			strs.add( tmpline );
			String[] tmpstrs = tmpline.split("\\s+");
			splitstrs.add( tmpstrs );
		}
		try {
			bufreader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("JX - INFO - TextFileReader: successfully read " + strs.size() + " lines in " + filepath.toString());
	}

	
	/**
	 * For people who want to read directly from outside (ie, read file line by line manually)
	 * Usage: while ( (tmpline = reader.readLine()) != null ) { String[] strs = tmpline.split("\\s+"); ... } reader.close(); 
	 * @return - maybe null
	 */
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
	    if ( tmpline == null )
	    	return null;
	    validNumberOfLines ++;  // all valid line number
	    return tmpline.trim();
	}

	
	/**
	 * close() for manually reading line by line, see Usage of "readLine()"
	 */
	public void close() {
		try {
			bufreader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	// more useful for manually reading by calling "readLine()"
	public void printReadStatus() {
		System.out.println("JX - INFO - TextFileReader: successfully read " + getValidNumberOfLines() + " lines in " + filepath.toString());
	}
	
	
	// more useful for manually reading by calling "readLine()"
	public int getValidNumberOfLines() {
		return this.validNumberOfLines;     // equals "this.strs.size()" when reading by "readFile()"
	}
	
}
