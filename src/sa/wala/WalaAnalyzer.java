package sa.wala;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.CollectionFilter;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.StringStuff;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.PDFViewUtil;
import com.text.TextFileReader;

import sa.wala.util.PDFCallGraph;
import sa.wala.util.PDFTypeHierarchy;
import sa.wala.util.PDFWalaIR;



public class WalaAnalyzer {   

	Path dirpath;            // Target dir. Should also support single *.jar file, But for now NOT NOT NOT 
  
    // WALA basis
    AnalysisScope scope;
    ClassHierarchy cha;
    HashSet<Entrypoint> entrypoints;
    CallGraph cg;
    public List<String> packageScopePrefixes = new ArrayList<String>();  //read from 'package-scope.txt' if exists
    
    // Statistics
    int nPackageFuncs = 0;       // the real functions we focuses      //must satisfy "isApplicationAndNonNativeMethod" first
    int nTotalFuncs = 0;
    int nApplicationFuncs = 0;    
    int nPremordialFuncs = 0;
    int nOtherFuncs = 0;
    
    // Others
    private final static boolean CHECK_GRAPH = false;
    final public static String CG_PDF_FILE = "cg.pdf";
  

    // Configuration For tests
    // for all
    String functionname_for_test = "doWork0("; //"org.apache.hadoop.hdfs.server.datanode.FSDataset$FSDir.getBlockInfo("; //"RetryCache.waitForCompletion(Lorg/apache/hadoop/ipc/RetryCache$CacheEntry;)"; //"org.apache.hadoop.hdfs.server.balancer.Balancer"; //"Balancer$Source.getBlockList";//"DirectoryScanner.scan"; //"ReadaheadPool.getInstance("; //"BPServiceActor.run("; //"DataNode.runDatanodeDaemon"; //"BPServiceActor.run("; //"BlockPoolManager.startAll"; //"NameNodeRpcServer"; //"BackupNode$BackupNodeRpcServer"; // //".DatanodeProtocolServerSideTranslatorPB"; //"DatanodeProtocolService$BlockingInterface"; //"sendHeartbeat("; //"org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolServerSideTranslatorPB";  //java.util.regex.Matcher.match(";
    int which_functionname_for_test = 1;   //1st? 2nd? 3rd?    //TODO - 0 means ALL, 1 to n means which one respectively
    // for testIR()
    String dotExe = "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe";  //like "C:\\Program Files (x86)\\Graphviz2.38\\bin\\dot.exe"
    String pdfExe = "C:\\Program Files (x86)\\Foxit Software\\Foxit Reader\\FoxitReader.exe";  //like "C:\\Program Files (x86)\\Foxit Software\\Foxit Reader\\FoxitReader.exe"
    String dotFile = "E:\\walaspace\\temp.dt";  //like "Z:\\walaspace\\temp.dt". ps: all parent dirs should exist. 
    String pdfFile = "E:\\walaspace\\ir.pdf";  //like "Z:\\walaspace\\ir.pdf". ps: all parent dirs should exist.
    
    
    /**
     * Input - 1. a dir path including *.jar or *.class, 
     * 		   2. 
     * 		   3. 
     */
    public WalaAnalyzer(String dirstr) {
    	this( Paths.get(dirstr) );
    }
    
    public WalaAnalyzer(Path dirpath) {
    	if ( !Files.exists(dirpath) ) {
    		System.out.println("JX - ERROR - " + "!Files.exists @ " + dirpath + " for Wala");
    		return;
    	}
    	this.dirpath = dirpath;
    	doWork();
    }
    
    public Path getTargetDirPath() {
    	return this.dirpath;
    }
    
    public CallGraph getCallGraph() {
    	return this.cg;
    }
    
    public ClassHierarchy getClassHierarchy() {
    	return this.cha;
    }
    
    public int getNPackageFuncs() {
    	return nPackageFuncs;
    }
    
    public int getNTotalFuncs() {
    	return nTotalFuncs;
    }
    
    public int getNApplicationFuncs() {
    	return nApplicationFuncs;
    }
    
    public int getNPremordialFuncs() {
    	return nPremordialFuncs;
    }
    
    public int getNOtherFuncs() {
    	return nOtherFuncs;
    }
    
 
    
    private void doWork() {
    	System.out.println("JX - INFO - WalaAnalyzer: doWork...");
    
    	try {
    		walaAnalysis(getJarsOrOthers());
    		infoWalaAnalysisEnv();
    		readPackageScope();
    		//testIClass();
    		//testTypeHierarchy();
    		//testCGNode();
    		//testPartialCallGraph();
    		//testIR();         		 //JX - need to configurate Dot and PDFViewer
    		//testWalaAPI();
    	} catch (IllegalArgumentException | CallGraphBuilderCancelException | IOException | UnsoundGraphException
			| WalaException e) {
    		e.printStackTrace();
    	}
    }

    
    
    
    /********************************************************************************
     * JX - Functions Region
     *******************************************************************************/
    
    private String getJarsOrOthers() {
    	String alljars = "";   
    	// Check a dir including *.jar or *.class     #check *.jar first
    	if (new File(dirpath.toString()).isDirectory()) {
    		//try to get all *.jar files under the dir if could, format like "jarpath1:jarpath2:jarpath3:xxx"
	    	try {
				alljars = PDFCallGraph.findJarFiles( new String[]{dirpath.toString()} );
			} catch (WalaException e) {
				e.printStackTrace();
			} 
	    	if ( !alljars.equals("") ) {
	    		System.out.println("JX - INFO - Test Goal - multi *.jar: " + alljars);
	    	}
	    	// ie, without *.jar under "dirpath" recursively. So
	    	else {
	    		alljars = dirpath.toString();
	    		System.out.println("JX - INFO - Test Goal - multi *.class in dir: " + alljars);
	    	}
	    } 
    	// Check a file like xx.jar or xx.class
	    else if (dirpath.toString().endsWith(".jar")) {
	    	alljars = dirpath.toString();
	    	System.out.println("JX - INFO - Test Goal - a x.jar file: " + alljars);
	    }	
	    else if (dirpath.toString().endsWith(".class")) {
	    	alljars = dirpath.toString();
	    	System.out.println("JX - INFO - Test Goal - a x.class file: " + alljars);
	    }
	    else {
	    	System.out.println("JX - ERROR - Test Goal - others: " + alljars);
	    	System.exit(1);
	    }
	    return alljars;
    }
    
 
    /**
     * @param alljars - absolute path or relative path, can be one of the following
     * 					1. multiple .jar files separated by ":"(linux) or ";"(win), "src/sa/res/ca-6744/xx.jar;xxx/xx.jar"
     * 					2. a class-file dir, can be any-level dir, like "bin/sa", "bin/sa/test", "bin/sa/test/testsrc", will identify all *.class recursively and overlook *.jar
     * 					3. single .jar file, "src/sa/res/ca-6744/xx.jar"
     * 					4. single .class file
     */
    private void walaAnalysis(String alljars) throws IOException, IllegalArgumentException, CallGraphBuilderCancelException, UnsoundGraphException, WalaException {
		System.out.println("JX - INFO - WalaAnalyzer: walaAnalysis...");

	    // Create a Scope                                                                           #"JXJavaRegressionExclusions.txt"
	    scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(alljars, (new FileProvider()).getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS)); //default: CallGraphTestUtil.REGRESSION_EXCLUSIONS
	    // Create a Class Hierarchy
	    cha = ClassHierarchy.make(scope);  
	    //testTypeHierarchy();
	    
	    // Create a Entry Points
	    entrypoints = new HashSet<Entrypoint>();
	    Iterable<Entrypoint> allappentrypoints = new AllApplicationEntrypoints(scope, cha);  //Usually: entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);  //get main entrypoints
	    // Get all entry points
	    entrypoints = (HashSet<Entrypoint>) allappentrypoints;
	    // TODO - can narrow entrypoints according to "scope.txt"
	    
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
	
    
    private void infoWalaAnalysisEnv() {
    	System.out.println("JX - INFO - WalaAnalyzer: infoWalaAnalysisEnv");
      
    	int nAppNatives = 0;
    	int nPriNatives = 0;
    	int nOthNatives = 0;
      
    	for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
    		CGNode f = it.next();
	      	nTotalFuncs ++;
	      	if ( isApplicationMethod(f) ) {
	      		nApplicationFuncs ++;
	      		if ( isNativeMethod(f) ) nAppNatives ++;
	      	}
	      	else if ( isPrimordialMethod(f) ) {
	      		nPremordialFuncs ++;
	      		if ( isNativeMethod(f) ) nPriNatives ++;
	      	}
	      	else {
	      		nOtherFuncs++;
	      		if ( isNativeMethod(f) ) nOthNatives ++;
	      	}
	      
	      	if ( isInPackageScope(f) ) {
	      		nPackageFuncs ++;
	      	}
    	}
      
    	System.out.println( "nTotalFuncs(" + nTotalFuncs 
    		  + ") = nApplicationFuncs(" + nApplicationFuncs + ") + nPremordialFuncs(" + nPremordialFuncs + ") + nOtherFuncs(" + nOtherFuncs + ")" );
    	System.out.println( "\t" + "nApplicationFuncs(" + nApplicationFuncs + ") includes " + nAppNatives + " native methods" );
    	System.out.println( "\t" + "nPremordialFuncs(" + nPremordialFuncs + ") includes " + nPriNatives + " native methods" );
    	System.out.println( "\t" + "nOtherFuncs(" + nOtherFuncs + ") includes " + nOthNatives + " native methods" );
    	System.out.println( "nPackageFuncs(" + nPackageFuncs + ") - Note: this should be isApplicationAndNonNativeMethod first" );
    }
  
    
    private void readPackageScope() {
    	System.out.println("JX - INFO - WalaAnalyzer: readPackageScope");
    	String filepath = Paths.get(dirpath.toString(), "package-scope.txt").toString();

    	File f = new File( filepath );
	  
    	if ( !f.exists() ) {
    		System.out.println("NOTICE - not find the 'package-scope.txt' file, so SCOPE is ALL methods!!");
    		return;
    	}

    	TextFileReader reader;
    	String tmpline;
		try {
			reader = new TextFileReader(filepath);
			while ( (tmpline = reader.readLine()) != null ) {
				String[] strs = tmpline.split("\\s+");
				packageScopePrefixes.add( strs[0] );
			}
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("JX - ERROR - when reading package-scpoe.txt files");
			e.printStackTrace();
		}
		System.out.print("NOTICE - successfully read the 'package-scope.txt' file as SCOPE, including:");
		for (String str: packageScopePrefixes)
			System.out.print( " " + str );
		System.out.println();
	
    }
  
    
    // must satisfy "isApplicationAndNonNativeMethod" first
    public boolean isInPackageScope(CGNode cgNode) {
    	// added 
    	if ( !isApplicationAndNonNativeMethod(cgNode) )
    		return false;
    	// if without 'package-scope.txt'
    	if (packageScopePrefixes.size() == 0)
    		return true;
    	String signature = cgNode.getMethod().getSignature();
    	for (String str: packageScopePrefixes)
    		if (signature.startsWith(str))
    			return true;
    	return false;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///  for Testing
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
  
    public void testIClass() throws WalaException {
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
  
    
    public void testTypeHierarchy() throws WalaException {
	    System.err.println("JX-breakpoint-testTypeHierarchy");
	    
	    // Test - class hierarchy
	    System.err.println("Number of All Classes = " + cha.getNumberOfClasses());
	    for (IClass c : cha) {  
	      if (c.getName().toString().indexOf(functionname_for_test) >= 0) {
	        System.err.println(c.getName().toString());
	      }
	    }
	    
	    // View the whole Type Hierarchy SWT if needed
	    Graph<IClass> g = typeHierarchy2Graph(cha);
	    g = pruneForAppLoader(g);
	    viewTypeHierarchySWT(g);
	    
	    // Print some related Type Hierarchy
	    /*
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
	    */
    }
  
  
    /**
     * Note - The way to print call graph from a entry point (DataNode.runDatanodeDaemon) is wrong, the call graph will be incomplete, 
     * because it will miss some context information in this method, so it will miss some call sites in this method.
     * E.g., DataNode.runDatanodeDaemon will miss all call sites (eg, blockPoolManager.startAll(); dataXceiverServer.start();),
     * because it can't get the variables blockPoolManager and dataXceiverServer
     */
    public void testPartialCallGraph() throws IllegalArgumentException, CallGraphBuilderCancelException, WalaException {  
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
  
  
    public void testCGNode() {
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
  
  
    
    
//    public CGNode getTestNode() {
//	    CGNode n = null;
//	    IMethod m = null;
//	    IR ir = null;
//	    int currentone = 0;
//	    
//	    // Get IR
//	    int num_of_ircgnode = 0;
//	    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
//	    	CGNode tmp_n = it.next();
//	    	IMethod tmp_m = tmp_n.getMethod();
//	    	if (tmp_m.getSignature().indexOf(functionname_for_test)>=0) {  //TODO - can't find "loopbackAddress(" at "InetAddress.getAllByName("&whichone=2, this ia s example for advanced pointer analysis
//	    		num_of_ircgnode ++;
//	    		if (++currentone==which_functionname_for_test) {
//	    			n = tmp_n;
//	    			m = tmp_m;
//	    			ir = n.getIR();
//	    			viewIR(ir);  
//	    			System.out.println(m.getSignature());
//	    			//findLocks(n);
//	    			//findLoops(n);  //add find var_name??????????????????????????????????????????????
//	    		} 
//	    	}
//	    }//for
//	    if (ir != null) {
//	      System.err.println( "Totally find " + num_of_ircgnode + " IR(s) for " + functionname_for_test );
//	    } else {
//	      System.err.println( "Can't find IR !!!!!!!!!!\n" );
//	      return;
//	    }
//	    
//    }
    
    
    public void testIR() throws WalaException {
	    System.err.println("JX - INFO - WalaAnalyzer: testIR");
	    
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
   
    public void testWalaAPI() {
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
  
 
    public void viewTypeHierarchySWT(Graph<IClass> g) throws WalaException {
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
	public Graph<IClass> typeHierarchy2Graph(IClassHierarchy cha) throws WalaException {
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
	Graph<IClass> pruneForAppLoader(Graph<IClass> g) throws WalaException {
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
	public <T> Graph<T> pruneGraph(Graph<T> g, Predicate<T> f) throws WalaException {
	    Collection<T> slice = GraphSlicer.slice(g, f);
	    return GraphSlicer.prune(g, new CollectionFilter<T>(slice));
	}
	  
	  
	public void viewCallGraphPDF(Graph<CGNode> g) throws WalaException {       
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
	  
	  
	public void viewCallGraphSWT(Graph<CGNode> g) throws WalaException {
	   
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
	    /* commented by JX temporarily - because couldn't import org.eclipse.jface.*
	    v.setGraphInput(g);
	    v.setRootsInput(inferRoots(g)); //Originally, InferGraphRoots.inferRoots(cg)
	    v.getPopUpActions().add(new ViewIRAction(v, g, psFile, dotFile, dotExe, gvExe));
	    v.run();
	    */
	}
	  
	public <T> Collection<T> inferRoots(Graph<T> g){
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

	  
	public void viewIR(IR ir) throws WalaException {
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
	    //String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
	    //String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
	    //String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
	    //String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
	    if ( !Files.exists( Paths.get(dotExe)) ) 
	    	System.out.println("JX - ERROR - the software location of 'dot' is wrong");
	    if ( !Files.exists( Paths.get(pdfExe)) )
	    	System.out.println("JX - ERROR - the software location of 'pdfviewer' is wrong");
	   
	    // Generate IR ControlFlowGraph's SWT viewer
	    //SSACFG cfg = ir.getControlFlowGraph();
	    
	    // Generate IR PDF viewer
	    PDFViewUtil.ghostviewIR(cha, ir, pdfFile, dotFile, dotExe, pdfExe); //that is, psFile, dotFile, dotExe, gvExe, originally
	}	  
	  
	  
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Static Methods - Begin
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static boolean isApplicationAndNonNativeMethod(CGNode f) {
  	  	if ( isApplicationMethod(f) && !isNativeMethod(f) )  //IMPO:  some native methods are App class, but can't IR#getControlFlowGraph or viewIR     #must be
  	  		return true;
  	  	return false;
    }
	    
    public static boolean isApplicationMethod(CGNode f) {
  	  	IMethod m = f.getMethod();
  	  	ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
  	  	if ( classloader_ref.equals(ClassLoaderReference.Application) )
  	  		return true;
  	  	return false;
    }
    
    public static boolean isNativeMethod(CGNode f) {
  	  	IMethod m = f.getMethod();
  	  	if ( m.isNative() )
  	  		return true;
  	  	return false;
    }

    public static boolean isPrimordialMethod(CGNode f) {
  	  	IMethod m = f.getMethod();
  	  	ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
  	  	if ( classloader_ref.equals(ClassLoaderReference.Primordial) )
  	  		return true;
  	  	return false;
    }
    // End - Static Methods 
    
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



