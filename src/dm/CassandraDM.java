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

/**
 * Created by Guangpu on 4/19/2016.
 */
public class CassandraDM {
	
    public static void premain(String agentArgs, Instrumentation inst) {
    	System.out.println("JX - INFO - started by Javassit DM. Agent arguments: " + agentArgs);
        inst.addTransformer(new CassandraTransformer(agentArgs));
    }
    
}


class CassandraTransformer extends Transformer {
	
	BugConfig bugConfig = new BugConfig("resource/bugconfig", true);
    ClassUtil classUtil;
	//added by JX
	Transformers transformers = new Transformers();
    
    
    public CassandraTransformer(String str) {
        super(str);
	    //CtClass.debugDump = "/home/hadoop/hadoop/dump";
	    option.setDelimiter("%");
	    option.addOption("s", "searchScope", "search path");
	    option.parse();
	
	    //-s parameter
	    classUtil = new ClassUtil();
	    classUtil.setSearchScope(option.getValue("s"));
    }

    
    
  	public void transformClass(CtClass cl) {
  		String className = cl.getName().toString();
	    
  		// FILTERS
                //no these two in v1.2.0
		if ( className.equals("org.apache.cassandra.net.Message")
				|| className.equals("org.apache.cassandra.net.Header")
				) {
			return;
		}
  		
		// DEBUG
        //if (className.contains("DataStreamer"))	
        //	System.out.println("JX - DEBUG - DM - className=" + className);
	
  		// LIMITS
  		// instrument for happens-before graph
		if ( className.startsWith("org.apache.cassandra.streaming.")
				|| className.startsWith("org.apache.cassandra.db.")
				|| className.startsWith("org.apache.cassandra.service.")
				|| className.startsWith("org.apache.cassandra.net.")
			  	|| className.startsWith("org.apache.cassandra.locator.")
				|| className.startsWith("org.apache.cassandra.gms.")
				|| className.startsWith("org.apache.cassandra.utils.")   //added newly
	           ) {
			transformClassForHappensBefore( cl );
  		}
  		
	    // instrument for target codes
		//System.out.println("JX - DEBUG - DM - 10");
	    transformers.transformClassForCodeSnippets( cl );

	    // instrument for all loops
		if ( className.startsWith("org.apache.cassandra.db.")
			  	|| className.startsWith("org.apache.cassandra.streaming.")
				|| className.startsWith("org.apache.cassandra.service.")
				|| className.startsWith("org.apache.cassandra.net.")
			  	|| className.startsWith("org.apache.cassandra.locator.")
				|| className.startsWith("org.apache.cassandra.gms.")
  				) {
				//System.out.println("JX - DEBUG - DM - 11");
                        if ( className.startsWith("org.apache.cassandra.db.filter.")
                             || className.startsWith("org.apache.cassandra.db.marshal.")
//|| className.startsWith("org.apache.cassandra.db.columniterator.") 
//|| className.startsWith("org.apache.cassandra.db.compaction.") 
//|| className.startsWith("org.apache.cassandra.db.index.") 
				) {
				// None
                        }
                        else
			transformers.transformClassForLoops( cl );
		}
		
  	}
  	
  	
    
    public void transformClassForHappensBefore(CtClass cl) {
		String className = cl.getName().toString();		
		CtBehavior[] methods = cl.getDeclaredBehaviors(); 	
  
	    for (CtBehavior method : methods) {
	        if ( method.isEmpty() ) continue;
	         	    	
	        MethodInfo methodInfo = method.getMethodInfo();
	        String methodName = method.getName().toString();

			if (method.equals("serialize") ||method.equals("deserialize") )
			    return;
		
			if (method == null) {
				System.out.println("method is null");
		    }
		    else if (method.getMethodInfo() == null) {
		    	System.out.println("m: " + methodName + "getMEthodInfo is null");
		    }
		    else if (method.getMethodInfo().getConstPool() == null) {
		    	System.out.println("m: " + methodName + "getConstpool is null");
		    }
		    else if (method.getMethodInfo().getCodeAttribute() == null) {
		    	System.out.println("m: " + methodName + "getCodeAtt is null");
		    	return;
		    }
		    else if (method.getMethodInfo().getCodeAttribute().iterator() == null) {
		    	System.out.println("m: " + methodName + "iterator is null");
		    }
	    
		    classUtil.setClassPool(method);
	        classUtil.updateClassPool();
	        //System.out.println("Debug... Method: " + method.getName());
	        //System.out.println("Debug... class: " + cl.getName());
	        MethodUtil methodUtil = new MethodUtil();
	        methodUtil.setMethod(method);
	        
	        
			String logClass = "LogClass._DM_Log";
	        String logFuncPre = "log";
	        String thdEnterLog 			= logFuncPre + "_" + "ThdEnter";
	        String thdExitLog 			= logFuncPre + "_" + "ThdExit";
	        String thdCreateLog 		= logFuncPre + "_" + "ThdCreate";
	        String thdJoinLog 			= logFuncPre + "_" + "ThdJoin";
	        String eventProcEnterLog 	= logFuncPre + "_" + "EventProcEnter";
	        String eventProcExitLog 	= logFuncPre + "_" + "EventProcExit";
	        String heapReadLog 			= logFuncPre + "_" + "HeapRead";
	        String heapWriteLog 		= logFuncPre + "_" + "HeapWrite";
	        String msgProcEnterLog 		= logFuncPre + "_" + "MsgProcEnter";
	        String msgProcExitLog 		= logFuncPre + "_" + "MsgProcExit";
	        String msgSendingLog 		= logFuncPre + "_" + "MsgSending";
		    //Added by JX
		    String lockRequireLog     = logFuncPre + "_" + "LockRequire";
		    String lockReleaseLog     = logFuncPre + "_" + "LockRelease";
		    String rWLockCreateLog    = logFuncPre + "_" + "RWLockCreate";
	        
			boolean injectFlag = false;
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
	                ) {
	            //insert ThdEnter & ThdExit log
	            methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
	            methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
	        }
	        //added by JX
		    else if (methodName.equals("call")
		    		&& classUtil.isCallableClass(className)
		    		//&& method.getSignature().endsWith("Ljava/lang/Object;")==false
		    		) {
			    methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
			    methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
			}
	        else if (methodName.equals("doVerb")) {
	        	System.out.println("---parser--- "+className+"." + methodName);
	      	    methodUtil.insertCallInstBefore(logClass, msgProcEnterLog,41);// 41 means 4 an 1 , 4 means CA, 1 means first speciall operation
	            methodUtil.insertCallInstAfter(logClass, msgProcExitLog, 41);
	        }
	        //added by JX, for ca-6744's streaming each file
	        else if (className.equals("org.apache.cassandra.streaming.IncomingStreamReader") && methodName.equals("read")) {
	      	    methodUtil.insertCallInstBefore(logClass, msgProcEnterLog,51);// 41 means 4 an 1 , 4 means CA, 1 means first speciall operation
	            methodUtil.insertCallInstAfter(logClass, msgProcExitLog, 51);
	        }
	        
	        // commented by JX, use the following one instead
	        /*
	        else if (methodName.equals("sendOneWay")) {
			    int plen = 0;
			    try {
		            plen = method.getParameterTypes().length;
			    if (plen == 2) 
			    	methodUtil.insertCallInstBefore(logClass, msgSendingLog, 41);
			    } catch (Exception e){
			    	e.printStackTrace();
			    }
	        }
	        */
	        else if (className.equals("org.apache.cassandra.net.OutboundTcpConnection") && methodName.equals("write")) {   //more specific than above one
			    int plen = 0;
			    try {
		            plen = method.getParameterTypes().length;
			    if (plen == 5) 
			    	methodUtil.insertCallInstBefore(logClass, msgSendingLog, 41);
			    } catch (Exception e){
			    	e.printStackTrace();
			    }
	        }
	        else if (className.equals("org.apache.cassandra.streaming.FileStreamTask") && methodName.equals("stream")) {
	        	methodUtil.insertCallInstBefore(logClass, msgSendingLog, 52);
	        }
	        
	
		    //if (injectFlag == false && calleeInfo.isCallee(className, methodName, method.getSignature())) {
		    if (injectFlag == false && methodName.contains("init") == false &&
		    	methodInfo.isConstructor() == false &&  methodInfo.isStaticInitializer() == false){ 
		    	injectFlag = true;

		    }
	
	  
		    /* for thread creation */
		    methodUtil.insertCallInst("java.lang.Thread", "start", 0, logClass, thdCreateLog, classUtil);
		    //added
		    //methodUtil.insertCallInst("java.util.concurrent.Executor", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ThreadPoolExecutor", "submit", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "execute", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "submit", 1, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.util.Timer", "schedule", 3, logClass, thdCreateLog, classUtil); 
		    methodUtil.insertCallInst("java.util.concurrent.CompletionService", "submit", 1, logClass, thdCreateLog, classUtil); //for ExecutorCompletionService in ResourceLocalizationService.java L625.
		    methodUtil.insertCallInst("java.util.concurrent.ScheduledThreadPoolExecutor", "schedule", 3, logClass, thdCreateLog, classUtil);
		    methodUtil.insertCallInst("java.lang.Runtime", "addShutdownHook", 1, logClass, thdCreateLog, classUtil);
		
		    /* for thread join */
		    methodUtil.insertCallInst("java.lang.Thread", "join", 0, logClass, thdJoinLog, classUtil);
		    
		    /**
		     * added by JX
		     */
		    methodUtil.insertCallInst("java.util.concurrent.Future", "get", 0, logClass, thdJoinLog, classUtil);
		    
		    /**
		     * lockRequire & lockRelease - for lock accesses
		     */
		    // added for ca-xxx
		    if ( !className.startsWith("org.apache.cassandra.net.")          //jx: acutally didn't use, because we didn't check  "lock-related"
		    		&& !className.startsWith("org.apache.cassandra.utils.")  //jx: acutally didn't use, because we didn't check  "lock-related"
		    		) {   //jx: coz this has lots of locks useless 
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
