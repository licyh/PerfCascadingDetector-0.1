package dt;

import java.io.*;
import java.util.*;

import com.RPCInfo;

import dm.Util.ClassUtil;
import dm.Util.MethodUtil;


import java.security.*;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;



public class MapReduceDM {

	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("Agent arguments: " + agentArgs);
		inst.addTransformer( new MapReduceTransformer(agentArgs) );
	}
  
}



class MapReduceTransformer extends Transformer {

	
	public MapReduceTransformer(String str) {
		super(str);   
	}
	

	@Override
	public boolean isInPackageScope(String className) { 
		/*if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
	      	return; //bypass all constructors.
	    }*/

	    if (className.contains("xerces") ||
	    		className.contains("xml") ||
	    		className.contains("xalan")) {
	    	return false; //these classes are about xml parser.
	    }

	    if (className.startsWith("java.") ||
	    		className.startsWith("sun.")) {
	    	return false; //bypass
	    }
	    	
	    if (className.startsWith("org.apache.hadoop.yarn.") == false
	        && className.startsWith("org.apache.hadoop.mapred.") == false
	        && className.startsWith("org.apache.hadoop.mapreduce.") == false
	        && className.startsWith("org.apache.hadoop.ipc.") == false
	        && className.startsWith("org.apache.hadoop.util.RunJar") == false
	        && className.startsWith("org.apache.hadoop.util.Shell") == false
	        //The CodeAttribute of some methods in util is empty. ignore them.
	        ) {
	    	return false;
	    }
	    if (className.contains("PBClientImpl") ||
	        className.contains("PBServiceImpl") ||
	        className.contains("org.apache.hadoop.yarn.event.EventHandler")) {
	    	return false;
	    }
	    
	    return true;
	}  
	

}
