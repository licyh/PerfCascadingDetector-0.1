/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package sa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;

import com.benchmark.Benchmarks;

//import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.io.CommandLine;

import sa.tc.HDrpc;
import sa.tc.MRrpc;
import sa.wala.WalaAnalyzer;
import sa.wala.util.PDFCallGraph;


public class JXLocks {
  // dir paths
  static String projectDir;  // read from arguments, like "/root/JXCascading-detector(/)"   #jx: couldn't obtain automatically, because of many scenarios
  static String appJarDir;   // read from arguments, like "/root/JXCascading-detector/src/sa/res/MapReduce/hadoop-0.23.3(/)"
  static String dtDir;       //should be "projectDir/src/dt/res/", but couldn't write directly like this
  static String dmDir;       
  
  // WALA basis
  //static WalaAnalysis wala;
  static WalaAnalyzer wala;
  static ClassHierarchy cha;
  static CallGraph cg;
  static int nPackageFuncs = 0;           // the real functions we focuses  //must satisfy "isApplicationAndNonNativeMethod" first
  static int nTotalFuncs = 0;
  static int nApplicationFuncs = 0;       
  static int nPremordialFuncs = 0;
  static int nOtherFuncs = 0;
  
  // Target System
  static String systemname = null;   // current system's name  
  
  // Lock Names
  static List<String> synchronizedtypes = Arrays.asList("synchronized_method", "synchronized_lock");
  static List<String> locktypes = Arrays.asList("lock", "readLock", "writeLock", "tryLock", "writeLockInterruptibly", "readLockInterruptibly", "lockInterruptibly"); //last two added by myself
  static List<String> unlocktypes = Arrays.asList("unlock", "readUnlock", "writeUnlock");
  static Map<String,String> mapOfLocktypes = new HashMap<String,String>() {{
    put("lock", "unlock");
    put("readLock", "readUnlock");
    put("writeLock", "writeUnlock");
    put("tryLock", "unlock");
    put("writeLockInterruptibly", "writeUnlock");
    put("readLockInterruptibly", "readUnlock");
    put("lockInterruptibly", "unlock");
  }};
  // map: function CGNode id -> locks, ONLY covers functions that really involve locks 
  static Map<Integer, List<LockInfo>> functions_with_locks = new HashMap<Integer, List<LockInfo>>();
  // map: function CGNode id -> loops, ONLY covers functions that really involve loops  
  static Map<Integer, List<LoopInfo>> functions_with_loops = new HashMap<Integer, List<LoopInfo>>();
  // map: function CGNode id -> traversed functions (including looping_locking_functions)
  static Map<Integer, FunctionInfo> functions = new HashMap<Integer, FunctionInfo>();
  
  // Statistics
  static int nLocks = 0;
  static int nLockingFuncs = 0;
  static int nLockGroups = 0;
  
  static int nLoops = 0;                  // unused
  static int nLoopingFuncs = 0;
  
  static int nLoopingLocks = 0;
  static int nLoopingLockingFuncs = 0;
  
  static int nHeavyLocks = 0;             // The number of time-consuming looping locks
  static int nHeavyLockGroups = 0;
  
  static int nHeartbeatLocks = 0;
  static int nHeartbeatLockGroups = 0;
  
  static int nSuspectedHeavyLocks = 0;
 
  
  // For test
  static String functionname_for_test = "org.apache.hadoop.hdfs.DFSOutputStream$DataStreamer$ResponseProcessor.run("; //"RetryCache.waitForCompletion(Lorg/apache/hadoop/ipc/RetryCache$CacheEntry;)"; //"org.apache.hadoop.hdfs.server.balancer.Balancer"; //"Balancer$Source.getBlockList";//"DirectoryScanner.scan"; //"ReadaheadPool.getInstance("; //"BPServiceActor.run("; //"DataNode.runDatanodeDaemon"; //"BPServiceActor.run("; //"BlockPoolManager.startAll"; //"NameNodeRpcServer"; //"BackupNode$BackupNodeRpcServer"; // //".DatanodeProtocolServerSideTranslatorPB"; //"DatanodeProtocolService$BlockingInterface"; //"sendHeartbeat("; //"org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolServerSideTranslatorPB";  //java.util.regex.Matcher.match(";
  static int which_functionname_for_test = 1;   //1st? 2nd? 3rd?    //TODO - 0 means ALL, 1 to n means which one respectively
  
  
  
  //===============================================================================================
  //++++++++++++++++++++++++++++++++++ JXLocks Methods ++++++++++++++++++++++++++++++++++++++++++++
  //===============================================================================================

  
  public static void main(String[] args) throws WalaException {
    System.out.println("JX-breakpoint-...");
    Properties p = CommandLine.parse(args);
    PDFCallGraph.validateCommandLine(p);
    new JXLocks().doWork(p);  // or JXLocks.run(p) or run(p) directly
  }

  
  public static void doWork(Properties p) {
    try {
      // Test Part -
      //testQuickly();
      init(p);
      doWalaAnalysis();
      
      // New added - RPC
      findRPCs();
      readTimeConsumingOperations();
      
      // Phase 1 - find out loops
      findLockingFunctions();      //JX - can be commented
      findLoopingFunctions();
      printLoopingFunctions();     //JX - write to local files. NO necessary, can be commented
      //findLoopingLockingFunctions();
      
      // Phase 2 - deal with loops
      findNestedLoopsInLoops();
      
      
      // init
      findTimeConsumingOperationsInLoops();     // for all loops
      //printTcOperationTypes();                //for test
      
      // Static Pruning
      staticPruningForCriticalLoops();

     
      
      // Phase 2 -
      //analyzeAllLocks();
      //analyzeLoopingLocks();
      
    } catch (Exception e) {
      System.err.println("JX-StackTrace-run-begin");
      e.printStackTrace();
      System.err.println("JX-StackTrace-run-end");
      return ;
    }
  }
  

  public static void init(Properties p) {
    // Read external arguments
    projectDir = p.getProperty("projectDir");
    appJarDir = p.getProperty("appJarDir");
    if (!projectDir.endsWith("/")) projectDir += "/";   //actually if we use File(xx, xx) or Path(xx, xx), we won't need this.
    if (!appJarDir.endsWith("/") )  appJarDir += "/";
    dtDir = Paths.get(projectDir, "src/dt/").toString();
    dmDir = Paths.get(projectDir, "src/dm/").toString();
  }
  
  
  public static void doWalaAnalysis() {
	  System.out.println("JX-doWalaAnalysis");
	  wala = new WalaAnalyzer(appJarDir);
	  systemname = Benchmarks.resolveSystem(appJarDir);
	  System.out.println("JX - DEBUG - system name = " + systemname);
	  cg = wala.getCallGrapth();
	  cha = wala.getClassHierarchy();
	  nPackageFuncs = wala.getNPackageFuncs();
	  nTotalFuncs = wala.getNTotalFuncs();
	  nApplicationFuncs = wala.getNApplicationFuncs();
	  nPremordialFuncs = wala.getNPremordialFuncs();
	  nOtherFuncs = wala.getNOtherFuncs();
  }

  
  public static void testQuickly() {
	    System.err.println("JX-breakpoint-testQuickly");
	    StackTraceElement[] traces = Thread.currentThread().getStackTrace();
	    String tmpstr = "";
	    for (int i = 1; i < traces.length; i++)
	      tmpstr += traces[i].getFileName().substring(0, traces[i].getFileName().indexOf('.'))+"."+traces[i].getMethodName()+":"+traces[i].getLineNumber() + "-";
	    System.out.println( tmpstr );
	    
	    System.out.println( new java.util.Date() );
	    java.text.DateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");    
	    System.out.println( sdf.format( new java.util.Date() ) );  
  }
 
  
 
//===============================================================================================
//+++++++++++++++++++++++++++++++++++++++++ Find RPCs +++++++++++++++++++++++++++++++++++++++++++
//===============================================================================================
  
  public static void findRPCs() {
	System.out.println("\nJX-findRPCs");

	switch (systemname) {
		case "HDFS":
			HDrpc hdrpc = new HDrpc(cha, projectDir+"src/sa/tc/");  
			hdrpc.doWork();
			break;
		case "MapReduce":
			MRrpc mrrpc = new MRrpc(cha, projectDir+"src/sa/tc/");
			mrrpc.doWork();
			HDrpc hdrpc2 = new HDrpc(cha, projectDir+"src/sa/tc/");  
			hdrpc2.doWork();
			break;
		case "HBase":
			break;
		default:
			break;
	}
  }
  
  static List<String> rpcMethodSigs = new ArrayList<String>();
  static List<String> ioMethodPrefixes = new ArrayList<String>();
  
  static public void readTimeConsumingOperations() {
	  System.out.println("\nJX-readTimeConsumingOperations");
	  String dirpath = projectDir + "src/sa/tc/";
	  String rpcfile = "";
	  String iofile = "";
	  String commonIOfile = dirpath + "res/io.txt";   //jx: io_specific.txt or io.txt
	  
	  switch (systemname) {
	  	case "HDFS":
	  		rpcfile = dirpath + "output/hd_rpc.txt";
	  		iofile =  dirpath + "res/hd_io.txt";
			break;
	  	case "MapReduce":
	  		rpcfile = dirpath + "output/mr_rpc.txt";
	  		iofile =  dirpath + "res/mr_io.txt";
			break;
	  	case "HBase":
	  		rpcfile = dirpath + "output/hb_rpc.txt";
	  		iofile =  dirpath + "res/hb_io.txt";
			break;
	  	default:
			break;
	  }  
	
	  BufferedReader bufreader;
	  String tmpline;
	  
	  //1. read RPC file
      int tmpnn = 0;
	  try {
		  bufreader = new BufferedReader( new FileReader( rpcfile ) );
		  tmpline = bufreader.readLine(); // the 1st line is useless
		  while ( (tmpline = bufreader.readLine()) != null ) {
			  String[] strs = tmpline.trim().split("\\s+");
			  if ( tmpline.trim().length() > 0 ) {
				  tmpnn++;
				  for (String str: strs)
					  rpcMethodSigs.add(str);
			  }
		  }
		  bufreader.close();
		
	  } catch (Exception e) {
		  // TODO Auto-generated catch block
		  System.out.println("JX - ERROR - when reading RPC files");
		  e.printStackTrace();
	  }
	  System.out.println("JX - successfully read " + tmpnn + "(total:" + rpcMethodSigs.size() + ") RPCs as time-consuming operations");
	  
	  //2. read IO file
	  try {
		  bufreader = new BufferedReader( new FileReader( commonIOfile ) );
		  tmpline = bufreader.readLine(); // the 1st line is useless
		  while ( (tmpline = bufreader.readLine()) != null ) {
			  String[] strs = tmpline.trim().split("\\s+");
			  if ( tmpline.trim().length() > 0 ) {
				  ioMethodPrefixes.add( strs[0] );
			  }
		  }
		  bufreader.close();
		  File f = new File( iofile );
		  if (f.exists()) {
			  bufreader = new BufferedReader( new FileReader( f ) );
			  tmpline = bufreader.readLine(); // the 1st line is useless
			  while ( (tmpline = bufreader.readLine()) != null ) {
				  String[] strs = tmpline.trim().split("\\s+");
				  if ( tmpline.trim().length() > 0 ) {
					  ioMethodPrefixes.add( strs[0] );
				  }
			  }
		  }
		  bufreader.close();
	  } catch (Exception e) {
		  // TODO Auto-generated catch block
		  System.out.println("JX - ERROR - when reading IO files");
		  e.printStackTrace();
	  }
	  System.out.println("JX - successfully read " + ioMethodPrefixes.size() + " IO Prefixes as time-consuming operations");
  }
  
  
//===============================================================================================
//++++++++++++++++++++++++++++++++++++++++ Find Locks +++++++++++++++++++++++++++++++++++++++++++
//===============================================================================================
  
  /**
   * Find Functions with locks
   * Note: Only focus on "Application" functions
   */
  
  
  public static void findLockingFunctions() {
      System.out.println("\nJX-findLockingFunctions");
    
      int nFiltered = 0;
      for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
	      CGNode f = it.next();
	      if ( !wala.isInPackageScope(f) ) continue;
	      
	      int id = f.getGraphNodeId();    
          String short_funcname = f.getMethod().getName().toString();
          if (locktypes.contains(short_funcname) || unlocktypes.contains(short_funcname)) //filter lock/unlock functions
              continue;
          List<LockInfo> locks = findLocks(f);
          if (locks.size() > 0) {
	          boolean verified = true;               //filter those functions cannot be figured out, ie, including "LockInfo.end_bb == -1", eg, readLock - NO readUnlock
	          for (Iterator<LockInfo> it_lock = locks.iterator(); it_lock.hasNext(); ) {
	              if (it_lock.next().end_bb == -1) {
	            	  System.err.println("Filtered function: " + f.getMethod().getSignature());
	            	  nFiltered++;
	            	  verified = false;
	            	  break;
	              }
	          }
	          if (!verified)
	              continue;
	          functions_with_locks.put(id, locks);
          }
      
      }//for
    
      nLockingFuncs = functions_with_locks.size();
    
      // Print the status
      int N_LOCKS = 20;
      int[] count = new int[N_LOCKS];
      count[0] = nPackageFuncs - nLockingFuncs;
      for (Iterator<Integer> it = functions_with_locks.keySet().iterator(); it.hasNext(); ) {
	      int id = it.next();
	      List<LockInfo> locks = functions_with_locks.get(id);
	      int size = locks.size();
	      nLocks += size;
	      //System.out.println(cg.getNode(id).getMethod().getSignature()); 
	      if (size < N_LOCKS) count[size]++;
	      /*
	      if (size == 5) {
	        System.out.println(cg.getNode(id).getMethod().getSignature());
	        System.out.println(locks);
	      }
	      */
      }
      System.out.println("The Status of Locks in All Functions:\n" 
          + "#scanned functions: " + nPackageFuncs 
          + " out of #Total:" + nTotalFuncs + "(#AppFuncs:" + nApplicationFuncs + "+#PremFuncs:" + nPremordialFuncs +")");
      System.out.println("#functions with locks: " + nLockingFuncs + "(" + nLocks + "locks)" + " (excluding " + nFiltered + " filtered functions that have locks)");
      // distribution of #locks
      System.out.println("//distribution of #locks");
      for (int i = 0; i < N_LOCKS; i++)
          System.out.print("#" + i + ":" + count[i] + ", ");
      System.out.println();
      // distribution of lock types
      Map<String, Integer> numOfLockTypes = new HashMap<String, Integer>();
      for (Iterator<Integer> it = functions_with_locks.keySet().iterator(); it.hasNext(); ) {
	      int id = it.next();
	      List<LockInfo> locks = functions_with_locks.get(id);
	      for (Iterator<LockInfo> it_2 = locks.iterator(); it_2.hasNext(); ) {
		        LockInfo lock = it_2.next();
		        if (!numOfLockTypes.containsKey(lock.lock_type))
		          numOfLockTypes.put(lock.lock_type, 1);
		        else
		          numOfLockTypes.put(lock.lock_type, numOfLockTypes.get(lock.lock_type)+1);
	      }
      }
      System.out.println("//distribution of lock types");
      for (Iterator<String> it = numOfLockTypes.keySet().iterator(); it.hasNext(); ) {
	      String s = it.next();
	      System.out.print("#" + s + ":" + numOfLockTypes.get(s) + ", ");
      }
      System.out.println("\n");
    
      //printFunctionsWithLocks();
  }
  
  
  /**
   * Not yet filter "synchronized (xx) {}" that located in "catch{}" or "finally{}"
   */
  public static List<LockInfo> findLocks(CGNode f) {
    
    int id = f.getGraphNodeId();
    IR ir = f.getIR();
    IMethod im = f.getMethod();
    String func_name = im.getSignature(); //im.getSignature().substring(0, im.getSignature().indexOf("("));
    //System.out.println(im.getSignature());
    
    SSACFG cfg = ir.getControlFlowGraph();
    SSAInstruction[] ssas = ir.getInstructions();
    boolean doPrimitives = false; // infer types for primitive vars?
    TypeInference ti = TypeInference.make(ir, doPrimitives);
    
    List<LockInfo> locks = new ArrayList<LockInfo>();
    
    // Handle "synchronized_method"              //seems I can't deal with locks in sync methods now, right? shall I?
    if (im.isSynchronized()) {  
      //if (im.isStatic()) System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! - im.isStatic() - " + func_name + "   class - " + im.getDeclaringClass().toString());
      LockInfo lock = new LockInfo();
      lock.func = f;
      lock.func_id = id;
      lock.func_name = func_name;
      lock.lock_type = synchronizedtypes.get(0);
      lock.lock_class = im.getDeclaringClass().toString();
      if (im.isStatic())
        lock.lock_name = "CLASS";   
      else
        lock.lock_name = "THIS";   
      lock.begin_bb = 0;
      lock.end_bb = cfg.exit().getNumber();      
      for (int i = lock.begin_bb; i <= lock.end_bb; i++)     
        lock.bbs.add(i);
      locks.add(lock);
      //printLocks(locks);
      return locks;
    }
    
    // Handle "synchronized_lock" and others
    int num = -1; //for Test
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      int bbnum = bb.getNumber();
      //System.err.println(bbnum);
      if (bbnum != ++num) System.err.println("bbnum != ++num");  //for Test
      for (Iterator<SSAInstruction> it_2 = bb.iterator(); it_2.hasNext(); ) {
        SSAInstruction ssa = it_2.next();
        // Handle "synchronized_lock"   //TODO - for now, we filter "synchronized(argu)", maybe should do in the future   
        if (ssa instanceof SSAMonitorInstruction) {
          SSAMonitorInstruction monitorssa = (SSAMonitorInstruction) ssa;  
          if ( monitorssa.isMonitorEnter() ) {   //JX - monitorenter 75
            LockInfo lock = new LockInfo();
            lock.func = f;
            lock.func_id = id;
            lock.func_name = func_name;            
            lock.lock_type = synchronizedtypes.get(1);
            lock.lock_name_vn = monitorssa.getRef();  //only for synchronized_lock now
            // Get lock.lock_class & lock.lock_name
            lock.lock_class = "??????????????";       //usually like synchronized(object) is good, but synchronized(xxx.object) is bad.
            lock.lock_name = "";
            getPreciseLockForSyncCS(f, ssa, lock);  //ps - ssa is the monitor SSA
            lock.begin_bb = bbnum;
            lock.end_bb = -1;
            // Get the basic block set for this lock
            lock.succbbs.add(lock.begin_bb);
            lock.dfsFromEnter(cfg.getNode(lock.begin_bb), cfg);
            /*
            current_stack.clear(); current_stack.add(lock.begin_bb); 
            traversed_nodes.clear(); traversed_nodes.add(lock.begin_bb);
            dfsToGetBasicBlocksForLock(1, bb, cfg, lock);
            */
            locks.add(lock);     
          } else { //exit
            int vn = monitorssa.getRef();
            for (int i = locks.size()-1; i>=0; i--) {
              LockInfo lock = locks.get(i);
              if (lock.lock_name_vn == vn) {  //should be pointer, is it right?
                lock.end_bb = bbnum;  //it seems we can't deal with two successive "synchronized (this){}", because they have the same vn????  
                // Get the basic block set for this lock
                lock.predbbs.clear(); 
                lock.predbbs.add(bbnum);
                lock.dfsFromExit(cfg.getNode(bbnum), cfg);
                lock.mergeLoop();
                break;
              }
            } //for-i
          } //else
        }
        
        // Handle other "lock"s, eg. lock, readLock, writeLock, ...
        else if (ssa instanceof SSAInvokeInstruction) {
          String short_funcname = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getName().toString();
          if (locktypes.contains(short_funcname)) {
            if (!short_funcname.equals("tryLock") && ssa.hasDef()) { //filter "tryLock" that returns a value, and forms like "'hostmapLock.readLock()'.lock()" which have about 10 out of 623
              continue;
            }
            LockInfo lock = new LockInfo();
            lock.func = f;
            lock.func_id = id;
            lock.func_name = func_name;
            lock.lock_type = short_funcname;
            lock.lock_class = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getDeclaringClass().toString();   // not precise
            lock.lock_name = "???";
            
            //System.err.println("funcname: " + func_name);
            ////System.err.println("previous ssa: " + ssa);
            ////System.err.println("ssa: " + ssa);
            //System.err.println("lock_class: " + lock.lock_class);
            //System.err.println("lock_name: " + lock.lock_name);
            
            lock.begin_bb = bbnum;
            lock.end_bb = -1;
            // Get the basic block set for this lock
            lock.succbbs.add(lock.begin_bb);
            lock.dfsFromEnter(cfg.getNode(lock.begin_bb), cfg);
            /*
            current_stack.clear(); current_stack.add(lock.begin_bb); 
            traversed_nodes.clear(); traversed_nodes.add(lock.begin_bb);
            dfsToGetBasicBlocksForLock(1, bb, cfg, lock);
            */
            locks.add(lock);     
          } else if (unlocktypes.contains(short_funcname)) {
            String lock_class = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getDeclaringClass().toString();
            for (int i = locks.size()-1; i>=0; i--) {
              LockInfo lock = locks.get(i);
              if (lock.lock_class.equals(lock_class) && mapOfLocktypes.get(lock.lock_type).equals(short_funcname)) {  //maybe these conditions are still insufficient
                if (bbnum > lock.end_bb)
                  lock.end_bb = bbnum;  //it seems we can't deal with two successive "synchronized (this){}", because they have the same vn????  
                // Get the basic block set for this lock
                lock.predbbs.clear(); 
                lock.predbbs.add(bbnum);
                lock.dfsFromExit(cfg.getNode(bbnum), cfg);
                lock.mergeLoop();
                break;
              }
            } //for-i
          }
        } else { //other SSAs
        }
 
      } //for-it_2
    } //for-it 
    //System.out.println("JX-debug-3");
    //printLocks(locks);
    return locks;
  }
   

  static int print_num = 0;
  /**
   * Note: track SSAs Back To Get Precise Lock For SyncCS
   * @param ssa - the monitorenter SSA
   */
  public static void getPreciseLockForSyncCS(CGNode function, SSAInstruction ssa, LockInfo lock) {
    
    IMethod im = function.getMethod();
    IR ir = function.getIR();
    SSACFG cfg = ir.getControlFlowGraph();
    SSAInstruction[] ssas = ir.getInstructions();
    
    String func_name = im.getSignature(); //im.getSignature().substring(0, im.getSignature().indexOf("("));
    
    // 1. synchronized (this)                       //PS - vn(n>=1) is used for single method, v1=this, v2=par1, v3=par2... ; for static methods, there is not "this", so v1=par1   
    if (lock.lock_name_vn == 1 && !im.isStatic()) { //vn=1 & !im.isStatic, 'this'
      lock.lock_class = im.getDeclaringClass().toString();
      lock.lock_name = "THIS";
      return ;
    } 
    
    // 2. synchronized (argu), agru from method parameters        
    if ( isPrimordialVn(function, lock.lock_name_vn) ) {  //vn in ([par1=this,] par2, par3, par4 ...)
      int index = getSSAIndexBySSA(ssas, ssa); 
      if (index == -1) {
        System.err.println("ERROR - sync(argu) - (index = -1)");
        return;
      }
      lock.lock_class = "???????from method parameter, filtered now; actually it should be upward searched to find the fields";  // TODO
      //if (ir.getLocalNames(index, lock.lock_name_vn) != null)
      lock.lock_name = "ARGU- "+ ir.getLocalNames(index, lock.lock_name_vn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
      return ;
    }
    
    // get succ SSA of the monitorenter SSA (para ssa)
    int index = getSSAIndexByDefvn(ssas, lock.lock_name_vn, "3.synchronized(class/object/this.object)");
    if (index == -1) { //phi,pi can't be found in ssas,TODO if needed; Eg, phi like v49 = phi v37,v35, eg, sync(block) in Balancer$Source.getBlockList()J
      System.err.println("ERROR - the succ SSA of monitor is non-existing!!! - SSA:" + ssa);
      //System.err.println("ERROR - " + "funcname: " + func_name);        // TODO - integrate into "dfsTrackSSAs"
      //System.err.println("ERROR - " + "ssa: " + ssa);
      //System.err.println("ERROR - " + "lock_type: " + lock.lock_type);
      lock.lock_class = im.getDeclaringClass().toString(); //!!!!
      lock.lock_name = "?????eg phi, Usually local obj? filter it ??"; //Eg, sync(block) in Balancer$Source.getBlockList()J
      return ;
    }
    // 3. synchronized(class/object/this.object), not synchronized(xxx.object)    //ie, vn > maxOfParameters, that is, vn is not 'this' and parameters of the method
    dfsTrackSSAs(function, ssas[index], ssa, lock);    // current: ssas[index], pred: ssa
    
    /*
    // Print for a single CS/lock if needed
    if (!(lock.lock_name.indexOf("THIS")==0 || lock.lock_name.indexOf("-GetField-")==0 || lock.lock_name.indexOf("CLASS")==0))
    {
    System.err.println("---------------------" + (print_num++) + "---------------------");
    System.err.println("funcname: " + func_name);
    System.err.println("ssa: " + ssa);
    System.err.println("succssa: " + ssas[index]);
    System.err.println("lock_type: " + lock.lock_type);    
    System.err.println("lock_class: " + lock.lock_class);
    System.err.println("lock_name: " + lock.lock_name);
    }
    */
  }

  
  public static void dfsTrackSSAs(CGNode function, SSAInstruction ssa, SSAInstruction predssa, LockInfo lock) {
       
    IMethod im = function.getMethod();
    IR ir = function.getIR();
    SSAInstruction[] ssas = ir.getInstructions();
    
    if (ssa instanceof SSALoadMetadataInstruction) {  // 3.1 synchronized (ClassName.class from LoadMetadata)   #only two org.apache.hadoop.conf.Configuration.<init>s are not static methods 
      SSALoadMetadataInstruction loadssa = (SSALoadMetadataInstruction) ssa;
      //System.err.println("synchronized (ClassName.class) - " + ssas[index] + " in a static " + im.isStatic() + " method?");
      lock.lock_class = loadssa.getToken().toString();    //previous usage should be wrong: im.getDeclaringClass().toString();
      lock.lock_name += "CLASS"; //((SSALoadMetadataInstruction)ssas[index]).getType().toString();
      //System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! - synchronized (ClassName.class) - " + func_name + "   class - " + lock.lock_class);
      return ;
    }
    
    /**         
     * Note: getfield SSAs, like "41 = getfield < Application, Lorg/apache/hadoop/hdfs/server/balancer/Balancer$Source, this$0, <Application,Lorg/apache/hadoop/hdfs/server/balancer/Balancer> > 1",
     *       all have a use vn, can be v1(this) and others like v8, v63; that means maybe v8/v63 = getfield .. v1 and so on.
     *       but for now, it seems we just need to stop at the first getfield, that's ok.
     *            * E.g., (2 examples now)
     * Scenario 1 (var in parameter, so ok, a little diff form ARGU)
     * Function - org.apache.hadoop.hdfs.server.datanode.BlockSender.<init>: DataNode datanode(para, v8), sycn(datanode.data)
     * SSAs - v37 = getfield < Application, Lorg/apache/hadoop/hdfs/server/datanode/DataNode, data, <Application,Lorg/apache/hadoop/hdfs/server/datanode/fsdataset/FsDatasetSpi> > v8
     *        monitorenter v37
     * 
     * WARN - for synchronized (OuterClass.this) (20+ for hdfs-2.3.0)
     * funcname: org.apache.hadoop.hdfs.server.balancer.Balancer$Source.dispatchBlocks(): synchronized(Balancer.this) 
     * previous ssa - 41 = getfield < Application, Lorg/apache/hadoop/hdfs/server/balancer/Balancer$Source, this$0, <Application,Lorg/apache/hadoop/hdfs/server/balancer/Balancer> > 1
     * lock_class: <Application,Lorg/apache/hadoop/hdfs/server/balancer/Balancer$Source>  //jx: wrong, should be its outerclass
     * lock_name: < Application, Lorg/apache/hadoop/hdfs/server/balancer/Balancer$Source, this$0, <Application,Lorg/apache/hadoop/hdfs/server/balancer/Balancer> >
     * Now hanlding - manually change above to "xxx.Balancer" & "THIS" if there is a 'this$0'
     */
    if (ssa instanceof SSAFieldAccessInstruction) {  // 3.2 synchronized (this.object/object/OuterClass.this from GetField)   #eg, like "v2=getfield<xx..xx>v1"   //whether "getstatic" SSAs like "v2=getstatic<xx..xx>" is involved or not?                     
      SSAFieldAccessInstruction fieldssa = (SSAFieldAccessInstruction) ssa;
      lock.lock_class = fieldssa.getDeclaredField().getDeclaringClass().toString();    //verified: = im.getDeclaringClass().toString();      
      lock.lock_name += "-GetField-" + fieldssa.getDeclaredField().toString();
      // for sync(OuterClass.this)
      if ( fieldssa.getDeclaredField().getName().toString().equals("this$0") ) {
        lock.lock_class = fieldssa.getDeclaredFieldType().toString();
        lock.lock_name = "THIS";
      }
      // for static var
      if (fieldssa.isStatic()) {
        lock.lock_name = "CLASS: " + lock.lock_name;
        if (lock.lock_name.equals("CLASS: THIS"))
          System.err.println("ERROR - CLASS: THIS - please please have a look");
      }
      return ;
    } 
    
    if (ssa instanceof SSAInvokeInstruction) { // 3.3 synchronized (object from a call - Invokexxx + GetField)
      SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
      if (invokessa.isDispatch()) {      //3.3.1 invokeinterface?/invokevirtual + getfield, ie, sync(obj from a call), hdfs-2.3.0-org.apache.hadoop.util.Shell.runCommand:stdout=process.getInputStream(),preocess is Shell's var #eg, 87 = invokevirtual < Application, Ljava/lang/Process, getInputStream()Ljava/io/InputStream; > 85 @352 exception:86
        lock.lock_name += "-InvokeVirtual/InvokeInterface-" + invokessa.getDeclaredTarget().toString() + " in ";
        int usevn = invokessa.getUse(0);
        if ( isPrimordialVn(function, usevn) ) {
          lock.lock_name += "-ARGU-" + ir.getLocalNames(getSSAIndexBySSA(ssas, ssa), usevn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
          return ;
        }
        int index = getSSAIndexByDefvn(ssas, usevn, "sync(object coming from invokevitural)");
        if (index == -1) { 
          System.err.println("ERROR - sync(object coming from invokevitural) - (index = -1)");
          return ;
        }
        dfsTrackSSAs(function, ssas[index], ssa, lock);
      }
      /**
       * Examples - 
       * Function1: org.apache.hadoop.hdfs.DFSOutputStream$DataStreamer$ResponseProcessor.run(): sycn(dataQueue);  dataQueue is a var in the outermost class of 'DFSOutputStream'
       * SSAs: (3) v66 = getfield < Application, Lorg/apache/hadoop/hdfs/DFSOutputStream$DataStreamer$ResponseProcessor, this$1, <Application,Lorg/apache/hadoop/hdfs/DFSOutputStream$DataStreamer> > v1
       *       (2) v67 = getfield < Application, Lorg/apache/hadoop/hdfs/DFSOutputStream$DataStreamer, this$0, <Application,Lorg/apache/hadoop/hdfs/DFSOutputStream> > v66
       *       (1) v69 = invokestatic < Application, Lorg/apache/hadoop/hdfs/DFSOutputStream, access$800(Lorg/apache/hadoop/hdfs/DFSOutputStream;)Ljava/util/LinkedList; > v6
       * Now handling - only get into (1), regardless of (2)&(3);  if we want to use (2) like 3.2-Getfield, we still don't need (3)
       * Function2 - org.apache.hadoop.hdfs.server.namenode.ha.StandbyCheckpointer$CheckpointerThread.doWork(): sync(cancelLock); cancelLock is a var in outerclass of 'StandbyCheckpointer'
       * SSAs - (2)- v100 = getfield < Application, Lorg/apache/hadoop/hdfs/server/namenode/ha/StandbyCheckpointer$CheckpointerThread, this$0, <Application,Lorg/apache/hadoop/hdfs/server/namenode/ha/StandbyCheckpointer> > v1   //now we haven't use this SSA
       *        (1)- v102 = invokestatic < Application, Lorg/apache/hadoop/hdfs/server/namenode/ha/StandbyCheckpointer, access$1100(Lorg/apache/hadoop/hdfs/server/namenode/ha/StandbyCheckpointer;)Ljava/lang/Object; > v100     
       * Now handling - we don't use (2), we only get into (1)access$1100, we must be, because var name is in access$1100. ps - cg.getPossibleTargets(function, invokessa.getCallSite())=1
       * lock_class: <Application,Lorg/apache/hadoop/hdfs/server/namenode/ha/StandbyCheckpointer>  
       * lock_name: -InvokeStatic--GetField-< Application, Lorg/apache/hadoop/hdfs/server/namenode/ha/StandbyCheckpointer, cancelLock, <Application,Ljava/lang/Object> >
       */
      /**
       * PS - WARN
       * hadoop-1.1.0/1.0.0 - org.apache.hadoop.mapred.JobEndNotifier.localRunnerNotification<static>: sync(Thread.currentThread)
       * Now handling - we can't deal with for now
       */
      else if (invokessa.isStatic()) { //3.3.2 invokestatic + getfield, ie, sync(obj which is outerclass's var)  #eg, v27 = invokestatic < Application, Lorg/apache/hadoop/hdfs/server/balancer/Balancer, access$2000(Lorg/apache/hadoop/hdfs/server/balancer/Balancer;)Ljava/util/Map; > v25 @75 exception:26
        lock.lock_name += "-InvokeStatic-";
        java.util.Set<CGNode> set = cg.getPossibleTargets(function, invokessa.getCallSite());
        if (set.size() == 0) {
          System.err.println("invokessa.getCallSite - " + invokessa.getCallSite());
          System.err.println("ERROR - Handling invokestatic - CallGraph#getPossibleTargets's size = 0 - because the class's SUPERCLASS isn't included in these JarFiles"); // for Test, how to solve the problem??
          return ;
        }
        //System.err.println("cg.getPossibleTargets(function, invokessa.getCallSite())=" + set.size());
        if (set.size() >  1) System.err.println("ERROR - Handling invokestatic - CallGraph#getPossibleTargets's size > 1"); // for Test, how to solve the problem??
        if (set.size() > 0) {            //JX: because I haven't yet added "hadoop-common"
          CGNode n = set.iterator().next(); 
          SSAInstruction[] invokessas = n.getIR().getInstructions();
          int index = -1;
          for (int i=0; i<invokessas.length; i++)                            
            if (invokessas[i] instanceof SSAFieldAccessInstruction) {     //like, a "getField" ssa in Balancer.access$2000
              index = i;
              break;
            }
          if (index == -1) {
            System.err.println("ERROR - !(invokessas[i] instanceof SSAFieldAccessInstruction)");
            return;
          }
          dfsTrackSSAs(n, invokessas[index], ssa, lock); //get into, diff from others
        }
      }
      /**
       * WARN -
       * hadoop-1.1.0/1.0.0 - org.apache.hadoop.metrics.spi.AbstractMetricsContext.remove(MetricsRecordImpl;)V: RecordMap recordMap = getRecordMap(recordName):return bufferedData.get(recordName);; synchronized (recordMap)      
       * hadoop-1.1.0/1.0.0 - org.apache.hadoop.mapred.TaskTracker.localizeJob:  RunningJob rjob = addTaskToJob(jobId, tip):rJob = new RunningJob(jobId); runningJobs.put(jobId, rJob); | rJob = runningJobs.get(jobId);  synchronized (rjob)
       */
      else if (invokessa.isSpecial()) { // 3.3.3 invokespecial?
        lock.lock_class = "????????invokespecial";  //like invokeinterface/invokevirtual?, that is, invokessa.getDeclaredTarget().getDeclaringClass().toString();
        lock.lock_name = "-InvokeSpecial-" + invokessa.getDeclaredTarget().toString();
        System.err.println("WARN - SSAInvokeInstruction isSpecial - " + invokessa);
      } else 
        System.err.println("ERROR - other SSAInvokeInstruction? - " + invokessa);
    }
    else if (ssa instanceof SSACheckCastInstruction) { // 3.4 synchronized (object from CheckCast + InvokeVirtual + GetField)
      SSACheckCastInstruction checkcastssa = (SSACheckCastInstruction) ssa;
      lock.lock_name += "-CheckCast-";
      int usevn = checkcastssa.getUse(0); 
      if ( isPrimordialVn(function, usevn) ) {
        lock.lock_name += "-ARGU-" + ir.getLocalNames(getSSAIndexBySSA(ssas, ssa), usevn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
        return ;
      }
      int index = getSSAIndexByDefvn(ssas, usevn, "sync(object coming from chestcast<+invokevitural>)");
      if (index == -1) { 
        System.err.println("ERROR - sync(object coming from chestcast<+invokevitural>) - (index = -1)");
        return;
      }
      dfsTrackSSAs(function, ssas[index], ssa, lock);
    }
    else if (ssa instanceof SSAArrayReferenceInstruction) { // 3.5 synchronized (object from ArrayReference + GetField)
      SSAArrayReferenceInstruction arrayrefssa = (SSAArrayReferenceInstruction) ssa;
      lock.lock_name += "-ArrayReference-" + "ELEMENT in "; //ps-ELEMENT=arrayrefssa.getElementType();  //jx - "getDeclaredTarget" is part of getCallSite
      int usevn = arrayrefssa.getUse(0);      
      if ( isPrimordialVn(function, usevn) ) {
        lock.lock_name += "-ARGU-" + ir.getLocalNames(getSSAIndexBySSA(ssas, ssa), usevn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
        return ;
      }
      int index = getSSAIndexByDefvn(ssas, usevn, "sync(object from ArrayReference + GetField)");
      if (index == -1) { 
        System.err.println("ERROR - sync(object from ArrayReference <+ GetField>) - (index = -1)");
        return;
      }
      dfsTrackSSAs(function, ssas[index], ssa, lock);
    }             //move up to the most front?? ie, the last ssa??
    else if (ssa instanceof SSANewInstruction) { // 3.6 synchronized (object from New)  must be local object??????
      SSANewInstruction newssa = (SSANewInstruction) ssa;
      lock.lock_class = im.getDeclaringClass().toString();    //??
      //int usevn = newssa.getUse(0);
      //if (usevn == -1) {
      lock.lock_name += "-New-" + "LOCALVAR-" + ir.getLocalNames(getSSAIndexBySSA(ssas, predssa), newssa.getDef())[0] + " in " + im.getSignature();
      /*
      } else {    // should be non-existing
        if ( isPrimordialVn(function, usevn) ) {
          lock.lock_name += "-ARGU-" + ir.getLocalNames(getSSAIndexBySSA(ssas, ssa), usevn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
          return ;
        }
        System.err.println("WARN - newssa.getUse(0) != -1, equals " + newssa.getUse(0));
        int index = getSSAIndexBySSA(ssas, ssa);
        if (index == -1) {
          System.err.println("ERROR - sync(obj from New) - (index = -1)");
          return ;
        }
        lock.lock_name += "-New-" + "LOCALVAR-" + ir.getLocalNames(index, usevn)[0] + " in " + im.getSignature();  
      }
      */
      //if (ir.getLocalNames(index, lock.lock_name_vn) != null)
    }
    else { // 3.7 other synchronized (xx), that is, other SSAInstructions
      //TODO - maybe something
      System.err.println("WARN - other SSAInstruction, please have a look - " + ssa);
    }
  }
  
  public static boolean isPrimordialVn(CGNode function, int vn) {
    IMethod im = function.getMethod();
    IR ir = function.getIR();
    if (!im.isStatic() && vn-1 <= ir.getNumberOfParameters()
        || im.isStatic() && vn <= ir.getNumberOfParameters())
      return true;
    return false;
  }
  
  public static int getSSAIndexBySSA(SSAInstruction[] ssas, SSAInstruction ssa) {
    int index = -1;
    for (int i=0; i < ssas.length; i++)
      if (ssas[i] != null)
        if (ssas[i].equals(ssa)) { 
          index = i; 
          break; 
        }
    return index;
  }
  /**
   * @param ssas
   * @param defvn
   * @param errmsg
   * @return index; -1 means null;
   * Note: finding format like "v27 = invokevirtual/checkcast <xxx, xxx, xxx> vx, vy"
   */
  public static int getSSAIndexByDefvn(SSAInstruction[] ssas, int defvn, String errmsg) {
    int index = -1; int num_index = 0;
    for (int i=0; i < ssas.length; i++)
      if (ssas[i] != null) {
        if (ssas[i].getDef() == defvn) { index = i; /*break;*/ }
        if (ssas[i].getDef() == defvn) { if (++num_index > 1) System.err.println("ERROR - getSSAIndexByDefvn - (++num_index > 1) - " + errmsg); }
      }
    return index;
  }
  
  /*
  static List<Integer> current_stack = new ArrayList<Integer>();
  static Set<Integer> traversed_nodes = new HashSet<Integer>(); 
  
  public static void dfsToGetBasicBlocksForLock(int layer, ISSABasicBlock bb, SSACFG cfg, LockInfo lock) {
  
    if (lock.isMatched(bb)) {
      for (int i = 0; i < layer; i++)
        lock.bbs.add(current_stack.get(i));
      return;
    }
    
    for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext(); ) {
      ISSABasicBlock succ = it.next();
      int succnum = succ.getNumber();
      if (!traversed_nodes.contains(succnum)) {
        traversed_nodes.add(succnum);
        if (current_stack.size() <= layer) current_stack.add(-1);
        current_stack.set(layer, succnum);
        dfsToGetBasicBlocksForLock(layer+1, succ, cfg, lock);
      }
      else { //traversed
        if (lock.bbs.contains(succnum)) {
          for (int i = 0; i < layer; i++)
            lock.bbs.add(current_stack.get(i));
        }
      }
    }//for-it
  }
  */

  
  public static void printFunctionsWithLocks() {
    //print all locks for those functions with locks
    for (Iterator<Integer> it = functions_with_locks.keySet().iterator(); it.hasNext(); ) {
      int id = it.next();
      List<LockInfo> locks = functions_with_locks.get(id);
      System.out.println(cg.getNode(id).getMethod().getSignature()); 
      printLocks(locks);
    }
  }
  
  public static void printLocks(List<LockInfo> locks) {
    // Print the function's Locks
    System.out.print("#locks-" + locks.size() + " - ");
    for (Iterator<LockInfo> it = locks.iterator(); it.hasNext(); ) {
      System.out.print (it.next() + ", ");
    }
    System.out.println();
  }
  
  
  
  
  /****************************************************************************************************
   * Find functions with loops
   * Note: we just focus on "Application" functions, without "Primordial" functions
   * @throws IOException 
   **************************************************************************************************
   */
  public static void findLoopingFunctions() throws IOException {
    System.out.println("\nJX-findLoopingFunctions");

    int totalloops = 0;
    
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
        CGNode f = it.next();
        if ( !wala.isInPackageScope(f) ) continue;
      
        int id = f.getGraphNodeId();
        // Find loops for each function
        List<LoopInfo> loops = findLoops(f);
        if (loops.size() > 0) {
          functions_with_loops.put(id, loops);
          totalloops += loops.size();
        }
    }
    nLoopingFuncs = functions_with_loops.size();
    nLoops = totalloops;
    
    // Print the status
    int N_LOOPS = 20;
    int[] count = new int[N_LOOPS];
    count[0] = nPackageFuncs - nLoopingFuncs;
    for (Iterator<List<LoopInfo>> it = functions_with_loops.values().iterator(); it.hasNext(); ) {
      int size = it.next().size();
      if (size < N_LOOPS) count[size]++;
    }
    System.out.println("The Status of Loops in All Functions:\n" 
        + "#scanned functions: " + nPackageFuncs 
        + " out of #Total:" + nTotalFuncs + "(#AppFuncs:" + nApplicationFuncs + "+#PremFuncs:" + nPremordialFuncs +")");    
    System.out.println("#functions with loops: " + nLoopingFuncs + " (#loops:" + nLoops + ")");
    System.out.println("//distribution of #loops");
    for (int i = 0; i < N_LOOPS; i++)
      System.out.print("#" + i + ":" + count[i] + ", ");
    System.out.println("\n");
    
  }
  
  
  // Find loops for each function
  public static List<LoopInfo> findLoops(CGNode f) {
    IR ir = f.getIR();
    SSACFG cfg = ir.getControlFlowGraph();
    //newly added - for source line number
    SSAInstruction[] ssas = ir.getInstructions();
    IBytecodeMethod bytecodemethod = (IBytecodeMethod) ir.getMethod();
    
    List<LoopInfo> loops = new ArrayList<LoopInfo>();
    int n_backedges = 0; //for Test
    int num = -1; //for Test
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      int bbnum = bb.getNumber();
      if (bbnum != ++num) System.err.println("bbnum != ++num");  //for Test
      for (IntIterator it_2 = cfg.getSuccNodeNumbers(bb).intIterator(); it_2.hasNext(); ) {
        int succ = it_2.next();
        if (succ < bbnum) {    //something like "catch" basic blocks have self-loops, so using "<". yes!!
          n_backedges ++;  //for Test
          //if (cfg.getSuccNodeCount(bb) != 1) System.err.println("for-exit basic block: cfg.getSuccNodeCount(bb) != 1" + "  how many:" + cfg.getSuccNodeCount(bb));  //for Test
          int existed = -1;
          for (int i = 0; i < loops.size(); i++)
            if (loops.get(i).begin_bb == succ) {
              existed = i;
              break;
            }
          if (existed == -1) { //the for hasn't yet been recorded  
            LoopInfo loop = new LoopInfo();   
            loop.begin_bb = succ;
            loop.end_bb = bbnum;
            loop.function = f;  //for the future
            loop.line_number = getSourceLineNumberFromBB( cfg.getBasicBlock(loop.begin_bb), ssas, bytecodemethod );
            //loop.var_name = ???
            // Get the basic block set for this loop
            loop.succbbs.add(loop.begin_bb);
            loop.dfsFromEnter(cfg.getNode(loop.begin_bb), cfg);
            loop.predbbs.add(loop.end_bb);
            loop.dfsFromExit(cfg.getNode(loop.end_bb), cfg);
            loop.mergeLoop();
            loops.add(loop);
          } else {            //the for has been recorded 
            LoopInfo loop = loops.get(existed);
            if (bbnum > loop.end_bb)
              loop.end_bb = bbnum;  //is it right? yes for now
            loop.predbbs.clear(); 
            loop.predbbs.add(bbnum);
            loop.dfsFromExit(cfg.getNode(bbnum), cfg);
            loop.mergeLoop();
          }
        }
      } //for-it_2
    } //for-it
    
    // for Test: #backedges by computeBackEdges - #self-loop = what I find by myself
    IBinaryNaturalRelation backedges = Acyclic.computeBackEdges(cfg, cfg.entry());
    int total = 0;
    for (Iterator<IntPair> it = backedges.iterator(); it.hasNext(); ) {
      IntPair ip = it.next();
      if (ip.getX() != ip.getY()) total ++;
    } 
    if (total != n_backedges) {  //for Test
      System.err.println("total != n_backedges  #backedges:" + total + " #real backedges:" + n_backedges + " Method:" + f.getMethod().getSignature());
    }
   
    //printLoops(loops);
    
    return loops;
  }
 
  
  public static void printLoopingFunctions() throws IOException {
	System.out.println("\nJX-printLoopingFunctions");
  	File loopfile = new File(appJarDir, "looplocations");
  	BufferedWriter bufwriter = new BufferedWriter(new FileWriter(loopfile));
  	
    // Print all loops - for test
  	/*
  	bufwriter.write( nLoopingFuncs + " " + nLoops + "\n" );
    for (List<LoopInfo> loops: functions_with_loops.values()) {
      //System.err.println(loops.get(0).function.getMethod().getSignature());
      bufwriter.write( loops.get(0).function.getMethod().getSignature() + " " );
      printLoops(loops, bufwriter);
    }
    */
    
    // Print all the loops that are in the "package-scope.txt"
    // ps: it will print ALL if without 'package-scope.txt'
    int nfuncsInScope = 0;
    int nloopsInScope = 0;
    for (List<LoopInfo> loops: functions_with_loops.values())
    	if ( wala.isInPackageScope(loops.get(0).function) ) {
    		nfuncsInScope ++;
    		nloopsInScope += loops.size();
    	}
    bufwriter.write( nfuncsInScope + " " + nloopsInScope + "\n" );
    for (List<LoopInfo> loops: functions_with_loops.values())
    	if ( wala.isInPackageScope(loops.get(0).function) ) {
    		bufwriter.write( loops.get(0).function.getMethod().getSignature() + " " );
    		printLoops(loops, bufwriter);
    	}
    	
    bufwriter.close();
    
    // copy the result of "looplocations" to the common directory of dt
    java.nio.file.Path newpath = Paths.get(dtDir, "res/looplocations");
    Files.copy(loopfile.toPath(), newpath, StandardCopyOption.REPLACE_EXISTING);
    System.out.println("Successfully write loops into " + loopfile.toString() + " & " + newpath.toString());
  }
  
  public static void printLoops(List<LoopInfo> loops, BufferedWriter bufwriter) throws IOException {
    // Print the function's loops
    //System.err.print("#loops=" + loops.size() + " - ");
    bufwriter.write( loops.size() + " " );
    for (LoopInfo loop: loops) {
      // print normal	
      //System.err.print(loop + ", ");
      // print source line number
      //System.err.print(loop.line_number + ", ");
      bufwriter.write( loop.line_number + " " );
    }
    //System.err.println();
    bufwriter.write( "\n" );
  }
 
  // 
  public static int getSourceLineNumberFromBB(ISSABasicBlock bb, SSAInstruction[] ssas, IBytecodeMethod bytecodemethod) {
	  
      for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext(); ) {
          SSAInstruction ssa = it.next();
          int index = getSSAIndexBySSA(ssas, ssa); 
          if (index != -1) {
        	  
			try {
				int bytecodeindex = bytecodemethod.getBytecodeIndex( index );
				int sourcelinenum = bytecodemethod.getLineNumber( bytecodeindex );
	            if (sourcelinenum != -1) 
	            	  return sourcelinenum;
			} catch (InvalidClassFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
          }
      }
	  
	  return -1;
  }
  
  public static int getSourceLineNumberFromSSA(SSAInstruction ssa, SSAInstruction[] ssas, IBytecodeMethod bytecodemethod) {
      int index = getSSAIndexBySSA(ssas, ssa); 
      if (index != -1) {
		try {
			int bytecodeindex = bytecodemethod.getBytecodeIndex( index );
			int sourcelinenum = bytecodemethod.getLineNumber( bytecodeindex );
            if (sourcelinenum != -1) 
            	  return sourcelinenum;
		} catch (InvalidClassFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      }
	  return -1;
  }
  
  
  
  
  /**************************************************************************
   * New added - JX - just find time-consuming operations    
   **************************************************************************/
  public static void findTimeConsumingOperationsInLoops() {
	  System.out.println("\nJX-findTimeConsumingOperationsInLoops");
	  // Initialize Time-consuming operation information by DFS for all looping functions
	  BitSet traversednodes = new BitSet();
	  traversednodes.clear();
	  for (Integer id: functions_with_loops.keySet() ) {
		  dfsToGetTimeConsumingOperations(cg.getNode(id), 0, traversednodes);
	  }
	  // Deal with the outermost loops
	  for (Integer id: functions_with_loops.keySet() ) {
		  List<LoopInfo> loops = functions_with_loops.get(id);
		  for (LoopInfo loop: loops)
			  findTimeConsumingOperationsForALoop( loop );  
	  }
	  
	  // Print - Test
	  for (List<LoopInfo> loops: functions_with_loops.values() )
		  for (LoopInfo loop: loops) {
			  if (loop.numOfTcOperations_recusively > 0) {
				  //System.out.println( loop );
			  }
		  }
	  System.out.println("success!");
  }
  
  
  public static int dfsToGetTimeConsumingOperations(CGNode f, int depth, BitSet traversednodes) {
    
	// for test - the depth can reach 58
    /*
	if (depth > 50) {
		System.err.println("JX - WARN - depth > " + depth);
	}
	*/

	if ( !f.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) 
	     || f.getMethod().isNative()) { // IMPO - native - must be
	    return 0;
	}

	int id = f.getGraphNodeId();
    if ( !functions.containsKey(id) ) {
    	FunctionInfo function = new FunctionInfo();
        functions.put(id, function);
    }
    FunctionInfo function = functions.get(id);
    
	if ( traversednodes.get( id ) )            // if has already been traversed, then return
    	return function.numOfTcOperations_recusively; //maybe 0(unfinished) or realValue(finished)
       
    traversednodes.set( id );                  // if hasn't been traversed
    function.numOfTcOperations = 0;            // "0" RIGHT NOW to prevent this scenario: func1 -call-> func2 -> func1(NOW func1 should be forbidden)
    function.numOfTcOperations_recusively = 0; // this value should keep 0 until go through the function, because it may be return in the process 

    IR ir = f.getIR();  //if (ir == null) return;
    SSAInstruction[] instructions = ir.getInstructions();
    int numOfTcOperations = 0;              //tmp var
    int numOfTcOperations_recusively = 0;  
 
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction ssa = instructions[i];
      if (ssa == null)
    	  continue;

      if ( checkTimeConsumingSSA(ssa) ) {
    	  function.tcOperations.add( ssa );
    	  numOfTcOperations ++;
    	  numOfTcOperations_recusively ++;
    	  continue;
      }
      // filter the rest I/Os
      if ( isIO(ssa) )
    	  continue;
      
      // if meeting a normal call(NOT RPC and I/O), Go into the call targets
      if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
    	  SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;   
    	  // get all possible targets
          java.util.Set<CGNode> set = cg.getPossibleTargets(f, invokessa.getCallSite());
          // traverse all possible targets
          for (CGNode cgnode: set) {
        	  numOfTcOperations_recusively += dfsToGetTimeConsumingOperations(cgnode, depth+1, traversednodes);
          }
      }
      else {
    	// TODO - if need be
      }
      
    }//for	 
    
    function.numOfTcOperations = numOfTcOperations;
    function.numOfTcOperations_recusively = numOfTcOperations_recusively;
    return function.numOfTcOperations_recusively;
  }
  
  
  static Set<String> tmpTcOps = new TreeSet<String>();
  
  public static boolean checkTimeConsumingSSA(SSAInstruction ssa) {
	  // if a invoke instruction
      if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
    	  SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
    	  String classname = invokessa.getDeclaredTarget().getDeclaringClass().getName().toString();
		  String methodname = invokessa.getDeclaredTarget().getName().toString();
		  
    	  // identify RPC
		  String signature = invokessa.getDeclaredTarget().getSignature().toString();
		  if ( invokessa.isDispatch() 
	    	   || invokessa.isSpecial() 
	    	   || invokessa.isStatic() 
				  ) {
			  //  !!!!tmp, have a problem
			  if (rpcMethodSigs.contains(signature)) {
		      	  tmpTcOps.add( "RPC Call: " + signature );
				  return true;
			  }
			  
		  }
    	  // identify I/O
    	  if ( invokessa.isDispatch() 
    		   || invokessa.isSpecial() 
    		   || invokessa.isStatic() 
    			  ) {
    		  if ( !methodname.equals("<init>") )
    		  if ( isInIoMethodPrefixes(signature) ) {     		  
    			  tmpTcOps.add( invokessa.getDeclaredTarget().getSignature().toString() );
    			  return true;
    		  }	 
    	  }
      }
      else {
    	  //TODO - if need be
      }
      return false;
  }
  
  public static boolean isInIoMethodPrefixes(String signature) {
	  for (String str: ioMethodPrefixes)
		  if (signature.startsWith(str))
			  return true;
	  return false;
  }
  
  
  public static boolean isIO(SSAInstruction ssa) {
	  //filter rest I/Os that are not time-consuming for avoiding to get into,   #this is also can be removed
	  if (ssa instanceof SSAInvokeInstruction) {
		  SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
		  String sig = invokessa.getDeclaredTarget().getSignature().toString();
		  if ( sig.startsWith("java.io.")
			   || sig.startsWith("java.nio.")
			   || sig.startsWith("java.net.")
			   || sig.startsWith("java.rmi.")
			   || sig.startsWith("java.sql.")
			   )
			  return true;
	  }
	  return false;
  }
  
  
  public static void findTimeConsumingOperationsForALoop(LoopInfo loop) {
	  
	  CGNode f = loop.function;
	  int id = f.getGraphNodeId();
	  IR ir = f.getIR();
	  SSACFG cfg = ir.getControlFlowGraph();
	  SSAInstruction[] instructions = ir.getInstructions();
    
	  FunctionInfo function = functions.get(id); 
      
	  /* already done at LoopInfo initializtion
	  loop.numOfTcOperations_recusively = 0;
	  loop.tcOperations_recusively = new ArrayList<SSAInstruction>();
	  */
	  BitSet traversednodes = new BitSet();
	  traversednodes.clear();
	  traversednodes.set( id );
	  
	  //for debug
	  String callpath = f.getMethod().getSignature().substring(0, f.getMethod().getSignature().indexOf('('));
	  
	  for (int bbnum: loop.bbs) {
        int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
        int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
        for (int i = first_index; i <= last_index; i++) {
        	SSAInstruction ssa = instructions[i];
        	if (ssa == null)
        		continue;
        	if ( function.tcOperations.contains( ssa ) ) {
        		loop.numOfTcOperations_recusively ++;
        		loop.tcOperations_recusively.add( ssa );
        		//added tc_info
        		TcOperationInfo tcOperation = new TcOperationInfo();
        		tcOperation.ssa = ssa;
        		tcOperation.function = f;
        		tcOperation.callpath = callpath;
        		tcOperation.line_number = getSourceLineNumberFromSSA(ssa, instructions, (IBytecodeMethod)f.getMethod());
        		loop.tcOperations_recusively_info.add( tcOperation );
        		//end
        		continue;
        	}
        	// filter the rest I/Os
            if ( isIO(ssa) )
          	  continue;
            if (ssa instanceof SSAInvokeInstruction) {  
          	  SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
                java.util.Set<CGNode> set = cg.getPossibleTargets(f, invokessa.getCallSite());
                for (CGNode cgnode: set) {
              	  dfsToGetTimeConsumingOperationsForSSA(cgnode, 0, traversednodes, loop,  callpath);
                }
            }
        }
      } //for-bbnum
	  
  }
  

  
  public static void dfsToGetTimeConsumingOperationsForSSA(CGNode f, int depth, BitSet traversednodes, LoopInfo loop, String callpath) {
	/* jx: if want to add this, then for MapReduce we need to more hadoop-common&hdfs like "org.apache.hadoop.conf" not juse 'fs/security' for it
	if ( !isInPackageScope(f) )
		return ;
	*/
    if ( !f.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) 
         || f.getMethod().isNative()) { // IMPO - native - must be
      return ;
    }
    
	int id = f.getGraphNodeId();
    FunctionInfo function = functions.get(id);
    
    if ( traversednodes.get(id) )
      return ;
    
    //test
    if (function == null) 
    	System.out.println("jx - error - function == null");
   
    traversednodes.set(id);
    loop.numOfTcOperations_recusively += function.tcOperations.size();
    loop.tcOperations_recusively.addAll( function.tcOperations );
    
    IR ir = f.getIR();  //if (ir == null) return;
    SSAInstruction[] instructions = ir.getInstructions();
    
    //for debug
    String curCallpath = callpath + "-" + f.getMethod().getSignature().substring(0, f.getMethod().getSignature().indexOf('('));
    
    //added tc_info
  	for (SSAInstruction ssa: function.tcOperations) {
  		TcOperationInfo tcOperation = new TcOperationInfo();
  		tcOperation.ssa = ssa;
  		tcOperation.function = f;
  		tcOperation.callpath = curCallpath;
  		tcOperation.line_number = getSourceLineNumberFromSSA(ssa, instructions, (IBytecodeMethod)f.getMethod());
  		loop.tcOperations_recusively_info.add( tcOperation );
      }
  	//end
    
    
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction ssa = instructions[i];
      if (ssa == null)
    	  continue;
      if ( function.tcOperations.contains( ssa ) )
    	  continue;
      // filter the rest I/Os
      if ( isIO(ssa) )
    	  continue;
      if (ssa instanceof SSAInvokeInstruction) {  
    	  SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
          java.util.Set<CGNode> set = cg.getPossibleTargets(f, invokessa.getCallSite());
          for (CGNode cgnode: set) {
        	  dfsToGetTimeConsumingOperationsForSSA(cgnode, depth+1, traversednodes, loop, curCallpath);
          }
      }
    }//for
    
  }
  
  
  public static void printTcOperationTypes() {
	  System.out.println("\nJX-printTcOperationTypes");
	  System.out.println("#types = " + tmpTcOps.size());
	  // test
	  for (String str: tmpTcOps) {
		  System.out.println(str);
	  }
  }
  
  
  
  /*********************************************************
   * New added - JX - just find nested loops                 
   ********************************************************/
  public static void findNestedLoopsInLoops() {
	  System.out.println("\nJX-findNestedLoops");
	  
	  // Initialize nested loop information by DFS for all looping functions
	  for (Integer id: functions_with_loops.keySet() ) {
		  dfsToGetFunctionInfos(cg.getNode(id), 0);
	  }
	  
	  // deal with outermost loops 
	  for (Integer id: functions_with_loops.keySet() ) {
		  findNested(cg.getNode(id));
	  }
	  	  
	  // Print the status
	  int N_NestedLOOPS = 20;
	  int[] count = new int[N_NestedLOOPS]; int othercount = 0;
	  for (List<LoopInfo> loops: functions_with_loops.values()) {
		  for (LoopInfo loop: loops) {
			  int depthOfLoops = loop.max_depthOfLoops;
			  if (depthOfLoops < N_NestedLOOPS) count[depthOfLoops]++;
			  else othercount = 0;
		  }
	  }
	  System.out.println("The Status of Loops in All Functions:\n" 
	        + "#scanned functions: " + nPackageFuncs 
	        + " out of #Total:" + nTotalFuncs + "(#AppFuncs:" + nApplicationFuncs + "+#PremFuncs:" + nPremordialFuncs +")");    
	  System.out.println("#loops: " + nLoops + " (#functions with loops:" + nLoopingFuncs + ")");
	  System.out.println("//distribution of #nestedloops");
	  for (int i = 0; i < N_NestedLOOPS; i++)
	      System.out.print("#" + i + ":" + count[i] + ", ");
	  System.out.println("#>=" + N_NestedLOOPS + ":" + othercount);
	  System.out.println("jx - functions.size() = " + functions.size() );
	  System.out.println();
  }
  
  
  public static void findNested(CGNode f) {
	  
    int id = f.getGraphNodeId();
    IR ir = f.getIR();
    SSACFG cfg = ir.getControlFlowGraph();
    List<LoopInfo> loops = functions_with_loops.get(id);
    
    FunctionInfo function = functions.get(id); 
      
    //System.out.print("function " + id + ": ");
    for (int i = 0; i < loops.size(); i++) {
      LoopInfo loop = loops.get(i);
      // tmp vars
      InstructionInfo max_instruction = null;
      int max_depthOfLoops_in_current_function = 0;
      // end-tmp
      for (Iterator<Integer> it = loop.bbs.iterator(); it.hasNext(); ) {
        int bbnum = it.next();
        int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
        int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
        for (int index = first_index; index <= last_index; index++) {
          InstructionInfo instruction = function.instructions.get(index);
          if (instruction == null)
            continue;
          // Re-compute the numOfLoops in current/first-level function
          int numOfSurroundingLoops_in_current_function = 0;
          if (instruction.numOfSurroundingLoops_in_current_function > 0) {
            for (int j = 0; j < instruction.surroundingLoops_in_current_function.size(); j ++) {
              LoopInfo loop2 = loops.get( instruction.surroundingLoops_in_current_function.get(j) );
              if (loop.bbs.containsAll(loop2.bbs))
                numOfSurroundingLoops_in_current_function ++;
            }
          }
          // Then
          int depthOfLoops = numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call;
          //if (instruction.numOfLoops_in_call >= 7 && instruction.numOfLoops_in_call <= 15) System.err.println("!!:" + instruction.numOfLoops_in_call);
          if (depthOfLoops <= 0)
            continue;
          
          if (depthOfLoops > loop.max_depthOfLoops) {
            loop.max_depthOfLoops = depthOfLoops;
            max_instruction = instruction;
            max_depthOfLoops_in_current_function = numOfSurroundingLoops_in_current_function;
          }
        }
      } //for-it
      //save others, ie function path, for the Loop             haven't yet verified
      if (max_instruction != null && max_instruction.call >= 0) {
        loop.function_chain_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).function_chain_for_max_depthOfLoops);
        loop.hasLoops_in_current_function_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).hasLoops_in_current_function_for_max_depthOfLoops);
      }
      loop.function_chain_for_max_depthOfLoops.add(id);
      loop.hasLoops_in_current_function_for_max_depthOfLoops.add(max_depthOfLoops_in_current_function);
      
     
      // For Test
      if (f.getMethod().getSignature().indexOf("BlockManager.processReport(") >=0 || loop.max_depthOfLoops == 15 && loop.max_depthOfLoops < 15) {
          System.err.println(loop.max_depthOfLoops + " : " + f.getMethod().getSignature() );
          // print the function chain
          for (int k = loop.function_chain_for_max_depthOfLoops.size()-1; k >= 0; k--)
            System.err.print(cg.getNode( loop.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getName() + "#" + loop.hasLoops_in_current_function_for_max_depthOfLoops.get(k) + "#" + "->");
          System.err.println("End");
      }
    }//for-outermost
  }
  
  
  
  
  
  /***********************************************************************************************************
   * Find Locks with Loops
   ***********************************************************************************************************
   */
  public static void findLoopingLockingFunctions() {
      System.out.println("\nJX-findLoopingLockingFunctions");
    
      // Initialization by DFS for locking functions
      for (Integer id: functions_with_locks.keySet()) {
	      //System.err.println(cg.getNode(id).getMethod().getSignature());
	      dfsToGetFunctionInfos(cg.getNode(id), 0);
      }   
      // Find Locks with loops for locking functions. For safety, can't combine with the above, because this may modify value in FunctionInfo for eventual usage.
      for (Integer id: functions_with_locks.keySet()) {
	      findForLockingFunction(cg.getNode(id));
      }
    
      // Print the status    
      int MAXN_LOOPS_FOR_A_LOCK = 20;
      int[] count = new int[MAXN_LOOPS_FOR_A_LOCK]; int rest = 0;
      for (Iterator<Integer> it = functions_with_locks.keySet().iterator(); it.hasNext(); ) {
	      int id = it.next();
	      FunctionInfo function = functions.get(id);
	      if (function.hasLoopingLocks) {
	          nLoopingLockingFuncs ++;
	          for (Iterator<LoopingLockInfo> it_2 = function.looping_locks.values().iterator(); it_2.hasNext(); ) {
		          int num = it_2.next().max_depthOfLoops;
		          if (num > 0) {
		            nLoopingLocks ++;
		            if (num < MAXN_LOOPS_FOR_A_LOCK) count[num] ++;
		            else rest ++;
		          }
	          }
	      }
      }
    count[0] = nLocks - nLoopingLocks;
    System.out.println("The Status of Critical Sections:");
    System.out.println("#functions that their critical sections involve loops: " + nLoopingLockingFuncs + "(" + nLoopingLocks + "critical sections)" 
        + " out of " + nLockingFuncs + "(" + nLocks + "critical sections)" + " functions with locks");
    System.out.println("//distribution of loop depth in " + nLocks + "(#>=1:" + nLoopingLocks + ")" + " critical sections");
    for (int i = 0; i < MAXN_LOOPS_FOR_A_LOCK; i++) {
      System.out.print("#" + i + ":" + count[i] + ", ");
    }
    System.out.println("#>=" + MAXN_LOOPS_FOR_A_LOCK + ":" + rest);
    // Print - distribution of loop depth in locking functions
    System.out.println("//PS: distribution of loop depth in " + nLockingFuncs + "(#>=1:" + nLoopingLockingFuncs + ") locking functions");
    int MAXN_LOOPS_FOR_A_FUNCTION = 20;
    int[] count2 = new int[MAXN_LOOPS_FOR_A_FUNCTION];
    rest = 0;
    for (Iterator<Integer> it = functions_with_locks.keySet().iterator(); it.hasNext(); ) {
      int id = it.next();
      FunctionInfo function = functions.get(id);
      int max_loops = 0;
      for (Iterator<LoopingLockInfo> it_2 = function.looping_locks.values().iterator(); it_2.hasNext(); ) {
        int num = it_2.next().max_depthOfLoops;
        max_loops = num > max_loops ? num : max_loops;
      }
      if (max_loops < MAXN_LOOPS_FOR_A_FUNCTION)
        count2[max_loops] ++;
      else rest ++;
    }
    for (int i = 0; i < MAXN_LOOPS_FOR_A_FUNCTION; i++) {
      System.out.print("#" + i + ":" + count2[i] + ", ");
    }
    System.out.println("#>=" + MAXN_LOOPS_FOR_A_FUNCTION + ":" + rest);
    System.out.println("jx - functions.size() = " + functions.size() );
    System.out.println();
    
  }
  
  
  static int MAXN_DEPTH = 100;   //default: 1000    for ha-1.0.0, using 100 get the same results
  
  public static void dfsToGetFunctionInfos(CGNode f, int depth) {
    
      FunctionInfo function = new FunctionInfo();
      function.max_depthOfLoops = 0;
      int id = f.getGraphNodeId();
      String short_funcname = f.getMethod().getName().toString();
    
      if (depth > MAXN_DEPTH) {
	      function.max_depthOfLoops = 0;
	      functions.put(id, function);
	      return ;
      }
    
      if ( !wala.isInPackageScope(f) ) { 
	      function.max_depthOfLoops = 0;
	      functions.put(id, function);
	      return ;
      }
    
      if (locktypes.contains(short_funcname) || unlocktypes.contains(short_funcname)) {  //TODO - others
	      function.max_depthOfLoops = 0;
	      functions.put(id, function);
	      return ;
      }
    
      IR ir = f.getIR();
      SSACFG cfg = ir.getControlFlowGraph();
      SSAInstruction[] instructions = ir.getInstructions();
      List<LoopInfo> loops = functions_with_loops.get(id);
    
      for (int i = 0; i < instructions.length; i++) {
	      SSAInstruction ssa = instructions[i];
	      if (ssa != null) {
	        int bb = cfg.getBlockForInstruction(i).getNumber();
	        InstructionInfo instruction = new InstructionInfo();
	        // Current function level
	        if (loops != null) {
	          instruction.numOfSurroundingLoops_in_current_function = 0;
	          for (int j = 0; j < loops.size(); j++)
	            if (loops.get(j).bbs.contains(bb)) {
	              instruction.numOfSurroundingLoops_in_current_function ++;
	              instruction.surroundingLoops_in_current_function.add(j);
	            }
	          //test
	          //if (instruction.numOfLoops_in_current_function > 0) System.err.println(instruction.numOfLoops_in_current_function);
	        }
	        else {
	          instruction.numOfSurroundingLoops_in_current_function = 0;
	        }
	        // Go into calls
	        if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
	          java.util.Set<CGNode> set = cg.getPossibleTargets(f, ((SSAInvokeInstruction) ssa).getCallSite());
	          //if (set.size() > 1) System.err.println("CallGraph#getPossibleTargets's size > 1"); // for Test, how to solve the problem??
	          if (set.size() > 0) {         //JX: because I haven't yet added "hadoop-common"
	            CGNode n = set.iterator().next(); 
	            if (!functions.containsKey(n.getGraphNodeId())) {
	              //function.max_depthOfLoops = 1;  //how many???????
	              //functions.put(n.getGraphNodeId(), function);
	              dfsToGetFunctionInfos(n, depth+1); //layer+1?
	            } else {  //especial case: recursive function.    //TODO - maybe something wrong
	              /*
	              if (id == n.getGraphNodeId()) {
	                function.max_depthOfLoops = 15;
	                functions.put(id, function);
	                System.err.println("asdafasfd!!!");
	                //return;
	              }
	              */
	            }
	            instruction.maxdepthOfLoops_in_call = functions.get(n.getGraphNodeId()).max_depthOfLoops;
	            instruction.call = n.getGraphNodeId();
	          } else {                     //if we can't find the called CGNode.
	            //TODO
	            instruction.maxdepthOfLoops_in_call = 0;
	          }  
	        } else {
	          //TODO
	          instruction.maxdepthOfLoops_in_call = 0;
	        }
	        // Put into FunctionInfo.Map<Integer, InstructionInfo>
	        function.instructions.put(i, instruction);
	      }
      }//for
    
    // find the instruction with maximal loops && save the function path
    InstructionInfo max_instruction = null;
    for (Iterator<Integer> it = function.instructions.keySet().iterator(); it.hasNext(); ) {
      int index = it.next();
      InstructionInfo instruction = function.instructions.get(index);
      if (instruction.numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call > function.max_depthOfLoops) {
        max_instruction = instruction;
        function.max_depthOfLoops = instruction.numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call;
      }
    }
    if (max_instruction != null && max_instruction.call >= 0) {
      function.function_chain_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).function_chain_for_max_depthOfLoops);
      function.hasLoops_in_current_function_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).hasLoops_in_current_function_for_max_depthOfLoops);
    }
    function.function_chain_for_max_depthOfLoops.add(id);
    if (max_instruction != null && max_instruction.numOfSurroundingLoops_in_current_function > 0)
      function.hasLoops_in_current_function_for_max_depthOfLoops.add(max_instruction.numOfSurroundingLoops_in_current_function);
    else
      function.hasLoops_in_current_function_for_max_depthOfLoops.add(0);
    
    //test - specified function's loop status
    if (f.getMethod().getSignature().indexOf(functionname_for_test) >= 0) {
      System.err.println("aa " + f.getMethod().getSignature());
      System.err.println("bb " + function.max_depthOfLoops);
      System.err.println(function.function_chain_for_max_depthOfLoops);
      System.err.println("cc " + cg.getNode(549).getMethod().getSignature());
      System.err.println("cc " + cg.getNode(280).getMethod().getSignature());
      // print the function chain
      for (int k = function.function_chain_for_max_depthOfLoops.size()-1; k >= 0; k--)
        System.out.print(cg.getNode( function.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getName() + "#" + function.hasLoops_in_current_function_for_max_depthOfLoops.get(k) + "#" + "->");
      System.out.println("End");
    }
    
    
    //if (!functions.containsKey(id))
    //  functions.put(id, function);
    //else if (function.max_depthOfLoops > functions.get(id).max_depthOfLoops)
    functions.put(id, function);
  }
  
  
  public static void findForLockingFunction(CGNode f) {
    
    int id = f.getGraphNodeId();
    IR ir = f.getIR();
    SSACFG cfg = ir.getControlFlowGraph();
    List<LockInfo> locks = functions_with_locks.get(id);
    List<LoopInfo> loops = functions_with_loops.get(id);
    
    FunctionInfo function = functions.get(id); 
    function.hasLoopingLocks = false;
    
    //System.out.print("function " + id + ": ");
    for (int i = 0; i < locks.size(); i++) {
      LockInfo lock = locks.get(i);
      LoopingLockInfo loopinglock = null;
      InstructionInfo max_instruction = null;
      int max_depthOfLoops_in_current_function = 0;
      for (Iterator<Integer> it = lock.bbs.iterator(); it.hasNext(); ) {
        int bbnum = it.next();
        int first_index = cfg.getBasicBlock(bbnum).getFirstInstructionIndex();
        int last_index = cfg.getBasicBlock(bbnum).getLastInstructionIndex();
        for (int index = first_index; index <= last_index; index++) {
          InstructionInfo instruction = function.instructions.get(index);
          if (instruction == null)
            continue;
          // Re-compute the numOfLoops in current/first-level function
          int numOfSurroundingLoops_in_current_function = 0;
          if (instruction.numOfSurroundingLoops_in_current_function > 0) {
            for (int j = 0; j < instruction.surroundingLoops_in_current_function.size(); j ++) {
              LoopInfo loop = loops.get( instruction.surroundingLoops_in_current_function.get(j) );
              if (lock.bbs.containsAll(loop.bbs))
                numOfSurroundingLoops_in_current_function ++;
            }
          }
          // Then
          int depthOfLoops = numOfSurroundingLoops_in_current_function + instruction.maxdepthOfLoops_in_call;
          //if (instruction.numOfLoops_in_call >= 7 && instruction.numOfLoops_in_call <= 15) System.err.println("!!:" + instruction.numOfLoops_in_call);
          if (depthOfLoops <= 0)
            continue;
          if (loopinglock == null) {
            loopinglock = new LoopingLockInfo();
            loopinglock.max_depthOfLoops = 0;
          }
          if (depthOfLoops > loopinglock.max_depthOfLoops) {
            loopinglock.max_depthOfLoops = depthOfLoops;
            max_instruction = instruction;
            max_depthOfLoops_in_current_function = numOfSurroundingLoops_in_current_function;
          }
        }
      }//for-it
      //save others, ie function path, for the loopinglock
      if (loopinglock != null) {
        if (max_instruction != null && max_instruction.call >= 0) {
          loopinglock.function_chain_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).function_chain_for_max_depthOfLoops);
          loopinglock.hasLoops_in_current_function_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).hasLoops_in_current_function_for_max_depthOfLoops);
        }
        loopinglock.function_chain_for_max_depthOfLoops.add(id);
        loopinglock.hasLoops_in_current_function_for_max_depthOfLoops.add(max_depthOfLoops_in_current_function);
        loopinglock.function = f;  //for the future
        loopinglock.lock = lock;   //for the future
        function.looping_locks.put(i, loopinglock);
      }
     
      // For Test
      //if (f.getMethod().getSignature().indexOf("FSNamesystem.processReport(") >=0)
      //  System.out.println("I do");
      if (loopinglock != null) {
        if (f.getMethod().getSignature().indexOf("BlockManager.processReport(") >=0 || loopinglock.max_depthOfLoops == 15 && loopinglock.max_depthOfLoops < 15) {
          System.err.println(loopinglock.max_depthOfLoops + " : " + f.getMethod().getSignature() + " : " + locks.get(i).lock_type);
          // print the function chain
          for (int k = loopinglock.function_chain_for_max_depthOfLoops.size()-1; k >= 0; k--)
            System.err.print(cg.getNode( loopinglock.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getName() + "#" + loopinglock.hasLoops_in_current_function_for_max_depthOfLoops.get(k) + "#" + "->");
          System.err.println("End");
        }
      }
      
    }//for-outermost
    
    if (function.looping_locks.size() > 0)
      function.hasLoopingLocks = true;
    //System.out.println(" PS - " + f.getMethod().getSignature());
    
  }
  
  
  
  /***********************************************************************************
   * staticPruningForCriticalLoops
   * Note: ONLY for Suspected/Critical loops that are read from da(dynamic analysis)
   **********************************************************************************/
  public static void staticPruningForCriticalLoops() {
	  System.out.println("\nJX-staticPruningForCriticalLoops"); 
	  StaticPruning printBugLoops = new StaticPruning(functions_with_loops, Paths.get(projectDir, "src/da/").toString());
	  printBugLoops.doWork();
  }
  
  
  
  
  public static void analyzeAllLocks() {
    System.out.println("\nJX-analyzeAllLocks");

    //for test
    Set<String> set_of_locks = new TreeSet<String>();    // for test, TreeSet is ordered by Java
    
    for (List<LockInfo> locks: functions_with_locks.values())
      for (LockInfo lock: locks) {
        lock.lock_identity = "ClassName- " + lock.lock_class + " LockName- " + lock.lock_name;
        set_of_locks.add( lock.lock_identity );
      }
    nLockGroups = set_of_locks.size();
    
    //Get results
    System.out.println("#Total Locks = " + nLocks);
    System.out.println("#Groups of total Locks (ie, real number): " + nLockGroups);
    //for (String str: set_of_locks)
    //  System.err.println( str );
    System.out.println();
  }
 
  
  static List<LoopingLockInfo> heavylocks = new ArrayList<LoopingLockInfo>();  // ie, Time-consuming Looping Locks
  static Set<String> set_of_heavylocks = new TreeSet<String>();   
  
  public static void analyzeLoopingLocks() {
    System.out.println("\nJX-analyzeLoopingLocks-5");
 
    int requiredDepth = 2;
    for (Integer id: functions_with_locks.keySet()) {
      FunctionInfo function = functions.get(id);
      if (!function.hasLoopingLocks)
        continue;
      for (Map.Entry<Integer, LoopingLockInfo> entry: function.looping_locks.entrySet()) {
        LoopingLockInfo loopinglock = entry.getValue();
        if (loopinglock.max_depthOfLoops >= requiredDepth && loopinglock.max_depthOfLoops < 3) {
          heavylocks.add( loopinglock );
          set_of_heavylocks.add( loopinglock.lock.lock_identity );
        }
      }//for
    }//for-outermost
    nHeavyLocks = heavylocks.size();
    nHeavyLockGroups = set_of_heavylocks.size();
    
    // Print the status
    System.out.println("#HeavyLocks (ie, time-consuming looping locks): " + nHeavyLocks + " out of " + nLoopingLocks + " looping locks" + " out of total " + nLocks  + " locks");
    System.out.println("#Groups of HeavyLocks (ie, real number): " + nHeavyLockGroups + " out of total " + nLockGroups + " lock groups");
    for (String str: set_of_heavylocks)
      System.err.println( str );
    System.out.println();
  }
  
   
}










