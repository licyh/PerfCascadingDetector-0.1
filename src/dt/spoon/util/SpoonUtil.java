package dt.spoon.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import dt.spoon.processors.AbsInvokeProcessor;
import dt.spoon.processors.InvokeProcessor;
import dt.spoon.processors.MethodProcessor;
import spoon.Launcher;


public class SpoonUtil {
	
	/**
	 * @param inputPath(Str) - input file or directory
	 * @param srcClasspath - input classpath
	 * @param outputDirPath(Str) - output directory
	 */
	public static void spoon(String inputPathStr, String srcClasspath, String outputDirPathStr) {
		spoon( Paths.get(inputPathStr), srcClasspath, Paths.get(outputDirPathStr) );
	}
	
	public static void spoon(Path inputPath, String srcClasspath, Path outputDirPath) {
		
		Launcher launcher = new Launcher();

		// Add Processors
		// for Loops
		//MethodProcessor methodProcessor = new MethodProcessor();
		//launcher.addProcessor(methodProcessor);
		// for IOs
		AbsInvokeProcessor absInvokeProcessor = new AbsInvokeProcessor();
		launcher.addProcessor(absInvokeProcessor);
		// for RPCs
		//InvokeProcessor invokeProcessor = new InvokeProcessor();
		//launcher.addProcessor(invokeProcessor);
		
		// Note: setArgs should be put at first, or will override all args 
                /*
		launcher.setArgs( new String[]{"--source-classpath", srcClasspath, "--output-type", "compilationunits"} );
		launcher.addInputResource( inputPath.toString() );
		launcher.setSourceOutputDirectory( outputDirPath.toString() );  //default is "spooned/"
		launcher.getEnvironment().setCopyResources(false);
		launcher.getEnvironment().setLevel("WARN");
		//launcher.getEnvironment().setSourceClasspath( new String[] {"build/lib/_DM_Log.jar"} );
		//launcher.getEnvironment().setShouldCompile(true);
		//launcher.getEnvironment().setPreserveLineNumbers(true); 
		//launcher.getEnvironment().setComplianceLevel(7);   
		//launcher.getEnvironment().setNoClasspath(true);
                */

		// just use this it's a good way.
		launcher.setArgs( new String[]{
				"-i", inputPath.toString(),
				"--source-classpath", srcClasspath,
				"-o", outputDirPath.toString(),
				"--output-type", "compilationunits",
	      			"--no-copy-resources",
	      			"--level", "WARN",
			} );
		
		
		
		launcher.run();
		
		/* Basic Usage */
		/*
		try {
			//jx - ps: xx[:|;]xx[:|;]xx[:|;]  in Linux & Windows respectively
			Launcher.main( new String[] {
					"-i", inputPath.toString(),						// input file or dir
					//"--source-classpath", "build/classes/", //"--source-classpath", "bin/",   // for "--compile" to load "LogClass._DM_Log" PS: a wrong "WARN" at console
					"-o", "spooned/",               				// default. 
					//"-p", "dt.spoon.processors.CatchProcessor",   // for test
					"-p", "dt.spoon.processors.MethodProcessor" 
						  + File.pathSeparator + "dt.spoon.processors.AbsInvokeProcessor",
					"--output-type", "compilationunits",   			// jx: means NO split a .java by its multi classes. The default is "classes",
					"--no-copy-resources",          				// jx - should be NO copy non-java files
					"--level", "WARN",
					//"--compile",                    // PS: "--compile/--precompile" used for compiling transformed/orignial codes respectively
					//"-d", "spooned-classes/", 		// default
					// No need for me now
					//"--lines",                    //jx - this couldn't really preserve all of line numbers
					//"--compliance", "7",          //default is 8, because of spoon's own compiler(from Eclipse), so even Eclipse is 7, it will still be its own setting
					//"--precompile",
			} );
		*/
	}
	
	
	/**
	 * @param inputPath(Str) - input file or directory
	 * @param outputDirPath(Str) - output directory
	 */
	public static void spoon(String inputPathStr, String outputDirPathStr) {
		spoon( Paths.get(inputPathStr), Paths.get(outputDirPathStr) );
	}
	
	public static void spoon(Path inputPath, Path outputDirPath) {
		spoon( inputPath, ".", outputDirPath);
	}
	
	
	/**
	 * @param inputPath(Str) - input file or directory
	 */
	public static void spoon(String inputPathStr) {
		spoon( Paths.get(inputPathStr) );
	}
	
	public static void spoon(Path inputPath) {
		spoon( inputPath, ".", Paths.get("spooned/") );
	}	
	
	
	/**
	 * @param inputPathStr - file or directory
	 */
	public static void guiSpoon(String inputPathStr) {
		guiSpoon( Paths.get(inputPathStr) );
	}
	
	public static void guiSpoon(Path inputPath) {
		// for Testing - Getting Spoon GUI Tree for a Directory
		Launcher launcher = new Launcher();
		launcher.run( new String[] {"-i", inputPath.toString(), "--gui"} );
		// Or 
		//Process: Launcher.main(String[]) -> run(String[]) -> run() + new XxGuiTree()
		//Launcher.main( new String[] {"-i", testingPath.toString(), "--gui"} );
	}

}
