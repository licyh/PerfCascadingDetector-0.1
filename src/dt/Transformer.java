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
import java.io.File;
import java.io.FileReader;
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
  
  //jx: added
  List<String> rpcMethodSigs = new ArrayList<String>();
  List<String> ioMethodPrefixes = new ArrayList<String>();
  
  
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
    
    //jx - read (rpc) + io
    readTimeConsumingOperations();
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

  public void readTimeConsumingOperations() {
	  System.out.println("\nJX-readTimeConsumingOperations");
	  String rpcfile = "";
	  String iofile = "";
	  String commonIOfile = "resource/io.txt";   //jx: io_specific.txt or io.tx
	  String systemname = "MapReduce";
	  
	  switch (systemname) {
	  	case "HDFS":
	  		rpcfile = "resource/hd_rpc.txt";
	  		iofile  = "resource/hd_io.txt";
			break;
	  	case "MapReduce":
	  		rpcfile = "resource/mr_rpc.txt";
	  		iofile  = "resource/mr_io.txt";
			break;
	  	case "HBase":
	  		rpcfile = "resource/hb_rpc.txt";
	  		iofile  = "resource/hb_io.txt";
			break;
	  	default:
			break;
	  }  
	
	  InputStream ins;
	  BufferedReader bufreader;
	  String tmpline;
	  
	  //1. read RPC file
      int tmpnn = 0;
	  try {
		  ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream( rpcfile );
		  bufreader = new BufferedReader( new InputStreamReader(ins) );
		  tmpline = bufreader.readLine(); // the 1st line is useless
		  while ( (tmpline = bufreader.readLine()) != null ) {
			  String[] strs = tmpline.trim().split("\\s+");
			  if ( tmpline.trim().length() > 0 ) {
				  tmpnn++;
				  for (String str: strs)
					  rpcMethodSigs.add(str);
			  }
		  }
		  bufreader.close();
		
	  } catch (Exception e) {
		  // TODO Auto-generated catch block
		  System.out.println("JX - ERROR - when reading RPC files");
		  e.printStackTrace();
	  }
	  System.out.println("JX - successfully read " + tmpnn + "(total:" + rpcMethodSigs.size() + ") RPCs as time-consuming operations");
	  
	  //2. read IO file
	  try {
		  ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream( commonIOfile );
		  bufreader = new BufferedReader( new InputStreamReader(ins) );  
		  tmpline = bufreader.readLine(); // the 1st line is useless
		  while ( (tmpline = bufreader.readLine()) != null ) {
			  String[] strs = tmpline.trim().split("\\s+");
			  if ( tmpline.trim().length() > 0 ) {
				  ioMethodPrefixes.add( strs[0] );
			  }
		  }
		  bufreader.close();
		
		  File f = new File( MapReduceTransformer.class.getClassLoader().getResource( iofile ).getPath() );
		  if (f.exists()) {
			  bufreader = new BufferedReader( new FileReader( f ) );
			  tmpline = bufreader.readLine(); // the 1st line is useless
			  while ( (tmpline = bufreader.readLine()) != null ) {
				  String[] strs = tmpline.trim().split("\\s+");
				  if ( tmpline.trim().length() > 0 ) {
					  ioMethodPrefixes.add( strs[0] );
				  }
			  }
		  }
		  bufreader.close();
	  } catch (Exception e) {
		  // TODO Auto-generated catch block
		  System.out.println("JX - ERROR - when reading IO files");
		  e.printStackTrace();
	  }
	  System.out.println("JX - successfully read " + ioMethodPrefixes.size() + " IO Prefixes as time-consuming operations");
  }
  
  
  
  
  
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // JX - Javassist's Transform Methods
    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    
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
  			
	  	      //Test - Added by JX
	  	      //System.out.println("JX - CLASS - " + cl.getName() );
	  	      /*
	  	      for (CtBehavior method : methods) 
	  	    	System.out.println( method.getName() + " @ " + method.getSignature() 
	  	  	  		+ " - constr?" + method.getMethodInfo().isConstructor() + " - cl?" + method.getMethodInfo().isStaticInitializer() + " - meth?" + method.getMethodInfo().isMethod());
	  	      */
	  	      //end-Added
  			
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
		//transformTimeConsumingOperations(cl, method);
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
  
  	
  	public boolean isInPackageScope(String className) { return false; }  //should be inherited
  	
  	
  	public void transformTimeConsumingOperations(CtClass cl, CtBehavior method) throws CannotCompileException {
  		
 	    MethodInfo methodInfo = method.getMethodInfo();
 	    String methodName = method.getName().toString();
 	    String className = cl.getName().toString();
 	 
 	    if ( !isInPackageScope(className) ) {
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
 	    String iOLog      	      = logFuncPre + "_" + "IO";

 	    boolean injectFlag = false;

 	    /* for rpc calling */
 	    methodUtil.insertRPCCallInst(logClass, msgSendingLog, rpcInfo);
 	    methodUtil.insertRPCInvoke(logClass, msgSendingLog);
 	    
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

 	    
 	    // I/Os 
 	    methodUtil.insertIOs(logClass, iOLog,         ioMethodPrefixes);
 	    
	}

}


