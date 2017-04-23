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

	/**
	 * @param args
	 * 		  args[0] is a dir path string
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		if (args.length != 1) {
			System.err.println("JX - ERROR - args.length != 1");
			return;
		}
		Path dirpath = Paths.get( args[0] );
		if ( !Files.exists(dirpath) ) {
			System.err.println("JX - ERROR - !Files.exists @ " + dirpath);
			return;
		}
		System.out.println("JX - INFO - " + "the target dir is " + dirpath.toAbsolutePath());
		
		
		// Testing - Getting Spoon GUI Tree for a Directory
		//Process: Launcher.main(String[]) -> run(String[]) -> run() + new XxGuiTree()
		//Launcher.main( new String[] {"-i", "src/dt/spoon/test", "--gui"} );
		// Or
		//Launcher guilauncher = new Launcher();
		//guilauncher.run( new String[] {"-i", "src/dt/spoon/test", "--gui"} );
		
		// Testing
		//new MySpoon().scanInputDir( Paths.get("src/dt/spoon/test/") );
		
		new MySpoon().scanInputDir( dirpath );
	}

	public MySpoon() {		
	}
	
	
	/**
	 * handle *.java files one by one, NOT based on a dir or subdir.
	 * @throws IOException 
	 */
	public void scanInputDir(Path dirpath) throws IOException {
		
		if ( !Files.exists(dirpath) ) {
			System.out.println("JX - ERROR - !Files.exists @ " + dirpath);
			return;
		}

        Files.walkFileTree( dirpath, new SimpleFileVisitor<Path>(){
                @Override 
                public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
                	if (filepath.getFileName().toString().endsWith(".java")) {
                		System.out.println("Processing file: " + filepath.toString());
                		System.out.println("\t\t" + filepath.toAbsolutePath());
                		spoon(filepath.toAbsolutePath());
                	}
                    return FileVisitResult.CONTINUE;
                }
                
                @Override  
                public FileVisitResult preVisitDirectory(Path dirpath, BasicFileAttributes attrs) throws IOException {
                	// TODO
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
					"--no-copy-resources",          // jx - should be NO copy non-java files
					"--level", "WARN",
					"--compile",                    // PS: "--compile/--precompile" used for compiling transformed/orignial codes respectively
					"-d", "spooned-classes/", 		// default
					//"--source-classpath", "bin/",   // for "--compile" to load "LogClass._DM_Log" PS: a wrong "WARN" at console
					"--source-classpath", "build/classes/",
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
		CatchProcessor catchProcessor = new CatchProcessor();
		launcher.addProcessor(catchProcessor);
		LoopProcessor loopProcessor = new LoopProcessor();
		launcher.addProcessor(loopProcessor);
		MethodProcessor methodProcessor = new MethodProcessor();
		launcher.addProcessor(methodProcessor);
		launcher.getEnvironment().setLevel("WARN");
		//launcher.getEnvironment().setPreserveLineNumbers(true); 
		launcher.getEnvironment().setCopyResources(false);        
		//launcher.getEnvironment().setComplianceLevel(7);   
		//launcher.getEnvironment().setSourceClasspath( new String[] {"build/lib/_DM_Log.jar"} );
		//launcher.getEnvironment().setNoClasspath(true);
		launcher.getEnvironment().setShouldCompile(true);
		
		launcher.run();
		*/
	
	}
	
}
