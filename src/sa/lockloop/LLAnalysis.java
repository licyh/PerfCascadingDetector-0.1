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
import sa.lock.LoopingLockAnalyzer;
import sa.loop.IOLoopUtil;
import sa.loop.LoopAnalyzer;
import sa.loop.LoopInfo;
import sa.loop.NestedLoopAnalyzer;
import sa.loop.TCLoopAnalyzer;
import sa.loop.TcOperationInfo;
import sa.wala.IRUtil;
import sa.wala.WalaAnalyzer;
import sa.wala.util.PDFCallGraph;


public class LLAnalysis {
	// dir paths
	String projectDir;  // read from arguments, like "/root/JXCascading-detector(/)"   #jx: couldn't obtain automatically, because of many scenarios
	String jarsDir;   // read from arguments, like "/root/JXCascading-detector/src/sa/res/MapReduce/hadoop-0.23.3(/)"     
  
	// WALA basis
	WalaAnalyzer wala;
	CallGraph cg;
	ClassHierarchy cha;

  
	LockAnalyzer lockAnalyzer;
	LoopAnalyzer loopAnalyzer;
	IOLoopUtil iolooputil;
	
	// results
	CGNodeList cgNodeList;
	
	// Target System
	String systemname = null;   // current system's name  
 
  
	// For test
	String functionname_for_test = "org.apache.hadoop.hdfs.DFSOutputStream$DataStreamer$ResponseProcessor.run("; //"RetryCache.waitForCompletion(Lorg/apache/hadoop/ipc/RetryCache$CacheEntry;)"; //"org.apache.hadoop.hdfs.server.balancer.Balancer"; //"Balancer$Source.getBlockList";//"DirectoryScanner.scan"; //"ReadaheadPool.getInstance("; //"BPServiceActor.run("; //"DataNode.runDatanodeDaemon"; //"BPServiceActor.run("; //"BlockPoolManager.startAll"; //"NameNodeRpcServer"; //"BackupNode$BackupNodeRpcServer"; // //".DatanodeProtocolServerSideTranslatorPB"; //"DatanodeProtocolService$BlockingInterface"; //"sendHeartbeat("; //"org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolServerSideTranslatorPB";  //java.util.regex.Matcher.match(";
	int which_functionname_for_test = 1;   //1st? 2nd? 3rd?    //TODO - 0 means ALL, 1 to n means which one respectively
  
  
  
	
	public LLAnalysis(WalaAnalyzer walaAnalyzer) {
		this(walaAnalyzer, ".");
	}
	
	public LLAnalysis(WalaAnalyzer walaAnalyzer, String projectDir) {
		this.wala = walaAnalyzer;
		this.projectDir = projectDir;
		this.jarsDir = wala.getTargetDirPath().toString();
		// others
		this.cgNodeList = new CGNodeList(this.wala.getCallGraph());
		doWork();
	}
	
  
	public void doWork() {
		System.out.println("\nJX - INFO - LLAnalysis.doWork");
	    try {

	     
	        // Read external arguments
	      
			systemname = Benchmarks.resolveSystem(jarsDir);
			System.out.println("JX - DEBUG - system name = " + systemname);
			this.cg = wala.getCallGraph();
			this.cha = wala.getClassHierarchy();

		

			// Lock analysis
			this.lockAnalyzer = new LockAnalyzer(this.wala, this.cgNodeList);
			lockAnalyzer.doWork();
			
			// Loop analysis
			this.loopAnalyzer = new LoopAnalyzer(this.wala, this.cgNodeList);
			loopAnalyzer.doWork();
			
			// loops-containing lock
			LoopingLockAnalyzer loopingLockAnalyzer = new LoopingLockAnalyzer(this.wala, this.lockAnalyzer, this.loopAnalyzer, this.cgNodeList);
			loopingLockAnalyzer.doWork();
			
			// loops-containing loop
			NestedLoopAnalyzer nestedLoopAnalyzer = new NestedLoopAnalyzer(this.wala, this.loopAnalyzer, this.cgNodeList);
			nestedLoopAnalyzer.doWork();
			
			TCLoopAnalyzer tcLoopAnalyzer = new TCLoopAnalyzer(this.wala, this.loopAnalyzer, this.cgNodeList);
			tcLoopAnalyzer.doWork();
		
			// Static Pruning      
			/**
			 * staticPruningForCriticalLoops
	       	* Note: ONLY for Suspected/Critical loops that are read from da(dynamic analysis)
	       	*/
			System.out.println("\nJX - INFO - staticPruningForCriticalLoops"); 
			StaticPruning printBugLoops = new StaticPruning(this.loopAnalyzer, Paths.get(projectDir, "src/da/").toString());
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
  
  

   
}










