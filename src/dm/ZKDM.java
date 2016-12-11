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


public class ZKDM {

  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("Agent arguments: " + agentArgs);
    inst.addTransformer(new ZKTransformer(agentArgs));

  }
}

class ZKTransformer extends Transformer {
  ClassUtil classUtil;
  APIInfo apiInfo = new APIInfo();
  ArrayList<API> apiRead = new ArrayList<API>();
  ArrayList<API> apiWrite = new ArrayList<API>();

  CalleeInfo calleeInfo = new CalleeInfo();

  public ZKTransformer(String str) {
    super(str);
    CtClass.debugDump = "/mnt/storage/haopeng/workstation/java-ws/DC-Detector/dump";
    option.setDelimiter("%");
    option.addOption("s", "searchScope", "search path");
    option.addOption("v", "version", "version id");
    option.parse();

    //-s parameter
    classUtil = new ClassUtil();
    classUtil.setSearchScope(option.getValue("s"));

    //api file
    apiInfo.setInfoFilePath("resource/api.txt");
    apiInfo.readFile();
    apiRead = apiInfo.allReadAPI();
    apiWrite = apiInfo.allWriteAPI();

    //callee
    String version = option.getValue("v");
    if (version.equals("1144") == false && version.equals("1270") == false) {
      System.out.println("Choose version id: 1144, 1270. Exit.");
      System.exit(-1);
    }
    String calleeFile = "resource/zk_callee_" + version + ".txt";
    calleeInfo.setInfoFilePath(calleeFile);
    calleeInfo.readFile();
  }

  public void transformMethod(CtClass cl, CtBehavior method) {
    MethodInfo methodInfo = method.getMethodInfo();
    String methodName = method.getName().toString();
    String className = cl.getName().toString();

    if (cl.getName().contains("xerces") ||
        cl.getName().contains("xml") ||
        cl.getName().contains("xalan")) {
      return; //these classes are about xml parser.
    }

    if (cl.getName().startsWith("java.") ||
        cl.getName().startsWith("sun.")) {
      return; //bypass
    }

    if (className.startsWith("org.apache.zookeeper.") == false
        && className.startsWith("org.apache.jute.") == false) {
      return;
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
    String lockEnterLog       = logFuncPre + "_" + "LockRequire";
    String lockExitLog        = logFuncPre + "_" + "LockRelease";

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

    //request process
    else if (methodName.equals("processRequest")) {
      methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 9);
      methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 9);
    }

            
    //callee
    if (calleeInfo.isCallee(className, methodName, method.getSignature())) {
    /*if (methodName.contains("init") == false && methodInfo.isConstructor() == false &&  methodInfo.isStaticInitializer() == false
        && className.endsWith("QuorumPeer") == false) {*/
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

      /* lock */
      methodUtil.insertSyncMethod(logClass, lockEnterLog, logClass, lockExitLog);
      methodUtil.insertMonitorInst(logClass, lockEnterLog, logClass, lockExitLog);
    }

    
    //thread create
    methodUtil.insertCallInst("java.lang.Thread", "start", 0, logClass, thdCreateLog, classUtil);
    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "execute", 1, logClass, thdCreateLog, classUtil);
    methodUtil.insertCallInst("java.util.concurrent.ExecutorService", "submit", 1, logClass, thdCreateLog, classUtil);
    methodUtil.insertCallInst("java.util.concurrent.CompletionService", "submit", 1, logClass, thdCreateLog, classUtil);
    methodUtil.insertCallInst("java.util.concurrent.ScheduledThreadPoolExecutor", "schedule", 3, logClass, thdCreateLog, classUtil);
    methodUtil.insertCallInst("java.lang.Runtime", "addShutdownHook", 1, logClass, thdCreateLog, classUtil);
    
    //msg send + receive
    if (methodName.equals("serialize") && classUtil.isTargetClass(className, "org.apache.jute.Record")) {
      methodUtil.insertCallInstAfter(logClass, msgSendingLog, 8);
    }
    else if (methodName.equals("deserialize") && classUtil.isTargetClass(className, "org.apache.jute.Record")) {
      methodUtil.insertCallInstAfter(logClass, msgProcEnterLog, 8);
    }
  }
}


