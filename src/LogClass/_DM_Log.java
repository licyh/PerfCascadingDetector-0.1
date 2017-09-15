//package dm.Util.LogClass; //performance overhead... 1min --> 2min
package LogClass;              //jx: new changed

import java.io.*;


public class _DM_Log {
		
	public static void log_ProcessCreate(String opValue) {
		log_Base(LogType.ProcessCreate.name(), opValue);
	}

	
	public static void log_ThdCreate(String opValue) {
	    log_Base(LogType.ThdCreate.name(), opValue);
	}
	public static void log_ThdEnter(String opValue) {
	    log_Base(LogType.ThdEnter.name(), opValue);
	}
	public static void log_ThdExit(String opValue) {
	    log_Base(LogType.ThdExit.name(), opValue);
	}
	public static void log_ThdJoin(String opValue) {
	    log_Base(LogType.ThdJoin.name(), opValue);
	}


	public static void log_MsgSending(String opValue) {
		log_Base(LogType.MsgSending.name(), opValue);
	}
	public static void log_MsgProcEnter(String opValue) {
		log_Base(LogType.MsgProcEnter.name(), opValue);
	}
	public static void log_MsgProcExit(String opValue) {
		log_Base(LogType.MsgProcExit.name(), opValue);
	}


	public static void log_EventCreate(String opValue) {
	    log_Base(LogType.EventCreate.name(), opValue);
	}
	public static void log_EventProcEnter(String opValue) {
	    log_Base(LogType.EventProcEnter.name(), opValue);
	}
	public static void log_EventProcExit(String opValue) {
	    log_Base(LogType.EventProcExit.name(), opValue);
	}
	  

	public static void log_HeapRead(String opValue) {
	    log_Base(LogType.HeapRead.name(), opValue);
	}
	public static void log_HeapWrite(String opValue) {
	    log_Base(LogType.HeapWrite.name(), opValue);
	}
  
	
	public static void log_LockRequire(String opValue) {
	    log_Base(LogType.LockRequire.name(), opValue);
	}
	public static void log_LockRelease(String opValue) {
	    log_Base(LogType.LockRelease.name(), opValue);
	}
  
	
	//added by JX
	public static void log_EventHandlerBegin(String opValue) {
		log_Base(LogType.EventHandlerBegin.name(), opValue);
	}
	public static void log_EventHandlerEnd(String opValue) {
		log_Base(LogType.EventHandlerEnd.name(), opValue);
	}
	
	
	//Added by JX
	public static void log_RWLockCreate(String opValue) {
		log_Base(LogType.RWLockCreate.name(), opValue);
	}
	  
	  
	public static void log_TargetCodeBegin(String opValue) {
		log_Base(LogType.TargetCodeBegin.name(), opValue);
	}
	public static void log_TargetCodeEnd(String opValue) {
		log_Base(LogType.TargetCodeEnd.name(), opValue);
	}
	  
	    
	public static void log_LargeLoopBegin(String opValue) {
		log_Base(LogType.LargeLoopBegin.name(), opValue);
	}
  
  
	/*
	//Added by JX
	public static void log_LoopCenter(String opValue) {
		String opType = OPTYPE.LoopCenter.name();
		_DM_Log.log_Base(opType, opValue);
	}
	//end-Added
	*/
	  
	public static void log_LoopBegin(String opValue) {
		log_Base(LogType.LoopBegin.name(), opValue);
	}  
  

    ///////////////////////////////////////////////////////////////
  	// ONLY for dt
  	///////////////////////////////////////////////////////////////
 
	public static void log_LoopCenter(String opValue) {
		log_Base(LogType.LoopCenter.name(), opValue);
	}
	
	public static void log_LoopPrint(String opValue) {
  		log_Base(LogType.LoopPrint.name(), opValue);
  	}
	
	public static void log_LoopEnd(String opValue) {
  		log_Base(LogType.LoopEnd.name(), opValue);
  	}
	
	public static void log_IO(String opValue) {
  		log_Base(LogType.IO.name(), opValue);
  	}  

	public static void log_RPC(String opValue) {     //same as log_MsgSending
  		log_Base(LogType.RPC.name(), opValue);
  	}  
	
	
	/**
	 * For dynamic slicing
	 */
	public static void log_DynamicPoint(String opValue) {  
  		log_Base(LogType.DynamicPoint.name(), opValue);
  	}  
	
	
	
	// only for trigger
	public static void log_SourceTimingBegin(String opValue) {  
  		log_Base(LogType.SourceTimingBegin.name(), opValue);
  	}  
	
	public static void log_SourceTimingEnd(String opValue) {  
  		log_Base(LogType.SourceTimingEnd.name(), opValue);
  	}
	
	public static void log_SinkTimingBegin(String opValue) {  
  		log_Base(LogType.SinkTimingBegin.name(), opValue);
  	}  
	
	public static void log_SinkTimingEnd(String opValue) {  
  		log_Base(LogType.SinkTimingEnd.name(), opValue);
  	}  
	
	
	
	
	/**
	 * jx - Basic log function
	 */
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
	
}

