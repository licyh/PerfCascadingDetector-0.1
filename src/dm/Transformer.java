package dm;

import java.security.*;
import java.util.ArrayList;
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
  // For target code instrumentation
  List<String> classesForInst = new ArrayList<String>();
  List<String> methodsForInst = new ArrayList<String>();
  List<String> linesForInst  = new ArrayList<String>();
  List<String> typesForInst  = new ArrayList<String>();
  List<Integer> flagsForInst = new ArrayList<Integer>();
  String instBegin = "";
  String instEnd = "";
  // For loop instrumentation
  List<String> loop_classesForInst = new ArrayList<String>();
  List<String> loop_methodsForInst = new ArrayList<String>();
  List<String> loop_linesForInst  = new ArrayList<String>();
  List<String> loop_typesForInst  = new ArrayList<String>();
  List<Integer> loop_flagsForInst = new ArrayList<Integer>();
  String loop_instBegin = "";
  String loop_instCenter = "";
  //String loop_instEnd = "";  
  //end-Added

  public Transformer(String args) {
    super();
    option = new DMOption(args);
    
    //Added by JX  
    InputStream ins;
    BufferedReader bufreader;
    String tmpline;
    try {
    	// Read target code instrumentation infos
    	ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/targetlocations");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
    	//BufferedReader bufreader = new BufferedReader( new FileReader("resource/targetlocations") );
		while ( (tmpline = bufreader.readLine()) != null ) {
			String[] strs = tmpline.trim().split("\\s+");
			if ( tmpline.trim().length() > 0 ) {
				classesForInst.add( strs[0] );
				methodsForInst.add( strs[1] );
				linesForInst.add( strs[2] );
				typesForInst.add( strs[3] );
				flagsForInst.add(0);
			}
		}
		bufreader.close();
    	ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/targetinstructions");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
		//bufreader = new BufferedReader( new FileReader("resource/targetinstructions") );
		instBegin = bufreader.readLine();
		instEnd = bufreader.readLine();
		bufreader.close();
		
		// Read loop instrumentation infos
		ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/looplocations");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
		while ( (tmpline = bufreader.readLine()) != null ) {
			String[] strs = tmpline.trim().split("\\s+");
			if ( tmpline.trim().length() > 0 ) {
				loop_classesForInst.add( strs[0] );
				loop_methodsForInst.add( strs[1] );
				loop_linesForInst.add( strs[2] );
				loop_typesForInst.add( strs[3] );
				loop_flagsForInst.add(0);
			}
		}
		bufreader.close();
    	ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/loopinstructions");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
		loop_instBegin = bufreader.readLine();
		loop_instCenter = bufreader.readLine();
		bufreader.close();
		
    } catch (Exception e) {
		// TODO Auto-generated catch block
    	System.out.println("JX - ERROR - when reading resource/xxxlocations&xxxinstructions at Transformer.java");
		e.printStackTrace();
	}
	
	System.out.println("JX - " + classesForInst.size() + " locations are loaded");
	System.out.println("JX - " + "classesForInst = " + classesForInst);
	System.out.println("JX - " + "methodsForInst = " + methodsForInst);
	System.out.println("JX - " + "linesForInst =  " + linesForInst );
	System.out.println("JX - " + "instructions = " + instBegin + "*" + instEnd + "*");
	
	System.out.println("JX - " + loop_classesForInst.size() + " locations are loaded");
	System.out.println("JX - " + "loop_classesForInst = " + loop_classesForInst);
	System.out.println("JX - " + "loop_methodsForInst = " + loop_methodsForInst);
	System.out.println("JX - " + "loop_linesForInst =  " + loop_linesForInst );
	System.out.println("JX - " + "loop_instructions = " + loop_instBegin + "*" + loop_instCenter + "*");
    
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
      //Added by JX
      //System.out.println("JX - CLASS - " + cl.getName() );
      /*
      for (CtBehavior method : methods) 
    	System.out.println( method.getName() + " @ " + method.getSignature() 
  	  		+ " - constr?" + method.getMethodInfo().isConstructor() + " - cl?" + method.getMethodInfo().isStaticInitializer() + " - meth?" + method.getMethodInfo().isMethod());
      */
      //end-Added
      for (CtBehavior method : methods) {
        if (method.isEmpty() == false) {
          //Added by JX
          /*
          if ( cl.getName().contains("BaseContainerTokenSecretManager")
        		  || cl.getName().contains("ContainerExecutor") ) {
        	  System.out.println( method.getName() + " @ " + method.getSignature() 
        	  	+ " - constr?" + method.getMethodInfo().isConstructor() + " - cl?" + method.getMethodInfo().isStaticInitializer() + " - meth?" + method.getMethodInfo().isMethod());
          }
          */
          //end-Added
          transformMethod(cl, method);
        }
      }
      
      //Added by JX
      // instrument for target codes");
      transformClassForCodeSnippets(cl, methods);
      // instrument for (large) loops
      transformClassForLoops(cl, methods);
      //end-Added
      
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
  
  
  public void transformMethod(CtClass cl, CtBehavior method) {} //implement in difficult application

  
  //Added by JX
  //public void transformClassForCodeSnippets(CtClass cl, CtBehavior[] methods) {}
  //end-Added
  
  //Added by JX
  public void transformClassForCodeSnippets(CtClass cl, CtBehavior[] methods) {
	  if ( !classesForInst.contains(cl.getName()) ) return;
	  //System.out.println("JX - @1 - " + cl.getName());
      for (CtBehavior method : methods) {
          if ( method.isEmpty() ) continue;
          //System.out.println("JX - @2 - " + method.getName());
          // traverse all locations for instrumentation
          for (int i = 0; i < classesForInst.size(); i++) {
    		  if ( classesForInst.get(i).equals(cl.getName())
    				  && methodsForInst.get(i).equals(method.getName()) ) {
    			  int linenumber = Integer.parseInt( linesForInst.get(i) );
    			  try {
    				  /* test
    				  for (int k = 224; k <= 248; k++) {
    					  System.out.println( "JX - " + "for line " + k + " will insert at " + method.insertAt(k, false, instBegin) );
    				  }
    				  */
	    			  if ( typesForInst.get(i).equals("TargetCodeBegin") ) {
	    				  System.out.println( "JX - TargetCodeBegin: expected linenumber = " + linenumber + ", will insert at " + method.insertAt(linenumber, false, instBegin) );
	    				  method.insertAt(linenumber, true, instBegin);
	    				  flagsForInst.set(i, flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "this is the " + flagsForInst.get(i) + " st/nd/rd/th time for location " + i );
	    			  }
	    			  else { //this is "TargetCodeEnd"
	    				  System.out.println( "JX - TargetCodeEnd: expected linenumber = " + linenumber + ", will insert at " + method.insertAt(linenumber, false, instEnd) );
	    				  method.insertAt(linenumber, true, instEnd);
	    				  flagsForInst.set(i, flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "this is the " + flagsForInst.get(i) + " st/nd/rd/th time for location " + i );
	    			  }
    			  } catch (Exception e) {
    				  // TODO Auto-generated catch block
    				  e.printStackTrace();
    			  }
    		  }
    	  }
      }//end-outer for
  }
  
  
  public void transformClassForLoops(CtClass cl, CtBehavior[] methods) {
	  if ( !loop_classesForInst.contains(cl.getName()) ) return;
	  System.out.println("JX - @1 - " + cl.getName());
      for (CtBehavior method : methods) {
          if ( method.isEmpty() ) continue;
          System.out.println("JX - @2 - " + method.getName());
          // traverse all locations for instrumentation
          for (int i = 0; i < loop_classesForInst.size(); i++) {
    		  if ( loop_classesForInst.get(i).equals(cl.getName())
    				  && loop_methodsForInst.get(i).equals(method.getName()) ) {
    			  try {
        			  
        			  int linenumber = Integer.parseInt( loop_linesForInst.get(i) );
    				  /* test
    				  for (int k = 224; k <= 248; k++) {
    					  System.out.println( "JX - " + "for line " + k + " will insert at " + method.insertAt(k, false, instBegin) );
    				  }
    				  */
	    			  if ( loop_typesForInst.get(i).equals("LoopBegin") ) {
	    				  // JX - only one time for a method
	    				  method.addLocalVariable("jxloop", CtClass.intType); 
	    				  System.out.println( "JX - LoopBegin: expected linenumber = " + linenumber + ", will insert at " + method.insertAt(linenumber, false, loop_instBegin) );
	    				  method.insertAt(linenumber, true, loop_instBegin);
	    				  loop_flagsForInst.set(i, loop_flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "this is the " + loop_flagsForInst.get(i) + " st/nd/rd/th time for location " + i );
	    			  }
	    			  else if ( loop_typesForInst.get(i).equals("LoopCenter") ) { //this is "TargetCodeEnd"
	    				  System.out.println( "JX - LoopCenter: expected linenumber = " + linenumber + ", will insert at " + method.insertAt(linenumber, false, loop_instCenter) );
	    				  method.insertAt(linenumber, true, loop_instCenter);
	    				  loop_flagsForInst.set(i, loop_flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "this is the " + loop_flagsForInst.get(i) + " st/nd/rd/th time for location " + i );
	    			  }
    			  } catch (Exception e) {
    				  // TODO Auto-generated catch block
    				  e.printStackTrace();
    			  }
    		  }
    	  }
      }//end-outer for
  }
  //end-Added
  
  
  
  
}


