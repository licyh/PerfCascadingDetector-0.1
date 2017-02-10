package dt;

import java.io.*;
import java.util.*;
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
  ClassUtil classUtil;

  
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

}
