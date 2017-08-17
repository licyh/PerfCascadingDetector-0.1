package ds;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.text.Checker;
import com.text.TextFileReader;

import LogClass.LogType;
import dm.Transformer;
import dm.util.MethodUtil;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import sa.wala.WalaUtil;

public class Transformers {
	
	// For target code instrumentation
	List<String> classesForInst = new ArrayList<String>();
	List<String> methodsForInst = new ArrayList<String>();
	List<String> linesForInst  = new ArrayList<String>();
	List<String> tagsForInst  = new ArrayList<String>();
	//List<Integer> flagsForInst = new ArrayList<Integer>();
  
	//Checker checker = new Checker();
	
	
	public Transformers() {
	    //read dynamic points
	    readTargets();
		//checker.addCheckFile("resource/dynamicpoints", true);
	}
	
	
	public void readTargets() {
		TextFileReader reader;
	    String tmpline;
	  
    	reader = new TextFileReader("resource/staticpoints", true);
		while ( (tmpline = reader.readLine()) != null ) {
			String[] strs = tmpline.split("\\s+", 4);
			classesForInst.add( formatClassName(strs[0]) );
			methodsForInst.add( strs[1] );
			linesForInst.add( strs[2] );
			tagsForInst.add( strs[3] );
		}
		reader.close();
		
		// for DEBUG
		System.out.println("JX - INFO - " + classesForInst.size() + " locations are loaded");
		System.out.println("JX - INFO - " + "classesForInst = " + classesForInst);
		System.out.println("JX - INFO - " + "methodsForInst = " + methodsForInst);
		System.out.println("JX - INFO - " + "linesForInst =  " + linesForInst );
	}
	
	
	// tmp
	public String formatClassName(String className) {
		String formalClassName = className;
		if (formalClassName.startsWith("L"))
			formalClassName = formalClassName.substring(1);
		formalClassName = formalClassName.replaceAll("/", ".");
		return formalClassName;
	}
	
		

	public void transformClassForDynamicPoints(CtClass cl) {
		String className = cl.getName();
        //if ( className.contains("DataStreamer") ) System.out.println("JX - DEBUG - Targetcode: " + cl.getName() );
		if ( !classesForInst.contains(className) ) return;
		CtBehavior[] methods = cl.getDeclaredBehaviors();
		
	    for (CtBehavior method : methods) {
	        if ( method.isEmpty() ) continue;
	        // traverse all locations for instrumentation
	        String methodName = method.getName();
	        MethodUtil methodUtil = new MethodUtil(method);
	        
	        for (int i = 0; i < classesForInst.size(); i++) {
	    	    if ( classesForInst.get(i).equals(className)
	    	    		&& methodsForInst.get(i).equals(methodName) 
	    	    		) {
	    	    	//System.out.println("JX - DEBUG - targetcode: " + cl.getName() + "." + method.getName() + method.getSignature());
	    			int lineNumber = Integer.parseInt( linesForInst.get(i) );
	    			methodUtil.insertAt(lineNumber, getInstCodeStr(LogType.DynamicPoint, i), LogType.DynamicPoint.name());
	    		}
	    	}
	    }//end-outer for
	}
		  
		  	  
    
    public String getInstCodeStr(LogType logType) {
    	return getInstCodeStr(logType, 0);
    }
    
    public String getInstCodeStr(LogType logType, int flag) {
    	String codestr = "";
    	String logMethod = "LogClass._DM_Log.log_" + logType.name();
    	
    	if (logType == LogType.DynamicPoint) {
    		String value = classesForInst.get(flag) + " " + methodsForInst.get(flag) + " " + linesForInst.get(flag) + " " + tagsForInst.get(flag);
    		codestr = logMethod + "(\"" + value + "\");";
    	} else {
    		
    	}
    
    	return codestr;
    }    
    
    /*
    	public String getInstCodeStr(LogType logType) {
    	return getInstCodeStr(logType, 0);
    }
    
    public String getInstCodeStr(LogType logType, int flag) {
    	String codestr = "";
    	String logMethod = "LogClass._DM_Log.log_" + logType.name();
    	
    	if (logType == LogType.EventHandlerBegin) {
    		switch (flag) {
			case 1:
	            codestr = "String opValue_tmp1 = \"xx\";"
	            		+ "if ($_ instanceof org.apache.hadoop.mapred.KillJobAction) {"
	            		+ "    opValue_tmp1 = ((org.apache.hadoop.mapred.KillJobAction) $_).getJobID().toString();"
	            		+ "}"
	            		+ "else if ($_ instanceof org.apache.hadoop.mapred.KillTaskAction) {"
	            		+ "    opValue_tmp1 = ((org.apache.hadoop.mapred.KillTaskAction) $_).taskId.getJobID().toString();"
	            		+ "}"
	            		+ logMethod + "(opValue_tmp1);";
                break;
			case 2:
				codestr = "String opValue_tmp1 = \"xx\";"
	            		+ "opValue_tmp1 = ((org.apache.hadoop.mapred.TaskTracker$TaskInProgress) $_).getTask().getJobID().toString();"
	            		+ logMethod + "(opValue_tmp1);";
				break;
			default:
	    		codestr = logMethod + "(\"xx\");";
    		}
    	}
    	else if (logType == LogType.EventHandlerEnd) {
    		codestr = logMethod + "(\"xx\");";
    	} else {
    		
    	}
    
    	return codestr;
    }
     */
    
    
    
    
  
}















