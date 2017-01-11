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


public class MapReduceDM {

  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("Agent arguments: " + agentArgs);
    inst.addTransformer(new MapReduceTransformer(agentArgs));

  }
}

class MapReduceTransformer extends Transformer {
  ClassUtil classUtil;
  APIInfo apiInfo = new APIInfo();
  ArrayList<API> apiRead = new ArrayList<API>();
  ArrayList<API> apiWrite = new ArrayList<API>();
  //ArrayList<String> rpcRequest = new ArrayList<String>();

  RPCInfo rpcInfo = new RPCInfo();
  CalleeInfo calleeInfo = new CalleeInfo();
  
  //Added by JX
  List<String> classesForInst = new ArrayList<String>();
  List<String> methodsForInst = new ArrayList<String>();
  List<String> linesForInst  = new ArrayList<String>();
  List<String> typesForInst  = new ArrayList<String>();
  List<Integer> flagsForInst = new ArrayList<Integer>();
  String instBegin = "";
  String instEnd = "";
  //end-Added

  public MapReduceTransformer(String str) {
    super(str);
    //CtClass.debugDump = "/home/hadoop/hadoop/dump";
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
    rpcInfo.setInfoFilePath("resource/mr_rpc.txt", 2);
    rpcInfo.setInfoFilePath("resource/mr_rpc_v1.txt", 1);
    rpcInfo.readFile();

    //callee
    calleeInfo.setInfoFilePath("resource/mr_callee.txt");
    calleeInfo.readFile();
    
    //Added by JX    
    try {
    	InputStream ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/targetlocations");
    	BufferedReader bufreader = new BufferedReader( new InputStreamReader(ins) );
    	//BufferedReader bufreader = new BufferedReader( new FileReader("resource/targetlocations") );
		String tmpline;
		while ( (tmpline = bufreader.readLine()) != null ) {
			String[] strs = tmpline.trim().split("\\s+");
			if ( tmpline.trim().length() > 0 ) {
				classesForInst.add( strs[0] );
				methodsForInst.add( strs[1] );
				linesForInst.add( strs[2] );
				typesForInst.add( strs[3] );
				flagsForInst.add(0);
			}
		}
		bufreader.close();
    	ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/targetinstructions");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
		//bufreader = new BufferedReader( new FileReader("resource/targetinstructions") );
		instBegin = bufreader.readLine();
		instEnd = bufreader.readLine();
		bufreader.close();
    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	System.out.println("JX - " + classesForInst.size() + " locations are loaded");
	System.out.println("JX - " + "classesForInst = " + classesForInst);
	System.out.println("JX - " + "methodsForInst = " + methodsForInst);
	System.out.println("JX - " + "linesForInst =  " + linesForInst );
	System.out.println("JX - " + "instructions = " + instBegin + " " + instEnd);
    //end-Added
    
  }

  public boolean speventcreate(String cn){
	if (cn.contains("SchedulerEventDispatcher")) return true;
	if (cn.contains("ContainerLauncherImpl")) return true;
	if (cn.contains("TaskCleanerImpl")) return true;
	return false;
  }

  
  //Added by JX
  public void transformClassForCodeSnippets(CtClass cl, CtBehavior[] methods) {
	  if ( !classesForInst.contains(cl.getName()) ) return;
	  System.out.println("JX - @1");
      for (CtBehavior method : methods) {
          if ( method.isEmpty() ) continue;
          System.out.println("JX - @2");
          // traverse all locations for instrumentation
          for (int i = 0; i < classesForInst.size(); i++) {
    		  if ( classesForInst.get(i).equals(cl.getName())
    				  && methodsForInst.get(i).equals(method.getName()) ) {
    			  System.out.println("JX - @3");
    			  int linenumber = Integer.parseInt( linesForInst.get(i) );
    			  try {
	    			  if ( typesForInst.get(i).equals("TargetCodeBegin") ) {
	    				  System.out.println("JX - @4");
	    				  System.out.println( "JX - " + "want to insert at " + method.insertAt(linenumber, false, instBegin) );
	    				  method.insertAt(linenumber, true, instBegin);
	    				  flagsForInst.set(i, flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "location " + i + " is found. this is the " + flagsForInst.get(i) + " st/nd/rd/th time." );
	    			  }
	    			  else { //this is "TargetCodeEnd"
	    				  System.out.println("JX - @5");
	    				  System.out.println( "JX - " + "want to insert at " + method.insertAt(linenumber, false, instEnd) );
	    				  method.insertAt(linenumber, true, instEnd);
	    				  flagsForInst.set(i, flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "location " + i + " is found. this is the " + flagsForInst.get(i) + " st/nd/rd/th time." );
	    			  }
    			  } catch (Exception e) {
    				  // TODO Auto-generated catch block
    				  e.printStackTrace();
    			  }
    		  }
    	  }
      }//end-outer for
  }
  //end-Added
  
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
    	
    if (className.startsWith("org.apache.hadoop.yarn.") == false
        && className.startsWith("org.apache.hadoop.mapred.") == false
        && className.startsWith("org.apache.hadoop.mapreduce.") == false
        && className.startsWith("org.apache.hadoop.ipc.") == false
        && className.startsWith("org.apache.hadoop.util.RunJar") == false
        && className.startsWith("org.apache.hadoop.util.Shell") == false
 //The CodeAttribute of some methods in util is empty. ignore them.
       ) {
      return;
    }
    if (className.contains("PBClientImpl") ||
        className.contains("PBServiceImpl") ||
	className.contains("org.apache.hadoop.yarn.event.EventHandler")) {
      return;
    }


    /*if (method == null) {
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
    }*/


    String logClass = "_DM_Log";
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
    String lockEnterLog       = logFuncPre + "_" + "LockRequire";
    String lockExitLog        = logFuncPre + "_" + "LockRelease";
    String rwlockCreateLog    = logFuncPre + "_" + "RWLockCreate";
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
    	/*
    	methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 43);
        methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 43);
        */
        //end-Commented
    } else if (methodName.equals("call") &&
           classUtil.isTargetClass(className, "java.util.concurrent.Callable") &&
           method.getSignature().endsWith("Ljava/lang/Object;") == false) {
    	//insert ThdEnter & ThdExit log
    	methodUtil.insertCallInstBefore(logClass, thdEnterLog, 4);
    	methodUtil.insertCallInstAfter(logClass, thdExitLog, 4);
    }

    else if (methodName.equals("handle")) {
	if (!speventcreate(className)){
          injectFlag = true;
      //insert eventEnter & eventExit log
          methodUtil.insertCallInstBefore(logClass, eventProcEnterLog, 1);
          methodUtil.insertCallInstAfter(logClass, eventProcExitLog, 1);
      /*
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
      */
    	}else
	methodUtil.insertCallInstBefore(logClass, eventCreateLog, 42);
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

      /*
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
      */
    }

    if (injectFlag == false && calleeInfo.isCallee(className, methodName, method.getSignature())) {
    //  if (injectFlag == false && methodName.contains("init") == false && methodInfo.isConstructor() == false &&  methodInfo.isStaticInitializer() == false) {
      injectFlag = true;
      /*
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
      //System.out.println("AAAA Callee cc: " + className + " " + methodName + " " + method.getSignature());
      */
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
      methodUtil.insertCallInstAfter(logClass, processCreateLog, 10);
    }

    /* lock */
    //Added by JX
    methodUtil.insertSyncMethod(logClass, lockEnterLog, logClass, lockExitLog);
    methodUtil.insertMonitorInst(logClass, lockEnterLog, logClass, lockExitLog);
    methodUtil.insertRWLock(logClass, rwlockCreateLog);
    //end-Added
    
    
    /*Instruction i = new Instruction();
    i.setMethod(method);
    ConstPool constPool = methodInfo.getConstPool();
    CodeAttribute codeAttr = methodInfo.getCodeAttribute();
    CodeIterator codeIter = codeAttr.iterator();
    try {
      while (codeIter.hasNext()) {
        int cur = codeIter.next();
        i.setPos(cur);
        if (i.isInvokeinterface() == true ||
            i.isInvokevirtual() == true) {
          InvokeInst invokeI = new InvokeInst(i);
          if (invokeI.calledClass().contains("ClientServiceDelegate")) {
            //other classes are IRC or un-wanted classes
            if (invokeI.calledMethod().equals("invoke")) {
              System.out.println("EEEE invoke: " + invokeI.calledMethodType());
              System.out.println("EEEE invoke: " + method.getName().toString() + " in cc: " + method.getDeclaringClass().getName().toString());

            }
          }
        }
      }
    } catch (Exception e) {}*/

    /*else if (methodName.equals("launch")) {
      System.out.println("HP launch function.");
      Instruction i = new Instruction();
      i.setMethod(method);

      ConstPool constPool = methodInfo.getConstPool();
      CodeAttribute codeAttr = methodInfo.getCodeAttribute();
      CodeIterator codeIter = codeAttr.iterator();
      try {
        while (codeIter.hasNext()) {
          int cur = codeIter.next();
          i.setPos(cur);
          if (i.isInvokeinterface() == true) {
            InvokeInst invokeI = new InvokeInst(i);
            if (invokeI.calledMethod().equals("startContainer")) {
              System.out.println(" HP> " + invokeI.calledClass() + " " + invokeI.calledMethod() + " " + invokeI.calledMethodType());
              String type = invokeI.calledMethodType();
              String paraType = type.substring(type.indexOf("(") + 1, type.indexOf(")"));
              System.out.println(" HP > > " + paraType.substring(1, paraType.length()-1));

              paraType = paraType.substring(1, paraType.length()-1);
              String paraClass = paraType.replaceAll("/", ".");
              System.out.println(" HP > > " + paraClass);

              Bytecode code = new Bytecode(constPool);
              int tmpLocal = methodUtil.allocLocal(1);
              code.addAstore(tmpLocal);
              code.addAload(tmpLocal);
              CtClass stream = classUtil.getClassPool().get("java.lang.System");
              code.addGetstatic(stream, "out", "Ljava/io/PrintStream;");

              code.addAload(tmpLocal);
              code.addInvokevirtual("java/lang/Object", "toString", "()Ljava/lang/String;");
              code.addInvokevirtual("java/lang/String", "hashCode", "()I");
              code.addInvokevirtual("java/io/PrintStream", "println", "(I)V");


              try {
                int loc = codeIter.insertExAt(cur, code.get());
                codeIter.insert(code.getExceptionTable(), loc);
                methodInfo.rebuildStackMapIf6(method.getDeclaringClass().getClassPool(),
                                method.getDeclaringClass().getClassFile2());
              } catch (BadBytecode e) {
                e.printStackTrace();
              }
            }
          }
        }
      } catch (Exception e) { e.printStackTrace(); }
    }*/

    /*else if (methodName.equals("startContainer") &&
             className.equals("org.apache.hadoop.yarn.server.nodemanager.containermanager.ContainerManagerImpl")) {
      System.out.println(" HP > method: " + methodName + "; CC: " + className);
      String str = "System.out.println(\"HP AAA>> \" + $1.toString().hashCode());";
      try {
        method.insertBefore(str);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }*/
    //else if ( paras != null && paras.length == 1 && paras[0].getName().toString().equals("org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest")) {
    /*else if ( paras != null && paras.length == 1 && rpcRequest.contains(paras[0].getName().toString())) {
      System.out.println("HP >> RPC function: " + methodName + " in CC: " + className);
      System.out.println("HP >>> RPC function para: " + paras[0].getName().toString());
      //insert RPCEnter log
      String rpcEnterLogFunc = logFuncPre + "_" + LogFunc.OPTYPE.MsgProcEnter;
      methodUtil.insertCallInstBefore(logClass, rpcEnterLogFunc);

      //insert RPCExit log
      String rpcExitLogFunc = logFuncPre + "_" + LogFunc.OPTYPE.MsgProcExit;
      methodUtil.insertCallInstAfter(logClass, rpcExitLogFunc);
    }*/





    /* RPC function:
     * 1. heap read
     * 2. heap write
     */

    /* for all functions:
     * 1. add ThdCreate
     * 2. add ThdJoin
     */





    //
    //methodUtil.insertHeapCallInst(classUtil, 2);
    //methodUtil.print();

    /*if (method.getName().equals("main")) {
      Instruction i = new Instruction();
      i.setMethod(method);

      MethodInfo methodInfo = method.getMethodInfo();
      CodeAttribute codeAttr = methodInfo.getCodeAttribute();
      CodeIterator codeIter = codeAttr.iterator();

      try {
        while (codeIter.hasNext()) {
          int pos = codeIter.next();
          i.setPos(pos);

          System.out.println(i.opcode());

          InstructionPrinter ip = new InstructionPrinter(System.out);
          ConstPool pool = methodInfo.getConstPool();
          System.out.println(ip.instructionString(codeIter, pos, pool));

          if (i.isLoad() == true) {
            LoadInst loadI = new LoadInst(i);
            System.out.println(" > " + loadI.toString());
          }

          if (i.isInvokevirtual() == true) {
            InvokeInst invokeI = new InvokeInst(i);
            System.out.println(" > " + invokeI.calledClass() + " " + invokeI.calledMethod() + " " + invokeI.calledMethodType());
            if (invokeI.isCalledThreadClass(option.getValue("s")) == true) {
               if (invokeI.calledMethod().equals("start")) {
                 System.out.println(" > > Yes");
               }
            }
          }
        }
      } catch (Exception e) { e.printStackTrace(); }
    }*/

  }

}
