package dm;

import java.io.*;
import java.util.*;
import java.security.*;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;

import dm.util.Bytecode.*;
import dm.util.Bytecode.Instruction;
import dm.util.Bytecode.InvokeInst;
import dm.transformers.Transformers;
import dm.util.ClassUtil;
import dm.util.MethodUtil;
import com.APIInfo;
import com.API;
import com.RPCInfo;
import com.benchmark.BugConfig;
import com.CalleeInfo;


public class MapReduceDM {

  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("JX - INFO - started by Javassit DM. Agent arguments: " + agentArgs);
    inst.addTransformer(new MapReduceTransformer(agentArgs));

  }
}

class MapReduceTransformer extends Transformer {
	
	BugConfig bugConfig = new BugConfig("resource/bugconfig", true);
	
	ClassUtil classUtil;
	RPCInfo rpcInfo = new RPCInfo();
  
	//added by JX
	Transformers transformers = new Transformers();
  
  
	public MapReduceTransformer(String str) {
	    super(str);
	    //CtClass.debugDump = "/home/hadoop/hadoop/dump";
	    option.setDelimiter("%");
	    option.addOption("s", "searchScope", "search path");
	    option.parse();
	
	    //-s parameter
	    classUtil = new ClassUtil();
	    classUtil.setSearchScope(option.getValue("s"));
	
	    //rpc
	    rpcInfo.setInfoFilePath("resource/mr_rpc.txt", 2);
	    rpcInfo.setInfoFilePath("resource/mr_rpc_v1.txt", 1);
	    rpcInfo.readFile();
	}


  	//added by JX
  	public void transformClass(CtClass cl) {
  		String className = cl.getName().toString();
	     
  		// FILTERS
		if ( className.startsWith("org.apache.hadoop.xxx.")
				// +
  				//&& !className.startsWith("org.apache.hadoop.io.IOUtils")   //for the real bug in mr-4576
  				//&& !className.startsWith("org.apache.hadoop.fs.")
	           ) {
	          return;
  		}
  		
		// DEBUG
        //if (className.contains("DataStreamer"))	
        //	System.out.println("JX - DEBUG - DM - className=" + className);
		
		
	    // LIMITS
		if ( className.startsWith("org.apache.hadoop.yarn.")
  				|| className.startsWith("org.apache.hadoop.mapred.") 
  				|| className.startsWith("org.apache.hadoop.mapreduce.")
	            || className.startsWith("org.apache.hadoop.ipc.")
	            || className.startsWith("org.apache.hadoop.util.RunJar")
	            || className.startsWith("org.apache.hadoop.util.Shell")
	            //The CodeAttribute of some methods in util is empty. ignore them.
	            //added by JX for mr-4576
	            || className.startsWith("org.apache.hadoop.filecache.")
	           ) {
			if (className.contains("PBClientImpl") ||
			        className.contains("PBServiceImpl") ||
				className.contains("org.apache.hadoop.yarn.event.EventHandler")) {
			      
			    }
			else {
				transformClassForHappensBefore(cl);
			}
  		}
	    
	    // instrument for target codes");
	    transformers.transformClassForCodeSnippets( cl );

	    // instrument for all loops
	    switch ( bugConfig.getBugId() ) {
	    case "mr-4576":
			if ( className.startsWith("org.apache.hadoop.yarn.")
	  				|| className.startsWith("org.apache.hadoop.mapred.") 
	  				|| className.startsWith("org.apache.hadoop.mapreduce.")
	  				|| className.startsWith("org.apache.hadoop.io.IOUtils")   //for the real bug in mr-4576
	  				// +
	  				//&& !className.startsWith("org.apache.hadoop.filecache.")
	  				//&& !className.startsWith("org.apache.hadoop.fs.")
		           ) {
				transformers.transformClassForLoops( cl );
	  		}
			break;
		default:
			if ( className.startsWith("org.apache.hadoop.yarn.")
	  				|| className.startsWith("org.apache.hadoop.mapred.") 
	  				|| className.startsWith("org.apache.hadoop.mapreduce.")
		           ) {
				transformers.transformClassForLoops( cl );
	  		}
	    }
	    
	    // instrument for (large) loops
	    //transformers.transformClassForLargeLoops( cl );
  	}
  
  	
	public void transformClassForHappensBefore(CtClass cl) {
		String className = cl.getName().toString();		
		CtBehavior[] methods = cl.getDeclaredBehaviors(); 	
  
	    for (CtBehavior method : methods) {
	        if ( method.isEmpty() ) continue;
  
			MethodInfo methodInfo = method.getMethodInfo();
		    String methodName = method.getName().toString();
	
		    /*if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
		      return; //bypass all constructors.
		    }*/
		
		    String logClass = "LogClass._DM_Log";
		    String logFuncPre = "log";
		
		    classUtil.setClassPool(method);
		    classUtil.updateClassPool();
		    //System.out.println("Debug... Method: " + method.getName());
		    //System.out.println("Debug... class: " + cl.getName());
		    MethodUtil methodUtil = new MethodUtil();
		    methodUtil.setMethod(method);
		
		    String thdEnterLog        = logFuncPre + "_" + "ThdEnter";
		    String thdExitLog         = logFuncPre + "_" + "ThdExit";
		    String thdCreateLog       = logFuncPre + "_" + "ThdCreate";
		    String thdJoinLog         = logFuncPre + "_" + "ThdJoin";
		    String eventProcEnterLog  = logFuncPre + "_" + "EventProcEnter";
		    String eventProcExitLog   = logFuncPre + "_" + "EventProcExit";
		    String heapReadLog        = logFuncPre + "_" + "HeapRead";
		    String heapWriteLog       = logFuncPre + "_" + "HeapWrite";
		    String msgProcEnterLog    = logFuncPre + "_" + "MsgProcEnter";
		    String msgProcExitLog     = logFuncPre + "_" + "MsgProcExit";
		    String msgSendingLog      = logFuncPre + "_" + "MsgSending";
		    String processCreateLog   = logFuncPre + "_" + "ProcessCreate";
		    String eventCreateLog     = logFuncPre + "_" + "EventCreate";
		    //Added by JX
		    String lockRequireLog     = logFuncPre + "_" + "LockRequire";
		    String lockReleaseLog     = logFuncPre + "_" + "LockRelease";
		    String rWLockCreateLog    = logFuncPre + "_" + "RWLockCreate";
		    //end-Added
		
		    boolean injectFlag = false;
		    
		    
		    // JX - Begin to insert LOG code
		    
		    /* main function:
		     * 1. add ThdEnter
		     * 2. add ThdExit
		     */
		    if (methodName.equals("main") &&
		        Modifier.toString(method.getModifiers()).contains("static")) {
		      //insert ThdEnter & ThdExit log
		      methodUtil.insertCallInstBefore(logClass, thdEnterLog, 0);
		      methodUtil.insertCallInstAfter(logClass, thdExitLog, 0);
		    }
		
		    /* child thread function:
		     * 1. add ThdEnter
		     * 2. add ThdExit
		     */
		    else if (methodName.equals("run") &&
		              (classUtil.isThreadClass(className) || classUtil.isRunnableClass(className))
		              && (!className.contains("EventProcessor"))
		            ) {
		    	//insert ThdEnter & ThdExit log
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
		    } else if (methodName.equals("run") && (className.contains("EventProcessor"))) {
		    	//Commented by JX - this is a bug
		    	//jx: this is for hadoop2-0.23.3 ("mr-4813")
		    	if ( !className.equals("org.apache.hadoop.yarn.server.resourcemanager.ResourceManager$SchedulerEventDispatcher$EventProcessor") ) {
		    		methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 43);
		    		methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 43);
		    	}
		        //end-Commented
		    } else if (methodName.equals("call") &&
		           classUtil.isTargetClass(className, "java.util.concurrent.Callable") &&
		           method.getSignature().endsWith("Ljava/lang/Object;") == false) {
		    	//insert ThdEnter & ThdExit log
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
		    }
		
		    else if (methodName.equals("handle")) {
		    	//jx: eventQueue.put(event)
		    	//jx: this is for hadoop2-0.23.3 ("mr-4813")
		    	if (className.contains("SchedulerEventDispatcher")
		    			|| className.contains("ContainerLauncherImpl")
		    			|| className.contains("TaskCleanerImpl")
		    			) {
		    		methodUtil.insertCallInstBefore(logClass, eventCreateLog, 42);
		    	}
		    	//jx: similar to "run" && "EventProcessor", but this is "handle"
		    	else {
			          injectFlag = true;
			          methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 1);
			          methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 1);    		
		    	}			
		    }
		    
		    
		    /* RPC function */
		    else if (rpcInfo.isRPCMethod(className, methodName) && //is a rpc
		             (rpcInfo.getVersion(className, methodName) == 1 || // version 1
		              (rpcInfo.getVersion(className, methodName) == 2 && method.getSignature().endsWith(")V") == false) 
		              //mainly for refreshServiceAcls method in AdminService.
		             )
		            ) {
		      injectFlag = true;
		      int rpc_version = rpcInfo.getVersion(className, methodName);
		      int rpc_flag = rpc_version == 2 ? 2 : 3; //see note in methodUtil.java. flag=2: mrv2 rpc. flag=3: mrv1 rpc.
		
		      //insert RPCEnter & RPCExit log
		      methodUtil.insertCallInstBefore(logClass, msgProcEnterLog, rpc_flag);
		      methodUtil.insertCallInstAfter(logClass, msgProcExitLog, rpc_flag);
		
		    }
		
		    if (injectFlag == false && calleeInfo.isCallee(className, methodName, method.getSignature())) {
		    //  if (injectFlag == false && methodName.contains("init") == false && methodInfo.isConstructor() == false &&  methodInfo.isStaticInitializer() == false) {
		      injectFlag = true;
		
		    }
		
		    /* for thread creation */
		    methodUtil.insertCallInst("java.lang.Thread", "start", 0, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "submit", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "submit", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.CompletionService", "submit", 1, logClass, thdCreateLog, classUtil); //for ExecutorCompletionService in ResourceLocalizationService.java L625.
		    methodUtil.insertCallInst("java.util.concurrent.ScheduledThreadPoolExecutor", "schedule", 3, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.lang.Runtime", "addShutdownHook", 1, logClass, thdCreateLog, classUtil);
		
		    /* for thread join */
		    methodUtil.insertCallInst("java.lang.Thread", "join", 0, logClass, thdJoinLog, classUtil);
		
		    /* for rpc calling */
		    methodUtil.insertRPCCallInst(logClass, msgSendingLog, rpcInfo);
		    methodUtil.insertRPCInvoke(logClass, msgSendingLog);
		
		    
		    /* for process create */
		    if (methodName.equals("runCommand") && className.endsWith("org.apache.hadoop.util.Shell")) {
		    	//JX - this is a bug, I've commented it at its subcall
		    	if (bugConfig.getBugId().equals("mr-4813"))
		    		methodUtil.insertCallInstAt(logClass, processCreateLog, 10, 149);
		    	else if (bugConfig.getBugId().equals("mr-4576"))
		    		methodUtil.insertCallInstAt(logClass, processCreateLog, 10, 201);
		    	else if (bugConfig.getBugId().equals("mr-4088"))
		    		methodUtil.insertCallInstAt(logClass, processCreateLog, 10, 201);
		    }
		   
		
		    /* lock */   //Added by JX
		    // added for MR/HDFS   //jx: coz this has lots of locks useless
		    if (className.startsWith("org.apache.hadoop.ipc.")) {  
		    	methodUtil.insertSyncMethod(logClass, lockRequireLog, logClass, lockReleaseLog);
		    	methodUtil.insertMonitorInst(logClass, lockRequireLog, logClass, lockReleaseLog);
		    	methodUtil.insertRWLock(logClass, rWLockCreateLog);
		    }
	    }
	}

}
