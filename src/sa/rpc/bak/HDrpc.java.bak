package sa.tc;

import java.io.*;
import java.util.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;


public class HDrpc { 
 
  ClassHierarchy cha;
  String packageDir;
  String rpcfilepath = "output/hd_rpc.txt";
	
  public HDrpc(ClassHierarchy cha, String packageDir) {
	  this.cha = cha;
	  this.packageDir = packageDir;
  }
  
  public void findRPC() {
    System.out.println("\nJX - HDFS RPC, seems like MRv1 RPC");
    ArrayList<IClass> mrv1Class = new ArrayList<IClass>();
    ArrayList<IClass> mrv1Iface = new ArrayList<IClass>();
    
    // 1. Get RPC classes and their RPC interfaces that include RPC methods we WANTED
    // Architecture: Get RPC class(1) <- RPC interfaces(1..*) [<- org.apache.hadoop.ipc.VersionedProtocol]
    for (IClass c : cha) {
      String className = c.getName().toString();
      // only focus on PACKAGEs of 'hdfs'
      if ( !className.startsWith("Lorg/apache/hadoop/hdfs/") )
        continue;
      
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
    

    String filepath = packageDir + rpcfilepath;
    try {
      PrintWriter outFile = new PrintWriter(filepath, "UTF-8");
      outFile.println("//format: implementation method's signature  \t  interface method's signature1  ..2 .. if any");
      for (String str : results) {
        outFile.println(str);
      }
      outFile.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  
  // JX - main method
  public void doWork() {
	// JX - check hdfs rpc, seems like MRv1 rpc
    findRPC();
  }
  
  
}
