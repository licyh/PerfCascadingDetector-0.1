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

	Sink sink;
	
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
	
	public void setSink(Sink sink) {
		this.sink = sink;
	}
	
	/************************************************************
	 * Core
	 ************************************************************/
	
    public ArrayList<Integer> identifyContentionResources(int beginIndex, int endIndex) {
    	ArrayList<Integer> resources = new ArrayList<Integer>();   //set of lock nodes for a single target code snippet
    	
    	// traversing for getting ImmediateBugs & Locks inside the executed code 
    	BitSet traversedNodes = new BitSet();
    	traversedNodes.clear();
    	  	
    	//debug
    	System.out.println("JX - INFO - close or not?: " + hbg.getReachSet().get(beginIndex).get(endIndex));
    	
    	
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
    	
    	if ( !Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-4088") 
    			&& !(Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-2705")&&(sink.getID().equals("1"))) 
    			)
    	if ( hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()) ) {
    		resources.add( x );
    	}
    	
    	// tmp
    	if ( Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-4088") 
    			|| Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-2705") )
    	if ( hbg.getNodeOPTY(x).equals(LogType.EventProcEnter.name())
    			) {
    		resources.add( x );
    	}
    	
    	
    	if (direction == 1)
        for (Pair pair: hbg.getEdge().get(x)) {
        	int y = pair.destination;
        	
        	if ( Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-4088") 
        			|| Benchmarks.resolveBugId(hbg.getTargetDir()).equals("mr-2705")&&sink.getID().equals("1") ) {
        		if ( !traversedNodes.get(y) //&& hbg.getReachSet().get(y).get(endIndex) 
            			)
            		dfsTraversing( y, direction, endIndex, traversedNodes, resources );
        	}
        	else {
        		if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) 
            			)
            		dfsTraversing( y, direction, endIndex, traversedNodes, resources );
        	}
        	
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
    
	
	
	
    public boolean validateChain(LoopBug loopbug) {
    	int cascadingLevel = loopbug.getCascadingLevel();
    	ArrayList<Integer> cascadingChain = loopbug.getCascadingChain();
    	//newly added
    	if (cascadingLevel <= 1) return true;
    	
    	HashSet<String> alreadyOwn = new HashSet<String>();
    	for (int x: cascadingChain) {
    		if ( ag.isContentionResource(x) )
    			alreadyOwn.add( ag.getCRvalue(x) );
    	}
    	
    	
    	//DEBUG
    	int node = loopbug.getNodeIndex();
    	boolean DEBUG = hbg.lastCallstack(node).contains("org.apache.hadoop.hdfs.server.blockmanagement.BlockManager-processReport-1730");
		if (DEBUG)
			System.out.println("JX - DEBUG - checkChain: own-" + alreadyOwn);
    	
    	
    	//System.out.println("JX - DEBUG - checkChain: own.size()=" + own.size());
    	for (int i = 0; i < cascadingChain.size(); i++)
    		if (i%2==1 || i==cascadingChain.size()-1) {    //means: loop -> 1:lock(HERE) <-> lock -> 3:LOCK(HERE) <-> lock -> 5:LOCK(HERE) <-> 6:LOCK(HERE)
    			int cr = cascadingChain.get(i);
    			if (!logInfo.getOuterResources().containsKey(cr)) continue;
    			if (logInfo.getOuterResources().get(cr) == null) continue;
    			
    			if (DEBUG)
    				System.out.println("JX - DEBUG - checkChain: Node-" + hbg.getPrintedIdentity(cr));
    			for (String crCode: logInfo.getOuterResources().get(cr)) {
    				if (crCode.equals( ag.getCRvalue(cr) )) continue;  //yes
    				if (DEBUG)
        				System.out.println("JX - DEBUG - checkChain: outerCRs-" + crCode);
    				if (alreadyOwn.contains(crCode))
    					return false;
    			}
    		}
    	return true;
    }
    
   
    /**
     * 
     */
    public boolean outerCRsContains(int crX, int cr) {
    	if ( !logInfo.getOuterResources().containsKey(crX) //|| logInfo.getOuterResources().get(crX) == null	
    			) return false;
    	Set<String> outerCRs = logInfo.getOuterResources().get(crX);
    	
    	return outerCRs.contains( ag.getCRvalue(cr) );
    }

}






