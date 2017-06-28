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
package sa.lockloop;

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

import sa.lock.LockAnalyzer;
import sa.lock.LockInfo;
import sa.lockloop.tc.IOLoopUtil;
import sa.loop.LoopAnalyzer;
import sa.loop.LoopInfo;
import sa.loop.TcOperationInfo;
import sa.wala.IRUtil;
import sa.wala.WalaAnalyzer;
import sa.wala.util.PDFCallGraph;


public class JXLocks {
	// dir paths
	String projectDir;  // read from arguments, like "/root/JXCascading-detector(/)"   #jx: couldn't obtain automatically, because of many scenarios
	String jarsDir;   // read from arguments, like "/root/JXCascading-detector/src/sa/res/MapReduce/hadoop-0.23.3(/)"
	String dtDir;       //should be "projectDir/src/dt/res/", but couldn't write directly like this
	String dmDir;       
  
	// WALA basis
	//WalaAnalysis wala;
	WalaAnalyzer wala;
	ClassHierarchy cha;
	CallGraph cg;
	int nPackageFuncs = 0;           // the real functions we focuses  //must satisfy "isApplicationAndNonNativeMethod" first
	int nTotalFuncs = 0;
	int nApplicationFuncs = 0;       
	int nPremordialFuncs = 0;
	int nOtherFuncs = 0;
  
	// Target System
	String systemname = null;   // current system's name  
  
	
	
	LockAnalyzer lockAnalyzer;
	LoopAnalyzer loopAnalyzer;
	
	//
	IOLoopUtil iolooputil;
  
  

	// map: function CGNode id -> locks, ONLY covers functions that really involve locks 
	Map<Integer, List<LockInfo>> functions_with_locks = new HashMap<Integer, List<LockInfo>>();
	// map: function CGNode id -> loops, ONLY covers functions that really involve loops  
	Map<Integer, List<LoopInfo>> functions_with_loops = new HashMap<Integer, List<LoopInfo>>();
	// map: function CGNode id -> traversed functions (including looping_locking_functions)
	Map<Integer, FunctionInfo> functions = new HashMap<Integer, FunctionInfo>();
  
	// Statistics
	int nLockGroups = 0;
 
	int nLoopingLocks = 0;
	int nLoopingLockingFuncs = 0;
  
	int nHeavyLocks = 0;             // The number of time-consuming looping locks
	int nHeavyLockGroups = 0;
  
	int nHeartbeatLocks = 0;
	int nHeartbeatLockGroups = 0;
  
	int nSuspectedHeavyLocks = 0;
 
  
	// For test
	String functionname_for_test = "org.apache.hadoop.hdfs.DFSOutputStream$DataStreamer$ResponseProcessor.run("; //"RetryCache.waitForCompletion(Lorg/apache/hadoop/ipc/RetryCache$CacheEntry;)"; //"org.apache.hadoop.hdfs.server.balancer.Balancer"; //"Balancer$Source.getBlockList";//"DirectoryScanner.scan"; //"ReadaheadPool.getInstance("; //"BPServiceActor.run("; //"DataNode.runDatanodeDaemon"; //"BPServiceActor.run("; //"BlockPoolManager.startAll"; //"NameNodeRpcServer"; //"BackupNode$BackupNodeRpcServer"; // //".DatanodeProtocolServerSideTranslatorPB"; //"DatanodeProtocolService$BlockingInterface"; //"sendHeartbeat("; //"org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolServerSideTranslatorPB";  //java.util.regex.Matcher.match(";
	int which_functionname_for_test = 1;   //1st? 2nd? 3rd?    //TODO - 0 means ALL, 1 to n means which one respectively
  
  
  
  //===============================================================================================
  //++++++++++++++++++++++++++++++++++ JXLocks Methods ++++++++++++++++++++++++++++++++++++++++++++
  //===============================================================================================

	public JXLocks(WalaAnalyzer walaAnalyzer) {
		this(walaAnalyzer, ".");
	}
	
	public JXLocks(WalaAnalyzer walaAnalyzer, String projectDir) {
		this.wala = walaAnalyzer;
		this.projectDir = projectDir;
		this.jarsDir = wala.getTargetDirPath().toString();
		doWork();
	}
	
  
	public void doWork() {
		System.out.println("JX - INFO - JXLocks.doWork");
	    try {
	    	// Test Part -
	    	//testQuickly();
	     
	        // Read external arguments
	        dtDir = Paths.get(projectDir, "src/dt/").toString();
	        dmDir = Paths.get(projectDir, "src/dm/").toString();
	      
			systemname = Benchmarks.resolveSystem(jarsDir);
			System.out.println("JX - DEBUG - system name = " + systemname);
			cg = wala.getCallGraph();
			cha = wala.getClassHierarchy();
			nPackageFuncs = wala.getNPackageFuncs();
			nTotalFuncs = wala.getNTotalFuncs();
			nApplicationFuncs = wala.getNApplicationFuncs();
			nPremordialFuncs = wala.getNPremordialFuncs();
			nOtherFuncs = wala.getNOtherFuncs();
			
	      
			// Lock analysis
			this.lockAnalyzer = new LockAnalyzer(this.wala);
			lockAnalyzer.doWork();
			functions_with_locks = lockAnalyzer.getResults();
			
			// Loop analysis
			this.loopAnalyzer = new LoopAnalyzer(this.wala);
			loopAnalyzer.doWork();
			functions_with_loops = loopAnalyzer.getResults();
			
			//findLoopingLockingFunctions();
	      
			// Phase 2 - deal with loops
			findNestedLoopsInLoops();
	      
	      
			//added
			iolooputil = new IOLoopUtil(jarsDir);
			// init
			findTimeConsumingOperationsInLoops();     // for all loops
			iolooputil.printTcOperationTypes();                //for test
	      
			// Static Pruning      
			/**
			 * staticPruningForCriticalLoops
	       	* Note: ONLY for Suspected/Critical loops that are read from da(dynamic analysis)
	       	*/
			System.out.println("\nJX - INFO - staticPruningForCriticalLoops"); 
			StaticPruning printBugLoops = new StaticPruning(functions_with_loops, Paths.get(projectDir, "src/da/").toString());
			printBugLoops.doWork();
	     
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
  
  
  
  	public void testQuickly() {
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
 
  
 

  
  

  
  
  
  
  /**************************************************************************
   * New added - JX - just find time-consuming operations    
   **************************************************************************/
  public void findTimeConsumingOperationsInLoops() {
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
  
  
  public int dfsToGetTimeConsumingOperations(CGNode f, int depth, BitSet traversednodes) {
    
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

      if ( iolooputil.isTimeConsumingSSA(ssa) ) {
    	  function.tcOperations.add( ssa );
    	  numOfTcOperations ++;
    	  numOfTcOperations_recusively ++;
    	  continue;
      }
      // filter the rest I/Os
      if ( iolooputil.isIOSSA(ssa) )
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
  
  

  
  
  public void findTimeConsumingOperationsForALoop(LoopInfo loop) {
	  
	  CGNode f = loop.cgNode;
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
        		tcOperation.line_number = IRUtil.getSourceLineNumberFromSSA(ssa, instructions, (IBytecodeMethod)f.getMethod());
        		loop.tcOperations_recusively_info.add( tcOperation );
        		//end
        		continue;
        	}
        	// filter the rest I/Os
            if ( iolooputil.isIOSSA(ssa) )
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
  

  
  public void dfsToGetTimeConsumingOperationsForSSA(CGNode f, int depth, BitSet traversednodes, LoopInfo loop, String callpath) {
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
  		tcOperation.line_number = IRUtil.getSourceLineNumberFromSSA(ssa, instructions, (IBytecodeMethod)f.getMethod());
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
      if ( iolooputil.isIOSSA(ssa) )
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
  
  

  
  
  /*********************************************************
   * New added - JX - just find nested loops                 
   ********************************************************/
  public void findNestedLoopsInLoops() {
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
	  System.out.println("#loops: see LoopAnalyzer"+ " (#functions with loops: see LoopAnalyzer" + ")");
	  System.out.println("//distribution of #nestedloops");
	  for (int i = 0; i < N_NestedLOOPS; i++)
	      System.out.print("#" + i + ":" + count[i] + ", ");
	  System.out.println("#>=" + N_NestedLOOPS + ":" + othercount);
	  System.out.println("jx - functions.size() = " + functions.size() );
	  System.out.println();
  }
  
  
  public void findNested(CGNode f) {
	  
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
  public void findLoopingLockingFunctions() {
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
      
      int nLocks = this.lockAnalyzer.getNLocks();
      int nLockingFuncs = this.lockAnalyzer.getNLockingFuncs();
      
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
  
  
  int MAXN_DEPTH = 100;   //default: 1000    for ha-1.0.0, using 100 get the same results
  
  public void dfsToGetFunctionInfos(CGNode f, int depth) {
    
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
    
      if (LockAnalyzer.locktypes.contains(short_funcname) || LockAnalyzer.unlocktypes.contains(short_funcname)) {  //TODO - others
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
  
  
  public void findForLockingFunction(CGNode f) {
    
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
  
  
 
  public void analyzeAllLocks() {
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
    int nLocks = this.lockAnalyzer.getNLocks();
    System.out.println("#Total Locks = " + nLocks);
    System.out.println("#Groups of total Locks (ie, real number): " + nLockGroups);
    //for (String str: set_of_locks)
    //  System.err.println( str );
    System.out.println();
  }
 
  
  List<LoopingLockInfo> heavylocks = new ArrayList<LoopingLockInfo>();  // ie, Time-consuming Looping Locks
  Set<String> set_of_heavylocks = new TreeSet<String>();   
  
  public void analyzeLoopingLocks() {
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
    int nLocks = this.lockAnalyzer.getNLocks();
    System.out.println("#HeavyLocks (ie, time-consuming looping locks): " + nHeavyLocks + " out of " + nLoopingLocks + " looping locks" + " out of total " + nLocks  + " locks");
    System.out.println("#Groups of HeavyLocks (ie, real number): " + nHeavyLockGroups + " out of total " + nLockGroups + " lock groups");
    for (String str: set_of_heavylocks)
      System.err.println( str );
    System.out.println();
  }
  
   
}










