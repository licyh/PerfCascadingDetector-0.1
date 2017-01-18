//package dm.Util.LogClass; //performance overhead... 1min --> 2min

import java.io.*;
import java.util.*;

public class _DM_Log {

  //copy from LogFunc.java
  public enum OPTYPE {
    ThdEnter,   //enter thread
    ThdExit,    //exit thread
    ThdCreate,  //create thread
    ThdJoin,    //join thread
    HeapRead,   //read a heap var
    HeapWrite,  //write a heap var
    MsgProcEnter, //msg handler enter
    MsgProcExit, //msg handler exit
    MsgSending, //msg sending (rpc or sendSocket)
    EventProcEnter, //event handler enter
    EventProcExit, //event handler exit
    EventCreate, //event create
    ProcessCreate, //process create
    LockRequire, //require lock
    LockRelease, //release lock
    //Added by JX
    RWLockCreate,   	//jx - for creating ReentrantReadWriteLock
    TargetCodeBegin,
    TargetCodeEnd,
    LoopBegin,
    //LoopCenter,         //tmp
    //end-Added
  };

  public static void log_Base(String opType, String opValue) {
    /*try {
      BufferedReader reader = new BufferedReader(new FileReader("/mnt/storage/haopeng/workstation/java-ws/DC-Detector/hbase_test_multinodes/logging_flag"));
      String flag = reader.readLine();
      reader.close();
      if (flag.equals("0")) { return; }
    } catch (Exception e) { }*/

    int logCallStackFlag = 1; //0: no call stack; 1: full call stack; 2: the last one call in stack.
    String tid = Long.toString(java.lang.Thread.currentThread().getId());
    String strProc = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
    String pid = Long.toString(Long.parseLong(strProc.split("@")[0]));

    String logDir = System.getProperty("_DM_Log_DIR");
    String logFile = logDir + "/" + pid +"-" + tid;
    try {
      File file = new File(logFile);
      if (!file.exists()) {
        file.createNewFile();
      }
      //FileWriter writer = new FileWriter(file, true);
      BufferedWriter writer = new BufferedWriter( new FileWriter(file, true) );
      writer.write(tid + " " + pid + " " + opType + " " + opValue + " " + "\n");
      
      //DataOutputStream writer = new DataOutputStream(new FileOutputStream(file, true));
      //writer.writeBytes(tid + " " + pid + " " + opType + " " + opValue + " " + "\n");

      if (logCallStackFlag != 0) { //log call stack
        java.lang.StackTraceElement[] ST = java.lang.Thread.currentThread().getStackTrace();
        int i = logCallStackFlag == 1 ? 3 : ST.length-1;
        for (; i < ST.length; i++) {
          writer.write(ST[i].getClassName() + " " + ST[i].getMethodName() + " " + ST[i].getLineNumber() + "\n");
          //writer.writeBytes(ST[i].getClassName() + " " + ST[i].getMethodName() + " " + ST[i].getLineNumber() + "\n");
        }
      }

      writer.flush();
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void log_ThdEnter(String opValue) {
    String opType = _DM_Log.OPTYPE.ThdEnter.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_ThdExit(String opValue) {
    String opType = _DM_Log.OPTYPE.ThdExit.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_ThdCreate(String opValue) {
    String opType = _DM_Log.OPTYPE.ThdCreate.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_ThdJoin(String opValue) {
    String opType = _DM_Log.OPTYPE.ThdJoin.name();
    _DM_Log.log_Base(opType, opValue);
  }

  public static void log_HeapRead(String opValue) {
    String opType = _DM_Log.OPTYPE.HeapRead.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_HeapWrite(String opValue) {
    String opType = _DM_Log.OPTYPE.HeapWrite.name();
    _DM_Log.log_Base(opType, opValue);
  }

  public static void log_MsgProcEnter(String opValue) {
    String opType = _DM_Log.OPTYPE.MsgProcEnter.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_MsgProcExit(String opValue) {
    String opType = _DM_Log.OPTYPE.MsgProcExit.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_MsgSending(String opValue) {
    String opType = _DM_Log.OPTYPE.MsgSending.name();
    _DM_Log.log_Base(opType, opValue);
  }

  public static void log_EventProcEnter(String opValue) {
    String opType = _DM_Log.OPTYPE.EventProcEnter.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_EventProcExit(String opValue) {
    String opType = _DM_Log.OPTYPE.EventProcExit.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_EventCreate(String opValue) {
    String opType = _DM_Log.OPTYPE.EventCreate.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_ProcessCreate(String opValue) {
    String opType = _DM_Log.OPTYPE.ProcessCreate.name();
    _DM_Log.log_Base(opType, opValue);
  }
  
  public static void log_LockRequire(String opValue) {
    String opType = _DM_Log.OPTYPE.LockRequire.name();
    _DM_Log.log_Base(opType, opValue);
  }
  public static void log_LockRelease(String opValue) {
    String opType = _DM_Log.OPTYPE.LockRelease.name();
    _DM_Log.log_Base(opType, opValue);
  }
  
  //Added by JX
  public static void log_RWLockCreate(String opValue) {
	String opType = _DM_Log.OPTYPE.RWLockCreate.name();
	_DM_Log.log_Base(opType, opValue);
  }
  //end-Added
  
  //Added by JX
  public static void log_TargetCodeBegin(String opValue) {
	String opType = _DM_Log.OPTYPE.TargetCodeBegin.name();
	_DM_Log.log_Base(opType, opValue);
  }
  //end-Added
  
  //Added by JX
  public static void log_TargetCodeEnd(String opValue) {
	String opType = _DM_Log.OPTYPE.TargetCodeEnd.name();
	_DM_Log.log_Base(opType, opValue);
  }
  //end-Added
  
  //Added by JX
  public static void log_LoopBegin(String opValue) {
	String opType = _DM_Log.OPTYPE.LoopBegin.name();
	_DM_Log.log_Base(opType, opValue);
  }
  //end-Added
  
  /*
  //Added by JX
  public static void log_LoopCenter(String opValue) {
	String opType = _DM_Log.OPTYPE.LoopCenter.name();
	_DM_Log.log_Base(opType, opValue);
  }
  //end-Added
  */
}
