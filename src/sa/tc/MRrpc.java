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
  
  ArrayList<IClass> classList = new ArrayList<IClass>();          //jx: a class list filtered from 'cha'
  ArrayList<IClass> requestClassList = new ArrayList<IClass>();
  ArrayList<String> requestNameList = new ArrayList<String>();
  ArrayList<IClass> responseClassList = new ArrayList<IClass>();
  ArrayList<String> responseNameList = new ArrayList<String>();
  ArrayList<IClass> otherClassList = new ArrayList<IClass>();
  ArrayList<String> otherNameList = new ArrayList<String>();
	
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

  public static boolean containMethod(IClass c, String method) {
    boolean rt = false;
    for (IMethod im : c.getDeclaredMethods()) {
      if (im.getName().toString().equals(method)) {
        rt = true;
      }
    }
    return rt;
  }
  
  
  /**
   * JX - how to find RPCs
   * v2
   * 1. find requests
   * 2. which RPC server-side methods use the requests (as a parameter) 
   */ 
  public void findRPCv2() {
	System.out.println("\nJX - MRV2 RPC");
	// JX - Find out MapReduce's Classes of RPC's "request" and "response"
    for (IClass c : cha) {
      if (c.getName().toString().startsWith("Lorg/apache/") == false) { System.err.println("not Lorg/apache/" + c.getName().toString()); continue; }
      /* for now, the results are same w/ w/o the this code snippet
      String className = c.getName().toString();
      if ( !className.startsWith("Lorg/apache/hadoop/mapred/") && 
       	   !className.startsWith("Lorg/apache/hadoop/mapreduce/") && 
       	   !className.startsWith("Lorg/apache/hadoop/yarn/") )
           continue;
      */
      classList.add(c);
      /* if the class is a subclass of ProtoBase.*/
      if (c.getSuperclass().getName().toString().endsWith("ProtoBase")) {
        if (c.getName().toString().contains("Request")) {
          requestClassList.add(c);
          requestNameList.add(c.getName().toString());
        }
        else if (c.getName().toString().contains("Response")) {
          responseClassList.add(c);
          responseNameList.add(c.getName().toString());
        }
        else { //this class is not a direct rpc related class.
          if (c.getName().toString().endsWith("LocalizerStatusPBImpl")) {
            otherClassList.add(c);
            otherNameList.add(c.getName().toString());
          }
        }
      }
    }

    // prune request and response
    Iterator<IClass> iter = requestClassList.iterator();
    while (iter.hasNext()) {
      IClass c = iter.next();
      String name = c.getName().toString();
      String responseName = name.replaceAll("Request", "Response");
      if (responseNameList.contains(responseName) == false) {
        iter.remove();
        requestNameList.remove(name);
      }
    }
    iter = responseClassList.iterator();
    while (iter.hasNext()) {
      IClass c = iter.next();
      String name = c.getName().toString();
      String requestName = name.replaceAll("Response", "Request");
      if (requestNameList.contains(requestName) == false) {
        iter.remove();
        responseNameList.remove(name);
      }
    }

    // get request's interface, & otherclass's interface?
    ArrayList<String> requestIface = new ArrayList<String>();
    for (IClass c : requestClassList) {
      for (IClass iface : c.getAllImplementedInterfaces()) {
        requestIface.add(iface.getName().toString());
        System.out.println("JX-" + iface.getName().toString());
      }
    }

    ArrayList<String> otherIface = new ArrayList<String>();
    for (IClass c : otherClassList) {
      for (IClass iface : c.getAllImplementedInterfaces()) {
        otherIface.add(iface.getName().toString());
        System.out.println(" debug iface: " + iface.getName());
      }
    }


        

    // get each rpc function and its class.
    ArrayList<String> out = new ArrayList<String>();


    // JX - ??
    for (IClass c : classList) {
      if (c.getName().toString().startsWith("Lorg/apache/") == false) { continue; }
      if (c.getName().toString().contains("$")) { continue; } // private class
      if (c.getName().toString().contains("ClientImpl")) { continue; } // client impl

      for (IMethod m : c.getDeclaredMethods()) {
        if (m.isAbstract() == true) { continue; } // abstrct method
        if (m.getNumberOfParameters() != 2) { continue; }

        String paraTy = m.getParameterType(1).toString(); // format: <Application, Lorg/.../Class>
        paraTy = paraTy.substring(paraTy.lastIndexOf(",")+1, paraTy.length()-1);
        if (requestIface.contains(paraTy)){
          String str = c.getName().toString();
          String outStr;
          //str = MRrpc.format(str);
          System.out.println(str + " ");
          outStr = str + " ";
          for (IClass iface : c.getAllImplementedInterfaces()) {
            if (MRrpc.containMethod(iface, m.getName().toString()) == true) {
              str = iface.getName().toString();
              //str = MRrpc.format(str);
              System.out.println(str + " ");
              outStr += str + " ";
            }
          }
          str = m.getName().toString();
          //str = MRrpc.format(str);
          System.out.println(str);
          outStr += str + " ";
          outStr += "1 Ljava/lang/Object";
          out.add(outStr);
        }
        else if (otherIface.contains(paraTy)) {
          System.out.println("Method: " + m.getName() + " in cc: " + c.getName());
        }

      }
    }
    System.out.println("JX - checkpoint - here - 1");
    
    //System.exit(-1);

    System.out.println("JX - checkpoint - here - 2");
    
    String filepath = packageDir + rpcfilepath; 
    try {
      PrintWriter outFile = new PrintWriter(filepath, "UTF-8");
      outFile.println("//format: class iface method #parameters ..");
      for (String str : out) {
        outFile.println(str);
      }
      outFile.close();
    } catch (Exception e) {
      e.printStackTrace();
    }


  }
  
  
  public void findRPCv1() {
    System.out.println("\nJX - MRV1 RPC");
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
        	  mrv1Iface.add(c);
          else
        	  mrv1Class.add(c);
          break;
        }
      }
    }//outer-for

    System.out.println( "mrv1Class(length=" + mrv1Class.size() + "): " + mrv1Class );
    System.out.println( "mrv1Iface(length=" + mrv1Iface.size() + "): " + mrv1Iface );

    ArrayList<String> mrv1Out = new ArrayList<String>();
    
    // 2. Get RPC methods that included in RPC interfaces
    for (IClass clazz : mrv1Class) {
      for (IClass iface : clazz.getAllImplementedInterfaces())
    	// only find out RPC interfaces   #one RPC class <- many (RPC or non-RPC) interfaces
        if ( mrv1Iface.contains(iface) ) {
          for (IMethod m : iface.getDeclaredMethods()) {
            String str = clazz.getName().toString() + " " 
            			+ iface.getName().toString() + " " 
            			+ m.getName().toString() + " " 
            			+ (m.getNumberOfParameters()-1) + " ";
            for (int i = 1; i < m.getNumberOfParameters(); i++) {
            	//if (m.getParameterType(i).isReferenceType())
            	str += m.getParameterType(i).getName() + " ";
            }
            mrv1Out.add(str);
            System.out.println(str);
          }
        }
    }//outer-for

    String filepath = packageDir + rpcfilepath;
    try {
      FileWriter outFile = new FileWriter(filepath, true);  //PrintWrite(filepath, "UTF-8");
      for (String str : mrv1Out) {
        outFile.write(str + "\n");
      }
      outFile.close();
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
