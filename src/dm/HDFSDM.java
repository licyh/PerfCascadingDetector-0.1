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
import com.CalleeInfo;


public class HDFSDM {
  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("JX - INFO - started by Javassit DM. Agent arguments: " + agentArgs);
    inst.addTransformer(new HDFSTransformer(agentArgs));

  }
}



class HDFSTransformer extends Transformer {
	ClassUtil classUtil;
	APIInfo apiInfo = new APIInfo();
	ArrayList<API> apiRead = new ArrayList<API>();
	ArrayList<API> apiWrite = new ArrayList<API>();
	//ArrayList<String> rpcRequest = new ArrayList<String>();

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
	    rpcInfo.setInfoFilePath("resource/hd_rpc.txt", 2);
	    rpcInfo.setInfoFilePath("resource/hd_rpc_v1.txt", 1);
	    rpcInfo.readFile();
	}

	public boolean speventcreate(String cn){
		if (cn.contains("SchedulerEventDispatcher")) return true;
		if (cn.contains("ContainerLauncherImpl")) return true;
		if (cn.contains("TaskCleanerImpl")) return true;
		return false;
	}


  	public void transformClass(CtClass cl) {
  		String className = cl.getName().toString();
	  
  	    if ( cl.getName().contains("xerces")
  	    		|| cl.getName().contains("xml") 
  	    		|| cl.getName().contains("xalan")
  	    		) {
  	        return; //these classes are about xml parser.
  	    }
  	    
  		if ( className.startsWith("java.")
  				|| className.startsWith("sun.")
  			 ) {
  			return; //bypass jdk
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
	    transformers.transformClassForCodeSnippets( cl );
	    
	    // instrument for (large) loops
	    transformers.transformClassForLargeLoops( cl );

	    // instrument for all loops
		if ( className.startsWith("org.apache.hadoop.hdfs.")
  				|| className.startsWith("org.apache.hadoop.io.IOUtils")
  				) {
			try {
				transformers.transformClassForLoops( cl );
			} catch (CannotCompileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	    
  	}
  
  
	public void transformClassForHappensBefore(CtClass cl) {
		String className = cl.getName().toString();
		
		if (className.contains("PBClientImpl") ||
				className.contains("PBServiceImpl") ||
				className.contains("org.apache.hadoop.yarn.event.EventHandler")) {
			return;
		}
		
		CtBehavior[] methods = cl.getDeclaredBehaviors();  
	
	    
	    for (CtBehavior method : methods) {
	        if ( method.isEmpty() )
	        	continue;
		
		    MethodInfo methodInfo = method.getMethodInfo();
		    String methodName = method.getName().toString();
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
		    if (methodName.equals("main")
		    		&& Modifier.toString(method.getModifiers()).contains("static")
		    		) {
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 0);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 0);
		    }
		    else if (methodName.equals("run") 
		    		&& (classUtil.isThreadClass(className) || classUtil.isRunnableClass(className))
		            && !className.contains("EventProcessor")
		            ) {
		    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
		    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
		    }
		    else if (methodName.equals("call")
		    		&& classUtil.isTargetClass(className, "java.util.concurrent.Callable")
		    		&& method.getSignature().endsWith("Ljava/lang/Object;")==false
		    		) {
			    methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
			    methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
			}
		    
		    /**
		     * EventProcEnter & EventProcExit
		     * EventCreate
		     */
		    else if (methodName.equals("run") 
		    		&& className.contains("EventProcessor") 
		    		) {
		    	//Commented by JX - this is a bug
		    	if ( !className.equals("org.apache.hadoop.yarn.server.resourcemanager.ResourceManager$SchedulerEventDispatcher$EventProcessor") ) {
		    		methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 43);
		    		methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 43);
		    	}
		        //end-Commented
		    } 
		    else if (methodName.equals("handle")) {
		    	if (!speventcreate(className)){
		    		injectFlag = true;
		    		methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 1);
		    		methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 1);
		    	} else
		    		methodUtil.insertCallInstBefore(logClass, eventCreateLog, 42);
		    }
		
		    /**
		     * MsgProcEnter & MsgProcExit - for RPC function
		     */
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
		    
		    /**
		     * ThdCreate - for thread creation
		     */
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
		    methodUtil.insertCallInst("java.lang.Thread", "join", 0, logClass, thdJoinLog, classUtil);
		
		    /**
		     * MsgSending - for rpc calling
		     */
		    methodUtil.insertRPCCallInst(logClass, msgSendingLog, rpcInfo);
		    methodUtil.insertRPCInvoke(logClass, msgSendingLog);
		
		    /**
		     * ProcessCreate - for process create
		     */
		    if (methodName.equals("runCommand") && className.endsWith("org.apache.hadoop.util.Shell")) {
		      //JX - this is a bug, I've commented it at its subcall
		      methodUtil.insertCallInstAfter(logClass, processCreateLog, 10);
		    }
		   
		
		    /**
		     * lockRequire & lockRelease - for lock accesses
		     */
		    // added for mr-4576
		    if (className.startsWith("org.apache.hadoop.ipc."))   //jx: coz this has lots of locks useless
		        return; 
		    methodUtil.insertSyncMethod(logClass, lockRequireLog, logClass, lockReleaseLog);
		    methodUtil.insertMonitorInst(logClass, lockRequireLog, logClass, lockReleaseLog);
		    methodUtil.insertRWLock(logClass, rWLockCreateLog);
		    //end-Added
		}
	}

}
