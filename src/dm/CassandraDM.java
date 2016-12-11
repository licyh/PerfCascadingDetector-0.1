package dm;

import java.io.*;
import java.util.*;
import java.security.*;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;

import dm.Util.Bytecode.*;
import dm.Util.Bytecode.Instruction;
import dm.Util.Bytecode.InvokeInst;
import dm.Util.ClassUtil;
import dm.Util.MethodUtil;
import com.APIInfo;
import com.API;
import com.RPCInfo;
import com.CalleeInfo;

/**
 * Created by Guangpu on 4/19/2016.
 */
public class CassandraDM {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent arguments: " + agentArgs);
        inst.addTransformer(new CassandraTransformer(agentArgs));
    }
}


class CassandraTransformer extends Transformer {
    ClassUtil classUtil;
    APIInfo apiInfo = new APIInfo();
    ArrayList<API> apiRead = new ArrayList<API>();
    ArrayList<API> apiWrite = new ArrayList<API>();
    //ArrayList<String> rpcRequest = new ArrayList<String>();
    //RPCInfo rpcInfo = new RPCInfo();
    CalleeInfo calleeInfo = new CalleeInfo();
    public CassandraTransformer(String str) {
        super(str);
	CtClass.debugDump = "/home/hadoop/hadoop/dump";
        option.setDelimiter("%");
        option.addOption("s", "searchScope", "search path");
        option.parse();

        //-s parameter
        classUtil = new ClassUtil();
	System.out.println("search path = " + option.getValue("s"));
        classUtil.setSearchScope(option.getValue("s"));

        //api file
        apiInfo.setInfoFilePath("resource/api.txt");
        apiInfo.readFile();
        apiRead = apiInfo.allReadAPI();
        apiWrite = apiInfo.allWriteAPI();
        //rpcInfo.readFile();
	calleeInfo.setInfoFilePath("resource/cassandra_callee.txt");
	calleeInfo.readFile();

    }

    public void transformMethod(CtClass cl, CtBehavior method) {
        MethodInfo methodInfo = method.getMethodInfo();
        String methodName = method.getName().toString();
        String className = cl.getName().toString();
    /*if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
      return; //bypass all constructors.
    }*/

        if (cl.getName().contains("xerces") ||
                cl.getName().contains("xml") ||
                cl.getName().contains("xalan")) {
            return; //these classes are about xml parser.
        }

        if (cl.getName().startsWith("java.") ||
                cl.getName().startsWith("sun.")) {
            return; //bypass
        }

	if (className.startsWith("org.apache.cassandra.net")==false
		&& className.startsWith("org.apache.cassandra.service")==false 
	  	&& className.startsWith("org.apache.cassandra.locator")==false
		&& className.startsWith("org.apache.cassandra.gms")==false)
      	    return;
	
	if (className.equals("org.apache.cassandra.net.Message") ||
		className.equals("org.apache.cassandra.net.Header"))
	    return;    
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
        String logClass = "_DM_Log";
        String logFuncPre = "log";
	//System.out.println("******  "+ methodName);
        classUtil.setClassPool(method);
        classUtil.updateClassPool();
        //System.out.println("Debug... Method: " + method.getName());
        //System.out.println("Debug... class: " + cl.getName());
        MethodUtil methodUtil = new MethodUtil();
        methodUtil.setMethod(method);

        String thdEnterLog = logFuncPre + "_" + "ThdEnter";
        String thdExitLog = logFuncPre + "_" + "ThdExit";
        String thdCreateLog = logFuncPre + "_" + "ThdCreate";
        String thdJoinLog = logFuncPre + "_" + "ThdJoin";
        String eventProcEnterLog = logFuncPre + "_" + "EventProcEnter";
        String eventProcExitLog = logFuncPre + "_" + "EventProcExit";
        String heapReadLog = logFuncPre + "_" + "HeapRead";
        String heapWriteLog = logFuncPre + "_" + "HeapWrite";
        String msgProcEnterLog = logFuncPre + "_" + "MsgProcEnter";
        String msgProcExitLog = logFuncPre + "_" + "MsgProcExit";
        String msgSendingLog = logFuncPre + "_" + "MsgSending";

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
	else if (methodName.equals("doVerb")){
   	    System.out.println("---parser--- "+className+"." + methodName);
      	    methodUtil.insertCallInstBefore(logClass, msgProcEnterLog,41);// 41 means 4 an 1 , 4 means CA, 1 means first speciall operation
            methodUtil.insertCallInstAfter(logClass, msgProcExitLog, 41);

            for (API apiI : apiRead) {
                methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapReadLog, classUtil);
            }

      /* heap write */
            for (API apiI : apiWrite) {
                methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapWriteLog, classUtil);
            }

      /* primitive type static/field read/write. */
            methodUtil.insertGetPutInst(logClass, heapReadLog, logClass, heapWriteLog);

	}
        else if (methodName.equals("sendOneWay")){
	    int plen = 0;
	    try {
            plen = method.getParameterTypes().length;
	    if (plen == 2) 
	       methodUtil.insertCallInstBefore(logClass, msgSendingLog, 41);
	    }catch (Exception e){
		e.printStackTrace();
	    }
        }


    //if (injectFlag == false && calleeInfo.isCallee(className, methodName, method.getSignature())) {
    if (injectFlag == false && methodName.contains("init") == false &&
methodInfo.isConstructor() == false &&  methodInfo.isStaticInitializer() == false){ 
      injectFlag = true;
      //System.out.println("+++parser+++"+className+"." + methodName);	
      /* heap read */
      for (API apiI : apiRead) {
        methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapReadLog, classUtil);
      }

      /* heap write */
      for (API apiI : apiWrite) {
        methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapWriteLog, classUtil);
      }

      /* primitive type static/field read/write. */
      methodUtil.insertGetPutInst(logClass, heapReadLog, logClass, heapWriteLog);
      //System.out.println("AAAA Callee cc: " + className + " " + methodName + " " + method.getSignature());
    }

    /* RPC function */
	/*
        else if (rpcInfo.isRPCMethod(className, methodName) && //is a rpc
                (rpcInfo.getVersion(className, methodName) == 1 || // version 1
                        (rpcInfo.getVersion(className, methodName) == 2 && method.getSignature().endsWith(")V") == false)
                        //mainly for refreshServiceAcls method in AdminService.
                )
                ) {
            int rpc_version = rpcInfo.getVersion(className, methodName);
            int rpc_flag = rpc_version == 2 ? 2 : 3; //see note in methodUtil.java. flag=2: mrv2 rpc. flag=3: mrv1 rpc.

            //insert RPCEnter & RPCExit log
            methodUtil.insertCallInstBefore(logClass, msgProcEnterLog, rpc_flag);
            methodUtil.insertCallInstAfter(logClass, msgProcExitLog, rpc_flag);

            for (API apiI : apiRead) {
                methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapReadLog, classUtil);
            }

            for (API apiI : apiWrite) {
                methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapWriteLog, classUtil);
            }

            methodUtil.insertGetPutInst(logClass, heapReadLog, logClass, heapWriteLog);
        }
	*/
    /* for thread creation */

    methodUtil.insertCallInst("java.lang.Thread", "start", 0, logClass, thdCreateLog, classUtil);
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
    
    }


}
