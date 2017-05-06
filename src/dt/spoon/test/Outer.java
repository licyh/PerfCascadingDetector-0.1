package dt.spoon.test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Outer {

	Thread eventHandlingThread = null;
		
		
	    public void method() {
	    	
	      for (int i = 0; i < 10; i++) {	
	    	
	        this.eventHandlingThread = new java.lang.Thread() {
	            public void run() {
	                int event = 1;
	                while ((!(java.lang.Thread.currentThread().isInterrupted()))) {
	                    event = 2;
	                    try {
	                        event = 3;
	                    } catch (java.lang.Throwable t) {
	                    	System.out.println(("Returning, interrupted : " + t));
	                        return ;
	                    }
	                }

	            }
	        };
	      }
	        
	        while (true) {
	    		int a = 0;
	    	}

	    }
		

	
	public void methodB() {
		int k = 0;
		

	      do {
	    	  int a = 0;
	      } while (true);
	      
	}
}
