package da.cascading;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import LogClass.LogType;
import da.graph.HappensBeforeGraph;
import da.graph.LogInfoExtractor;
import da.graph.Pair;


public class QueueCase {

	String projectDir;
	
	HappensBeforeGraph hbg;
	
	// Queue-related Info
    LinkedHashMap<Integer, Integer> eventHandlerBlocks;   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> loopBlocks;   // beginIndex -> endIndex
	
    // results
    BugPool bugPool;
    
	
	public QueueCase(String projectDir, HappensBeforeGraph hbg, LogInfoExtractor logInfo) {
		this.projectDir = projectDir;
		this.hbg = hbg;
	    this.eventHandlerBlocks = logInfo.getEventHandlerBlocks(); 
	    this.loopBlocks = logInfo.getLoopBlocks();
	    
	    this.bugPool = new BugPool(this.projectDir, this.hbg);
	}
	
	public void doWork() {
        // Traverse event handlers
		traverseEventHandlers();
		bugPool.printResults(true);
	}
	
	
	public void traverseEventHandlers() {
		List< Entry<Integer, Integer> > list = new ArrayList<>( eventHandlerBlocks.entrySet() );
		//traverse every pair of Event Hanlder (i:length-1..0, j:i-1..0)
		for( int i = list.size()-1; i >= 0; i--) {
		    Entry<Integer, Integer> iEntry = list.get(i);
		    if (iEntry.getValue() == null) continue;
		    int iBeginIndex = iEntry.getKey();
		    int iEndIndex = iEntry.getValue();
		    
		    for (int j = i-1; j >= 0; j--) {
		    	Entry<Integer, Integer> jEntry = list.get(j);
		    	if (jEntry.getValue() == null) continue;
		    	int jBeginIndex = jEntry.getKey();
			    int jEndIndex = jEntry.getValue();
			    if ( !hbg.isSameThread(iBeginIndex, jBeginIndex) ) break;
			    //
			    if ( !hbg.getNodeOPVAL(iBeginIndex).equals("xx")
			    		|| !hbg.getNodeOPVAL(jBeginIndex).equals("xx")
			    		|| !hbg.isSameValue(iBeginIndex, jBeginIndex) 
			    		) {
			    	findBugLoops(jBeginIndex, jEndIndex);
			    	// do we need to consider the reverse case?
			    	//findBugLoops(iBeginIndex, iEndIndex);
			    }
		    }
		    
		}
	}
	
	
	/**
	 * find bug loops between (beginIndex, endIndex)
	 */
	public void findBugLoops(int beginIndex, int endIndex) {
	    // tmp vars
	    BitSet traversedNodes = new BitSet();  	//tmp var. set of traversed nodes for a single code snippet, e.g, event handler
		traversedNodes.clear();
		dfsTraversing(beginIndex, endIndex, traversedNodes);
	}
	
	
    public void dfsTraversing( int x, int endIndex, BitSet traversedNodes ) {
    	traversedNodes.set( x );
    	// find the bug loop
    	if ( hbg.getNodeOPTY(x).equals(LogType.LoopBegin.name()) ) {
    		bugPool.addLoopBug( x, 1 );
    	}

        List<Pair> list = hbg.getEdge().get(x);
        for (Pair pair: list) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) )
        		dfsTraversing( y, endIndex, traversedNodes );
        }
    }
    
	
}
