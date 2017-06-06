package dm;

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

import dm.util.DMOption;


public class Transformer implements ClassFileTransformer {

	DMOption option;

	public Transformer(String args) {
	    super();
	    option = new DMOption(args);    
	}
	
	
	/**
	 * default function in javassist
	 */
	@Override   
	public byte[] transform(ClassLoader loader, String className, Class<?> redefiningClass,
	    ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
	    return transformClass(redefiningClass, bytes);
	}

	
	public byte[] transformClass(Class<?> classToTrans, byte[] b) {
	    ClassPool pool = ClassPool.getDefault();
	    pool.importPackage("javax.xml.parsers.DocumentBuilderFactory"); //add for xml
	    CtClass cl = null;
	    try {
	    	cl = pool.makeClass(new java.io.ByteArrayInputStream(b));    //may + CtBehavior[] methods = cl.getDeclaredBehaviors();
	    	String className = cl.getName().toString();
                if (className.contains("DFSClient")) {  //DFSClient$DFSOutputStream$DataStreamer
                     System.out.println( "JX - DEBUG - className=" + className );
                }

	    	// Top Filters - bypass jdk
	  		if ( className.startsWith("java.")
	  				|| className.startsWith("sun.")
	  				// or
	  				|| className.startsWith("javax.")
	  				|| className.contains("xerces")  //these 3 kinds of classes are about xml parser.
	  	    		|| className.contains("xml") 
	  	    		|| className.contains("xalan")
	  			 ) {
	  			//NONE
	  		}
	  		else {
		    	System.out.println("JX - DEBUG - Class: " + className); 
		    	System.out.println("JX - DEBUG - -1");
		    	
		    	transformClass( cl );
		       
		    	System.out.println("JX - DEBUG - +1");
	  		}
	      	b = cl.toBytecode();
	    }
	    catch (Exception e) {
	    	e.printStackTrace();
	    }
	    finally {
	    	if (cl != null) {
	    		cl.detach();
	    	}
	    }
	    System.out.println("JX - DEBUG - last");
	    return b;
	}
  
	
  	// modifiedd by JX for mr-4576 & ha-4584
  	public void transformClass(CtClass cl) {}
  	//public void transformMethod(CtClass cl, CtBehavior method) {} //implement in difficult application

}


