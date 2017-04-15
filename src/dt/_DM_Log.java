package dt;

import java.io.*;
import java.util.*;


public class _DM_Log {

  //copy from LogFunc.java
  public enum OPTYPE {

	//
    MsgProcEnter, //msg handler enter
	MsgProcExit, //msg handler exit
	MsgSending, //msg sending (rpc or sendSocket)
	    
    LoopPrint,
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
  
  	public static void log_LoopPrint(String opValue) {
  		String opType = _DM_Log.OPTYPE.LoopPrint.name();
  		_DM_Log.log_Base(opType, opValue);
  	}
}
