package dt.spoon;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import dt.spoon.processors.CatchProcessor;
import dt.spoon.processors.LoopProcessor;
import dt.spoon.processors.MethodProcessor;
import spoon.Launcher;   


public class MySpoon {

	int nProcessedJavaFiles = 0;
	/**
	 * @param args
	 * 		  args[0] is a dir or file path string
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		// Testing - Getting Spoon GUI Tree for a Directory
		//Process: Launcher.main(String[]) -> run(String[]) -> run() + new XxGuiTree()
		//Launcher.main( new String[] {"-i", "src/dt/spoon/test", "--gui"} );
		// Or
		//Launcher guilauncher = new Launcher();
		//guilauncher.run( new String[] {"-i", "src/dt/spoon/test", "--gui"} );
				
		// Testing
		//new MySpoon().scanInputDir( Paths.get("src/dt/spoon/util/Util.java") );
				
		
		if (args.length != 1) {
			System.err.println("JX - ERROR - args.length != 1");
			return;
		}
		Path path = Paths.get( args[0] );
		if ( !Files.exists(path) ) {
			System.err.println("JX - ERROR - !Files.exists @ " + path);
			return;
		}
		System.out.println("JX - INFO - " + "the target dir/file is " + path.toAbsolutePath());
		
		MySpoon myspoon = new MySpoon();
		myspoon.scanInputDir( path );
		System.out.println("JX - INFO - finished for " + myspoon.nProcessedJavaFiles + " *.java files");
	}

	public MySpoon() {	
		this.nProcessedJavaFiles = 0;
	}
	
	
	/**
	 * Handle *.java files one by one, NOT based on a dir or subdir.
	 * @param path - a dir or file path
	 * @throws IOException 
	 */
	public void scanInputDir(Path path) throws IOException {
		
		if ( !Files.exists(path) ) {
			System.out.println("JX - ERROR - !Files.exists @ " + path);
			return;
		}

        Files.walkFileTree( path, new SimpleFileVisitor<Path>(){
                @Override 
                public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
                	if (filepath.getFileName().toString().endsWith(".java")) {
                		System.out.println("Processing file: " + filepath.toString());
                		System.out.println("\t\t" + filepath.toAbsolutePath());
                		
                	}
                    return FileVisitResult.CONTINUE;
                }
                
                @Override  
                public FileVisitResult preVisitDirectory(Path dirpath, BasicFileAttributes attrs) throws IOException {
                	// TODO
                	System.out.println("Processing dir: " + dirpath.toString());
                	String dirname = dirpath.getFileName().toString(); 
                	if ( dirname.equals("java") ) {
                		long start_time = System.currentTimeMillis();
                		spoon( dirpath.toAbsolutePath() );
                		System.out.println("JX - Completion Time: " + (double)(System.currentTimeMillis()-start_time)/1000 + "s");
                		nProcessedJavaFiles ++;
                		
                	}
                	 
                	/*
                	if ( dirname.equals("test")
                			|| dirname.equals("target") 
                			|| dirname.contains("examples")
                			) {
                		return FileVisitResult.SKIP_SUBTREE;
                	}
                	*/
                	return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dirpath, IOException exc) throws IOException {
                    // TODO
                    return FileVisitResult.CONTINUE;
                }
            });
		
	}

	
	public void spoon(Path filepath) {
		/* Basic Usage */

		try {
			//jx - ps: xx[:|;]xx[:|;]xx[:|;]  in Linux & Windows respectively
			Launcher.main( new String[] {
					"-i", filepath.toString(),		// input file or dir
					"-o", "spooned/",               // default. 
					"-p", "dt.spoon.processors.CatchProcessor"
							+ File.pathSeparator + "dt.spoon.processors.LoopProcessor"
							+ File.pathSeparator + "dt.spoon.processors.MethodProcessor",
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
		
		
		/* Usage in Java Style */
		/*
		Launcher launcher = new Launcher();
		launcher.addInputResource( filepath.toString() );
		launcher.setSourceOutputDirectory("spooned/");
		// Add Processors
		CatchProcessor catchProcessor = new CatchProcessor();
		launcher.addProcessor(catchProcessor);
		LoopProcessor loopProcessor = new LoopProcessor();
		launcher.addProcessor(loopProcessor);
		MethodProcessor methodProcessor = new MethodProcessor();
		launcher.addProcessor(methodProcessor);
		launcher.getEnvironment().setLevel("WARN");
		launcher.getEnvironment().setCopyResources(false);
		//launcher.getEnvironment().setShouldCompile(true);
		//launcher.getEnvironment().setPreserveLineNumbers(true); 
		//launcher.getEnvironment().setComplianceLevel(7);   
		//launcher.getEnvironment().setSourceClasspath( new String[] {"build/lib/_DM_Log.jar"} );
		//launcher.getEnvironment().setNoClasspath(true);
		
		launcher.run();
		*/
	
	}
	
}
