package da.cascading.core;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import com.benchmark.Benchmarks;

import LogClass.LogType;
import da.cascading.LoopBug;
import da.graph.AccidentalHBGraph;
import da.graph.HappensBeforeGraph;
import da.graph.LogInfoExtractor;
import da.graph.Pair;
import da.tagging.JobTagger;

public class CascadingUtil {

	String projectDir;
	HappensBeforeGraph hbg;
	AccidentalHBGraph ag;
	LogInfoExtractor logInfo;
	
	public CascadingUtil(String projectDir, HappensBeforeGraph hbg, AccidentalHBGraph ag, LogInfoExtractor logInfo) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		this.ag = ag;
		this.logInfo = logInfo;
	}
	
	
	
	/************************************************************
	 * Core
	 ************************************************************/
	
    public ArrayList<Integer> identifyContentionResources(int beginIndex, int endIndex) {
    	ArrayList<Integer> resources = new ArrayList<Integer>();   //set of lock nodes for a single target code snippet
    	
    	// traversing for getting ImmediateBugs & Locks inside the executed code 
    	BitSet traversedNodes = new BitSet();
    	traversedNodes.clear();
    	  	
		//tmply add, only for ca-6744
    	if ( Benchmarks.resolveBugId(hbg.getTargetDir()) != null && 
    			( Benchmarks.resolveBugId(hbg.getTargetDir()).equals("ca-6744")
    					//|| Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-4088")
    					)) {
    		//System.out.print("JX - DEBUG - LockCass: enter ca-6744");
        	scanAndDfs(beginIndex, endIndex, traversedNodes, resources);	
    	}
    	else {
    		/*
    		boolean isClosedCycle = hbg.getReachSet().get(beginIndex).get(endIndex);
        	if ( isClosedCycle ) {
        	*/
        		dfsTraversing( beginIndex, 1, endIndex, traversedNodes, resources);  //modified for ca-6744
        	/*
    		}
        	else {
        		System.out.println("JX - WARN - " + "No ClosedCycle: couldn't reach from " + beginIndex + " to " + endIndex);
        		// TODO - for each nodeIndex, do a dfsTraversing
        	}
    		*/
    	}
    		
    	
    	// analyzing Locks that are inside the executed code
    	Set<String> setofinvolvingthreads = new HashSet<String>();
    	for (int index: resources) {
    		setofinvolvingthreads.add( hbg.getNodePIDTID(index) );
    	}
    	System.out.println("this snippet includes " + traversedNodes.cardinality()
    						+ " nodes, #Contention Resources in Sink = " + resources.size() + ", #involving threads = " + setofinvolvingthreads);
    	
    	return resources;
    }
    
    /**
     * @param direction   1(--->) or -1 (<---)
     */
    public void dfsTraversing( int x, int direction, int endIndex, BitSet traversedNodes, ArrayList<Integer> resources) {
    	traversedNodes.set( x );
    	
    	if ( !Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-4088") )
    	if ( hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()) ) {
    		resources.add( x );
    	}
    	
    	// TODO - for Loop - suspected bugs
    	if ( hbg.getNodeOPTY(x).equals(LogType.EventHandlerBegin.name()) ) {
    		resources.add( x );
    	}
    	
    	if (direction == 1)
        for (Pair pair: hbg.getEdge().get(x)) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) //&& hbg.getReachSet().get(y).get(endIndex) 
        			)
        		dfsTraversing( y, direction, endIndex, traversedNodes, resources );
        }
        
    }
    
    
    //tmp
    public void scanAndDfs( int beginIndex, int endIndex, BitSet traversedNodes, ArrayList<Integer> resources ) {
    	for (int i = beginIndex; i <= endIndex; i++) {
    		dfsTraversing2(i, 1, i, traversedNodes, resources);
    	}
    }
    

    /**
     * @param direction   1(--->) or -1 (<---)
     */
    public void dfsTraversing2( int x, int direction, int endIndex, BitSet traversedNodes, ArrayList<Integer> resources) {
    	traversedNodes.set( x );
    	
    	/*
    	if ( hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()) ) {
    		resources.add( x );
    	}
    	*/
    	
    	if ( hbg.getNodeOPTY(x).equals(LogType.ThdEnter.name() )
    			|| hbg.getNodeOPTY(x).equals(LogType.EventProcEnter.name())
    			|| hbg.getNodeOPTY(x).equals(LogType.MsgProcEnter.name())
    			) {
    		resources.add( x );
    	}
    	
    	
    	// Termination Conditions
    	if ( hbg.getNodeOPTY(x).equals(LogType.ThdEnter.name()) ) {
    		System.out.println("JX - debug - Sink: termination at ThdEnter " + hbg.getPrintedIdentity(x) );
    		return;
    	}
    	
    	/*
    	// Non-cascaded loops
    	if ( hbg.getNodeOPTY(x).equals("LoopBegin") ) {
    		// add to bug pool
    		if ( ONLY_LOCK_RELATED_BUGS ) {
    			//Do Nothing
    		} else {
    			addLoopBug( x, 1 );
    		}
    	}
    	*/


    	if (direction == 1)
        for (Pair pair: hbg.getEdge().get(x)) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) )
        		dfsTraversing2( y, direction, endIndex, traversedNodes, resources );
        }
       
        
    	// for needed in the future
        for (Pair pair: hbg.getBackEdge().get(x)) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) ) {
        		dfsTraversing2( y, -1, endIndex, traversedNodes, resources );
        	}
        }
    }
    
	
	
	
    public boolean checkChain(LoopBug loopbug) {
    	int cascadingLevel = loopbug.getCascadingLevel();
    	
    	//newly added
    	if (cascadingLevel <= 1)
    		return true;
    	
    	HashSet<String> own = new HashSet<String>();
    	for (int x: loopbug.getCascadingChain()) {
    		if (hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()))
    			own.add( hbg.getNodePIDOPVAL0(x) );
    	}
    	//System.out.println("JX - DEBUG - checkChain: own.size()=" + own.size());
    	for (int i = 0; i<loopbug.getCascadingChain().size(); i++) {
    		int x = loopbug.getCascadingChain().get(i);
    		if (i%2==1 || i==loopbug.getCascadingChain().size()-1) {
    			if (!logInfo.getOuterLocks().containsKey(x)) continue;
    			
    			for (String each: logInfo.getOuterLocks().get(x)) {
    				if (each.equals( hbg.getNodePIDOPVAL0(x) )) continue;
    				if (own.contains(each))
    					return false;
    			}
    		}
    	}
    	return true;
    }
    
    
    /*
    public boolean checkJobID(int index1, int index2) {
    	//Pruning 2 - Checking job identity - ie, false positive pruning
    	return new JobTagger(this.hbg).isSameJobID(index1, index2);
    }
    */

}






