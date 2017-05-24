package sa.rpc;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.TextFileWriter;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;


public class HDrpc { 
 
	ClassHierarchy cha;
	String parentDir;
	String rpcfilepath = "hd_rpc.txt";
	
	public HDrpc(ClassHierarchy cha, String parentDir) {
		this.cha = cha;
		this.parentDir = parentDir;
	}
  
	// JX - main method
	public void doWork() {
		// JX - check hdfs rpc, seems like MRv1 rpc
		findRPC();
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
	    ArrayList<String> results_2 = new ArrayList<String>();
	    
	    // 2. Get RPC methods that included in RPC
	    for (IClass c : mrv1Class) {
	      for (IMethod m : c.getDeclaredMethods()) { 
	    	  String str = m.getSignature() + "\t";
	    	  String str_2 = MRrpc.format( m.getDeclaringClass().getName().toString() ) + " ";
	    	  boolean find = false;
		      for (IClass iface : c.getAllImplementedInterfaces())
		    	// only find out RPC interfaces   #one RPC class <- many (RPC or non-RPC) interfaces
		        if ( mrv1Iface.contains(iface) ) {
		        	String ifacemethodsig = MRrpc.containMethod(iface, m.getSelector().toString());
		        	if (ifacemethodsig != null) {
		        		str += ifacemethodsig + "\t";
		        		str_2 += MRrpc.format( iface.getName().toString() ) + " "
		        				+ m.getName().toString() + " "
		        				+ "0";
		        		find = true;
		        	}
		        }
		      if (find) {
		 	     results.add(str);
		 	     results_2.add(str_2);
		      }
	      }
	    }//outer-for 
	    

	    // write to file
	    /*
	    String filepath = Paths.get(parentDir, rpcfilepath).toString();
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
	    */
	    
	    TextFileWriter writer = new TextFileWriter( Paths.get(parentDir, rpcfilepath), true );
	    writer.writeLine("//format: 1.implementation class name  2.interface class name  3. method name  4. count of args  5+: args' class names ");
	    for (String str: results_2) {
	    	writer.writeLine(str);
	    }
	    writer.close();
	    
	    
	}
  
}
