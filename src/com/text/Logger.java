package com.text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Logger {

	Path logDir = Paths.get(".");
	
	public Logger() {
		
	}
	
	
	public void setLogDir(String logDir) {
		setLogDir( Paths.get(logDir) );
	}
	public void setLogDir(Path logDir) {
		this.logDir = logDir;
	}
	
	
	public void log(String aLine) {
	    String strProc = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
	    String pid = Long.toString( Long.parseLong(strProc.split("@")[0]) );
	    String tid = Long.toString( java.lang.Thread.currentThread().getId() );
	    String filename = pid +"-" + tid;
	    
	    Path logFile = logDir.resolve( filename );
	    TextFileWriter writer = new TextFileWriter(logFile, true);
	    writer.writeLine(aLine);
	    writer.close();
	}
	
	
	public static void log(String logDir, String aLine) {
		log(Paths.get(logDir), aLine);
	}
	public static void log(Path logDir, String aLine) {
	    String strProc = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
	    String pid = Long.toString( Long.parseLong(strProc.split("@")[0]) );
	    String tid = Long.toString( java.lang.Thread.currentThread().getId() );
	    String filename = pid +"-" + tid;
	    
	    Path logFile = logDir.resolve( filename );
	    TextFileWriter writer = new TextFileWriter(logFile, true);
	    writer.writeLine(aLine);
	    writer.close();
	}
	
}
