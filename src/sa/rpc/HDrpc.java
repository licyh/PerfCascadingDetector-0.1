package sa.rpc;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.text.TextFileWriter;

import sa.wala.WalaUtil;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;


public class HDrpc { 
 
	ClassHierarchy cha;
	String outputDir;
	String rpcfile = "hd_rpc.txt";  //can be "dirxx/dirxx/hd_rpc.txt"
	
	public HDrpc(ClassHierarchy cha, String outputDir) {
		this.cha = cha;
		this.outputDir = outputDir;
	}
  
	
	public void doWork() {
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
	    	if ( !className.startsWith("Lorg/apache/hadoop/hdfs/") ) continue; // only focus on PACKAGEs of 'hdfs'
	      
	    	// General
	    	/*
	    	for (IClass cc : c.getAllImplementedInterfaces() ) {
		    	// Get RPC classes (ie, server-side rpc implementation) - based on that all v1 rpc classes implements "org.apache.hadoop.ipc.VersionedProtocol"
		    	// for a class, get its all interfaces, including its all ancestors'.
	    		if (cc.getName().toString().endsWith("VersionedProtocol")) {  //like pplication,Lorg/apache/hadoop/hdfs/protocol/ClientProtocol>, <Application,Lorg/apache/hadoop/hdfs/protocol/ClientDatanodeProtocol>, <Application,Lorg/apache/hadoop/hdfs/server/protocol/InterDatanodeProtocol>, <Application,Lorg/apache/hadoop/hdfs/server/protocol/DatanodeProtocol>, <Application,Lorg/apache/hadoop/hdfs/server/protocol/NamenodeProtocol>
	    			if (c.isInterface()) 
	    				mrv1Iface.add(c);
	    			else
	    				mrv1Class.add(c);
	    			break;
	    		}
	    	}
	    	*/
	    	// for hadoop-2.3.0 (hd-5153)
    		for (IClass cc : c.getAllImplementedInterfaces()) {
    			if (cc.getName().toString().endsWith("Protocol") || cc.getName().toString().endsWith("Protocols")) {
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
	    		
	    		String className = WalaUtil.formatClassName( m.getDeclaringClass().getName().toString() );
	    		for (IClass iface : c.getAllImplementedInterfaces()) {
	    			// only find out RPC interfaces   #one RPC class <- many (RPC or non-RPC) interfaces
	    			// jx: because one may have multiple interfaces, I don't know if it's mistaken made by human or not
	    			if ( mrv1Iface.contains(iface) ) { 
	    				String ifacemethodsig = WalaUtil.containMethod(iface, m.getSelector().toString());
	    				if (ifacemethodsig != null) {
	    					String line = className + " "
	    								+ WalaUtil.formatClassName( iface.getName().toString() ) + " "
	    								+ m.getName().toString() + " "
	    								+ "0";
	    					results.add(line);
	    				}
	    			}
	    		}
	    	}
	    }//outer-for 
	    
	    TextFileWriter writer = new TextFileWriter( Paths.get(outputDir, rpcfile) );
	    writer.writeLine("//format: 1.implementation class name  2.interface class name  3. method name  4. count of args  5+: args' class names ");
	    for (String str: results) {
	    	writer.writeLine(str);
	    }
	    writer.close();
	}
	
	
  
}
