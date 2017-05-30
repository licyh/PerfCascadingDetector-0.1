package dm;

import java.util.*;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;

import dm.transformers.Transformers;
import dm.Util.ClassUtil;
import dm.Util.MethodUtil;
import com.APIInfo;
import com.API;
import com.RPCInfo;
import com.benchmark.BugConfig;
import com.CalleeInfo;


public class HDFSDM {
  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("JX - INFO - started by Javassit DM. Agent arguments: " + agentArgs);
    inst.addTransformer(new HDFSTransformer(agentArgs));

  }
}



class HDFSTransformer extends Transformer {
	
	BugConfig bugConfig = new BugConfig("resource/bugconfig", true);
	
	ClassUtil classUtil;
	RPCInfo rpcInfo = new RPCInfo();	
	CalleeInfo calleeInfo = new CalleeInfo();
  
	//added by JX
	Transformers transformers = new Transformers();

	
  
	public HDFSTransformer(String str) {
	    super(str);
	    //CtClass.debugDump = "/home/hadoop/hadoop/dump";
	    option.setDelimiter("%");
	    option.addOption("s", "searchScope", "search path");
	    option.parse();
	
	    //-s parameter
	    classUtil = new ClassUtil();
	    classUtil.setSearchScope(option.getValue("s"));
	
	    //rpc
	    rpcInfo.setInfoFilePath("resource/hd_rpc.txt", 1);
	    rpcInfo.readFile();
	}


  	public void transformClass(CtClass cl) {
  		String className = cl.getName().toString();
	  
  	    if ( cl.getName().contains("xerces")
  	    		|| cl.getName().contains("xml") 
  	    		|| cl.getName().contains("xalan")
  	    		) {
  	        return; //these classes are about xml parser.
  	    }
	    
  		// FILTERS
		if ( className.startsWith("org.apache.hadoop.xxx.")
				// +
  				//&& !className.startsWith("org.apache.hadoop.io.IOUtils")   //for the real bug in mr-4576
  				//&& !className.startsWith("org.apache.hadoop.fs.")
	           ) {
	          return;
  		}
  		
		
  		// LIMITS
  		// instrument for happens-before graph
		if ( className.startsWith("org.apache.hadoop.hdfs.")
	            || className.startsWith("org.apache.hadoop.ipc.")
	            || className.startsWith("org.apache.hadoop.util.RunJar")
	            || className.startsWith("org.apache.hadoop.util.Shell")   //?The CodeAttribute of some methods in util is empty. ignore them.
	           ) {
			transformClassForHappensBefore( cl );
  		}
  		
	    // instrument for target codes
		System.out.println("JX - DEBUG - DM - 10");
	    transformers.transformClassForCodeSnippets( cl );

	    // instrument for all loops
		if ( className.startsWith("org.apache.hadoop.hdfs.")
				|| className.startsWith("org.apache.hadoop.fs.")
				//|| className.startsWith("org.apache.hadoop.io.")
				//|| className.startsWith("org.apache.hadoop.util.")
  				) {
			try {
				System.out.println("JX - DEBUG - DM - 11");
				transformers.transformClassForLoops( cl );
			} catch (CannotCompileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		    
		    if (className.contains("ipc.Server"))
		    System.out.println("JX - DEBUG - method: " + className + "." + methodName);
	        
	        //System.out.println("JX - DEBUG - DM - 0");

		    /*if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
		      return; //bypass all constructors.
		    }*/
		
		    classUtil.setClassPool(method);
		    classUtil.updateClassPool();
		    //System.out.println("Debug... Method: " + method.getName());
		    //System.out.println("Debug... class: " + cl.getName());
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
		    
		    
		    /** 
		     * ThdEnter & ThdExit
		     * 1. main function
		     * 2. child thread function
		     */
		    //System.out.println("JX - DEBUG - DM - 1");
		    if (methodName.equals("main")
		    		&& Modifier.toString(method.getModifiers()).contains("static")
		    		) {
		    	//System.out.println("JX - DEBUG - DM - 1.1");
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 0);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 0);
		    }
		    else if (methodName.equals("run") 
		    		&& (classUtil.isThreadClass(className) || classUtil.isRunnableClass(className))  //&& !className.contains("EventProcessor")
		            ) {
		    	//System.out.println("JX - DEBUG - DM - 1.2");
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
		    }
		    else if (methodName.equals("call")
		    		&& classUtil.isTargetClass(className, "java.util.concurrent.Callable")
		    		&& method.getSignature().endsWith("Ljava/lang/Object;")==false
		    		) {
		    	//System.out.println("JX - DEBUG - DM - 1.3");
			    methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
			    methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
			}
		    
		    /**
		     * EventProcEnter & EventProcExit
		     * EventCreate
		     *like methodName.equals("run") && className.contains("EventProcessor") || methodName.equals("handle") 
		     */
		    // TODO - jx: for now it seems non-exisent  
		   
		
		    /**
		     * MsgProcEnter & MsgProcExit - for RPC function
		     * insert RPCEnter & RPCExit log
		     */
		    else if (rpcInfo.isRPCMethod(className, methodName)
		    		&& javassist.Modifier.isPublic(method.getModifiers()) ) {
		    	//System.out.println("JX - DEBUG - DM - 1.4");
		    	int rpc_flag = 3; //flag=3: mrv1 rpc. flag=2: mrv2 rpc. 
		    	methodUtil.insertCallInstBefore(logClass, msgProcEnterLog, rpc_flag);
		    	methodUtil.insertCallInstAfter(logClass, msgProcExitLog, rpc_flag);
		    }
		    
		    /**
		     * MsgSending - for rpc calling
		     */
		    //System.out.println("JX - DEBUG - DM - 2");
		    methodUtil.insertRPCCallInst(logClass, msgSendingLog, rpcInfo);
		    //methodUtil.insertRPCInvoke(logClass, msgSendingLog);
		    
		    /**
		     * ThdCreate - for thread creation
		     */
		    //System.out.println("JX - DEBUG - DM - 3");
		    methodUtil.insertCallInst("java.lang.Thread", "start", 0, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "submit", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "submit", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.CompletionService", "submit", 1, logClass, thdCreateLog, classUtil); //for ExecutorCompletionService in ResourceLocalizationService.java L625.
		    methodUtil.insertCallInst("java.util.concurrent.ScheduledThreadPoolExecutor", "schedule", 3, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.lang.Runtime", "addShutdownHook", 1, logClass, thdCreateLog, classUtil);
		
		    /**
		     * ThdJoin - for thread join
		     */
		    //System.out.println("JX - DEBUG - DM - 4");
		    methodUtil.insertCallInst("java.lang.Thread", "join", 0, logClass, thdJoinLog, classUtil);
		
		    /**
		     * ProcessCreate - for process create
		     */
		    //System.out.println("JX - DEBUG - DM - 5");
		    if (methodName.equals("runCommand") && className.endsWith("org.apache.hadoop.util.Shell")) {
		      //JX - this is a bug, I've commented it at its subcall
		    	if (bugConfig.getBugId().equals("ha-4584"))
		    		methodUtil.insertCallInstAt(logClass, processCreateLog, 10, 201);
		    	else if (bugConfig.getBugId().equals("xxx")) {
		    	}
		    }
		   
		
		    /**
		     * lockRequire & lockRelease - for lock accesses
		     */
		    // added for mr-4576
		    //System.out.println("JX - DEBUG - DM - 6");
		    if (className.startsWith("org.apache.hadoop.ipc."))   //jx: coz this has lots of locks useless
		        return; 
		    methodUtil.insertSyncMethod(logClass, lockRequireLog, logClass, lockReleaseLog);
		    //System.out.println("JX - DEBUG - DM - 7");
		    methodUtil.insertMonitorInst(logClass, lockRequireLog, logClass, lockReleaseLog);
		    //System.out.println("JX - DEBUG - DM - 8");
		    methodUtil.insertRWLock(logClass, rWLockCreateLog);
		    //end-Added
		}
	}

}
