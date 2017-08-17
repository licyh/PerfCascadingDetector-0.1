package da.tagging;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import LogClass.LogType;
import da.graph.HappensBeforeGraph;
import da.graph.Pair;




public class JobTagger {

	HappensBeforeGraph hbg;
	
	
	
	public JobTagger(HappensBeforeGraph hbg) {
		this.hbg = hbg;
	}
	
	
	/**
	 * findJobIdentity - look backward to find its fathers
	 * Note: find the Job Entry/API (RPC handler Enter / Event hander Enter / Thread Create / Process Create)
	 */
	public void findJobIdentity(int nodeIndex) {
	    BitSet traversedNodes = new BitSet();  	//tmp var. set of traversed nodes for a single code snippet, e.g, event handler
		System.out.println("JX - INFO - findJobIdentity for " + hbg.getPrintedIdentity(nodeIndex));
		dfsTraversing(nodeIndex, traversedNodes);
	}
	
	
    public void dfsTraversing( int x, BitSet traversedNodes ) {
    	if ( isConnection(x) )
    		 System.out.println("JX - DEBUG - path: " + hbg.getPrintedIdentity(x));
    	//traversedNodes.set( x );
    	
    	//termination condition
    	if (hbg.getNodeTID(x).equals("1"))
    		return;
    	if (hbg.getBackEdge().get(x).size() <= 0)
    		return;
    	
    	if ( !isEnter(x) ) {
    		int y = x-1;  //so that is, upward only on its thread, without considering thread.join/future.get, but should consider xxxEnter etc.
    		if ( !traversedNodes.get(y) ) {
    			dfsTraversing( y, traversedNodes );
    		}
    	}
    	else {
    		//System.out.println("JX - INFO - meet a Enter: " + hbg.getPrintedIdentity(x) );
            List<Pair> list = hbg.getBackEdge().get(x);
            int flag = 0;
            for (Pair pair: list) {
            	int y = pair.destination;
            	if ( !traversedNodes.get(y) && isMatched(x, y) ) {
            		if (++flag > 1) {
            			System.out.println("JX - ERROR - JobTagger: Many creations for " + hbg.getPrintedIdentity(x));
            			return;
            		}
            		dfsTraversing( y, traversedNodes );
            	}
            }        
    	}    	
    }
    
    
    public boolean isConnection(int index) {
    	if ( isEnter(index) 
    			|| hbg.getNodeOPTY(index).equals( LogType.EventHandlerEnd.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.EventProcExit.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.MsgProcExit.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.ThdExit.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.EventHandlerCreate.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.EventCreate.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.MsgSending.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.ThdCreate.name() )
    			)
    		return true;
    	return false;
    }
    
    public boolean isEnter(int index) {
    	if ( hbg.getNodeOPTY(index).equals( LogType.EventHandlerBegin.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.EventProcEnter.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.MsgProcEnter.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.ThdEnter.name() )
    			) {
    		return true;
    	}
    	return false;
    }
	
    
    public boolean isMatched(int index, int father) {
    	if ( hbg.getNodeOPTY(index).equals( LogType.EventHandlerBegin.name() )) {
    		return hbg.getNodeOPTY(father).equals( LogType.EventHandlerCreate.name() );
    	}
    	else if ( hbg.getNodeOPTY(index).equals( LogType.EventProcEnter.name() )) {
    		return hbg.getNodeOPTY(father).equals( LogType.EventCreate.name() );
    	}
    	else if ( hbg.getNodeOPTY(index).equals( LogType.MsgProcEnter.name() )) {
    		return hbg.getNodeOPTY(father).equals( LogType.MsgSending.name() );
    	}
    	else if ( hbg.getNodeOPTY(index).equals( LogType.ThdEnter.name() )) {
    		return hbg.getNodeOPTY(father).equals( LogType.ThdCreate.name() );
    	}
    	return false;
    }
}
