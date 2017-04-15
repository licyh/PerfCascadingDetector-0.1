package dt;

import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.RPCInfo;

import dm.Util.ClassUtil;
import dm.Util.DMOption;
import dm.Util.MethodUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;



public class Transformer implements ClassFileTransformer {

  DMOption option;
  ClassUtil classUtil;
  //Added by JX
  HashMap<String, Integer[]> looplocations = new HashMap<String, Integer[]>();

  //jx: RPC
  RPCInfo rpcInfo = new RPCInfo();
  
  public Transformer(String args) {
    super();
    option = new DMOption(args);
    //CtClass.debugDump = "/home/hadoop/hadoop/dump";
    option.setDelimiter("%");
    option.addOption("s", "searchScope", "search path");
    option.parse();

    //-s parameter
    classUtil = new ClassUtil();
    classUtil.setSearchScope(option.getValue("s"));
    //read loop locations' file for instrumentation
    readLoopLocations();
  }
  
  public void readLoopLocations() {
	//Added by JX  
	InputStream ins;
    BufferedReader bufreader;
    String tmpline;
    try {
		// Read loop instrumentation infos
		ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/looplocations");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
    	String[] nums = bufreader.readLine().trim().split("\\s+");
    	int num_of_methods = Integer.parseInt( nums[0] );
    	int num_of_loops = Integer.parseInt( nums[1] );
        	
		while ( (tmpline = bufreader.readLine()) != null ) {
			String[] strs = tmpline.trim().split("\\s+");
			if ( tmpline.trim().length() > 0 ) {
				String methodsig = strs[0];
				int nloops = Integer.parseInt( strs[1] );
				Integer[] loops = new Integer[nloops];
				for (int i = 0; i < nloops; i++)
					loops[i] = Integer.parseInt( strs[2+i] );
				looplocations.put(methodsig, loops);
			}
		}
		bufreader.close();
		
    } catch (Exception e) {
		// TODO Auto-generated catch block
    	System.out.println("JX - ERROR - when reading resource/looplocations at Transformer.java");
		e.printStackTrace();
	}
	System.out.println("JX - successfully read " + looplocations.size() + " loop locations for instrumentation");      
  }

  
  	// default function in javassist
  	public byte[] transform(ClassLoader loader, String className, Class redefiningClass, 
  			ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
  		return transformClass(redefiningClass, bytes);
  	}

 
  	public byte[] transformClass(Class classToTrans, byte[] b) {
  		ClassPool pool = ClassPool.getDefault();
  		pool.importPackage("javax.xml.parsers.DocumentBuilderFactory"); //add for xml
  		CtClass cl = null;
  		try {
  			cl = pool.makeClass(new java.io.ByteArrayInputStream(b));
  			CtBehavior[] methods = cl.getDeclaredBehaviors();

  			for (CtBehavior method : methods) {
  				if ( method.isEmpty() ) continue;
  				// default
  				transformMethod(cl, method);
  			}
           
  			b = cl.toBytecode();
  		}
  		catch (Exception e) { e.printStackTrace();}
  		finally {
  			if (cl != null) {
  				cl.detach();
  			}
  		}
  		return b;
  	}

  
  	public void transformMethod(CtClass cl, CtBehavior method) throws CannotCompileException {
		// for general
  		transformGeneral(cl, method);
  		
  		// JX - instrument for all loops
		transformLoops(cl, method);
		// JX - instrument for Time-Consuming operations like RPCs, I/Os, network operations...
		transformTimeConsumingOperations(cl, method);
  	} 
  	
  	
  	public void transformGeneral(CtClass cl, CtBehavior method) {}  //TODO - if needed 
 
  	
  	public void transformLoops(CtClass cl, CtBehavior method) throws CannotCompileException {
	  
		String methodsig = cl.getName() + "." + method.getName() + method.getSignature();  //full signature, like wala's signature
		//System.out.println( "JX - method signature: " + methodsig );   
		if ( !looplocations.containsKey( methodsig ) ) return;
      
		System.out.println( "JX - IN - method sig = " + methodsig );  
		Integer[] loops = looplocations.get( methodsig );
      
		// insert before
		for (int i = 0; i < loops.length; i++) {
			method.addLocalVariable( "loop" + i, CtClass.intType );
			method.insertBefore( "loop" + i + " = 0;" );
		}
      
		// insert loops
		// for test - TODO - please see
		for (int i = 0; i < loops.length; i++)
			for (int j = 0; j < loops.length; j++)
				if ( loops[i]+1 == loops[j] ) {
					System.err.println( "JX - WARN - " + i + "&" + j + " for " + methodsig );
				}
		// end-test
		for (int i = 0; i < loops.length; i++) {
			int linenumber = loops[i] + 1;
			int actualline = method.insertAt(linenumber, false, "loop" + i + "++;");
			if ( linenumber == actualline ) //some particular examples: "do { .." OR "while (true) ( .." would became insert at next line than normal
				method.insertAt( linenumber, "loop" + i + "++;" );
			else {
				// TODO - please see
				System.err.println( "JX - WARN - cannot insert at " + loops[i] + " (actual:" + actualline + ") for " + methodsig );
			}
		}
      
		// insert after
		for (int i = 0; i < loops.length; i++) {
			method.insertAfter( "dt._DM_Log.log_LoopPrint( \"loop_\" + " + i + " + \"_\" + loop" + i + ");" );
		}          
  		
  	}
  
  	
  	public void transformTimeConsumingOperations(CtClass cl, CtBehavior method) throws CannotCompileException {
  		
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

 	    String logClass = "_DM_Log";
 	    String logFuncPre = "log";

 	    classUtil.setClassPool(method);
 	    classUtil.updateClassPool();

 	    MethodUtil methodUtil = new MethodUtil();
 	    methodUtil.setMethod(method);


 	    String msgProcEnterLog    = logFuncPre + "_" + "MsgProcEnter";
 	    String msgProcExitLog     = logFuncPre + "_" + "MsgProcExit";
 	    String msgSendingLog      = logFuncPre + "_" + "MsgSending";
 	    

 	    boolean injectFlag = false;
 	    
 	    /* RPC function */
 	    if (rpcInfo.isRPCMethod(className, methodName) && //is a rpc
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

 	    /* for rpc calling */
 	    methodUtil.insertRPCCallInst(logClass, msgSendingLog, rpcInfo);
 	    methodUtil.insertRPCInvoke(logClass, msgSendingLog);

 	  }

}


