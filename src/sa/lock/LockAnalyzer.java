package sa.lock;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
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

import sa.lockloop.CGNodeInfo;
import sa.lockloop.CGNodeList;
import sa.loop.LoopInfo;
import sa.wala.IRUtil;
import sa.wala.WalaAnalyzer;



public class LockAnalyzer {
	
	// wala
	WalaAnalyzer walaAnalyzer;
	CallGraph cg;
	Path outputDir;
	// database
	CGNodeList cgNodeList = null;  	//from outside
	
	// results - couldn't use cgNodeList directly, because we maybe need to modify cgNodeList when using it
	ArrayList<CGNodeInfo> lockCGNodes = new ArrayList<CGNodeInfo>();   //entry in CGNodeList
	int nLocks = 0;
	int nLockingCGNodes = 0;
	int nLockGroups = 0; 
	//tmp
	int nFiltered = 0;
	
	
	
	// Lock Names
	public static List<String> synchronizedtypes = Arrays.asList("synchronized_method", "synchronized_lock");
	public static List<String> locktypes = Arrays.asList("lock", "readLock", "writeLock", "tryLock", "writeLockInterruptibly", "readLockInterruptibly", "lockInterruptibly"); //last two added by myself
	public static List<String> unlocktypes = Arrays.asList("unlock", "readUnlock", "writeUnlock");
	Map<String,String> mapOfLocktypes = new HashMap<String,String>() {{
		put("lock", "unlock");
		put("readLock", "readUnlock");
		put("writeLock", "writeUnlock");
		put("tryLock", "unlock");
		put("writeLockInterruptibly", "writeUnlock");
		put("readLockInterruptibly", "readUnlock");
		put("lockInterruptibly", "unlock");
	}};

	
	

	
	public LockAnalyzer(WalaAnalyzer walaAnalyzer, CGNodeList cgNodeList) {
		this.walaAnalyzer = walaAnalyzer;
		this.cg = this.walaAnalyzer.getCallGraph();
		this.outputDir = this.walaAnalyzer.getTargetDirPath();
		this.cgNodeList = cgNodeList;
	}
	
	/** ONLY used for independently call 'findLocksForCGNode(CGNode f)' */
	public LockAnalyzer(WalaAnalyzer walaAnalyzer) {
		this(walaAnalyzer, null);
	}
	
	// Please call doWork() manually
	public void doWork() {
		System.out.println("\nJX - INFO - LockAnalyzer: doWork...");
		if (this.cgNodeList == null) {
			System.out.println("\nJX - ERROR - LockAnalyzer: doWork - " + "this.cgNodeList == null");
			return;
		}
		
		findLocksForAllCGNodes();      //JX - can be commented    
		printResultStatus();
		analyzeAllLocks();
	}
	
	
	public ArrayList<CGNodeInfo> getLockCGNodes() { 
		return this.lockCGNodes;
	}
	
	
	public int getNLocks() {
		return this.nLocks;
	}
	
	
	public int getNLockingCGNodes() {
		return this.nLockingCGNodes;
	}
	
	
	public int getNLockGroups() {
		return this.nLockGroups;
	}
	
	
	//
	public Path getOutputDir(Path outputDir) {
		return this.outputDir;
	}
	
	public void setOutputDir(String outputDirStr) {
		setOutputDir( Paths.get(outputDirStr) );
	}
	public void setOutputDir(Path outputDir) {
		this.outputDir = outputDir;
	}
	
	
	
	//===============================================================================================
	//++++++++++++++++++++++++++++++++++++++++ Find Locks +++++++++++++++++++++++++++++++++++++++++++
	//===============================================================================================
	  
	/**
	 * Find Functions with locks
	 * Note: Only focus on "Application" functions
	 */
	private void findLocksForAllCGNodes() {
		System.out.println("JX - INFO - LockAnalyzer: findLocksForAllCGNodes");
    
		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext(); ) {
			CGNode cgNode = it.next();
			if ( !walaAnalyzer.isInPackageScope(cgNode) ) continue;
	      
			int id = cgNode.getGraphNodeId();    
			String short_funcname = cgNode.getMethod().getName().toString();
			if (locktypes.contains(short_funcname) || unlocktypes.contains(short_funcname)) //filter lock/unlock functions
				continue;
			List<LockInfo> locks = findLocksForCGNode(cgNode);
			if (locks.size() > 0) {
				boolean verified = true;               //filter those functions cannot be figured out, ie, including "LockInfo.end_bb == -1", eg, readLock - NO readUnlock
				for (Iterator<LockInfo> it_lock = locks.iterator(); it_lock.hasNext(); ) {
					if (it_lock.next().end_bb == -1) {
						System.err.println("Filtered function: " + cgNode.getMethod().getSignature());
						nFiltered++;
						verified = false;
						break;
					}
				}
				if (!verified)
					continue;
				cgNodeList.forceGet(id).setLocks(locks);
			}
		}//for
	
		// Get nLocks & nLockingCGNodes
		for (CGNodeInfo cgNodeInfo: cgNodeList.values() ) {
	    	if ( !cgNodeInfo.hasLocks() ) continue;
	    	lockCGNodes.add( cgNodeInfo );
	    	nLocks += cgNodeInfo.getLocks().size();
	    	nLockingCGNodes ++;
		}
	}
	  
	  
	/**
	 * Not yet filter "synchronized (xx) {}" that located in "catch{}" or "finally{}"
	 */
	public List<LockInfo> findLocksForCGNode(CGNode f) {
	    
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
	    	//if (im.isStatic()) System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! - im.isStatic() - " + cgNode_name + "   class - " + im.getDeclaringClass().toString());
	    	LockInfo lock = new LockInfo();
	    	lock.cgNode = f;
	    	lock.cgNode_id = id;
	    	lock.cgNode_name = func_name;
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
	    				lock.cgNode = f;
	    				lock.cgNode_id = id;
	    				lock.cgNode_name = func_name;            
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
			            lock.cgNode = f;
			            lock.cgNode_id = id;
			            lock.cgNode_name = func_name;
			            lock.lock_type = short_funcname;
			            lock.lock_class = ((SSAInvokeInstruction) ssa).getDeclaredTarget().getDeclaringClass().toString();   // not precise
			            lock.lock_name = "???";
			            
			            //System.err.println("funcname: " + cgNode_name);
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
	   
	
	  
	public void printResultStatus() {
	    // Print the status
		System.out.println("JX - INFO - LockAnalyzer: The status of results");
		int nPackageFuncs = walaAnalyzer.getNPackageFuncs();
		int nTotalFuncs = walaAnalyzer.getNTotalFuncs();
		int nApplicationFuncs = walaAnalyzer.getNApplicationFuncs();
		int nPremordialFuncs = walaAnalyzer.getNPremordialFuncs();
		int nOtherFuncs = walaAnalyzer.getNOtherFuncs();
		
	    int N_LOCKS = 20;
	    int[] count = new int[N_LOCKS];
	    count[0] = nPackageFuncs - nLockingCGNodes;
	    
	    for (CGNodeInfo cgNodeInfo: lockCGNodes ) {
	    	List<LockInfo> locks = cgNodeInfo.getLocks();
		    int size = locks.size();
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
	    System.out.println("#functions with locks: " + nLockingCGNodes + "(" + nLocks + "locks)" + " (excluding " + nFiltered + " filtered functions that have locks)");
	    // distribution of #locks
	    System.out.println("//distribution of #locks");
	    for (int i = 0; i < N_LOCKS; i++)
	        System.out.print("#" + i + ":" + count[i] + ", ");
	    System.out.println();
	    // distribution of lock types
	    Map<String, Integer> numOfLockTypes = new HashMap<String, Integer>();
	    
	    for (CGNodeInfo cgNodeInfo: lockCGNodes ) {
	    	List<LockInfo> locks = cgNodeInfo.getLocks();
	    	for (LockInfo lock: locks) {
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
	  

	
	
	  int print_num = 0;
	  /**
	   * Note: track SSAs Back To Get Precise Lock For SyncCS
	   * @param ssa - the monitorenter SSA
	   */
	  public void getPreciseLockForSyncCS(CGNode function, SSAInstruction ssa, LockInfo lock) {
	    
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
	      int index = IRUtil.getSSAIndex(function, ssa); 
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
	      //System.err.println("ERROR - " + "funcname: " + cgNode_name);        // TODO - integrate into "dfsTrackSSAs"
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
	    System.err.println("funcname: " + cgNode_name);
	    System.err.println("ssa: " + ssa);
	    System.err.println("succssa: " + ssas[index]);
	    System.err.println("lock_type: " + lock.lock_type);    
	    System.err.println("lock_class: " + lock.lock_class);
	    System.err.println("lock_name: " + lock.lock_name);
	    }
	    */
	  }

	  
	  public void dfsTrackSSAs(CGNode function, SSAInstruction ssa, SSAInstruction predssa, LockInfo lock) {
	       
	    IMethod im = function.getMethod();
	    IR ir = function.getIR();
	    SSAInstruction[] ssas = ir.getInstructions();
	    
	    if (ssa instanceof SSALoadMetadataInstruction) {  // 3.1 synchronized (ClassName.class from LoadMetadata)   #only two org.apache.hadoop.conf.Configuration.<init>s are not static methods 
	      SSALoadMetadataInstruction loadssa = (SSALoadMetadataInstruction) ssa;
	      //System.err.println("synchronized (ClassName.class) - " + ssas[index] + " in a static " + im.isStatic() + " method?");
	      lock.lock_class = loadssa.getToken().toString();    //previous usage should be wrong: im.getDeclaringClass().toString();
	      lock.lock_name += "CLASS"; //((SSALoadMetadataInstruction)ssas[index]).getType().toString();
	      //System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! - synchronized (ClassName.class) - " + cgNode_name + "   class - " + lock.lock_class);
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
	          lock.lock_name += "-ARGU-" + ir.getLocalNames(IRUtil.getSSAIndex(function, ssa), usevn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
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
	        lock.lock_name += "-ARGU-" + ir.getLocalNames(IRUtil.getSSAIndex(function, ssa), usevn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
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
	        lock.lock_name += "-ARGU-" + ir.getLocalNames(IRUtil.getSSAIndex(function, ssa), usevn)[0];  //should be found for this particular situation  #only for this kind of synchronized_lock now
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
	      lock.lock_name += "-New-" + "LOCALVAR-" + ir.getLocalNames(IRUtil.getSSAIndex(function, predssa), newssa.getDef())[0] + " in " + im.getSignature();
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
	  List<Integer> current_stack = new ArrayList<Integer>();
	  Set<Integer> traversed_nodes = new HashSet<Integer>(); 
	  
	  public void dfsToGetBasicBlocksForLock(int layer, ISSABasicBlock bb, SSACFG cfg, LockInfo lock) {
	  
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

	  
	  public void printFunctionsWithLocks() {
	    //print all locks for those functions with locks
	    for (Iterator<Integer> it = cgNodeList.keySet().iterator(); it.hasNext(); ) {
	      int id = it.next();
	      if ( !cgNodeList.get(id).hasLocks() ) continue;
	      List<LockInfo> locks = cgNodeList.get(id).getLocks();
	      System.out.println(cg.getNode(id).getMethod().getSignature()); 
	      printLocks(locks);
	    }
	  }
	  
	  public void printLocks(List<LockInfo> locks) {
	    // Print the function's Locks
	    System.out.print("#locks-" + locks.size() + " - ");
	    for (Iterator<LockInfo> it = locks.iterator(); it.hasNext(); ) {
	      System.out.print (it.next() + ", ");
	    }
	    System.out.println();
	  }
	  
	  
	
	  
	public void analyzeAllLocks() {
	    System.out.println("JX - INFO - LockAnalyzer: analyzeAllLocks");

	    //for test
	    Set<String> set_of_locks = new TreeSet<String>();    // for test, TreeSet is ordered by Java
	    
	    for (CGNodeInfo cgNodeInfo: lockCGNodes) {
	    	for (LockInfo lock: cgNodeInfo.getLocks()) {
	    		lock.lock_identity = "ClassName- " + lock.lock_class + " LockName- " + lock.lock_name;
	    		set_of_locks.add( lock.lock_identity );
	    	}
	    }
	    nLockGroups = set_of_locks.size();
	    
	    //Get results
	    System.out.println("#Total Locks = " + nLocks);
	    System.out.println("#Groups of total Locks (ie, real number): " + nLockGroups);
	    //for (String str: set_of_locks)
	    //  System.err.println( str );
	    System.out.println();
	}
		 
	 
	  
}
