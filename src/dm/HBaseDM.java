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


public class HBaseDM {

  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("Agent arguments: " + agentArgs);
    inst.addTransformer(new HBaseTransformer(agentArgs));

  }
}

class HBaseTransformer extends Transformer {
	
	BugConfig bugConfig = new BugConfig("resource/bugconfig", true);
	
	ClassUtil classUtil;
	RPCInfo rpcInfo = new RPCInfo();
  
	//added by JX
	Transformers transformers = new Transformers();
	
  
  	public HBaseTransformer(String str) {
  		super(str);
  		option.setDelimiter("%");
  		option.addOption("s", "searchScope", "search path");
  		option.parse();

  		//-s parameter
  		classUtil = new ClassUtil();
  		classUtil.setSearchScope(option.getValue("s"));

  		//rpc
  		//Different version is hard-written. TODO: change it to be controlled by argument.
  		rpcInfo.setInfoFilePath("resource/hbase_rpc_4539.txt", 1);
  		//rpcInfo.setInfoFilePath("resource/hbase_rpc.txt", 1);
  		rpcInfo.readFile();
  	}
  
  

  
	public void transformClass(CtClass cl) {
  		String className = cl.getName().toString();
	    
  		// FILTERS
		if ( className.startsWith("org.apache.hadoop.xxx.")
				|| className.startsWith("org.apache.hadoop.hbase.io")                        //for the real bug in hb-3483
				|| className.startsWith("org.apache.hadoop.hbase.regionserver.StoreFile")
				|| className.startsWith("org.apache.hadoop.hbase.regionserver.wal.HLog")
				|| className.startsWith("org.apache.hadoop.hbase.client.HConnectionManager")
				//|| className.startsWith("org.apache.hadoop.hbase.ipc.")
	           ) {
			return;
  		}
		
  		// LIMITS
  		// instrument for happens-before graph
		if ( className.startsWith("org.apache.hadoop.hbase.")
				|| className.startsWith("org.jruby.")
	           ) {
			transformClassForHappensBefore( cl );
  		}
  		
	    // instrument for target codes
		//System.out.println("JX - DEBUG - DM - 10");
	    transformers.transformClassForCodeSnippets( cl );

	    // instrument for all loops
		if ( className.startsWith("org.apache.hadoop.hbase.")
  				) {
				//System.out.println("JX - DEBUG - DM - 11");
			transformers.transformClassForLoops( cl );
		}
		
	    // instrument for (large) loops
	    //transformers.transformClassForLargeLoops( cl );
	    
  	}
  
  

	public void transformClassForHappensBefore(CtClass cl) {
		String className = cl.getName().toString();		
		CtBehavior[] methods = cl.getDeclaredBehaviors(); 	
    
		for (CtBehavior method: methods) {
			if ( method.isEmpty() ) continue;
        
	        MethodInfo methodInfo = method.getMethodInfo();
	        String methodName = method.getName().toString();
	        /*if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
	        return; //bypass all constructors.
	        }*/
	  
		    if (methodName.equals("closeRegion")) {
		      System.out.println("DebugAAAAA m: " + methodName + " in cc: " + className);
		      System.out.println("DebugAAAA " + method.getMethodInfo().toString());
		      System.out.println("DebugAAAA " + javassist.Modifier.toString(method.getModifiers()));
		    }
		
		    classUtil.setClassPool(method);
		    classUtil.updateClassPool();
		    MethodUtil methodUtil = new MethodUtil();
		    methodUtil.setMethod(method);
		
		    String logClass = "LogClass._DM_Log";
		    String logFuncPre = "log";
		    String processCreateLog   = logFuncPre + "_" + "ProcessCreate";
		    String thdCreateLog       = logFuncPre + "_" + "ThdCreate";
		    String thdEnterLog        = logFuncPre + "_" + "ThdEnter";
		    String thdExitLog         = logFuncPre + "_" + "ThdExit";
		    String thdJoinLog         = logFuncPre + "_" + "ThdJoin";
		    String eventCreateLog     = logFuncPre + "_" + "EventCreate";
		    String eventProcEnterLog  = logFuncPre + "_" + "EventProcEnter";
		    String eventProcExitLog   = logFuncPre + "_" + "EventProcExit";
		    String msgProcEnterLog    = logFuncPre + "_" + "MsgProcEnter";
		    String msgProcExitLog     = logFuncPre + "_" + "MsgProcExit";
		    String msgSendingLog      = logFuncPre + "_" + "MsgSending";
		    //Added by JX
		    String lockRequireLog     = logFuncPre + "_" + "LockRequire";
		    String lockReleaseLog     = logFuncPre + "_" + "LockRelease";
		    String rWLockCreateLog    = logFuncPre + "_" + "RWLockCreate";
		    //end-Added
		    String heapReadLog        = logFuncPre + "_" + "HeapRead";
		    String heapWriteLog       = logFuncPre + "_" + "HeapWrite";
		    
		    boolean injectFlag = false;
		    
		    
		    /* main function:
		     * 1. add ThdEnter
		     * 2. add ThdExit
		     */
		    if (methodName.equals("main") 
		    		&& Modifier.toString(method.getModifiers()).contains("static")
		    		) {
		    	//insert ThdEnter & ThdExit log
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 0);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 0);
		    }
		    else if (methodName.equals("run") 
		    		&& (classUtil.isThreadClass(className) || classUtil.isRunnableClass(className))
		              //&& classUtil.isTargetClass(className, "org.apache.hadoop.hbase.executor.EventHandler") == false //event handler
		              //classUtil.isThreadClass(className)
		            ) {
		    	//insert ThdEnter & ThdExit log
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
		    }
		    else if (methodName.equals("call") 
		    		&& classUtil.isCallableClass(className)
		    		&& method.getSignature().endsWith("Ljava/lang/Object;") == false) {
		    	//insert ThdEnter & ThdExit log
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
		    }
		
		
		    // only hbase - watcher:
		    else if (methodName.equals("process") &&
		             className.equals("org.apache.hadoop.hbase.zookeeper.ZooKeeperWatcher")) {
		      //insert ThdEnter & ThdExit log
		      methodUtil.insertCallInstBefore(logClass, msgProcEnterLog, 7);
		      methodUtil.insertCallInstAfter(logClass, msgProcExitLog, 7);
		    }
		
		    //event process
		    else if (methodName.equals("process") &&
		             classUtil.isTargetClass(className, "org.apache.hadoop.hbase.executor.EventHandler")) {
		    	injectFlag = true;
		    	//insert eventEnter & eventExit log
		    	methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 5);
		    	methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 5);
		    }
		
		    //TODO: rpc process
		    else if (rpcInfo.isRPCMethod(className, methodName) &&
		             javassist.Modifier.isPublic(method.getModifiers())) {
		    	injectFlag = true;
		    	//insert RPCEnter & RPCExit log
		    	methodUtil.insertCallInstBefore(logClass, msgProcEnterLog, 6);
		    	methodUtil.insertCallInstAfter(logClass, msgProcExitLog, 6);
		    }
		
		 
		    //Thd create:
		    methodUtil.insertCallInst("java.lang.Thread", "start", 0, logClass, thdCreateLog, classUtil);
		    //duplicate with ExecutorService!
		    //methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "execute", 1, logClass, thdCreateLog, classUtil);
		    //methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "submit", 1, logClass, thdCreateLog, classUtil);
		    if (methodName.equals("processBatchCallback")) {
		    	System.out.println("DebugBBBB1 m: " + methodName + " in cc: " + className);
		    }
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "submit", 1, logClass, thdCreateLog, classUtil);
		    //methodUtil.insertCallInst("java.util.concurrent.CompletionService", "submit", 1, logClass, thdCreateLog, classUtil); //for ExecutorCompletionService in ResourceLocalizationService.java L625.
		    methodUtil.insertCallInst("java.util.concurrent.ScheduledThreadPoolExecutor", "schedule", 3, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.lang.Runtime", "addShutdownHook", 1, logClass, thdCreateLog, classUtil);
		
		    //TODO: Event create:
		    methodUtil.insertCallInst("org.apache.hadoop.hbase.executor.ExecutorService", "submit", 1, logClass, eventCreateLog, classUtil);
		    
		    //TODO: RPC create:
		    methodUtil.insertRPCCallInst(logClass, msgSendingLog, rpcInfo);
		
		
		    //only hbase - watched-event; only write can trigger event.
		    methodUtil.insertCallInst("org.apache.zookeeper.ZooKeeper", "create", 4, logClass, msgSendingLog, classUtil);
		    methodUtil.insertCallInst("org.apache.zookeeper.ZooKeeper", "setData", 3, logClass, msgSendingLog, classUtil);
		    methodUtil.insertCallInst("org.apache.zookeeper.ZooKeeper", "delete", 4, logClass, msgSendingLog, classUtil);
		
		    
		    /**
		     * lockRequire & lockRelease - for lock accesses
		     */
		    // added for mr-4576 & ha-4584 & hd-5153
		    if ( !className.startsWith("org.apache.hadoop.hbase.ipc.") ) {   //jx: coz this has lots of locks useless 
                //System.out.println("JX - DEBUG - DM - 6");      
		    	methodUtil.insertSyncMethod(logClass, lockRequireLog, logClass, lockReleaseLog);
		    	//System.out.println("JX - DEBUG - DM - 7");
		    	methodUtil.insertMonitorInst(logClass, lockRequireLog, logClass, lockReleaseLog);
		    	//System.out.println("JX - DEBUG - DM - 8");
		    	methodUtil.insertRWLock(logClass, rWLockCreateLog);
            }
		    //end-Added
		}
  	}
	
	
}


