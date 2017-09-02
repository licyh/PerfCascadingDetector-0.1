package da.tagging;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.text.Checker;
import com.text.TextFileReader;

import LogClass.LogType;
import da.graph.HappensBeforeGraph;
import da.graph.Pair;
import dm.transformers.SourceLine;




public class JobTagger {

	HappensBeforeGraph hbg;
	Map<Integer, String> jobIDs = new HashMap<Integer, String>();  //node index -> its job ID
	
	Checker customizedJobChecker = new Checker();
	
	
	public JobTagger(HappensBeforeGraph hbg) {
		this.hbg = hbg;
		readCustomizedJobs();  //this is completely no need if with strong a hb graph
	}
	
	
	public void readCustomizedJobs() {
		customizedJobChecker.addCheckFile("src/da/tagging/backgroundthreads.txt");	
	}
	

	
	/***************************************************************************
	 * Core
	 ***************************************************************************/
	
	public boolean isSameJobID(int xIndex, int yIndex) {
		return getJobID(xIndex).equals(getJobID(yIndex));
	}
	
	
	public String getJobID(int nodeIndex) {
		//for DEBUG
	    System.out.println("JX - INFO - findJobIdentity for " + hbg.getPrintedIdentity(nodeIndex));
	    if ( !jobIDs.containsKey(nodeIndex) ) {
	    	String ID = findJobID(nodeIndex);
	    	jobIDs.put(nodeIndex, ID);
	    }
	    
	    //System.out.println( "JX - INFO - jobID: " + jobIDs.get(nodeIndex) );
    	return jobIDs.get(nodeIndex);
	}
	
	
	/**
	 * findJobIdentity - look backward to find its fathers
	 * Note: find the Job Entry/API (RPC handler Enter / Event hander Enter / Thread Create / Process Create)
	 */
	public String findJobID(int nodeIndex) {
	    BitSet traversedNodes = new BitSet();  	//tmp var. set of traversed nodes for a single code snippet, e.g, event handler
		
		ArrayList<Integer> pathToRoot = new ArrayList<Integer>();
		dfsTraversing(nodeIndex, traversedNodes, pathToRoot);
		if (pathToRoot.size() <= 0) {
			return "XXXX";        //jx: should not occur, but it can occur if HB graph is incomplete
		}
		
		int jobIndex = -1;
		String jobID = null;
		// Message - RPC/Socket/Event
		for (int i=pathToRoot.size()-1; i>=0; i--) {
			int index = pathToRoot.get(i);
			if ( isMsgEnter(index) ) {
				jobIndex = index;
				jobID = hbg.getNodeOPVAL(index);    		//msg value
				return jobID;
			}
		}
		
		// customized - for missing messages
		for (int i=pathToRoot.size()-1; i>=0; i--) {
			int index = pathToRoot.get(i);
			if ( isCustomized(index) ) {
				jobIndex = index;
				jobID = hbg.getNodeOPVAL(index);    		//msg value?
				return jobID;
			}
		}
		
		for (int i=pathToRoot.size()-1; i>=0; i--) {
			int index = pathToRoot.get(i);
			if ( isThdEnter(index) && !hbg.getNodeTID(index).equals("1") ) {
				jobIndex = index;
				jobID = hbg.getNodeOPVAL(index);    		//msg value?
				return jobID;
			}
		}
		
		for (int i=pathToRoot.size()-1; i>=0; i--) {
			int index = pathToRoot.get(i);
			if ( !hbg.getNodeTID(index).equals("1") ) {
				jobIndex = index;
				jobID = hbg.getNodePIDTID(index);			//pid+tid
				return jobID;
			}
		}
		
		jobIndex = pathToRoot.get(pathToRoot.size()-1);
		jobID = hbg.getNodePIDTID(jobIndex);				//pid+tid
		return jobID;
	}
	
	
	/**
	 * Note: should be only one path, so we use a List for path
	 */
    public void dfsTraversing( int x, BitSet traversedNodes, List<Integer> pathToRoot ) {
    	//if ( isConnection(x) ) {
    	if ( !isGeneral(x) ) {
    		// for DEBUG
    		System.out.println("JX - DEBUG - path: " + hbg.getPrintedIdentity(x));
    		pathToRoot.add(x);
    	}
    	//traversedNodes.set( x );
    	
    	//termination condition
    	if (hbg.getNodeTID(x).equals("1"))
    		return;
    	if (hbg.getBackEdge().get(x).size() <= 0)
    		return;
    	
    	if ( !isEnter(x) ) {
    		int y = x-1;  //so that is, upward only on its thread, without considering thread.join/future.get, but should consider xxxEnter etc.
    		if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(x) ) {
    			dfsTraversing( y, traversedNodes, pathToRoot );
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
            		dfsTraversing( y, traversedNodes, pathToRoot );
            	}
            }        
    	}    	
    }
    
    
    public boolean isGeneral(int index) {
    	if ( hbg.getNodeOPTY(index).equals( LogType.LockRequire.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.LockRelease.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.LoopBegin.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.LoopEnd.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.RWLockCreate.name() )
    			) {
    		return true;
    	}
    	return false;
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
    	if ( hbg.getNodeOPTY(index).equals( LogType.MsgProcEnter.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.EventProcEnter.name() )
    			|| hbg.getNodeOPTY(index).equals( LogType.ThdEnter.name() )
    			) {
    		return true;
    	}
    	return false;
    }

    public boolean isMsgEnter(int index) {
    	if ( hbg.getNodeOPTY(index).equals( LogType.MsgProcEnter.name() )			//RPC handler or Socket accept
    			|| hbg.getNodeOPTY(index).equals( LogType.EventProcEnter.name() )
    			) {
    		return true;
    	}
    	return false;
    }
    
    public boolean isCustomized(int index) {
    	if ( customizedJobChecker.isTarget_contains( hbg.lastCallstack_2(index) ) )
    		return true;
    	return false;
    }
    
    public boolean isThdEnter(int index) {
    	if ( hbg.getNodeOPTY(index).equals( LogType.ThdEnter.name() )
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
