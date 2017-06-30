package com.system;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.text.TextFileWriter;

public class Timer {

	long startTime = 0;
	long lastStopTime = 0;
	TextFileWriter writer = null;
	 
	public Timer() {
		this.startTime = System.currentTimeMillis();  
	}
	
	public Timer(String timingFileStr) {
		this( Paths.get(timingFileStr) );  
	}
	
	public Timer(Path timingFile) {
		this.startTime = System.currentTimeMillis();
		this.writer = new TextFileWriter(timingFile);
	}
	 
	
	public void tic() {
		tic("");
	}
	public void tic(String msg) {
		this.startTime = System.currentTimeMillis();
		this.lastStopTime = this.startTime;
		String MSG = "";
		if (msg.length() > 0) MSG = ", MSG: " + msg;
		// core
		String log = "JX - INFO - Timer(tic)" + MSG;
		System.out.println( log );
		if ( writer != null ) {
			writer.writeLine( log );
		}
	}
	
	
	public void toc() {
		toc("");
	}
	public void toc(String msg) {
		String MSG = "";
		if (msg.length() > 0) MSG = ", MSG: " + msg;
		// core
		long currentTime = System.currentTimeMillis();
		String log = "JX - INFO - Timer(toc->toc): " + (double)(currentTime-this.lastStopTime)/1000 + "s"
				   + ", (tic->toc): " + (double)(currentTime-this.startTime)/1000 + "s"
				   + MSG;
		this.lastStopTime = currentTime;
		System.out.println( log );
		if ( writer != null ) {
			writer.writeLine( log );
		}	
	}
	
	
	/**
	 * close the Timer, if needed
	 */
	public void close() {
		if ( writer != null ) {
			writer.close();
		}
	}
	
	
	
	/***************************************************************************************************
	 * Usage:
	 * 	Timer.globalTic()
	 *  Timer.globalToc()
	 **************************************************************************************************/
	static long global_start_time = 0;
	
	
	public static void globalTic() {
		globalTic("");
	}
	public static void globalTic(String msg) {
		global_start_time = System.currentTimeMillis();
		String MSG = "";
		if (msg.length() > 0) MSG = ", MSG: " + msg; 
		System.out.println( "JX - INFO - Timer(tic)" + MSG );
	}
	
	
	public static void globalToc() {
		globalToc("");
	}
	public static void globalToc(String msg) {
		String MSG = "";
		if (msg.length() > 0) MSG = ", MSG: " + msg;
		System.out.println( "JX - INFO - Timer(toc): " + (double)(System.currentTimeMillis()-global_start_time)/1000 + "s" + MSG );	
	}
	
}
