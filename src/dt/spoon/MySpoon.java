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

import com.benchmark.Benchmarks;

import dt.spoon.processors.CatchProcessor;
import dt.spoon.processors.LoopProcessor;
import dt.spoon.processors.MethodProcessor;
import dt.spoon.util.SpoonUtil;
import spoon.Launcher;   


public class MySpoon {

	// Input Argus
	Path bugConfigDirPath;
	Path srcDirPath;           //INPUT: Source code Dir
	String srcClasspath;       //Source code's Classpath
	Path spoonedDirPath;       //OUTPUT: spooned result 
	Path dstDirPath;           //dest: copy from OUTPUT to dest 

	// Getting
	String BugId;
	public static int loopcount = 0;
	public static int iocount = 0;
	public static int rpccount = 0; 

	
	public static void main(String[] args) throws Exception {
			
		if (args.length != 5) {
			System.err.println("JX - ERROR - args.length != 5");
			return;
		}                            //jx: args[0] is newly added
		MySpoon myspoon = new MySpoon(args[0], args[1], args[2], args[3], args[4]);
		System.out.println("JX - INFO - finished.");
	}
	
	
	public MySpoon(String bugConfigDir, String srcDir, String srcClasspath, String spoonedDir, String dstDir) {	
		this(Paths.get(bugConfigDir), Paths.get(srcDir), srcClasspath, Paths.get(spoonedDir), Paths.get(dstDir));
	}
	
	
	public MySpoon(Path bugConfigDirPath, Path srcDirPath, String srcClasspath, Path spoonedDirPath, Path dstDirPath) {
		this.bugConfigDirPath = bugConfigDirPath;
		this.srcDirPath = srcDirPath;
		this.srcClasspath = srcClasspath;
		this.spoonedDirPath = spoonedDirPath;
		this.dstDirPath = dstDirPath;
		if ( !Files.exists(srcDirPath) ) {
			System.err.println("JX - ERROR - !Files.exists @ " + srcDirPath);
			return;
		}
		System.out.println("JX - INFO - " + "Begin: the target dir/file for spooning is " + srcDirPath.toAbsolutePath());
		try {
			doWork();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void doWork() throws IOException {
		BugId = Benchmarks.resolveBugId( bugConfigDirPath.toString() );
		SpoonUtil spoonUtil = new SpoonUtil(bugConfigDirPath);
		
		if ( BugId.equals("mr-4813") ) {
			List<Path> javadirs = getJavaDirs(srcDirPath);
			System.out.println("JX - INFO - #java dirs = " + javadirs.size());
			int num = 0;
            for (Path javapath: javadirs) {
            	System.out.println("Java Dir("+(++num)+"): " + javapath);
            }
            num = 0;
			for (Path javapath: javadirs) {
				System.out.println("Now Spooning Java Dir("+(++num)+"): " + javapath);
				spoonUtil.spoon( javapath, srcClasspath, spoonedDirPath );
				copySpooned( javapath );
			}
		}
		else if ( BugId.equals("mr-4576") ) {
			List<Path> javadirs = getDirs(srcDirPath);
			int num = 0;
            for (Path javapath: javadirs) {
            	System.out.println("Java Dir("+(++num)+"): " + javapath);
            }
            num = 0;
			for (Path javapath: javadirs) {
				System.out.println("Now Spooning Java Dir("+(++num)+"): " + javapath);
				spoonUtil.spoon( javapath, srcClasspath, spoonedDirPath );
				copySpooned( javapath );
			}
		}
		summary();
	}
	
	
	public void summary() {
		System.out.println("JX - INFO - processed loopcount = " + loopcount);
		System.out.println("JX - INFO - processed iocount = " + iocount);
		System.out.println("JX - INFO - processed rpccount = " + rpccount);
	}
	

	public void copySpooned( Path javapath ) throws IOException {
		  Path relative = srcDirPath.relativize(javapath);
		  final Path dstpath = dstDirPath.resolve(relative);
		  System.out.println("spoonedPath: " + spoonedDirPath);
		  System.out.println("dstpath: " + dstpath);
		  
		  // copy from "spooned" to "dstpath"
		  // traverse "spooned"
		  Files.walkFileTree( spoonedDirPath, new SimpleFileVisitor<Path>(){
	          @Override 
	          public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
	        	  if ( !filepath.getFileName().toString().endsWith(".java") ) {
	        		  System.out.println("JX - ERROR - filepath didn't end withs .java");
	        		  return FileVisitResult.CONTINUE;
	        	  }
	        	  Files.copy(filepath, dstpath.resolve(spoonedDirPath.relativize(filepath)), StandardCopyOption.REPLACE_EXISTING);
	              return FileVisitResult.CONTINUE;
	          }
	          
	      });
		  
		  // delete "spooned" or all subdirs and subfiles of "spooned"
		  Files.walkFileTree( spoonedDirPath, new SimpleFileVisitor<Path>(){
	          @Override 
	          public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
	        	  Files.delete(filepath);
	              return FileVisitResult.CONTINUE;
	          }
	      }); //xx   x
    }
	
		
	/**
	 * For mr-4813
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
                        // filters for subprojects
                        if ( dirname.equals("hadoop-tools")
                             || dirname.equals("hadoop-common-project")
                             || dirname.equals("hadoop-hdfs-project")
                             || dirname.equals("hadoop-yarn-common")
                             )
                             return FileVisitResult.SKIP_SUBTREE;


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
	               		// special filters
	               		if (dirpath.endsWith("hadoop-mapreduce-project/src/java")) 
	               			return FileVisitResult.SKIP_SUBTREE;
	               		
	               		dirs.add( dirpath.toAbsolutePath() );
	               		return FileVisitResult.SKIP_SUBTREE;
	               	}
	               	return FileVisitResult.CONTINUE;
               }
               
           });
		return dirs;
	}
	
	
	
	
	// For mr-4576
	public List<Path> getDirs(Path dirpath) {
		final List<Path> dirs = new ArrayList<Path>();
		//if want to use filters, can filter "tools/benchmarks/contrib/c++/test/packages/native/ant/docs/examples/webapps/hdfs"
		//only focus on "src/"
		dirs.add( dirpath.resolve("src/core") );   	//including org/apache/hadoop/io
		dirs.add( dirpath.resolve("src/mapred") );	//including org/apache/hadoop/mapreduce, org/apache/hadoop/filecache, org/apache/hadoop/mapred, 
		return dirs;
	}
	
	
	
}
