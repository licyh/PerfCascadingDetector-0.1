package dt.spoon;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import dt.spoon.processors.AbsInvokeProcessor;
import dt.spoon.processors.CatchProcessor;
import dt.spoon.processors.InvokeProcessor;
import dt.spoon.processors.LoopProcessor;
import dt.spoon.processors.MethodProcessor;
import spoon.Launcher;   


public class MySpoon {

	Path dirpath;           //Source code Dir
	String srcClasspath;    //Source code's Classpath
	
	// for Testing
	static boolean isTesting = true;  
	// End - for Testing
	
	
	/**
	 * @param args
	 * 		  args[0] is a dir or file path string
	 */
	public static void main(String[] args) throws Exception {
						
		// Testing
		if (isTesting) {
			System.out.println("JX - WARN - Under Testing State!!!");
			Path testingPath = Paths.get( "src/dt/spoon/test" );
			
			new MySpoon( testingPath, "" );
			// Testing - Getting Spoon GUI Tree for a Directory
			//Launcher guilauncher = new Launcher();
			//guilauncher.run( new String[] {"-i", testingPath.toString(), "--gui"} );
			// Or
			//Process: Launcher.main(String[]) -> run(String[]) -> run() + new XxGuiTree()
			//Launcher.main( new String[] {"-i", testingPath.toString(), "--gui"} );
			return;
		}
				
		// Regular codes
		if (args.length != 2) {
			System.err.println("JX - ERROR - args.length != 2");
			return;
		}
		MySpoon myspoon = new MySpoon(args[0], args[1]);
		System.out.println("JX - INFO - finished.");
	}

	
	public MySpoon(String dirstr, String srcClasspath) {	
		this(Paths.get(dirstr), srcClasspath);
	}
	
	public MySpoon(Path dirpath, String srcClasspath) {
		this.dirpath = dirpath;
		this.srcClasspath = srcClasspath;
		if ( !Files.exists(dirpath) ) {
			System.err.println("JX - ERROR - !Files.exists @ " + dirpath);
			return;
		}
		System.out.println("JX - INFO - " + "the target dir/file for spooning is " + dirpath.toAbsolutePath());
		doWork();
	}
	
	
	public void doWork() {
		try {
			List<Path> javadirs = getJavaDirs(dirpath);
			System.out.println("JX - INFO - #java dirs = " + javadirs.size());
			for (Path path: javadirs) {
				System.out.println("Java Dir: " + path);
				spoon( path );
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	



	/**
	 * Spooning
	 * @param path - is a absolute path
	 */
	public void spoon(Path path) throws IOException {
		// Get all normal *.java files for this SPOON process
		//String inputlist = getSpecificJavaFiles(path);
		
		/* Basic Usage */
		/*
		try {
			//jx - ps: xx[:|;]xx[:|;]xx[:|;]  in Linux & Windows respectively
			Launcher.main( new String[] {
					"-i", inputlist,						// input file or dir
					"-o", "spooned/",               				// default. 
					//"-p", "dt.spoon.processors.CatchProcessor",   // for test
					"-p", "dt.spoon.processors.MethodProcessor" 
						  + File.pathSeparator + "dt.spoon.processors.AbsInvokeProcessor",
					"--output-type", "compilationunits",   // jx: means NO split a .java by its multi classes. The default is "classes",
					"--level", "WARN",
					"--no-copy-resources",          // jx - should be NO copy non-java files
					//"--compile",                    // PS: "--compile/--precompile" used for compiling transformed/orignial codes respectively
					//"-d", "spooned-classes/", 		// default
					//"--source-classpath", "build/classes/", //"--source-classpath", "bin/",   // for "--compile" to load "LogClass._DM_Log" PS: a wrong "WARN" at console
					// No need for me now
					//"--lines",                    //jx - this couldn't really preserve all of line numbers
					//"--compliance", "7",          //default is 8, because of spoon's own compiler(from Eclipse), so even Eclipse is 7, it will still be its own setting
					//"--precompile",
			} );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		/* Usage in Java Style */
		
		Launcher launcher = new Launcher();
		launcher.addInputResource( path.toString() );
		launcher.setSourceOutputDirectory("spooned/");
		
		// Add Processors
		// for Loops
		MethodProcessor methodProcessor = new MethodProcessor();
		launcher.addProcessor(methodProcessor);
		// for IOs
		AbsInvokeProcessor absInvokeProcessor = new AbsInvokeProcessor();
		launcher.addProcessor(absInvokeProcessor);
		// for RPCs
		InvokeProcessor invokeProcessor = new InvokeProcessor();
		launcher.addProcessor(invokeProcessor);
		
		launcher.setArgs( new String[]{"--source-classpath", srcClasspath, "--output-type", "compilationunits"} );
		launcher.getEnvironment().setCopyResources(false);
		launcher.getEnvironment().setLevel("WARN");
		
		//launcher.getEnvironment().setSourceClasspath( new String[] {"build/lib/_DM_Log.jar"} );
		//launcher.getEnvironment().setShouldCompile(true);
		//launcher.getEnvironment().setPreserveLineNumbers(true); 
		//launcher.getEnvironment().setComplianceLevel(7);   
		//launcher.getEnvironment().setNoClasspath(true);
		
		launcher.run();
		
	}
	
		
	/**
	 * Get absolute Java Dirs
	 */
	public List<Path> getJavaDirs(Path path) throws IOException {
		final List<Path> dirs = new ArrayList<Path>();
		if ( isTesting || !Files.isDirectory(path) ) {
			dirs.add(path.toAbsolutePath());
			return dirs;
		}
		
	    Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
               @Override 
               public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
                   return FileVisitResult.CONTINUE;
               }
               
               @Override  
               public FileVisitResult preVisitDirectory(Path dirpath, BasicFileAttributes attrs) throws IOException {

	               	String dirname = dirpath.getFileName().toString();
	               	// filters
	               	if ( dirname.contains("examples") 
	               			|| dirname.contains("benchmarks")
	               			|| dirname.contains("generated")
	               			|| dirname.equals("test")                  //test-related *.java files
	               			|| dirname.equals("contrib")
	               			|| dirname.equals("target") 
	               			|| dirname.equals("tools")
	               			|| dirname.equals("ant")
	               			|| dirname.equals("webapp")
	               			) {
	               		return FileVisitResult.SKIP_SUBTREE;
	               	}
	               	// get "java" dirs
	               	if ( dirname.equals("java") ) {
	               		dirs.add( dirpath.toAbsolutePath() );
	               		return FileVisitResult.SKIP_SUBTREE;
	               	}
	               	return FileVisitResult.CONTINUE;
               }
               
           });
		return dirs;
	}
	
	
	/**  NOT USE NOW
	 * Get all normal *.java files for this SPOON process
	 * @param path : a dir or file path
	 */
	String inputlist;
	public String getSpecificJavaFiles(Path path) throws IOException {
	
		if ( !Files.isDirectory(path) ) {
			inputlist = path.toString();
			return inputlist;
		}
		
		inputlist = "";
        Files.walkFileTree( path, new SimpleFileVisitor<Path>(){
            @Override 
            public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
            	String absoluteFilename = filepath.toAbsolutePath().toString();
            	String filename = filepath.getFileName().toString();
            	// filters
            	if ( absoluteFilename.endsWith(".java")
            			&& !filename.equals("package-info.java")        // Empty java files without "class xxx {}", just "package xx" or nothing that will cause "RuntimeException: inconsistent compilation unit" when using "--output-type", "compilationunits"
            			&& !BlackList.isBlack(absoluteFilename)			// customized
            			) {
            		if (inputlist.length() == 0)
            			inputlist = absoluteFilename;
            		else
            			inputlist += File.pathSeparator + absoluteFilename;
            	}
                return FileVisitResult.CONTINUE;
            }
        });
		//System.out.println("JX - INFO - " + "inputlist: " + inputlist);
		return inputlist;
	}
	
}
