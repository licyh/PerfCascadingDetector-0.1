package ds;

import java.io.*;
import java.util.*;
import java.security.*;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import dm.util.Bytecode.*;
import dm.util.Bytecode.Instruction;
import dm.util.Bytecode.InvokeInst;
import dm.Transformer;
import dm.util.ClassUtil;
import dm.util.MethodUtil;
import com.APIInfo;
import com.API;
import com.RPCInfo;
import com.benchmark.BugConfig;
import com.text.Logger;

import LogClass.LogType;

import com.CalleeInfo;




public class MapReduceDM {
  public static void premain(String agentArgs, Instrumentation inst) {
    System.out.println("JX - INFO - started by Javassit DM. Agent arguments: " + agentArgs);
    inst.addTransformer(new MapReduceTransformer(agentArgs));
  }
}



class MapReduceTransformer extends Transformer {
	
	BugConfig bugConfig = new BugConfig("resource/bugconfig", true);
	
	ClassUtil classUtil;
	
	//added by JX
	Transformers transformers = new Transformers();
  
  
	public MapReduceTransformer(String str) {
	    super(str);
	    //CtClass.debugDump = "/home/hadoop/hadoop/dump";
	    option.setDelimiter("%");
	    option.addOption("s", "searchScope", "search path");
	    option.parse();
	
	    //-s parameter
	    classUtil = new ClassUtil();
	    classUtil.setSearchScope(option.getValue("s"));	    
	}


	
  	//added by JX
  	public void transformClass(CtClass cl) {
  		String className = cl.getName().toString();   
  		// FILTERS
		if ( className.startsWith("org.apache.hadoop.xxx.")
  				//&& !className.startsWith("org.apache.hadoop.io.IOUtils")   //for the real bug in mr-4576
	           ) {
	          return;
  		}
	    // LIMITS
		//if ( className.startsWith("org.apache.hadoop.yarn.")
  				//|| className.startsWith("org.apache.hadoop.mapred.") 
	      //     ) {	      
			transformers.transformClassForDynamicPoints(cl);
  		//}
	   
  	}  

}
