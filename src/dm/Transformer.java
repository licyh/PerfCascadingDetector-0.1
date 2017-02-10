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
  List<String> largeloop_classesForInst = new ArrayList<String>();
  List<String> largeloop_methodsForInst = new ArrayList<String>();
  List<String> largeloop_linesForInst  = new ArrayList<String>();
  List<String> largeloop_typesForInst  = new ArrayList<String>();
  List<Integer> largeloop_flagsForInst = new ArrayList<Integer>();
  String largeloop_instBegin = "";
  String largeloop_instCenter = "";
  //String largeloop_instEnd = "";  
  //end-Added

  public Transformer(String args) {
    super();
    option = new DMOption(args);
    
    //read targetlocations & largelooplocations
    read();
  }
  
  public void read() {
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
		ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/largelooplocations");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
		while ( (tmpline = bufreader.readLine()) != null ) {
			String[] strs = tmpline.trim().split("\\s+");
			if ( tmpline.trim().length() > 0 ) {
				largeloop_classesForInst.add( strs[0] );
				largeloop_methodsForInst.add( strs[1] );
				largeloop_linesForInst.add( strs[2] );
				largeloop_typesForInst.add( strs[3] );
				largeloop_flagsForInst.add(0);
			}
		}
		bufreader.close();
    	ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/largeloopinstructions");
    	bufreader = new BufferedReader( new InputStreamReader(ins) );
		largeloop_instBegin = bufreader.readLine();
		largeloop_instCenter = bufreader.readLine();
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
	
	System.out.println("JX - " + largeloop_classesForInst.size() + " locations are loaded");
	System.out.println("JX - " + "largeloop_classesForInst = " + largeloop_classesForInst);
	System.out.println("JX - " + "largeloop_methodsForInst = " + largeloop_methodsForInst);
	System.out.println("JX - " + "largeloop_linesForInst =  " + largeloop_linesForInst );
	System.out.println("JX - " + "largeloop_instructions = " + largeloop_instBegin + "*" + largeloop_instCenter + "*");
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
      transformClassForLargeLoops(cl, methods);
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
  
  
  public void transformClassForLargeLoops(CtClass cl, CtBehavior[] methods) {
	  if ( !largeloop_classesForInst.contains(cl.getName()) ) return;
	  System.out.println("JX - @1 - " + cl.getName());
      for (CtBehavior method : methods) {
          if ( method.isEmpty() ) continue;
          System.out.println("JX - @2 - " + method.getName());
          // traverse all locations for instrumentation
          for (int i = 0; i < largeloop_classesForInst.size(); i++) {
    		  if ( largeloop_classesForInst.get(i).equals(cl.getName())
    				  && largeloop_methodsForInst.get(i).equals(method.getName()) ) {
    			  try {
        			  
        			  int linenumber = Integer.parseInt( largeloop_linesForInst.get(i) );
    				  /* test
    				  for (int k = 224; k <= 248; k++) {
    					  System.out.println( "JX - " + "for line " + k + " will insert at " + method.insertAt(k, false, instBegin) );
    				  }
    				  */
	    			  if ( largeloop_typesForInst.get(i).equals("LargeLoopBegin") ) {
	    				  // JX - only one time for a method
	    				  method.addLocalVariable("jxloop", CtClass.intType); 
	    				  System.out.println( "JX - LargeLoopBegin: expected linenumber = " + linenumber + ", will insert at " + method.insertAt(linenumber, false, largeloop_instBegin) );
	    				  method.insertAt(linenumber, true, largeloop_instBegin);
	    				  largeloop_flagsForInst.set(i, largeloop_flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "this is the " + largeloop_flagsForInst.get(i) + " st/nd/rd/th time for location " + i );
	    			  }
	    			  else if ( largeloop_typesForInst.get(i).equals("LargeLoopCenter") ) { //this is "TargetCodeEnd"
	    				  System.out.println( "JX - LargeLoopCenter: expected linenumber = " + linenumber + ", will insert at " + method.insertAt(linenumber, false, largeloop_instCenter) );
	    				  method.insertAt(linenumber, true, largeloop_instCenter);
	    				  largeloop_flagsForInst.set(i, largeloop_flagsForInst.get(i)+1);
	    				  System.out.println( "JX - " + "this is the " + largeloop_flagsForInst.get(i) + " st/nd/rd/th time for location " + i );
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


