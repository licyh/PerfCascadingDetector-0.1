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
  List<String> classesForInst = new ArrayList<String>();
  List<String> methodsForInst = new ArrayList<String>();
  List<String> linesForInst  = new ArrayList<String>();
  List<String> typesForInst  = new ArrayList<String>();
  List<Integer> flagsForInst = new ArrayList<Integer>();
  String instBegin = "";
  String instEnd = "";
  //end-Added

  public Transformer(String args) {
    super();
    option = new DMOption(args);
    
    
    //Added by JX    
    try {
    	InputStream ins = MapReduceTransformer.class.getClassLoader().getResourceAsStream("resource/targetlocations");
    	BufferedReader bufreader = new BufferedReader( new InputStreamReader(ins) );
    	//BufferedReader bufreader = new BufferedReader( new FileReader("resource/targetlocations") );
		String tmpline;
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
    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	System.out.println("JX - " + classesForInst.size() + " locations are loaded");
	System.out.println("JX - " + "classesForInst = " + classesForInst);
	System.out.println("JX - " + "methodsForInst = " + methodsForInst);
	System.out.println("JX - " + "linesForInst =  " + linesForInst );
	System.out.println("JX - " + "instructions = " + instBegin + "*" + instEnd + "*");
    
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
      //System.out.println("JX - will enter a class for instrumenting target codes");
      transformClassForCodeSnippets(cl, methods);
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
  //end-Added
  
  
  public void transformMethod(CtClass cl, CtBehavior method) {} //implement in difficult application
  
}


