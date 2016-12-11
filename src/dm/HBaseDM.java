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


public class HBaseDM {

  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("Agent arguments: " + agentArgs);
    inst.addTransformer(new HBaseTransformer(agentArgs));

  }
}

class HBaseTransformer extends Transformer {
  ClassUtil classUtil;
  APIInfo apiInfo = new APIInfo();
  ArrayList<API> apiRead = new ArrayList<API>();
  ArrayList<API> apiWrite = new ArrayList<API>();

  RPCInfo rpcInfo = new RPCInfo();
  CalleeInfo calleeInfo = new CalleeInfo();

  public HBaseTransformer(String str) {
    super(str);
    option.setDelimiter("%");
    option.addOption("s", "searchScope", "search path");
    option.parse();

    //-s parameter
    classUtil = new ClassUtil();
    classUtil.setSearchScope(option.getValue("s"));

    //api file
    apiInfo.setInfoFilePath("resource/api.txt");
    apiInfo.readFile();
    apiRead = apiInfo.allReadAPI();
    apiWrite = apiInfo.allWriteAPI();

    //rpc
    //Different version is hard-written. TODO: change it to be controlled by argument.
    rpcInfo.setInfoFilePath("resource/hbase_rpc_4539.txt", 1);
    //rpcInfo.setInfoFilePath("resource/hbase_rpc.txt", 1);
    rpcInfo.readFile();

    //callee
    calleeInfo.setInfoFilePath("resource/hbase_callee_4539.txt");
    //calleeInfo.setInfoFilePath("resource/hbase_callee.txt");
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

    /*if (className.startsWith("org.apache.hadoop.hbase.ipc.")) {
      return;
    }*/

    if (className.startsWith("org.apache.hadoop.hbase.") == false
        && className.startsWith("org.jruby.") == false) {
      return;
    }
    if (className.startsWith("org.apache.hadoop.hbase.io")) { return; }
    if (className.startsWith("org.apache.hadoop.hbase.regionserver.StoreFile")) { return; }
    if (className.startsWith("org.apache.hadoop.hbase.regionserver.wal.HLog")) { return; }
    if (className.startsWith("org.apache.hadoop.hbase.client.HConnectionManager")) { return; }
    if (methodName.equals("closeRegion")) {
      System.out.println("DebugAAAAA m: " + methodName + " in cc: " + className);
      System.out.println("DebugAAAA " + method.getMethodInfo().toString());
      System.out.println("DebugAAAA " + javassist.Modifier.toString(method.getModifiers()));
    }

    String logClass = "_DM_Log";
    String logFuncPre = "log";

    classUtil.setClassPool(method);
    classUtil.updateClassPool();
    MethodUtil methodUtil = new MethodUtil();
    methodUtil.setMethod(method);

    String thdEnterLog        = logFuncPre + "_" + "ThdEnter";
    String thdExitLog         = logFuncPre + "_" + "ThdExit";
    String thdCreateLog       = logFuncPre + "_" + "ThdCreate";
    String thdJoinLog         = logFuncPre + "_" + "ThdJoin";
    String eventProcEnterLog  = logFuncPre + "_" + "EventProcEnter";
    String eventProcExitLog   = logFuncPre + "_" + "EventProcExit";
    String eventCreateLog     = logFuncPre + "_" + "EventCreate";
    String heapReadLog        = logFuncPre + "_" + "HeapRead";
    String heapWriteLog       = logFuncPre + "_" + "HeapWrite";
    String msgProcEnterLog    = logFuncPre + "_" + "MsgProcEnter";
    String msgProcExitLog     = logFuncPre + "_" + "MsgProcExit";
    String msgSendingLog      = logFuncPre + "_" + "MsgSending";

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
    else if (methodName.equals("run") &&
              (classUtil.isThreadClass(className) || classUtil.isRunnableClass(className))
              //&& classUtil.isTargetClass(className, "org.apache.hadoop.hbase.executor.EventHandler") == false //event handler
              //classUtil.isThreadClass(className)
            ) {
      //insert ThdEnter & ThdExit log
      methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
      methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
    }
    else if (methodName.equals("call") &&
           classUtil.isTargetClass(className, "java.util.concurrent.Callable") &&
           method.getSignature().endsWith("Ljava/lang/Object;") == false) {
      //insert ThdEnter & ThdExit log
      methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
      methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
    }


    //watcher:
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

      // heap read
      for (API apiI : apiRead) {
        methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapReadLog, classUtil);
      }

      // heap write
      for (API apiI : apiWrite) {
        methodUtil.insertCallInst(apiI.className(), apiI.methodName(), apiI.paraNumber(), logClass, heapWriteLog, classUtil);
      }

      // primitive type static/field read/write.
      methodUtil.insertGetPutInst(logClass, heapReadLog, logClass, heapWriteLog);
    }

    //TODO: rpc process
    else if (rpcInfo.isRPCMethod(className, methodName) &&
             javassist.Modifier.isPublic(method.getModifiers())) {
      injectFlag = true;

      //insert RPCEnter & RPCExit log
      methodUtil.insertCallInstBefore(logClass, msgProcEnterLog, 6);
      methodUtil.insertCallInstAfter(logClass, msgProcExitLog, 6);

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
    }

    if (injectFlag == false && calleeInfo.isCallee(className, methodName, method.getSignature())) {
    //if (injectFlag == false && methodName.contains("init") == false && methodInfo.isConstructor() == false &&  methodInfo.isStaticInitializer() == false) {
      injectFlag = true;
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


    //watched-event; only write can trigger event.
    methodUtil.insertCallInst("org.apache.zookeeper.ZooKeeper", "create", 4, logClass, msgSendingLog, classUtil);
    methodUtil.insertCallInst("org.apache.zookeeper.ZooKeeper", "setData", 3, logClass, msgSendingLog, classUtil);
    methodUtil.insertCallInst("org.apache.zookeeper.ZooKeeper", "delete", 4, logClass, msgSendingLog, classUtil);


  }
}


