package dt.spoon;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import dt.spoon.processors.AbsInvokeProcessor;
import dt.spoon.processors.CatchProcessor;
import dt.spoon.processors.InvokeProcessor;
import dt.spoon.processors.LoopProcessor;
import dt.spoon.processors.MethodProcessor;
import dt.spoon.util.SpoonUtil;
import spoon.Launcher;   


public class MySpoon {

	Path srcDirPath;           //INPUT: Source code Dir
	String srcClasspath;       //Source code's Classpath
	Path spoonedDirPath;       //OUTPUT: spooned result 
	Path dstDirPath;           //dest: copy from OUTPUT to dest 
	

	public static void main(String[] args) throws Exception {
			
		if (args.length != 4) {
			System.err.println("JX - ERROR - args.length != 4");
			return;
		}
		MySpoon myspoon = new MySpoon(args[0], args[1], args[2], args[3]);
		System.out.println("JX - INFO - finished.");
	}


	public MySpoon(String srcDir, String srcClasspath, String spoonedDir, String dstDir) {	
		this(Paths.get(srcDir), srcClasspath, Paths.get(spoonedDir), Paths.get(dstDir));
	}
	
	
	public MySpoon(Path srcDirPath, String srcClasspath, Path spoonedDirPath, Path dstDirPath) {
		this.srcDirPath = srcDirPath;
		this.srcClasspath = srcClasspath;
		this.spoonedDirPath = spoonedDirPath;
		this.dstDirPath = dstDirPath;
		if ( !Files.exists(srcDirPath) ) {
			System.err.println("JX - ERROR - !Files.exists @ " + srcDirPath);
			return;
		}
		System.out.println("JX - INFO - " + "Begin: the target dir/file for spooning is " + srcDirPath.toAbsolutePath());
		doWork();
	}
	
	
	public void doWork() {
		try {
			List<Path> javadirs = getJavaDirs(srcDirPath);
			System.out.println("JX - INFO - #java dirs = " + javadirs.size());
			for (Path path: javadirs) {
				System.out.println("Java Dir: " + path);
				SpoonUtil.spoon( path, srcClasspath, spoonedDirPath );
				//copySpooned( path );
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	public void copySpooned(String str) throws IOException {
		  Path srcpath = Paths.get(str);
		  Path relative = inDirPath.relativize(srcpath);
		  
		  final Path dstpath = outDirPath.resolve(relative);
		  System.out.println("srcpath: " + srcpath);
		  System.out.println("spoonedPath: " + spoonedPath);
		  System.out.println("dstpath: " + dstpath);
		  
		  // copy from "spooned" to "dstpath"
		  // traverse "spooned"
		  Files.walkFileTree( spoonedPath, new SimpleFileVisitor<Path>(){
	          @Override 
	          public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
	        	  if ( !filepath.getFileName().toString().endsWith(".java") ) {
	        		  System.out.println("JX - ERROR - filepath didn't end withs .java");
	        		  return FileVisitResult.CONTINUE;
	        	  }
	        	  Files.copy(filepath, dstpath.resolve(spoonedPath.relativize(filepath)), StandardCopyOption.REPLACE_EXISTING);
	              return FileVisitResult.CONTINUE;
	          }
	          
	      });
		  
		  // delete "spooned" or all subdirs and subfiles of "spooned"
		  Files.walkFileTree( spoonedPath, new SimpleFileVisitor<Path>(){
	          @Override 
	          public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
	        	  Files.delete(filepath);
	              return FileVisitResult.CONTINUE;
	          }
	      }); //xx   x
		  
    }
	*/
		
	/**
	 * Get absolute Java Dirs
	 */
	public List<Path> getJavaDirs(Path dirpath) throws IOException {
		final List<Path> dirs = new ArrayList<Path>();
		
	    Files.walkFileTree( dirpath, new SimpleFileVisitor<Path>() {
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
