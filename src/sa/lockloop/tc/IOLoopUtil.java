package sa.lockloop.tc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.benchmark.Benchmarks;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.text.Checker;

import sa.rpc.RPCFinder;
import sa.wala.WalaAnalyzer;
import sa.wala.WalaUtil;

public class IOLoopUtil {
	
	List<String> rpcMethodSigs = new ArrayList<String>();
	//List<String> ioMethodPrefixes = new ArrayList<String>();
	Set<String> tmpTcOps = new TreeSet<String>();
	
	Path jarsDirPath;               //ie, xxx/JXCascading-detector/src/sa/res/ha-4584
	String systemName;             //by default;
	Checker rpcChecker = new Checker();
	Checker ioChecker = new Checker();
	
	
	
	public IOLoopUtil(String jarsDir) {
		this.jarsDirPath = Paths.get(jarsDir);
		this.systemName = Benchmarks.resolveSystem(jarsDir);
		doWork();
	}
	
	public void doWork() {
	    readRPCsAndIOs();
	}
	
	
	public void readRPCsAndIOs() {	
		Path rpcfile = null;
		Path commonIOfile = jarsDirPath.getParent().resolve( "io/io.txt" );  //ie, JXCascading-detector/src/sa/res/io/io.txt
		Path iofile = null;
		//System.out.println("JX - DEBUG - path: " + commonIOfile);
		
		switch ( systemName ) {
			case Benchmarks.MR:
		  		rpcfile = jarsDirPath.resolve("mr_rpc.txt");
		  		iofile  = jarsDirPath.getParent().resolve("io/mr_io.txt");
				break;
		  	case Benchmarks.HD:
		  		rpcfile = jarsDirPath.resolve("hd_rpc.txt");
		  		iofile  = jarsDirPath.getParent().resolve("io/hd_io.txt");
				break;
		  	case Benchmarks.HB:
		  		rpcfile = jarsDirPath.resolve("hb_rpc.txt");
		  		iofile  = jarsDirPath.getParent().resolve("io/hb_io.txt");
				break;
		  	default:
				break;
		}  
		
		rpcChecker.addCheckFile( rpcfile );
		ioChecker.addCheckFile( commonIOfile );
		ioChecker.addCheckFile( iofile );
	}
	
	
	
	/**
	 * is TimeConsuming SSA or not
	 */
	public boolean isTimeConsumingSSA(SSAInstruction ssa) {
		
		// it must be a invoke instruction
	    if (ssa instanceof SSAInvokeInstruction) {  //SSAAbstractInvokeInstruction
	    	SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
	    	String className = WalaUtil.formatClassName( invokessa.getDeclaredTarget().getDeclaringClass().getName().toString() );
			String methodName = invokessa.getDeclaredTarget().getName().toString();
			String signature = invokessa.getDeclaredTarget().getSignature().toString();
			//System.out.println("JX - DEBUG - className: " + className);
			//System.out.println("JX - DEBUG - methodName: " + methodName);
			//System.out.println("JX - DEBUG - signature: " + signature);
			
	    	// is RPC or not?
			if ( invokessa.isDispatch() 
		    	 || invokessa.isSpecial() 
		    	 || invokessa.isStatic() 
					) {
				if ( rpcChecker.isSplitTarget(1, className, 2, methodName) 
						//|| rpcChecker.isSplitTarget(0, className, 2, methodName)  //may bring in some not rpc just at server -> server
						) {
			      	tmpTcOps.add( "RPC Call: " + signature );
					return true;
				}
				  
			}
			
	    	// is I/O or not?
	    	if ( invokessa.isDispatch() 
	    		 || invokessa.isSpecial() 
	    		 || invokessa.isStatic() 
	    			) {
	    		if ( !methodName.equals("<init>") 
	    			 && ioChecker.isTarget(signature) ) {     		  
	    				tmpTcOps.add( signature );
	    				return true;
	    			}	 
	    	}
	    }
	    else {
	    	//TODO - if need be
	    }
	    
	    return false;
	}
	  
	
	/**
	 * is Real IO SSA or not
	 */
	public boolean isIOSSA(SSAInstruction ssa) {
		//filter rest I/Os that are not time-consuming for avoiding to get into,   #this is also can be removed
		if (ssa instanceof SSAInvokeInstruction) {
			SSAInvokeInstruction invokessa = (SSAInvokeInstruction) ssa;
			String sig = invokessa.getDeclaredTarget().getSignature().toString();
			if ( sig.startsWith("java.io.")
				 || sig.startsWith("java.nio.")
				 || sig.startsWith("java.net.")
				 || sig.startsWith("java.rmi.")
				 || sig.startsWith("java.sql.")
					)
				return true;
		}
		return false;
	}
	 
	  
	  
	// for test
	public void printTcOperationTypes() {
		System.out.println("\n JX - INFO - printTcOperationTypes");
		System.out.println("#types = " + tmpTcOps.size());
		// test
		for (String str: tmpTcOps) {
			System.out.println(str);
		}
	}
	  
	  
}
