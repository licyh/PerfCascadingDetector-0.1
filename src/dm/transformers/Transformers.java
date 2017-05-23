package dm.transformers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dm.Transformer;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

public class Transformers {
	
	// For target code instrumentation
	List<String> classesForInst = new ArrayList<String>();
	List<String> methodsForInst = new ArrayList<String>();
	List<String> linesForInst  = new ArrayList<String>();
	List<String> typesForInst  = new ArrayList<String>();
	List<Integer> flagsForInst = new ArrayList<Integer>();
	String instBegin = "";
	String instEnd = "";
  
	// For all loop instrumentation
	HashMap<String, Integer[]> looplocations = new HashMap<String, Integer[]>();
	
	// For large loop instrumentation
	List<String> largeloop_classesForInst = new ArrayList<String>();
	List<String> largeloop_methodsForInst = new ArrayList<String>();
	List<String> largeloop_linesForInst  = new ArrayList<String>();
	List<String> largeloop_typesForInst  = new ArrayList<String>();
	List<Integer> largeloop_flagsForInst = new ArrayList<Integer>();
	String largeloop_instBegin = "";
	String largeloop_instCenter = "";
	//String largeloop_instEnd = "";  
 
	
	public Transformers() {
	    //read targetlocations & largelooplocations
	    read();
	    //read loop locations' file for instrumentation
	    readLoopLocations();
	}
	
	
	public void read() {
	    InputStream ins;
	    BufferedReader bufreader;
	    String tmpline;
	    try {
	    	// Read target code instrumentation infos
	    	ins = Transformer.class.getClassLoader().getResourceAsStream("resource/targetlocations");
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
	    	ins = Transformer.class.getClassLoader().getResourceAsStream("resource/targetinstructions");
	    	bufreader = new BufferedReader( new InputStreamReader(ins) );
			//bufreader = new BufferedReader( new FileReader("resource/targetinstructions") );
			instBegin = bufreader.readLine();
			instEnd = bufreader.readLine();
			bufreader.close();
			
			// Read loop instrumentation infos
			ins = Transformer.class.getClassLoader().getResourceAsStream("resource/largelooplocations");
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
	    	ins = Transformer.class.getClassLoader().getResourceAsStream("resource/largeloopinstructions");
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
  
	
	public void readLoopLocations() {
		InputStream ins;
	    BufferedReader bufreader;
	    String tmpline;
	    try {
			// Read loop instrumentation infos
			ins = Transformer.class.getClassLoader().getResourceAsStream("resource/looplocations");
	    	bufreader = new BufferedReader( new InputStreamReader(ins) );
	    	String[] nums = bufreader.readLine().trim().split("\\s+");
	    	int num_of_methods = Integer.parseInt( nums[0] );
	    	int num_of_loops = Integer.parseInt( nums[1] );
	        	
			while ( (tmpline = bufreader.readLine()) != null ) {
				String[] strs = tmpline.trim().split("\\s+");
				if ( tmpline.trim().length() > 0 ) {
					String methodsig = strs[0];
					int nloops = Integer.parseInt( strs[1] );
					Integer[] loops = new Integer[nloops];
					for (int i = 0; i < nloops; i++)
						loops[i] = Integer.parseInt( strs[2+i] );
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
		  
		  
		//Added by JX
		  //public void transformClassForCodeSnippets(CtClass cl, CtBehavior[] methods) {}
		  //end-Added
		  
		  //Added by JX
		  public void transformClassForCodeSnippets(CtClass cl) {
			  if ( !classesForInst.contains(cl.getName()) ) return;
			  CtBehavior[] methods = cl.getDeclaredBehaviors();
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
		  
		  
		  public void transformClassForLargeLoops(CtClass cl) {
			  if ( !largeloop_classesForInst.contains(cl.getName()) ) return;
			  CtBehavior[] methods = cl.getDeclaredBehaviors();
			  //System.out.println("JX - @1 - " + cl.getName());
		      for (CtBehavior method : methods) {
		          if ( method.isEmpty() ) continue;
		          //System.out.println("JX - @2 - " + method.getName());
		          // traverse all locations for instrumentation
		          for (int i = 0; i < largeloop_classesForInst.size(); i++) {
		    		  if ( largeloop_classesForInst.get(i).equals(cl.getName())
		    				  && largeloop_methodsForInst.get(i).equals(method.getName()) ) {
		    			  try {
		    				  System.out.println("JX - LargeLoop - IN - " + method.getName());
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
		  
		  public void transformClassForLoops(CtClass cl) throws CannotCompileException {
			  CtBehavior[] methods = cl.getDeclaredBehaviors();
		      for (CtBehavior method : methods) {
		          if ( method.isEmpty() ) continue; 
		          String methodsig = cl.getName() + "." + method.getName() + method.getSignature();  //full signature, like wala's signature
		          //System.out.println( "JX - method signature: " + methodsig );   
		          if ( !looplocations.containsKey( methodsig ) ) continue;
		          
		          System.out.println( "JX - IN - method sig = " + methodsig );  
		          Integer[] loops = looplocations.get( methodsig );
		          
		          // insert loops
		          // for test - TODO - please see
		          for (int i = 0; i < loops.length; i++)
		        	  for (int j = 0; j < loops.length; j++)
		        		  if ( loops[i]+1 == loops[j] ) {
		        			  System.err.println( "JX - WARN - " + i + "&" + j + " for " + methodsig );
		        		  }
		          // end-test
		          for (int i = 0; i < loops.length; i++) {
		        	  int linenumber = loops[i];         //jx: some particular examples: "do { .." OR "while (true) ( .." would became insert inside
		        	  int actualline = method.insertAt(linenumber, false, "LogClass._DM_Log.log_LoopBegin( \"xx\" );");
		        	  if ( linenumber == actualline )    //some particular examples: "do { .." OR "while (true) ( .." would became insert at next line than normal
		        		  method.insertAt( linenumber, "LogClass._DM_Log.log_LoopBegin( \"xx\" );" );
		        	  else {
		        		  // TODO - please see
		        		  System.err.println( "JX - WARN - cannot insert at " + loops[i] + " (actual:" + actualline + ") for " + methodsig );
		        	  }    
		          }
		          
		          /*
		          // insert before
		          for (int i = 0; i < loops.length; i++) {
		        	  method.addLocalVariable( "loop" + i, CtClass.intType );
		        	  method.insertBefore( "loop" + i + " = 0;" );
		          }
		          
		          // insert loops
		          // for test - TODO - please see
		          for (int i = 0; i < loops.length; i++)
		        	  for (int j = 0; j < loops.length; j++)
		        		  if ( loops[i]+1 == loops[j] ) {
		        			  System.err.println( "JX - WARN - " + i + "&" + j + " for " + methodsig );
		        		  }
		          // end-test
		          for (int i = 0; i < loops.length; i++) {
		        	  int linenumber = loops[i] + 1;             //in body
		        	  int actualline = method.insertAt(linenumber, false, "loop" + i + "++;");
		        	  if ( linenumber == actualline ) //some particular examples: "do { .." OR "while (true) ( .." would became insert at next line than normal
		        		  method.insertAt( linenumber, "loop" + i + "++;" );
		        	  else {
		        		  // TODO - please see
		        		  System.err.println( "JX - WARN - cannot insert at " + loops[i] + " (actual:" + actualline + ") for " + methodsig );
		        	  }
		          }
		          
		          // insert after
		          for (int i = 0; i < loops.length; i++) {
		        	  method.insertAfter( "LogClass._DM_Log.log_LoopPrint( \"loop_\" + " + i + " + \"_\" + loop" + i + ");" );
		          }  
		          */        
		      }//end-outer for
		  }
}
