package dt;

import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.*;

import javassist.*;
import javassist.bytecode.*;

import dm.Util.DMOption;

public class Transformer implements ClassFileTransformer {

  DMOption option;
  //Added by JX
  HashMap<String, Integer[]> looplocations = new HashMap<String, Integer[]>();

  public Transformer(String args) {
    super();
    option = new DMOption(args);
    
    //read loop locations' file for instrumentation
    readLoopLocations();
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
    	int num_of_method = Integer.parseInt( bufreader.readLine() );
        	
		while ( (tmpline = bufreader.readLine()) != null ) {
			String[] strs = tmpline.trim().split("\\s+");
			if ( tmpline.trim().length() > 0 ) {
				String methodsig = strs[0];
				int nloops = Integer.parseInt( strs[1] );
				Integer[] loops = new Integer[nloops];
				for (int i = 0; i < nloops; i++)
					loops[i] = Integer.parseInt( strs[i] );
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

  
  //default function in javassist
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

      // JX - instrument for all loops
      transformClassForLoops(cl, methods);
      
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
    
 
  public void transformClassForLoops(CtClass cl, CtBehavior[] methods) throws CannotCompileException {
	  
      for (CtBehavior method : methods) {
          if ( method.isEmpty() ) continue;
          String methodsig = method.getSignature();
          System.out.println( "JX - method signature: " + methodsig );   //method.getGenericSignature();
          if ( !looplocations.containsKey( methodsig ) ) continue;
          
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
        		  if ( i+1 == j ) {
        			  System.err.println( "JX - WARN - " + i + "&" + j + " for " + methodsig );
        		  }
          // end-test
          for (int i = 0; i < loops.length; i++) {
        	  int linenumber = loops[i] + 1;
        	  if ( method.insertAt(linenumber, false, "loop" + i + "++;") == linenumber ) //some particular examples: "do { .." OR "while (true) ( .." would became insert at next line than normal
        		  method.insertAt( linenumber, "loop" + i + "++;" );
        	  else {
        		  // TODO - please see
        		  System.err.println( "JX - WARN - cannot insert at " + loops[i] + " for " + methodsig );
        	  }
          }
          
          // insert after
          for (int i = 0; i < loops.length; i++) {
        	  method.insertAfter( "_DM_Log.log_LoopPrint( \"loop_\"" + i + "\"_\" + loop" + i + ");" );
          }          
      }//end-outer for
  }
   
}


