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
	    //rpc
	    rpcInfo.setInfoFilePath("resource/mr_rpc.txt", 2);
	    rpcInfo.setInfoFilePath("resource/mr_rpc_v1.txt", 1);
	    rpcInfo.readFile();
	}

}
