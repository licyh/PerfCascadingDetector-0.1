package com.text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextFileWriter {

	Path filepath;
	BufferedWriter bufwriter;
	List<String> strs = new ArrayList<String>();
	
	public TextFileWriter(String filestr) {
		this( Paths.get(filestr) );
	}
	
	public TextFileWriter(Path filepath) {
		this( filepath, false );
	}
	
	public TextFileWriter(String filestr, boolean append) {
		this( Paths.get(filestr), append );
	}
	
	public TextFileWriter(Path filepath, boolean append) {
		this.filepath = filepath;
		try {
			this.bufwriter = new BufferedWriter( new FileWriter(filepath.toString(), append) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public void writeLine(String line) {
		try {
			bufwriter.write( line + "\n" );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * TODO
	 */
	public void writeFile() {
		System.out.println("JX - successfully write " + "xxx" + " lines into " + filepath.toString());
	}
	
	public void close() {
		try {
			bufwriter.flush();
			bufwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
