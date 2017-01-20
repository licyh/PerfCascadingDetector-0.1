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
package com.ibm.wala.examples.drivers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.analysis.pointers.BasicHeapGraph;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.ide.ui.ViewIRAction;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.DefaultContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNodeFactory;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.ReflectiveMemberAccess;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAAbstractUnaryInstruction;
import com.ibm.wala.ssa.SSAAddressOfInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayReferenceInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAStoreIndirectInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Acyclic;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;

public class JXLocks {

  // WALA basis
  private final static boolean CHECK_GRAPH = false;
  final public static String CG_PDF_FILE = "cg.pdf";
  static AnalysisScope scope;
  static ClassHierarchy cha;
  static HashSet<Entrypoint> entrypoints;
  static CallGraph cg;
  
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
  // map: function id -> looping_locking_functions
  static Map<Integer, FunctionInfo> functions = new HashMap<Integer, FunctionInfo>();
  
  // Statistics
  static int nTotalFuncs = 0;
  static int nApplicationFuncs = 0;       // The FOCUS
  static int nPremordialFuncs = 0;
  
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
 
  
  // List of distributed systems
  static List<String> systems = Arrays.asList("HDFS", "MapReduce", "HBase");
  // Whether it will use all Entry Points for different distributed systems or not
  static Map<String,String> mapOfWhetherAllEntries = new HashMap<String,String>() {{
    put("HDFS", "No");
    put("MapReduce", "Yes");  //should be No I think, now using Yes for testing, that is, 7396 vs 13942 scanned functions.   #??? 3 or more?   
    put("HBase", "Yes");
  }};
  // Entry Points for different distributed systems
  static Map<String,List<String>> mapOfSystemEntries = new HashMap<String,List<String>>() {{
    put("HDFS", Arrays.asList("org.apache.hadoop.hdfs"));
    put("MapReduce", Arrays.asList("org.apache.hadoop.mapred", "org.apache.hadoop.mapreduce"));  //3 or more?
    put("HBase", Arrays.asList("org.apache.hadoop.xx"));
  }};
  
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
    new JXLocks().run(p);  // or JXLocks.run(p) or run(p) directly
  }

  
  public static void run(Properties p) {
    try {
      // Test Part -
      testQuickly();
      init(p);
      //testIClass();
      //testTypeHierarchy();
      //testCGNode();
      //testPartialCallGraph();
      testIR();
      //testWalaAPI();
      
      // Phase 1 -
      findLockingFunctions();
      findLoopingFunctions();
      findLoopingLockingFunctions();
      
      // Phase 2 -
      analyzeAllLocks();
      analyzeLoopingLocks();
      
      // Phase 3 - 
      findLocksForHeartbeat();
      //findAllCGNodesOfLocks();
      //System.out.println(real_layer);
    } catch (Exception e) {
      System.err.println("JX-StackTrace-run-begin");
      e.printStackTrace();
      System.err.println("JX-StackTrace-run-end");
      return ;
    }
  }
  
  
  public static void init(Properties p) 
      throws IOException, IllegalArgumentException, CallGraphBuilderCancelException, UnsoundGraphException, WalaException {
    System.out.println("Testing Information");
    
    // Read external arguments
    String appJar = p.getProperty("appJar");
    //System.out.println("Testing directory - " + appJar);
    
    // Get the Target System Name for testing
    String systemname = null;
    String[] path_components = appJar.split( "\\/" );           // can't use File.separator
    //for (int i=0; i < path_components.length; i++) System.out.println(path_components[i]);
    for (int i=0; i < path_components.length; i++)
      for (int j=0; j < systems.size(); j++)
        if (path_components[i].toUpperCase().equals( systems.get(j).toUpperCase() )) {
          systemname = systems.get(j);
          System.out.println("Target system - " + systemname);
          break;
        }
    if (systemname == null) {
      System.err.println("Error - Target System is UNREADY !!!!!");
      return;
    }
    
    // Get all Jar Files for testing
    if (PDFCallGraph.isDirectory(appJar)) {
      appJar = PDFCallGraph.findJarFiles(new String[] { appJar }); 
      System.out.println("Testing paths - " + appJar);  
    }
    
    // Create a Scope                                                                           #"JXJavaRegressionExclusions.txt"
    scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS)); //default: CallGraphTestUtil.REGRESSION_EXCLUSIONS
    // Create a Class Hierarchy
    cha = ClassHierarchy.make(scope); 
    //testTypeHierarchy();
    
    // Create a Entry Points
    entrypoints = new HashSet<Entrypoint>();
    Iterable<Entrypoint> allappentrypoints = new AllApplicationEntrypoints(scope, cha);  //Usually: entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);  //get main entrypoints
    // Get all entry points
    if (mapOfWhetherAllEntries.get(systemname).toUpperCase().equals("YES"))
      entrypoints = (HashSet<Entrypoint>) allappentrypoints;
    // Get the specified system's entries, ie, HDFS without hadoop-common
    else {
      for (Iterator<Entrypoint> it = allappentrypoints.iterator(); it.hasNext();) {
        Entrypoint entry = it.next();
        String sig = entry.getMethod().getSignature();
        boolean realentry = false;
        for (int i=0; i < mapOfSystemEntries.get(systemname).size(); i++)
          if (sig.indexOf( mapOfSystemEntries.get(systemname).get(i) ) >= 0) {
            realentry = true;
            break;
          }
        if ( realentry ) {
          entrypoints.add(entry);
          //System.err.println(entry.getMethod().getSignatur e());
        }
      }
    }//else
    //System.err.println("Number of Entrypoints = " + entrypoints.size());
    
    
    // Create Analysis Options
    AnalysisOptions options = new AnalysisOptions(scope, entrypoints); 
    options.setReflectionOptions(ReflectionOptions.ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE);   //ReflectionOptions.FULL will just cause a few more nodes and methods 

    // Create a builder - default: Context-insensitive   
    //#makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null, null); 
    //#makeVanillaZeroOneCFABuilder(options, new AnalysisCache(), cha, scope, null, null);   // this will take 20+ mins to finish
    //#makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
    //#makeZeroOneContainerCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
    com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null, null); 
    // Context-sensitive
    /*
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha); 
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha); 
    //ContextSelector contextSelector = new DefaultContextSelector(options);    
    //SSAContextInterpreter contextInterpreter = new DefaultSSAInterpreter(options, Cache);
    SSAPropagationCallGraphBuilder builder = new nCFABuilder(1, cha, options, new AnalysisCache(), null, null); 
    AllocationSiteInNodeFactory factory = new AllocationSiteInNodeFactory(options, cha);
    builder.setInstanceKeys(factory);
    */
    
    // Build the call graph JX: time-consuming
    cg = builder.makeCallGraph(options, null);
    System.out.println(CallGraphStats.getStats(cg));
    
    // Get pointer analysis results
    /*
    PointerAnalysis pa = builder.getPointerAnalysis();
    HeapModel hm = pa.getHeapModel();   //JX: #getHeapModel's reslult is com.ibm .wala.ipa.callgraph.propagation.PointerAnalysisImpl$HModel@24ccf6a8
    BasicHeapGraph hg = new BasicHeapGraph(pa, cg);
    System.err.println(hg);
    */
    //System.err.println(builder.getPointerAnalysis().getHeapGraph());  

    if (CHECK_GRAPH) {
      GraphIntegrity.check(cg);
    }
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
  
  
  public static void testIClass() throws WalaException {
    System.err.println("JX-breakpoint-testIClass");
    
    // Fetch a class from ClassHierarchy
    IClass ic = null;
    for (IClass c : cha) {  
      //System.err.println(c.getName().toString()); //output like Lorg/apache/hadoop/io/ObjectWritable  //ps - IClass.getClass(.getName/getCanonicalName) = class com.ibm.wala.classLoader.ShrikeClass
      if (c.getName().toString().indexOf(functionname_for_test.replace(".", "/")) >= 0) {
        System.err.println("ic - " + c.toString());  //equals "c", output like <Application,Lorg/apache/hadoop/hdfs/server/balancer/Balancer>
        System.err.println("ic.getName - " + c.getName().toString());  //output like Lorg/apache/hadoop/hdfs/server/balancer/Balancer
        ic = c;
        System.err.println( ic.getSourceFileName() );     //null
        System.err.println( ic.getAllFields() );          //[< Application, Lorg/apache/hadoop/hdfs/server/balancer/Balancer, globalBlockList, <Application,Ljava/util/Map> >, xx... ]
        System.err.println( ic.getAllInstanceFields() );
        System.err.println( ic.getAllStaticFields() );
        System.err.println( ic.getAnnotations() );        //[Annotation type <Application,Lorg/apache/hadoop/classification/InterfaceAudience$Private>]
        //break;
      }
    }
    
    // Fetch a class
    TypeReference tempclass = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lorg/apache/hadoop/hdfs/server/blockmanagement/BlockManager");
    //TypeReference tempclass = TypeReference.findOrCreate(ClassLoaderReference.Application, "Lorg/apache/hadoop/hdfs/server/namenode/FSNamesystem");
    IClass tempic = cha.lookupClass(tempclass);
    System.out.println(tempic.getAllMethods().size());
    for (Iterator<IMethod> it =tempic.getAllMethods().iterator(); it.hasNext();)
      System.out.println(it.next());
  }
  
  public static void testTypeHierarchy() throws WalaException {
    System.err.println("JX-breakpoint-testTypeHierarchy");
    
    // Test - class hierarchy
    System.err.println("Number of All Classes = " + cha.getNumberOfClasses());
    for (IClass c : cha) {  
      if (c.getName().toString().indexOf(functionname_for_test) >= 0) {
        System.err.println(c.getName().toString());
      }
    }
    
    // View the whole Type Hierarchy SWT if needed
    /*
    Graph<IClass> g = typeHierarchy2Graph(cha);
    g = pruneForAppLoader(g);
    viewTypeHierarchySWT(g);
    */
    
    // Print some related Type Hierarchy
    Graph<IClass> result = SlowSparseNumberedGraph.make();
    for (IClass c : cha) {   //JX: this step should ensure including all needed nodes used below
      //if (c.getName().toString().indexOf(functionname_for_test) >= 0) {
        //System.err.println(c.getName().toString());
        result.addNode(c);
      //}
    }
    for (IClass c : cha) {
      if (c.getName().toString().indexOf(functionname_for_test) >= 0) {
        for (IClass x : cha.getImmediateSubclasses(c)) {
          System.err.println(x.getName().toString());
          result.addEdge(c, x);
        }
        if (c.isInterface()) {  
          for (IClass x : cha.getImplementors(c.getReference())) {
            result.addEdge(c, x);
          }
        }
      }
    }
    result = pruneForAppLoader(result);
    viewTypeHierarchySWT(result);
  }
  
  
  /**
   * Note - The way to print call graph from a entry point (DataNode.runDatanodeDaemon) is wrong, the call graph will be incomplete, 
   * because it will miss some context information in this method, so it will miss some call sites in this method.
   * E.g., DataNode.runDatanodeDaemon will miss all call sites (eg, blockPoolManager.startAll(); dataXceiverServer.start();),
   * because it can't get the variables blockPoolManager and dataXceiverServer
   */
  public static void testPartialCallGraph() throws IllegalArgumentException, CallGraphBuilderCancelException, WalaException {  
    System.err.println("JX-breakpoint-testPartialCallGraph");
    // Method 1
    //Graph<CGNode> g = buildPrunedCallGraph(appJar, (new FileProvider()).getFile(exclusionFile));
    
    // Method 2
    HashSet<Entrypoint> tmp_eps = HashSetFactory.make();
    // get from Application entry points
    for (Iterator<Entrypoint> it = entrypoints.iterator(); it.hasNext();) {
      Entrypoint entry = it.next();
      if (entry.getMethod().getSignature().indexOf(functionname_for_test) >= 0) {
        System.err.println("Entry - " + entry.getMethod().getSignature());
        tmp_eps.add(entry);
      }
    }
    // get from call graph nodes
    /*
    CGNode n = null;
    IMethod m;
    int currentone = 0;
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
      n = it.next();
      m = n.getMethod();
      if (m.getSignature().indexOf(functionname_for_test) >= 0)   // Memo - FSNamesystem$DefaultAuditLogger.logAuditMessage( Log4JLogger.info(    InetAddress.getByName(
        if (++currentone == which_functionname_for_test) {
          System.err.println("entrypoint: " + m.getSignature());
          entrypoints.add(new DefaultEntrypoint(m, cha));
          break;
        } 
    }//for
    */
    System.err.println("Entrypoints' size = " + tmp_eps.size() + " : " + tmp_eps);
    
    /*
    //test
    System.err.println("current nodes:");
    System.err.println(n.getMethod().getSignature());
    System.err.println("pred nodes:");
    for (Iterator<CGNode> it = cg.getPredNodes(n); it.hasNext(); ) {
      CGNode node = it.next();
      IMethod mm = node.getMethod();
      System.err.println(mm.getSignature());
    }
    System.err.println("succ nodes:");
    for (Iterator<CGNode> it = cg.getSuccNodes(n); it.hasNext(); ) {
      CGNode node = it.next();
      IMethod mm = node.getMethod();
      System.err.println(mm.getSignature());
    }
    */
    
    AnalysisOptions options = new AnalysisOptions(scope, tmp_eps); 
    options.setReflectionOptions(ReflectionOptions.FULL); //ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE); //ONE_FLOW_TO_CASTS_NO_METHOD_INVOKE);
    // Contex-insensitive
    com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope, null, null);
    // Context-sensitive
    /*
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultSelectors(options, cha); 
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha); 
    //ContextSelector contextSelector = new DefaultContextSelector(options);    
    //SSAContextInterpreter contextInterpreter = new DefaultSSAInterpreter(options, Cache);
    SSAPropagationCallGraphBuilder builder = new nCFABuilder(1, cha, options, new AnalysisCache(), null, null); 
    AllocationSiteInNodeFactory factory = new AllocationSiteInNodeFactory(options, cha);
    builder.setInstanceKeys(factory);
    */  
  
    CallGraph g = builder.makeCallGraph(options, null);
    System.err.println("CallGraph.getEntrypointNodes : " + g.getEntrypointNodes());
    System.err.println(CallGraphStats.getStats(g) + "\n");
    
    viewCallGraphSWT(g);
    Graph<CGNode> newg = pruneGraph(g, new ApplicationLoaderFilter());  
    viewCallGraphPDF(newg);
  }
  
  
  public static void testCGNode() {
    System.err.println("JX-breakpoint-testCGNode");
    
    int currentone = 0;
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
      CGNode n = it.next();
      IMethod m = n.getMethod();
      // test - ClassLoader category   #results NOW - only App & Pri, nothing else
      if (!n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) 
          && !n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Primordial))
         System.err.println(n.getMethod().getDeclaringClass().getClassLoader().getReference().toString());
      // print specified
      if (n.getMethod().getSignature().indexOf(functionname_for_test) >= 0) {
        //if (++currentone == which_functionname_for_test) {
          System.err.println("name: " + n.getMethod().getSignature());
          // see the function's class loader
          System.err.println(n.getMethod().getDeclaringClass().getClassLoader().getReference().toString());
          //break;
        //}
      }
    }
  
    /*
    // Test if all CGNodes are not Interface    #JX: this is unrelated to the method "getPartialCallGraphPDFForTest"
    // test results: only 1(<clinit>) out of 10000+ function belongs to Interface Class
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
      CGNode n = it.next();
      IMethod m = n.getMethod();
      if (m.getDeclaringClass().isInterface()) {
        System.err.println("!!!hasInterface");
        System.err.println(m.getSignature());
      }
      //if (m.getDeclaringClass().isAbstract()) System.err.println("!!!hasAbstract");
    }
    */
  }
  
  
  public static void testIR() throws WalaException {
    System.err.println("JX-breakpoint-testIR");
    
    // Memo - "InetAddress.getAllByName("  "FSDirectory.mkdirs("  //"hdfs.qjournal.client.IPCLoggerChannel$7.call()Ljava/lang/Void"
    CGNode n = null;
    IMethod m = null;
    IR ir = null;
    int currentone = 0;
    
    // Get IR
    int num_of_ircgnode = 0;
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode tmp_n = it.next();
      IMethod tmp_m = tmp_n.getMethod();
      if (tmp_m.getSignature().indexOf(functionname_for_test)>=0) {  //TODO - can't find "loopbackAddress(" at "InetAddress.getAllByName("&whichone=2, this ia s example for advanced pointer analysis
        num_of_ircgnode ++;
        if (++currentone==which_functionname_for_test) {
          n = tmp_n;
          m = tmp_m;
          ir = n.getIR();
          viewIR(ir);  
          System.out.println(m.getSignature());
          //findLocks(n);
          //findLoops(n);  //add find var_name??????????????????????????????????????????????
        } 
      }
    }//for
    if (ir != null) {
      System.err.println( "Totally find " + num_of_ircgnode + " IR(s) for " + functionname_for_test );
    } else {
      System.err.println( "Can't find IR !!!!!!!!!!\n" );
      return;
    }
    
    // test TypeInference
    /*
    boolean doPrimitives = false; // infer types for primitive vars?
    TypeInference ti = TypeInference.make(ir, doPrimitives);
    //TypeAbstraction type = ti.getType(vn);
    //TypeAbstraction type = ti.getType(lock.lock_name_vn);
    //lock.lock_name = type.getClass().toString()+" "+type.getType().toString()+" "+type.getTypeReference().toString();//type.toString();
    for (int i=1; i<50; i++) {
      TypeAbstraction type = ti.getType(i);     //WARN: i can't be 0 or >maxValueNumber, if so, it will cause exception!!!!!!!!!
      if (type != null) {
        System.out.println("ti.getType(" + i + ") - ");
        System.out.println("- " + type.toString());
        System.out.println("- " + type.getClass().toString());             //this and below are part of "type" actually, so we just need to know "type", then to get "getClass"/"getType"/...
        //System.out.println("- " + type.getType().toString());            //some will cause exception!!
        //System.out.println("- " + type.getTypeReference().toString());   //some will cause exception!!
      }
    }
    */
    
    
    //IR#getInstructions
    /*
    SSAInstruction[] ssas= ir.getInstructions();
    for (int i = 0; i < ssas.length; i++) {
      System.out.println("i=" + i + ": " + ssas[i]);
    }
    System.out.println();
    */
    
    
    //test pred nodes and succ nodes
    System.err.println("#pred CGNodes = " + cg.getPredNodeCount(n));
    for (Iterator<CGNode> it = cg.getPredNodes(n); it.hasNext();)
      System.err.println("----" + it.next().toString());
    System.err.println("#succ CGNodes = " + cg.getSuccNodeCount(n));
    for (Iterator<CGNode> it = cg.getSuccNodes(n); it.hasNext();)
      System.err.println("----" + it.next().toString());
    
    //test BasicBlocks and SSAs + call sites
    System.out.println("JX - test BasicBlocks and SSAs + call sites");
    int k=0;
    SSACFG cfg = ir.getControlFlowGraph();
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      //System.out.println(bb);
      for (Iterator<SSAInstruction> it_2 = bb.iterator(); it_2.hasNext(); ) {
        SSAInstruction ssa = it_2.next();
        
        //int num = 0;
        //for (int j=0; j<ssas.length; j++)
        //  if (ssas[j] != null)
        //    if (ssas[j] == ssa) { num++; k = j;}  
        //    //if (ssas[j].equals(ssa)) { num++; k = j;}  
        //    //if (ssas[j].toString().equals(ssa.toString())) { num++; k = j; }
        //if (num != 1) System.err.println("num = " + num + " " + ssa + " k=" + k);
        
        
        if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
          System.out.println(ssa.toString());
          java.util.Set<CGNode> set = cg.getPossibleTargets(n, ((SSAInvokeInstruction) ssa).getCallSite());
          //if (set.size() > 1)
          //  System.err.println("CallGraph#getPossibleTargets's size > 1");
          if (set.size() > 0) {                    //JX: because I haven't yet added "hadoop-common"
            System.err.println("CallGraph#getPossibleTargets's size = " + set.size() + "   cg.getSuccNodeNumbers = " + cg.getSuccNodeCount(n));
            for (CGNode cgnode: set) {
              System.err.println(cgnode.toString());
            }
            /*
            CGNode cgnode = set.iterator().next(); 
            System.out.println(cgnode.toString());
            */
          } else {
            System.err.println("!:can't find");
          }  
        } else {
          //TODO
        }
        
      }//for-it_2
    }//for-it
    
    
    // test value numbers
    System.err.println("Test Value Numbers - ");
    System.out.println("ir.getParameterValueNumbers: " + ir.getParameterValueNumbers()); //JX: it's an array, only following operations can get value numbers.
    for (int i=0; i<ir.getParameterValueNumbers().length; i++)
      System.out.println("i=" + i + ": " + ir.getParameterValueNumbers()[i]);
    System.out.println(ir.getNumberOfParameters());                             //JX: it's 5 in #processReport IR, ie, 5 incoming parameters.
    for (int i=0; i<ir.getNumberOfParameters(); i++)
      System.out.println("i=" + i + ": " + ir.getParameter(i) + " ** " + ir.getParameterType(i));  //JX:#getParameter(i) equals #getParameterValueNumbers()[i]  
    SymbolTable symboltable = ir.getSymbolTable();                                                 //JX: seems same??
    System.out.println("ir.getSymbolTable: " + symboltable);
    System.out.println(symboltable.getNumberOfParameters());
    for (int i=0; i<symboltable.getNumberOfParameters(); i++)
      System.out.println(symboltable.getParameter(i)); 
    System.out.println(symboltable.getMaxValueNumber());  //JX: the maximal variable number, it's 123 in #processReport IR
    System.out.println(symboltable.getPhiValue(1));
    System.out.println(symboltable.getValueString(0) + " " + symboltable.getValueString(1) + " " + symboltable.getValueString(2));
    System.out.println(ir.getOptions());         //?   
  }
  
  
  static MethodReference mr_FS_writeLock, mr_FS_writeUnlock, mr_processReport, mr_processFirstBlockReport, mr_processReport_2;
  static IMethod m_FS_writeLock, m_FS_writeUnlock, m_processReport, m_processFirstBlockReport, m_processReport_2;
   
  public static void testWalaAPI() {
    System.err.println("JX-breakpoint-testWalaAPI");
    
    // Get the Methods of "Locks" what we want to study     
    // FSNamesystem#writeLock
    mr_FS_writeLock = StringStuff.makeMethodReference(
        "org.apache.hadoop.hdfs.server.namenode.FSNamesystem.writeLock()V");  
    m_FS_writeLock = cha.resolveMethod(mr_FS_writeLock);
    System.out.println("method = " + m_FS_writeLock.getSignature()); 
    System.out.println("method = " + m_FS_writeLock); 
    // FSNamesystem#writeUnlock
    mr_FS_writeUnlock = StringStuff.makeMethodReference(
        "org.apache.hadoop.hdfs.server.namenode.FSNamesystem.writeUnlock()V");  
    m_FS_writeUnlock = cha.resolveMethod(mr_FS_writeUnlock);
    System.out.println("method = " + m_FS_writeUnlock.getSignature()); 
    // BlockManager#processReport, not a lock
    mr_processReport = StringStuff.makeMethodReference(
        "org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.processReport(Lorg/apache/hadoop/hdfs/protocol/DatanodeID;Lorg/apache/hadoop/hdfs/server/protocol/DatanodeStorage;Ljava/lang/String;Lorg/apache/hadoop/hdfs/protocol/BlockListAsLongs;)V");
    m_processReport = cha.resolveMethod(mr_processReport);
    System.out.println("method = " + m_processReport);
    System.out.println("method = " + m_processReport.getDeclaringClass());
    System.out.println("method = " + m_processReport.getName());
    System.out.println("method = " + m_processReport.getDescriptor());
    System.out.println("method = " + m_processReport.getReturnType());
    // BlockManager#processFirstBlockReport, not a lock
    mr_processFirstBlockReport = StringStuff.makeMethodReference(
        "org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.processFirstBlockReport(Lorg/apache/hadoop/hdfs/server/blockmanagement/DatanodeDescriptor;Ljava/lang/String;Lorg/apache/hadoop/hdfs/protocol/BlockListAsLongs;)V");
    m_processFirstBlockReport = cha.resolveMethod(mr_processFirstBlockReport);
    System.out.println("method = " + m_processFirstBlockReport);
    // BlockManager#processReport_2, not a lock
    mr_processReport_2 = StringStuff.makeMethodReference(
        "org.apache.hadoop.hdfs.server.blockmanagement.BlockManager.processReport(Lorg/apache/hadoop/hdfs/server/blockmanagement/DatanodeDescriptor;Lorg/apache/hadoop/hdfs/server/protocol/DatanodeStorage;Lorg/apache/hadoop/hdfs/protocol/BlockListAsLongs;)V");
    m_processReport_2 = cha.resolveMethod(mr_processReport_2);
    System.out.println("method = " + m_processReport_2);
    System.out.println("method = " + m_processReport_2.getDeclaringClass());
    System.out.println("method = " + m_processReport_2.getName());
    System.out.println("method = " + m_processReport_2.getDescriptor());
    System.out.println("method = " + m_processReport_2.getReference());
  }
  
 
  
  /**
   * Find Functions with locks
   * Note: Only focus on "Application" functions
   */
  public static void findLockingFunctions() {
    System.out.println("JX-breakpoint-1");
    
    int nFiltered = 0;
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
      CGNode f = it.next();
      IMethod m = f.getMethod();
      int id = f.getGraphNodeId();
      /*
      // test - print
      String sig = m.getSignature();
      if (sig.indexOf(functionname_for_test) >= 0) {
        System.err.println("* - " + sig + "\n" + m.getDeclaringClass().getClassLoader().getReference() + "\n*\n");
        continue;
      }
      */
      ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
      if (classloader_ref.equals(ClassLoaderReference.Application) && !m.isNative()) {     //IMPO:  native method is App class, but can't IR#getControlFlowGraph or viewIR     #must be
        nApplicationFuncs++;
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
      } else {
        //System.out.println(classloader_ref + sig); 
        nPremordialFuncs++;
      }
    }//for
    
    nTotalFuncs = nApplicationFuncs + nPremordialFuncs;
    nLockingFuncs = functions_with_locks.size();
    
    // Print the status
    int N_LOCKS = 20;
    int[] count = new int[N_LOCKS];
    count[0] = nApplicationFuncs - nLockingFuncs;
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
        + "#scanned functions: " + nApplicationFuncs 
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
  
  
  /**
   * Find functions with loops
   * Note: we just focus on "Application" functions, without "Primordial" functions
   */
  public static void findLoopingFunctions() {
    System.out.println("JX-breakpoint-2");

    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
      CGNode function = it.next();
      IMethod m = function.getMethod();
      //String sig = m.getSignature();
      ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
      if (classloader_ref.equals(ClassLoaderReference.Application) && !m.isNative()) {   //IMPO:  native method is App class, but can't IR#getControlFlowGraph or viewIR    #must be
        int id = function.getGraphNodeId();
        List<LoopInfo> loops = findLoops(function);
        if (loops.size() > 0)
          functions_with_loops.put(id, loops);
      } else {
        //System.out.println(classloader_ref + sig); 
      }
    }
    nLoopingFuncs = functions_with_loops.size();
    
    // Print the status
    int N_LOOPS = 20;
    int[] count = new int[N_LOOPS];
    count[0] = nApplicationFuncs - nLoopingFuncs;
    for (Iterator<List<LoopInfo>> it = functions_with_loops.values().iterator(); it.hasNext(); ) {
      int size = it.next().size();
      if (size < N_LOOPS) count[size]++;
    }
    System.out.println("The Status of Loops in All Functions:\n" 
        + "#scanned functions: " + nApplicationFuncs 
        + " out of #Total:" + nTotalFuncs + "(#AppFuncs:" + nApplicationFuncs + "+#PremFuncs:" + nPremordialFuncs +")");    
    System.out.println("#functions with loops: " + nLoopingFuncs);
    System.out.println("//distribution of #loops");
    for (int i = 0; i < N_LOOPS; i++)
      System.out.print("#" + i + ":" + count[i] + ", ");
    System.out.println("\n");
    
    printFunctionsWithLoops();
  }
  
  
  public static List<LoopInfo> findLoops(CGNode function) {
    IR ir = function.getIR();
    SSACFG cfg = ir.getControlFlowGraph();
    
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
      System.err.println("total != n_backedges  #backedges:" + total + " #real backedges:" + n_backedges + " Method:" + function.getMethod().getSignature());
    }
   
    //printLoops(loops);
    
    return loops;
  }
 
  
  public static void printFunctionsWithLoops() {
    //print all locks for those functions with loops
    for (Iterator<Integer> it = functions_with_loops.keySet().iterator(); it.hasNext(); ) {
      int id = it.next();
      List<LoopInfo> loops = functions_with_loops.get(id);
      if (!(cg.getNode(id).getMethod().getSignature().indexOf(functionname_for_test) >= 0)) continue;
      System.err.println(cg.getNode(id).getMethod().getSignature()); 
      printLoops(loops);
    }
  }
  
  public static void printLoops(List<LoopInfo> loops) {
    // Print the function's loops
    //System.out.println(function.getMethod().getSignature());
    System.err.print("#loops-" +loops.size() + " - ");
    for (Iterator<LoopInfo> it = loops.iterator(); it.hasNext(); )
      System.err.print(it.next() + ", ");
    System.err.println();
  }
  
  
 
  /**
   * Find Locks with Loops
   */
  public static void findLoopingLockingFunctions() {
    System.out.println("JX-checkpoint-3");
    
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
    
    if (!f.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) || f.getMethod().isNative()) { // IMPO - native - must be
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
          instruction.numOfLoops_in_current_function = 0;
          for (int j = 0; j < loops.size(); j++)
            if (loops.get(j).bbs.contains(bb)) {
              instruction.numOfLoops_in_current_function ++;
              instruction.loops_in_current_function.add(j);
            }
          //test
          //if (instruction.numOfLoops_in_current_function > 0) System.err.println(instruction.numOfLoops_in_current_function);
        }
        else {
          instruction.numOfLoops_in_current_function = 0;
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
            instruction.numOfLoops_in_call = functions.get(n.getGraphNodeId()).max_depthOfLoops;
            instruction.call = n.getGraphNodeId();
          } else {                     //if we can't find the called CGNode.
            //TODO
            instruction.numOfLoops_in_call = 0;
          }  
        } else {
          //TODO
          instruction.numOfLoops_in_call = 0;
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
      if (instruction.numOfLoops_in_current_function + instruction.numOfLoops_in_call > function.max_depthOfLoops) {
        max_instruction = instruction;
        function.max_depthOfLoops = instruction.numOfLoops_in_current_function + instruction.numOfLoops_in_call;
      }
    }
    if (max_instruction != null && max_instruction.call >= 0) {
      function.function_chain_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).function_chain_for_max_depthOfLoops);
      function.hasLoops_in_current_function_for_max_depthOfLoops.addAll(functions.get(max_instruction.call).hasLoops_in_current_function_for_max_depthOfLoops);
    }
    function.function_chain_for_max_depthOfLoops.add(id);
    if (max_instruction != null && max_instruction.numOfLoops_in_current_function > 0)
      function.hasLoops_in_current_function_for_max_depthOfLoops.add(max_instruction.numOfLoops_in_current_function);
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
          int numOfLoops_in_current_function = 0;
          if (instruction.numOfLoops_in_current_function > 0) {
            for (int j = 0; j < instruction.loops_in_current_function.size(); j ++) {
              LoopInfo loop = loops.get( instruction.loops_in_current_function.get(j) );
              if (lock.bbs.containsAll(loop.bbs))
                numOfLoops_in_current_function ++;
            }
          }
          // Then
          int numOfLoops = numOfLoops_in_current_function + instruction.numOfLoops_in_call;
          //if (instruction.numOfLoops_in_call >= 7 && instruction.numOfLoops_in_call <= 15) System.err.println("!!:" + instruction.numOfLoops_in_call);
          if (numOfLoops <= 0)
            continue;
          if (loopinglock == null) {
            loopinglock = new LoopingLockInfo();
            loopinglock.max_depthOfLoops = 0;
          }
          if (numOfLoops > loopinglock.max_depthOfLoops) {
            loopinglock.max_depthOfLoops = numOfLoops;
            max_instruction = instruction;
            max_depthOfLoops_in_current_function = numOfLoops_in_current_function;
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
  
  
  
  
  public static void analyzeAllLocks() {
    System.out.println("JX-breakpoint-4");

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
    System.out.println("JX-breakpoint-5");
 
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
  
  
  static List<LockInfo> heartbeatlocks = new ArrayList<LockInfo>();
  static Set<String> set_of_heartbeatlocks = new TreeSet<String>();
  static String heartbeatname = "TaskTracker.offerService";    // MapReduce - hadoop-1.0.0
  //String heartbeatname = "BPServiceActor.offerService"; //# or "BPServiceActor.run(";  //HDFS-2.3.0 
  
  public static void findLocksForHeartbeat() throws InterruptedException {
    System.out.println("JX-checkpoint-6");
    // Get heartbeate locks & lock groups
    int numOfHeartbeat = 0;
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
      CGNode f = it.next();
      if (!(f.getMethod().getSignature().indexOf( heartbeatname )>=0) )  //functionname_for_test
        continue;
      if (++numOfHeartbeat > 1) {
        System.err.println("WARN - the number of Hearbeart Methods > 1 - we only deal with the FIRST one here!!!!");
        break;
      }
      Set<Integer> traversed_ids = new HashSet<Integer>();      //tmp
      dfsToGetLocks(f, 0, heartbeatlocks, traversed_ids);
    }//for-outermost
    for (LockInfo lock: heartbeatlocks)
      set_of_heartbeatlocks.add( lock.lock_identity );
    nHeartbeatLocks = heartbeatlocks.size();
    nHeartbeatLockGroups = set_of_heartbeatlocks.size();
    // Get common lock groups
    Set<String> tmpset = new TreeSet<String>();
    tmpset.addAll(set_of_heavylocks);
    tmpset.retainAll(set_of_heartbeatlocks);
    int nCommonLockGroups = tmpset.size();
    
    // Print the status
    System.out.println("#hearbeat locks: " + nHeartbeatLocks + " out of total " + nLocks + " locks");
    System.out.println("#groups of heartbeat locks (ie, real number): " + nHeartbeatLockGroups + " out of total " + nLockGroups + " lock groups");
    //for (String str: set_of_heartbeatlocks)
    //  System.err.println("  " + str);
    System.out.println("#common lock groups: " + nCommonLockGroups);
    System.out.println("#common (suspected heavy) locks are as follows -");
    
    nSuspectedHeavyLocks = 0;
    for (LoopingLockInfo loopinglock: heavylocks) {
      boolean suspected = false;
      for (LockInfo lock2: heartbeatlocks)
        if (loopinglock.lock.lock_identity.equals( lock2.lock_identity )) {
          suspected = true;
          //System.out.println(lock2);
        }
      if (suspected) {
        nSuspectedHeavyLocks++;
        System.out.println("#" + nSuspectedHeavyLocks + ": " + loopinglock.lock);
        Thread.sleep(200);
        // Print the Suspected Heavy Lock (ie, time-consuming looping lock)
        System.err.println("Depth of loops = " + loopinglock.max_depthOfLoops + " :: " + loopinglock.lock.func_name);
        // print the function chain
        for (int k = loopinglock.function_chain_for_max_depthOfLoops.size()-1; k >= 0; k--) {
          //String short_fn = cg.getNode( loopinglock.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getName().toString();
          String sig = cg.getNode( loopinglock.function_chain_for_max_depthOfLoops.get(k) ).getMethod().getSignature();  //use getName() to only get function name.
          int s1 = sig.lastIndexOf('.', sig.lastIndexOf('.')-1)+1;
          int e1 = sig.indexOf('(');
          System.err.print(sig.substring(s1, e1) + "#" + loopinglock.hasLoops_in_current_function_for_max_depthOfLoops.get(k) + "#" + "->");
        }
        System.err.println("End");
        Thread.sleep(200);
      }
    }
    
    // Print the status
    System.out.println("#Suspected Heavy Locks = " + nSuspectedHeavyLocks);
    System.out.println();
  }
  
  
  static int MAXN_HEARTBEAT_DEPTH = 30;
  
  public static void dfsToGetLocks(CGNode f, int depth, List<LockInfo> locks_in_heartbeat, Set<Integer> traversed_ids) {
    
    // Test - Print call graph tree
    /*
    for (int i=0; i<depth; i++) System.err.print("----");
    System.err.println(f.getMethod().getSignature());
    */
    int id = f.getGraphNodeId();
    String short_funcname = f.getMethod().getName().toString();
    
    if (traversed_ids.contains(id))
      return;
    else 
      traversed_ids.add(id);
    
    if (depth > MAXN_HEARTBEAT_DEPTH) {
      return ;
    }
    if (!f.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application) || f.getMethod().isNative()) { // IMPO - native - must be
      return ;
    }
    if (locktypes.contains(short_funcname) || unlocktypes.contains(short_funcname)) {  //TODO - others
      return ;
    }
    
    if (functions_with_locks.containsKey(id))
      locks_in_heartbeat.addAll( functions_with_locks.get(id) );
    
    IR ir = f.getIR();
    SSAInstruction[] instructions = ir.getInstructions();
    
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction ssa = instructions[i];
      if (ssa == null) continue;
      if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
        java.util.Set<CGNode> set = cg.getPossibleTargets(f, ((SSAInvokeInstruction) ssa).getCallSite());
        // Test - Print possible targets
        /*
        if (set.size() == 0) {
          System.err.println("#cg.getPossibleTargets(n) = " + set.size() + " - " + set);  //if (set.size() > 1) System.err.println("CallGraph#getPossibleTargets's size > 1"); // for Test, how to solve the problem??
          System.err.println("ssa - " + ssa + " IN " + f.getMethod().getSignature());
        }
        if (set.size() > 0) {
          System.err.println("#cg.getPossibleTargets(n) = " + set.size() + " - " + set);  //if (set.size() > 1) System.err.println("CallGraph#getPossibleTargets's size > 1"); // for Test, how to solve the problem??
          System.err.println("ssa - " + ssa + " IN " + f.getMethod().getSignature());
        }
        */
        if (set.size() > 0) {                 //JX: because I haven't yet added "hadoop-common"
          CGNode n = set.iterator().next();
          /*
          if (n.getMethod().getSignature().indexOf("JobTracker")>=0)
            System.err.println("!!!!!!JobTracker is from " + f.getMethod().getSignature() + " - " + ssa);
          */
          dfsToGetLocks(n, depth+1, locks_in_heartbeat, traversed_ids);
        }
      }
    }//for
  }
    
  
  
  
  
  
  
  
  
  static Map<Integer, FuncRecord> records_for_all_functions = new HashMap<Integer, FuncRecord>();
  
  class FuncRecord {
    int max_depth_of_loops;
    List<Integer> longest_function_chain_ids;
    //String[] variables;
    
    public FuncRecord() {
      this.max_depth_of_loops = 0;
      this.longest_function_chain_ids = new ArrayList<Integer>();
      //this.variables = new String[JXLocks.MAX_LAYER];
    }
  }
  
  static int MAX_LAYER = 100;       //200? should deal with recursive functions particularly
  static int real_layer = 0;
                                   
                                                       //Tmp: CGNode lock, CGNode unlock
  public static void dfs(CGNode function, int layer) { 
    
    FuncRecord record = new FuncRecord();
    
    if (layer > real_layer) real_layer = layer;
    if (layer >= MAX_LAYER-1) { //this is a tradeoff
      record.max_depth_of_loops = 0;
      record.longest_function_chain_ids.add(function.getGraphNodeId());
      records_for_all_functions.put(function.getGraphNodeId(), record);
    }
    if (!function.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) { //JX£ºneeded
      record.max_depth_of_loops = 0; //maybe some can be marked as a big value;
      record.longest_function_chain_ids.add(function.getGraphNodeId());
      records_for_all_functions.put(function.getGraphNodeId(), record);
    }
    String short_funcname = function.getMethod().getName().toString();
    if (locktypes.contains(short_funcname) || unlocktypes.contains(short_funcname)) {  //TODO - others
      record.max_depth_of_loops = 0; //maybe some can be marked as a big value;
      record.longest_function_chain_ids.add(function.getGraphNodeId());
      records_for_all_functions.put(function.getGraphNodeId(), record);
    }
    
    IR ir = function.getIR();
    SSACFG cfg = ir.getControlFlowGraph();
    int numOfSSAs = -1;
    List<FuncRecord> tmp = new ArrayList<FuncRecord>(); 
    
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      for (Iterator<SSAInstruction> it_2 = bb.iterator(); it_2.hasNext(); ) {
        SSAInstruction ssa = it_2.next();
        
        if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
          java.util.Set<CGNode> set = cg.getPossibleTargets(function, ((SSAInvokeInstruction) ssa).getCallSite());
          if (set.size() > 1)
            System.err.println("CallGraph#getPossibleTargets's size > 1");
          if (set.size() > 0) {                    //JX: because I haven't yet added "hadoop-common"
            CGNode cgnode = set.iterator().next(); 
            if (!records_for_all_functions.containsKey(cgnode.getGraphNodeId()))
              dfs(cgnode, layer+1); 
            numOfSSAs ++;
          } else {
            //TODO
            record.max_depth_of_loops = 0; //maybe some can be marked as a big value;
            record.longest_function_chain_ids.add(function.getGraphNodeId());
            records_for_all_functions.put(function.getGraphNodeId(), record);
          }  
          tmp.add(e)
        } else {
          //TODO
          
        }
      } //end-it_2
    } //end-it
    
    
    int nCalls = 0;
    int id = function.getGraphNodeId();
    if (!functions_with_loops.containsKey(id)) {
      
    } else {
      functions_with_loops.get(function.getGraphNodeId())
    }
    
    // Scan the current function for the current layer
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      for (Iterator<SSAInstruction> it_2 = bb.iterator(); it_2.hasNext(); ) {
        SSAInstruction ssa = it_2.next();
        if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
          //System.out.println("ssa-Invoke");
        } else if (ssa instanceof SSAFieldAccessInstruction) {
          //System.out.println("ssa-FieldAccess");
        } else if (ssa instanceof SSANewInstruction) {
          //System.out.println("ssa-New");
        } else if (ssa instanceof SSAConditionalBranchInstruction) {
          //System.out.println("ssa-ConditionalBranch");
        } else if (ssa instanceof SSAGotoInstruction) {
          //System.out.println("ssa-Goto");
        } else if (ssa instanceof SSAReturnInstruction) {
          //System.out.println("ssa-Return");
        } else if (ssa instanceof SSASwitchInstruction) {
          //System.out.println("ssa-Switch");
        } else if (ssa instanceof SSABinaryOpInstruction) {
          //System.out.println("ssa-BinaryOp");
        } else if (ssa instanceof SSACheckCastInstruction) {
          //System.out.println("ssa-CheckCast");
        } else if  (ssa instanceof SSAGetCaughtExceptionInstruction) {
          //System.out.println("ssa-GetCaughtException");
        } else if  (ssa instanceof SSAAbstractThrowInstruction) {
          //System.out.println("ssa-AbstractThrow");
        } else if  (ssa instanceof AstAssertInstruction) {
          //System.out.println("ssa-AstAssert");
        } else if  (ssa instanceof SSAMonitorInstruction) {
          //System.out.println("ssa-Monitor");
        } else if (ssa instanceof SSAComparisonInstruction) {
          //System.out.println("ssa-Comparison");
        } else if (ssa instanceof SSAConversionInstruction) {
          //System.out.println("ssa-Conversion");
        } else if (ssa instanceof SSAInstanceofInstruction) {
          //System.out.println("ssa-Instanceof");
        } else if (ssa instanceof SSAAddressOfInstruction) {
          //System.out.println("ssa-AddressOf");
        } else if (ssa instanceof SSAArrayLengthInstruction) {
          //System.out.println("ssa-ArrayLength");
        } else if (ssa instanceof SSAArrayReferenceInstruction) {
          //System.out.println("ssa-ArrayReference");
        } else if (ssa instanceof SSAPhiInstruction) {
          //System.out.println("ssa-Phi");
        } else if  (ssa instanceof ReflectiveMemberAccess) {
          //System.out.println("ssa-ReflectiveMemberAccess");
        } else if  (ssa instanceof SSALoadMetadataInstruction) {
          //System.out.println("ssa-LoadMetadata");
        } else if  (ssa instanceof SSAStoreIndirectInstruction) {
          //System.out.println("ssa-StoreIndirect");
        } else if  (ssa instanceof SSAAbstractUnaryInstruction) {
          //System.out.println("ssa-AbstractUnary");
        } else if  (ssa instanceof AstEchoInstruction) {
          //System.out.println("ssa-AstEcho");
        } else if  (ssa instanceof AstIsDefinedInstruction) {
          //System.out.println("ssa-AstIsDefined");
        } else if  (ssa instanceof AstLexicalAccess) {
          //System.out.println("ssa-AstLexicalAcces");
        } else {
          //case (ssa instanceof EnclosingObjectReference):
          System.out.println("ssa-Others");
        }
      } //end-it_2
    } //end-it
    
    // Merge??????????????????????????????????????????????????????????????????????????????????
    
    records_for_all_functions.put(function.getGraphNodeId(), record);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  public static void findAllCGNodesOfLocks() {
    // Find all CGNodes of "Lock" functions by traversing the whole call graph
    // TODO - how to get all kinds of locks
    CGNode[] lock = findLock(m_FS_writeLock);
    handleLock(lock[0], lock[1]);
  }
  
  
  public static CGNode[] findLock(IMethod method) {
    CGNode[] lock = new CGNode[2];
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode n = it.next();
      IMethod m = n.getMethod();
      /*
      if (m.getSignature().indexOf("process")>=0)   //like "readLock" "writeLock" 
        System.out.println(m.getSignature());
      */
      if (m.getSignature().equals(method.getSignature())) {    //can't use ==, this is reference-related
        lock[0] = n;
        //break;
      } else if (m.getSignature().equals(m_FS_writeUnlock.getSignature())) {  // Temporary for Unlock
        lock[1] = n;
      }
      
    }
    return lock;
  }
  
  
  public static void handleLock(CGNode lock, CGNode unlock) {
    // Traverse lock-related methods
    System.out.println("JX-breakpoint-3");
    for (Iterator<CGNode> it = cg.getPredNodes(lock); it.hasNext(); ) {
      CGNode n = it.next();
      //System.out.println(n);
      IMethod m = n.getMethod();
      if (!m.getSignature().equals(m_processReport.getSignature()))
        continue;
      handleFunction(n, lock, unlock);
    }
  }
  
  
  public static void handleFunction(CGNode function, CGNode lock, CGNode unlock) { 

    critisec = dfs(function, 0, lock, unlock);
    
    /*
    // Acquiring ONLY lock-related SSAs
    IR ir = function.getIR();  //viewIR(ir);
    SSACFG cfg = ir.getControlFlowGraph();
    boolean begin = false;
    boolean end = false;
    
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      //System.out.println("bb: " + bb);
      for (Iterator<SSAInstruction> it_2 = bb.iterator(); it_2.hasNext(); ) {
        SSAInstruction ssa = it_2.next();
        //System.out.println("ssa: " + ssa);
        
        if (ssa instanceof SSAInvokeInstruction) {
          java.util.Set<CGNode> set = cg.getPossibleTargets(function, ((SSAInvokeInstruction) ssa).getCallSite());
          if (set.size() > 1)
            System.err.println("CallGraph#getPossibleTargets's size > 1");
          CGNode n = set.iterator().next(); 
          if (n.equals(lock))
            begin = true;
          else if (n.equals(unlock)) {
            end = true;
            break;
          }
          // Acquiring lock-related SSAs
          
        }
      }
      if (end) break;
    }  
    
    // Print the results
    */
  }
  
  /*
  static int MAX_LAYER = 10;
  static int MAX_N_SSAS = 10000;  //maybe prob!
  static int real_layer = 0;
  static ValFunc critisec = new ValFunc();
                                                   //Tmp: CGNode lock, CGNode unlock
  public static ValFunc dfs(CGNode function, int layer, CGNode lock, CGNode unlock) { 
    
    ValFunc valfunc = new ValFunc();
    
    if (layer > real_layer) real_layer = layer;
    if (layer >= MAX_LAYER-1) {
      valfunc.max_numOfFors = 1;
      valfunc.functions[layer] = function.getMethod().getSignature();
      valfunc.variables[layer] = "";
      valfunc.max_layer = layer+1;
      return valfunc;
    }
    if (function.equals(lock) || function.equals(unlock)) {
      valfunc.max_numOfFors = 1;
      valfunc.functions[layer] = function.getMethod().getSignature();
      valfunc.variables[layer] = "";
      valfunc.max_layer = layer+1;
      return valfunc;
    }
    if (!function.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)) { //JX£ºneeded
      valfunc.max_numOfFors = 1;
      valfunc.functions[layer] = function.getMethod().getSignature();
      valfunc.variables[layer] = "";
      valfunc.max_layer = layer+1;
      return valfunc;
    }
    
    ValFunc[] tmps = new ValFunc[MAX_N_SSAS];
    int num = 0;
    
    IR ir = function.getIR();
    SSACFG cfg = ir.getControlFlowGraph();
    
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      for (Iterator<SSAInstruction> it_2 = bb.iterator(); it_2.hasNext(); ) {
        SSAInstruction ssa = it_2.next();
        if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
          System.out.println("ssa-Invoke");
          System.out.println(ssa);
          java.util.Set<CGNode> set = cg.getPossibleTargets(function, ((SSAInvokeInstruction) ssa).getCallSite());
          if (set.size() > 1)
            System.err.println("CallGraph#getPossibleTargets's size > 1");
          if (set.size() > 0) {                    //JX: because I haven't yet added "hadoop-common"
            CGNode n = set.iterator().next(); 
            //System.out.println(n);
            tmps[num++] = dfs(n, layer+1, lock, unlock);
          } else {
             
          }  
        } else {
          System.out.println("ssa-non-Invoke");
          
        }
      } //end-it_2
    } //end-it
    
    // Find for-loop and Compute the current layer
    for (Iterator<ISSABasicBlock> it = cfg.iterator(); it.hasNext(); ) {
      ISSABasicBlock bb = it.next();
      for (Iterator<SSAInstruction> it_2 = bb.iterator(); it_2.hasNext(); ) {
        SSAInstruction ssa = it_2.next();
        if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
          //System.out.println("ssa-Invoke");
        } else if (ssa instanceof SSAFieldAccessInstruction) {
          //System.out.println("ssa-FieldAccess");
        } else if (ssa instanceof SSANewInstruction) {
          //System.out.println("ssa-New");
        } else if (ssa instanceof SSAConditionalBranchInstruction) {
          //System.out.println("ssa-ConditionalBranch");
        } else if (ssa instanceof SSAGotoInstruction) {
          //System.out.println("ssa-Goto");
        } else if (ssa instanceof SSAReturnInstruction) {
          //System.out.println("ssa-Return");
        } else if (ssa instanceof SSASwitchInstruction) {
          //System.out.println("ssa-Switch");
        } else if (ssa instanceof SSABinaryOpInstruction) {
          //System.out.println("ssa-BinaryOp");
        } else if (ssa instanceof SSACheckCastInstruction) {
          //System.out.println("ssa-CheckCast");
        } else if  (ssa instanceof SSAGetCaughtExceptionInstruction) {
          //System.out.println("ssa-GetCaughtException");
        } else if  (ssa instanceof SSAAbstractThrowInstruction) {
          //System.out.println("ssa-AbstractThrow");
        } else if  (ssa instanceof AstAssertInstruction) {
          //System.out.println("ssa-AstAssert");
        } else if  (ssa instanceof SSAMonitorInstruction) {
          //System.out.println("ssa-Monitor");
        } else if (ssa instanceof SSAComparisonInstruction) {
          //System.out.println("ssa-Comparison");
        } else if (ssa instanceof SSAConversionInstruction) {
          //System.out.println("ssa-Conversion");
        } else if (ssa instanceof SSAInstanceofInstruction) {
          //System.out.println("ssa-Instanceof");
        } else if (ssa instanceof SSAAddressOfInstruction) {
          //System.out.println("ssa-AddressOf");
        } else if (ssa instanceof SSAArrayLengthInstruction) {
          //System.out.println("ssa-ArrayLength");
        } else if (ssa instanceof SSAArrayReferenceInstruction) {
          //System.out.println("ssa-ArrayReference");
        } else if (ssa instanceof SSAPhiInstruction) {
          //System.out.println("ssa-Phi");
        } else if  (ssa instanceof ReflectiveMemberAccess) {
          //System.out.println("ssa-ReflectiveMemberAccess");
        } else if  (ssa instanceof SSALoadMetadataInstruction) {
          //System.out.println("ssa-LoadMetadata");
        } else if  (ssa instanceof SSAStoreIndirectInstruction) {
          //System.out.println("ssa-StoreIndirect");
        } else if  (ssa instanceof SSAAbstractUnaryInstruction) {
          //System.out.println("ssa-AbstractUnary");
        } else if  (ssa instanceof AstEchoInstruction) {
          //System.out.println("ssa-AstEcho");
        } else if  (ssa instanceof AstIsDefinedInstruction) {
          //System.out.println("ssa-AstIsDefined");
        } else if  (ssa instanceof AstLexicalAccess) {
          //System.out.println("ssa-AstLexicalAcces");
        } else {
          //case (ssa instanceof EnclosingObjectReference):
          System.out.println("ssa-Others");
        }
      } //end-it_2
    } //end-it
    
    // Merge??????????????????????????????????????????????????????????????????????????????????
    if (num > 0) {
      int max_numOfFors = 0;
      int tick = 0;
      for (int i=0; i<num; i++)
        if (tmps[i].max_numOfFors > valfunc.max_numOfFors) {
          valfunc.max_numOfFors = tmps[i].max_numOfFors;
          tick = i;
        }
      valfunc.max_layer = tmps[tick].max_layer;
      for (int i=layer+1; i<valfunc.max_layer; i++) {
        valfunc.functions[i] = tmps[tick].functions[i];
        valfunc.variables[i] = tmps[tick].variables[i];
      }
    }
    else {
      
    }
    return valfunc;
  }
  */
  
     
  /*
  public static void (Properties p) throws WalaException {

    try {
      
      // Find BlockManager#processReport_2#processFirstBlockReport
      CGNode cgnode = null;
      for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
        CGNode n = it.next();
        IMethod m = n.getMethod();
        if (m.getSignature().equals(m_processFirstBlockReport.getSignature())) {    //can't use ==, this is reference-related
          cgnode = n;
          break;
        }
      }  
      IR ir_2 = cgnode.getIR();
      //viewIR(ir_2);
          
    } catch (Exception e) {
      e.printStackTrace();
      return ;
    }
  }
  */
  

  public static void viewTypeHierarchySWT(Graph<IClass> g) throws WalaException {
    // create and run the viewer
    final SWTTreeViewer v = new SWTTreeViewer();
    v.setGraphInput(g);
    Collection<IClass> roots = InferGraphRoots.inferRoots(g);
    if (roots.size() < 1) {
      System.err.println("PANIC: roots.size()=" + roots.size());
      System.exit(-1);
    }
    v.setRootsInput(roots);
    v.run();
    //return v.getApplicationWindow();
  }

  /**
  * Return a view of an {@link IClassHierarchy} as a {@link Graph}, with edges from classes to immediate subtypes
  */
  public static Graph<IClass> typeHierarchy2Graph(IClassHierarchy cha) throws WalaException {
    Graph<IClass> result = SlowSparseNumberedGraph.make();
    for (IClass c : cha) {
      result.addNode(c);
    }
    for (IClass c : cha) {
      for (IClass x : cha.getImmediateSubclasses(c)) {
        result.addEdge(c, x);
      }
      if (c.isInterface()) {  
        for (IClass x : cha.getImplementors(c.getReference())) {
          result.addEdge(c, x);
        }
      }
    }
    return result;
  }
  
  /**
  * Restrict g to nodes from the Application loader   //JX: only require classes from App loader, No Bootstrap/Extension class loader
  */
  static Graph<IClass> pruneForAppLoader(Graph<IClass> g) throws WalaException {
    Predicate<IClass> f = new Predicate<IClass>() {
      @Override public boolean test(IClass c) {
        //return true;    //by JX
        return (c.getClassLoader().getReference().equals(ClassLoaderReference.Application));    //JX: only acquire classes from App loader, No Bootstrap/Extension class loader
      }
    };
    return pruneGraph(g, f);
  }
  
  /**
  * Remove from a graph g any nodes that are not accepted by a {@link Predicate}
  */
  public static <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) throws WalaException {
    Collection<T> slice = GraphSlicer.slice(g, f);
    return GraphSlicer.prune(g, new CollectionFilter<T>(slice));
  }
  
  
  public static void viewCallGraphPDF(Graph<CGNode> g) throws WalaException {       
    /**
     * we can't build overall graph, it's tooooooo big.
     * So we need to prune the call graph or use entry points of interests.
     */
    Properties p = null;
    try {
      p = WalaExamplesProperties.loadProperties();
      p.putAll(WalaProperties.loadProperties());
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    //System.out.println("here1");
    
    String pdfFile = p.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + CG_PDF_FILE;
    String dotExe = p.getProperty(WalaExamplesProperties.DOT_EXE);
    DotUtil.dotify(g, null, PDFTypeHierarchy.DOT_FILE, pdfFile, dotExe);
    String gvExe = p.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
    
    //System.out.println("here2");
    PDFViewUtil.launchPDFView(pdfFile, gvExe);  
  }
  
  
  public static void viewCallGraphSWT(Graph<CGNode> g) throws WalaException {
   
    Properties wp = null;
    try {
      wp = WalaProperties.loadProperties();
      wp.putAll(WalaExamplesProperties.loadProperties());
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
    String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
    String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
    String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);

    // create and run the viewer
    final SWTTreeViewer v = new SWTTreeViewer();
    v.setGraphInput(g);
    v.setRootsInput(inferRoots(g)); //Originally, InferGraphRoots.inferRoots(cg)
    v.getPopUpActions().add(new ViewIRAction(v, g, psFile, dotFile, dotExe, gvExe));
    v.run();
  }
  
  public static <T> Collection<T> inferRoots(Graph<T> g){
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    HashSet<T> s = HashSetFactory.make();
    for (Iterator<? extends T> it = g.iterator(); it.hasNext(); ) {
      T node = it.next();   
      if (g.getPredNodeCount(node) == 0) {
        System.err.println("root : " + ((CGNode) node).getMethod().getSignature());
        s.add(node);
      }
    }
    return s;
  }

  
  public static void viewIR(IR ir) throws WalaException {
    /**
     * JX: it seems "viewIR" is not suitable for some functions like "LeaseManager.checkLeases", 
     * its Exception: failed to find <Application,Lorg/apache/hadoop/fs/UnresolvedLinkException>
     */
    
    // Print IR's basic blocks and SSA instructions.    //JX: good, it includes variable names 
    System.err.println(ir.toString());  
    
    // Preparing
    Properties wp = null;
    try {
      wp = WalaProperties.loadProperties();
      wp.putAll(WalaExamplesProperties.loadProperties());
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
    String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
    String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
    String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
   
    // Generate IR ControlFlowGraph's SWT viewer
    //SSACFG cfg = ir.getControlFlowGraph();
    
    // Generate IR PDF viewer
    PDFViewUtil.ghostviewIR(cha, ir, psFile, dotFile, dotExe, gvExe);
  }
  
}



//===============================================================================================
//++++++++++++++++++++++++++++++++++ External Classes +++++++++++++++++++++++++++++++++++++++++++
//===============================================================================================


class ApplicationLoaderFilter extends Predicate<CGNode> {
  @Override public boolean test(CGNode o) {
    //return true;   //by JX
    if (o instanceof CGNode) {
      CGNode n = (CGNode) o;
      return n.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }
    else if (o instanceof LocalPointerKey) {
      LocalPointerKey l = (LocalPointerKey) o;
      return test(l.getNode());
    } 
    else {
      return false;
    }
  }
}


class LockInfo {
  CGNode func;
  int func_id;       //ie, func.getGraphNodeId()  
  String func_name;  //ie, func.getMethod().getSignature();
  
  String lock_identity;   //only be used in & after the phase of "analyzeAllLocks"
  
  String lock_type;  //for now: "synchronized_method", "synchronized_lock" + "lock", "readLock", "writeLock", "tryLock", "writeLockInterruptibly", "readLockInterruptibly", "lockInterruptibly"
  int lock_name_vn;  //only for "synchronized (vn)"
  String lock_class; //only for "class.lock()/readLock()/writeLock()"    #will not just like this
  String lock_name;  //useless now
  
  int begin_bb;         // bb - basic block in WALA
  int end_bb;  
  Set<Integer> bbs;
  Set<Integer> succbbs; //used as a temporary variable
  Set<Integer> predbbs; //used as a temporary variable
  
  LockInfo() {  
    this.func_id = -1;
    this.func_name = "";
    
    this.lock_type = "";
    this.lock_name_vn = -1;
    this.lock_name = "";
    this.lock_class = "";

    this.begin_bb = -1;
    this.end_bb = -1;
    this.bbs = new TreeSet<Integer>();
    this.succbbs = new HashSet<Integer>();
    this.predbbs = new HashSet<Integer>();
  }
 
  /*
  public void dfsFromEnter(ISSABasicBlock bb, SSACFG cfg) {
    if (isMatched(bb))
      return;
    for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext(); ) {
      ISSABasicBlock succ = it.next();
      int succnum = succ.getNumber();
      //if (succ.isExitBlock()) System.err.println("succ.isExitBlock() = " + bb.getNumber() + ":" + succnum);
      if (!succ.isExitBlock() && !this.bbs.contains(succnum)) {  //add "!succ.isExitBlock()" because there are many edges to Exit Block, but the PDF IR never show that but IR.toString do
        this.bbs.add(succnum);
        dfsFromEnter(succ, cfg);
      }
    } 
  }
  */
  
  public void dfsFromEnter(ISSABasicBlock bb, SSACFG cfg) {
    for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext(); ) {
      ISSABasicBlock succ = it.next();
      int succnum = succ.getNumber();
      if (!this.succbbs.contains(succnum)) {
        this.succbbs.add(succnum);
        dfsFromEnter(succ, cfg);
      }
    } 
  }
  
  public void dfsFromExit(ISSABasicBlock bb, SSACFG cfg) {
    if (this.isMatched(bb))
      return;
    for (Iterator<ISSABasicBlock> it = cfg.getPredNodes(bb); it.hasNext(); ) {
      ISSABasicBlock pred = it.next();
      int prednum = pred.getNumber();
      if (!this.predbbs.contains(prednum)) {
        this.predbbs.add(prednum);
        dfsFromExit(pred, cfg);
      }
    } 
  }
  
  public void mergeLoop() {
    this.predbbs.retainAll(this.succbbs);   
    this.bbs.addAll(this.predbbs);
  }
  
  public boolean isMatched(ISSABasicBlock bb) {
    if (this.lock_type.equals(JXLocks.synchronizedtypes.get(1))) {
      for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext(); ) {
        SSAInstruction ssa = it.next();
        if (ssa instanceof SSAMonitorInstruction)
          if (((SSAMonitorInstruction) ssa).isMonitorEnter()) { //enter
            int vn = ((SSAMonitorInstruction) ssa).getRef();
            if (this.lock_name_vn == vn)
              return true;
          }
      } //for-it
    }
    else { //this.lock_type.equals.("lock/readLock/WriteLock/...")
      for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext(); ) {
        SSAInstruction ssa = it.next();
        if (ssa instanceof SSAInvokeInstruction) {
          String short_funcname = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getName().toString();
          if (JXLocks.locktypes.contains(short_funcname)) { //lock
            String lock_class = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getDeclaringClass().toString();
            if (this.lock_class.equals(lock_class) && this.lock_type.equals(short_funcname))
              return true;
          }
        }
      } //for-it
    }
    return false;
  }
  
  @Override
  public String toString() {
    return "{function:" + func_name + "\t" 
      + " lock_class:" + lock_class + "\t" + " lock_type:" + lock_type + "\t" 
      + " lock_name:" + lock_name + "\t" + " lock_name_vn:" + lock_name_vn + "\t" 
      + " begin:" + begin_bb + " end:" + end_bb + " bbs:" + bbs + "}";
  }
}


class LoopInfo {
  int begin_bb;         // bb - basic block in WALA
  int end_bb;
  String var_name;
  Set<Integer> bbs;
  
  Set<Integer> succbbs; //used as a temporary variable
  Set<Integer> predbbs; //used as a temporary variable
  
  LoopInfo() {
    this.bbs = new TreeSet<Integer>();
    this.succbbs = new HashSet<Integer>();
    this.predbbs = new HashSet<Integer>();
  }
  
  public void dfsFromEnter(ISSABasicBlock bb, SSACFG cfg) {
    for (Iterator<ISSABasicBlock> it = cfg.getSuccNodes(bb); it.hasNext(); ) {
      ISSABasicBlock succ = it.next();
      int succnum = succ.getNumber();
      if (!this.succbbs.contains(succnum)) {
        this.succbbs.add(succnum);
        dfsFromEnter(succ, cfg);
      }
    } 
  }
  
  public void dfsFromExit(ISSABasicBlock bb, SSACFG cfg) {
    if (bb.equals(cfg.getNode(this.begin_bb)))
      return;
    for (Iterator<ISSABasicBlock> it = cfg.getPredNodes(bb); it.hasNext(); ) {
      ISSABasicBlock pred = it.next();
      int prednum = pred.getNumber();
      if (!this.predbbs.contains(prednum)) {
        this.predbbs.add(prednum);
        dfsFromExit(pred, cfg);
      }
    } 
  }
  
  public void mergeLoop() {
    this.predbbs.retainAll(this.succbbs);
    this.bbs.addAll(this.predbbs);
  }
  
  @Override
  public String toString() {
    return "{begin:" + begin_bb + " end:" + end_bb + " var_name:" + var_name + " bbs:" + bbs + "}";
  }
}


class ValFunc {
  int max_numOfFors;
  String[] functions;
  String[] variables;
  int max_layer;
  
  public ValFunc() {
    this.max_numOfFors = 0;
    this.functions = new String[JXLocks.MAX_LAYER];
    this.variables = new String[JXLocks.MAX_LAYER];
  }
}


class FunctionInfo {
  boolean hasLoopingLocks;                     //only for first-level functions that have locks
  Map<Integer, LoopingLockInfo> looping_locks;  //only for first-level functions that have locks, map: lock-id -> max_depthOfLoops
  
  //Newest! just added
  List<LockInfo> locks = null;                //Type - ArrayList<LoopInfo>
  List<LoopInfo> loops = null;                //Type - ArrayList<LoopInfo>
  
  int max_depthOfLoops;
  List<Integer> function_chain_for_max_depthOfLoops;
  List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
  
  Map<Integer, InstructionInfo> instructions;
  
  FunctionInfo() {
    this.hasLoopingLocks = false;                           //only for first-level functions that have locks
    this.looping_locks = new TreeMap<Integer, LoopingLockInfo>(); //only for first-level functions that have locks
    
    this.max_depthOfLoops = 0;
    this.function_chain_for_max_depthOfLoops = new ArrayList<Integer>();
    this.hasLoops_in_current_function_for_max_depthOfLoops = new ArrayList<Integer>();
    
    this.instructions = new TreeMap<Integer, InstructionInfo>();
  }
  
  @Override
  public String toString() {
    String result = "FunctionInfo{ ";
    for (int i = 0; i < instructions.size(); i++) {
      result.concat(instructions.get(i).toString() + " ");
    }
    result.concat("}");
    return result;
  }
}

class LoopingLockInfo {
  CGNode function;
  LockInfo lock;
  
  int max_depthOfLoops;
  List<Integer> function_chain_for_max_depthOfLoops;
  List<Integer> hasLoops_in_current_function_for_max_depthOfLoops;
  
  LoopingLockInfo() {
    this.function = null;
    this.lock = null;
    
    this.max_depthOfLoops = 0;
    this.function_chain_for_max_depthOfLoops = new ArrayList<Integer>();
    this.hasLoops_in_current_function_for_max_depthOfLoops = new ArrayList<Integer>();
  }
}


class InstructionInfo {
  int numOfLoops_in_current_function;         //only for current-level function
  List<Integer> loops_in_current_function;    //only for current-level function
  //List<String> variables;
  
  int numOfLoops_in_call; //only save the longest loop chain
  int call;               //CGNode id, if any 
  List<Integer> function_chain; //CGNode id
  List<Integer> instruction_chain; //instruction index
  
  InstructionInfo() {
    this.numOfLoops_in_current_function = 0;                    //only for current-level function
    this.loops_in_current_function = new ArrayList<Integer>();  //only for current-level function
    
    this.numOfLoops_in_call = 0;
    this.call = -1;
    this.function_chain = new ArrayList<Integer>();
    this.instruction_chain = new ArrayList<Integer>();
  }
  
  @Override
  public String toString() {
    return "InstructionInfo{numOfLoops_in_current_function:" + numOfLoops_in_current_function + ",numOfLoops_in_call:" + numOfLoops_in_call + ",function_chain:" + function_chain + "}";
  }
}





