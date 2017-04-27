package sa.tc;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;


public class MRrpc { 
 
  ClassHierarchy cha;
  String packageDir;
  String rpcfilepath = "output/mr_rpc.txt";   //including mr_rpc_v2.txt & mr_rpc_v1.txt 
  
	
  public MRrpc(ClassHierarchy cha, String packageDir) {
	  this.cha = cha;
	  this.packageDir = packageDir;
  }
	
  
  public static String format(String str) {
    String rt = str;
    if (rt.startsWith("L")) {
      rt = rt.substring(1);
    }
    rt = rt.replaceAll("/", ".");
    return rt;
  }

  public static String containMethod(IClass c, String methodSelector) {
    for (IMethod im : c.getDeclaredMethods()) {
      if ( im.getSelector().toString().equals(methodSelector) ) {
    	return im.getSignature();
      }
    }
    return null;
  }
  
  
  /**
   * JX - how to find RPCs
   * v2
   * 1. find requests
   * 2. which RPC server-side methods use the requests (as a parameter) 
   */ 
  public void findRPCv2() {
	System.out.println("\nJX - findRPCv2 - MRV2 RPC");
	
	ArrayList<IClass> requestClasses = new ArrayList<IClass>();
	ArrayList<String> requestNames = new ArrayList<String>();
	ArrayList<IClass> responseClasses = new ArrayList<IClass>();
	ArrayList<String> responseNames = new ArrayList<String>();
	ArrayList<IClass> otherClasses = new ArrayList<IClass>();
	ArrayList<String> otherNames = new ArrayList<String>();
	  
	// JX - Find out MapReduce's Classes of RPC's "request" and "response"
    for (IClass c : cha) {
      if ( !c.getName().toString().startsWith("Lorg/apache/") ) 
    	  continue;
      /* for now, the results are same w/ w/o the this code snippet
      String className = c.getName().toString();
      if ( !className.startsWith("Lorg/apache/hadoop/mapred/") && 
       	   !className.startsWith("Lorg/apache/hadoop/mapreduce/") && 
       	   !className.startsWith("Lorg/apache/hadoop/yarn/") )
           continue;
      */
      // if the class is a subclass of ProtoBase
      if (c.getSuperclass().getName().toString().endsWith("ProtoBase")) {     //this is the IMPO
        if (c.getName().toString().contains("Request")) {   //jx: like FailTaskAttemptRequestPBImpl
          requestClasses.add(c);
          requestNames.add(c.getName().toString());
        }
        else if (c.getName().toString().contains("Response")) {
          responseClasses.add(c);
          responseNames.add(c.getName().toString());
        }
        else { //this class is not a direct rpc-related class.
          if (c.getName().toString().endsWith("LocalizerStatusPBImpl")) {
            otherClasses.add(c);
            otherNames.add(c.getName().toString());
          }
        }
      }
    }

    // Prune request and response, jx: Actually, only need to focus on request
    Iterator<IClass> iter = requestClasses.iterator();
    while (iter.hasNext()) {
      IClass c = iter.next();
      String name = c.getName().toString();
      String responseName = name.replaceAll("Request", "Response");
      if ( !responseNames.contains(responseName) ) {
        iter.remove();
        requestNames.remove(name);
      }
    }
    iter = responseClasses.iterator(); //this step is not needed actually
    while (iter.hasNext()) {
      IClass c = iter.next();
      String name = c.getName().toString();
      String requestName = name.replaceAll("Response", "Request");
      if ( !requestNames.contains(requestName) ) {
        iter.remove();
        responseNames.remove(name);
      }
    }

    // Get request's interface, & otherclass's interface?
    System.out.println("requestClasses.size()=" + requestClasses.size() + " - " + requestClasses);  //jx: like FailTaskAttemptRequestPBImpl
    ArrayList<String> requestIfaces = new ArrayList<String>();                                      //jx: like FailTaskAttemptRequest
    for (IClass c : requestClasses) {
      for (IClass iface : c.getAllImplementedInterfaces()) {
        requestIfaces.add(iface.getName().toString());
      }
    }
    System.out.println("requestIfaces.size()=" + requestIfaces.size() + " - " + requestIfaces);

    ArrayList<String> otherIfaces = new ArrayList<String>();
    for (IClass c : otherClasses) {
      for (IClass iface : c.getAllImplementedInterfaces()) {
        otherIfaces.add(iface.getName().toString());
        System.out.println("WARN - debug iface: " + iface.getName());
      }
    }



    // Get each rpc function's 1) implementation method and its interface method 2)
    ArrayList<String> results = new ArrayList<String>();

    for (IClass c : cha) {
      if ( !c.getName().toString().startsWith("Lorg/apache/") ) 
    	  continue;
      if (c.getName().toString().contains("$")) { continue; } // private class
      if (c.getName().toString().contains("ClientImpl")) { continue; } // client impl

      for (IMethod m : c.getDeclaredMethods()) {    	      	  
    	if (m.isAbstract() == true) { continue; }          // jx: avoid abstrct methods,ie, only get implemented methods, also can be abstract class's or general class's
        if (m.getNumberOfParameters() != 2) { continue; }  // param0: this, param1: request 

        String paraTy = m.getParameterType(1).toString(); // format: <Application, Lorg/.../Class>
        paraTy = paraTy.substring(paraTy.lastIndexOf(",")+1, paraTy.length()-1); //jx: like FailTaskAttemptRequest, NOT xxxRequestPBImpl                
        
        if (requestIfaces.contains(paraTy)){
          String outStr = m.getSignature() + "\t";
          for (IClass iface : c.getAllImplementedInterfaces()) {
        	  String ifacemethodsig = MRrpc.containMethod(iface, m.getSelector().toString());
        	  if (ifacemethodsig != null) {   //jx: may have serval: eg,  m1 in Class A <- abstract m1 in Abstract Class B <- abstract m1 in Interface C
        		  outStr += ifacemethodsig + "\t";
        	  }
          }
          results.add(outStr);
        }
        else if (otherIfaces.contains(paraTy)) {
          System.out.println("Method: " + m.getName() + " in cc: " + c.getName());
        }

      }
    }
    
    // write to file
    String filepath = packageDir + rpcfilepath; 
    try {
      PrintWriter writer = new PrintWriter(filepath, "UTF-8");
      writer.println("//format: implementation method's signature  \t  interface method's signature1  ..2 .. if any");
      for (String str : results) {
    	  writer.println(str);
      }
      writer.println();
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }


  }
  
  
  public void findRPCv1() {
    System.out.println("\nJX - findRPCv1 - MRV1 RPC");
    ArrayList<IClass> mrv1Class = new ArrayList<IClass>();
    ArrayList<IClass> mrv1Iface = new ArrayList<IClass>();
    
    // 1. Get RPC classes and their RPC interfaces that include RPC methods we WANTED
    // Architecture: Get RPC class(1) <- RPC interfaces(1..*) [<- org.apache.hadoop.ipc.VersionedProtocol]
    for (IClass c : cha) {
      String className = c.getName().toString();
      // only focus on PACKAGEs of 'mapred', 'mapreduce' and 'yarn'
      if ( !className.startsWith("Lorg/apache/hadoop/mapred/") && 
    	   !className.startsWith("Lorg/apache/hadoop/mapreduce/") && 
    	   !className.startsWith("Lorg/apache/hadoop/yarn/") )
        continue;
      
      // filter/remove CLASSes of specified ones
      /*
      if ( className.contains("Local") ||    //jx: can keep
    	   className.contains("Avro") )      //jx: totally can remove, just "org.apache.hadoop.ipc.AvroRpcEngine$TunnelResponder org.apache.hadoop.ipc.AvroRpcEngine$TunnelProtocol call "
        continue;
      */
      
      // Get RPC classes (ie, server-side rpc implementation) - based on that all v1 rpc classes implements "org.apache.hadoop.ipc.VersionedProtocol"
      // for a class, get its all interfaces, including its all ancestors'.
      for (IClass cc : c.getAllImplementedInterfaces() ) {
        if (cc.getName().toString().endsWith("VersionedProtocol")) {
          if (c.isInterface()) 
        	  mrv1Iface.add(c);  //interface classes
          else
        	  mrv1Class.add(c);  //implemented classes
          break;
        }
      }
    }//outer-for

    System.out.println( "mrv1Class(length=" + mrv1Class.size() + "): " + mrv1Class );  
    System.out.println( "mrv1Iface(length=" + mrv1Iface.size() + "): " + mrv1Iface );

    ArrayList<String> results = new ArrayList<String>();
    
    // 2. Get RPC methods that included in RPC
    for (IClass c : mrv1Class) {
      for (IMethod m : c.getDeclaredMethods()) { 
    	  String str = m.getSignature() + "\t";
    	  boolean find = false;
	      for (IClass iface : c.getAllImplementedInterfaces())
	    	// only find out RPC interfaces   #one RPC class <- many (RPC or non-RPC) interfaces
	        if ( mrv1Iface.contains(iface) ) {
	        	String ifacemethodsig = MRrpc.containMethod(iface, m.getSelector().toString());
	        	if (ifacemethodsig != null) {
	        		str += ifacemethodsig + "\t";
	        		find = true;
	        	}
	        }
	      if (find)
	 	     results.add(str);
      }
    }//outer-for 
    
    
    // write to file
    String filepath = packageDir + rpcfilepath;
    try {
      FileWriter writer = new FileWriter(filepath, true);  //PrintWrite(filepath, "UTF-8");
      for (String str : results) {
    	  writer.write(str + "\n");
      }
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  // JX - main method
  public void doWork() {
	// JX - check mrv2 rpc
	findRPCv2();  
	// JX - check mrv1 rpc
    findRPCv1();
  }
  
  
}
