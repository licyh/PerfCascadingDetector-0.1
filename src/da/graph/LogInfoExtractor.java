package da.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import LogClass.LogType;
import da.cascading.core.Sink;
import da.cascading.core.SinkInstance;



public class LogInfoExtractor {
	
	HappensBeforeGraph hbg;
	
    LinkedHashMap<Integer, Integer> targetCodeBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> lockBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> loopBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    //added for CA
    LinkedHashMap<Integer, Integer> handlerBlocks = new LinkedHashMap<Integer, Integer>(); 
    LinkedHashMap<Integer, Integer> eventHandlerBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> rpcHandlerBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    
    //computed
    List<Sink> sinks = new ArrayList<Sink>();
    LinkedHashMap<Integer, Integer> handlerThreads = new LinkedHashMap<Integer, Integer>();    //No.handlerBlock->No.handlerBlock    //for threadpool#submit/execute
    LinkedHashMap<Integer, Integer> rpcHandlerThreads = new LinkedHashMap<Integer, Integer>();
   
    
    
    public LogInfoExtractor(HappensBeforeGraph hbg) {
    	this.hbg = hbg;
    	doWork();
    }
    
    public void doWork() {
    	System.out.println("JX - INFO - LogInfoExtractor: doWork...");
    	extractLogInfo();
    	computeLogInfo();   //including computeHandlerInfo();
    }
    
    
    /**
     * APIs
     */
    public List<Sink> getSinks() {
    	return this.sinks;
    }
    
    public LinkedHashMap<Integer, Integer> getTargetCodeBlocks() {
    	return this.targetCodeBlocks;
    }
    
    public LinkedHashMap<Integer, Integer> getLockBlocks() {
    	return this.lockBlocks;
    }
    
    public LinkedHashMap<Integer, Integer> getLoopBlocks() {
    	return this.loopBlocks;
    }
    
    
    public LinkedHashMap<Integer, Integer> getHandlerBlocks() {
    	return this.handlerBlocks;
    }
    //more computing
    public LinkedHashMap<Integer, Integer> getHandlerThreads() {
    	return this.handlerThreads;
    }
    
    
    public LinkedHashMap<Integer, Integer> getEventHandlerBlocks() {
    	return this.eventHandlerBlocks;
    }
    
    
    public LinkedHashMap<Integer, Integer> getRPCHandlerBlocks() {
    	return this.rpcHandlerBlocks;
    }
    //more computing
    public LinkedHashMap<Integer, Integer> getRPCHandlerThreads() {
    	return this.rpcHandlerThreads;
    }
    
    
    //for locks
    /**
     * @param a - the begin index of lock A
     * @param b - the begin index of lock B
     */
    public boolean lockContains(int a, int b) {
    	if (lockBlocks.get(a) == null || lockBlocks.get(b) == null) return false;
    	if ( !hbg.isSameThread(a, b) ) return false;        // is it necessary?
    	if (a < b && lockBlocks.get(a) > lockBlocks.get(b))
    		return true;
		return false;
    }
    
    
    
    
	/******************************************************************
	 * Core
	 *****************************************************************/

    
    public void extractLogInfo() {
        
        extractTargetCodeInfo();
        extractLockInfo();
        extractLoopInfo();
        //add
        //added for threadpool for CA
        extractHandlerInfo();
        //added for queue in MR
        extractEventHandlerInfo();
    	//add RPC event handlers
        extractRPCHandlerInfo();
        
    	// for test
    	Set<String> tmpset = new HashSet<String>();
    	
    	tmpset.clear();
    	for (Integer index: targetCodeBlocks.keySet()) {
    		tmpset.add( hbg.lastCallstack(index) );
    	}
    	int ntargetsInSourceCode = tmpset.size();
    	
    	tmpset.clear();
    	for (Integer index: eventHandlerBlocks.keySet()) {
    		tmpset.add( hbg.lastCallstack(index) );
    	}
    	int nEventHandlersInSourceCode = tmpset.size();
    	
    	tmpset.clear();
    	for (Integer index: lockBlocks.keySet()) {
    		tmpset.add( hbg.lastCallstack(index) );
    	}
    	int nlocksInSourceCode = tmpset.size();
    	
    	tmpset.clear();
    	for (Integer index: loopBlocks.keySet()) {
    		tmpset.add( hbg.lastCallstack(index) );
    	}
    	int nloopsInSourceCode = tmpset.size();
    			
    	// for test
    	System.out.println("#targetCodeBlocks = " + targetCodeBlocks.size() + " -> ntargetsInSourceCode=" + ntargetsInSourceCode);
    	System.out.println("#eventHandlerBlocks = " + eventHandlerBlocks.size() + " -> nEventHandlersInSourceCode=" + nEventHandlersInSourceCode);
    	System.out.println("#lockBlocks = " + lockBlocks.size() + " -> nlocksInSourceCode=" + nlocksInSourceCode);
    	System.out.println("#loopBlocks = " + loopBlocks.size() + " -> nloopsInSourceCode=" + nloopsInSourceCode);
    
    	// build the relationship between locks and loops
    	// JX - it seems NO NEED
    }
    
    
    public void computeLogInfo() {
    	computeTargetCodeInfo();
    	computeHandlerInfo();
    	computeRPCHandlerInfo();
    }
    
    
    /***************************************************************************
     * 
     ***************************************************************************/
    /**
     * Get nodes with the specified types, like TargetCodeBegin&TargetCodeEnd, LoopBegin&LoopEnd, .. 
     */
    public ArrayList<Integer> getTypedNodes(String ... types) {
    	ArrayList<Integer> items = new ArrayList<Integer>();
    	// scan all nodes
    	for (int i = 0; i < hbg.getNodeList().size(); i++) {
    		String opty = hbg.getNodeOPTY(i);
    		for (String type: types)
    			if (opty.equals(type)) {
    				items.add( i );
    				break;
    			}
    	}
    	return items;
    }
    
    
    public void extractTargetCodeInfo() {
    	// Get all TargetCodeBegin&TargetCodeEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.TargetCodeBegin.name(), LogType.TargetCodeEnd.name());
    	
    	// Handle target codes
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(LogType.TargetCodeBegin.name()) ) continue;
    		
			int flag = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				if ( hbg.getNodeOPTY( jIndex ).equals(LogType.TargetCodeBegin.name()) ) flag ++;
				else flag --;
				if (flag == 0) {
					targetCodeBlocks.put( iIndex, jIndex );
					break;
				}
			}
			if ( !targetCodeBlocks.containsKey( iIndex ) )
				targetCodeBlocks.put( iIndex, null );
    	}
    }
        
    
    public void extractLockInfo() {
    	// Get all EventHandlerBegin&EventHandlerEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.LockRequire.name(), LogType.LockRelease.name());
    
    	// Handle lock codes
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(LogType.LockRequire.name()) ) continue;
    		
    		String opval = hbg.getNodeOPVAL(iIndex);
			int reenter = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				// modified: bug fix
				if ( hbg.getNodeOPTY(jIndex).equals(LogType.LockRequire.name()) && hbg.getNodeOPVAL(jIndex).equals(opval) )  
					reenter ++;
				if ( hbg.getNodeOPTY(jIndex).equals(LogType.LockRelease.name()) && hbg.getNodeOPVAL(jIndex).equals(opval) ) {
					reenter --;
					if (reenter == 0) {
						lockBlocks.put(iIndex, jIndex);
						break;
					}
				}
				// end - modified
			}
			if ( !lockBlocks.containsKey(iIndex) ) 
				lockBlocks.put(iIndex, null);
    	}
    
    	/** old method, it is definitely right
    	for (int i = 0; i < hbg.getNodeList().size(); i++) {
    		// find out all Lock code blocks
    		if ( hbg.getNodeOPTY(i).equals("LockRequire") ) {
    			String opval = hbg.getNodeOPVAL(i);
    			int reenter = 1;
    			for (int j = i+1; j < hbg.getNodeList().size(); j++) {
    				if ( !hbg.isSameThread(j, i) ) break;
    				// modified: bug fix
    				if ( hbg.getNodeOPTY(j).equals("LockRequire") && hbg.getNodeOPVAL(j).equals(opval) )  
    					reenter ++;
    				if ( hbg.getNodeOPTY(j).equals("LockRelease") && hbg.getNodeOPVAL(j).equals(opval) ) {
    					reenter --;
    					if (reenter == 0) {
    						lockBlocks.put(i, j);
    						break;
    					}
    				}
    				// end - modified
    			}
    			if ( !lockBlocks.containsKey(i) ) 
    				lockBlocks.put(i, null);
    		}
    	}
    	*/
    }


    public void extractLoopInfo() {
    	// Get all LoopBegin&LoopEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.LoopBegin.name(), LogType.LoopEnd.name());
    	
    	// Handle loop codes
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(LogType.LoopBegin.name()) ) continue;
    		
			int flag = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				if ( hbg.getNodeOPTY( jIndex ).equals(LogType.LoopBegin.name()) ) flag ++;
				else flag --;
				if (flag == 0) {
					loopBlocks.put( iIndex, jIndex );
					break;
				}
			}
			if ( !loopBlocks.containsKey( iIndex ) )
				loopBlocks.put( iIndex, null );
		
    	}
    }

    
    public void extractEventHandlerInfo() {
    	// Get all EventHandlerBegin&EventHandlerEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.EventHandlerBegin.name(), LogType.EventHandlerEnd.name());
    	
    	// Handle loop codes
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(LogType.EventHandlerBegin.name()) ) continue;
    			
			int flag = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				if ( hbg.getNodeOPTY( jIndex ).equals(LogType.EventHandlerBegin.name()) ) flag ++;
				else flag --;
				if (flag == 0) {
					eventHandlerBlocks.put( iIndex, jIndex );
					break;
				}
			}
			if ( !eventHandlerBlocks.containsKey( iIndex ) )
				eventHandlerBlocks.put( iIndex, null );
    	}
    }
    
    
    /**
     * only for threadpool's thread now.
     */
    public void extractHandlerInfo() {
        // threadpool's event - ThdEnter & ThdExit
    	// Get all ThdEnter&ThdExit nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.ThdEnter.name(), LogType.ThdExit.name());
    
    	// Handle lock codes
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(LogType.ThdEnter.name()) ) continue;
    		
    		String opval = hbg.getNodeOPVAL(iIndex);
			int reenter = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				// modified: bug fix
				if ( hbg.getNodeOPTY(jIndex).equals(LogType.ThdEnter.name()) && hbg.getNodeOPVAL(jIndex).equals(opval) )  
					reenter ++;
				if ( hbg.getNodeOPTY(jIndex).equals(LogType.ThdExit.name()) && hbg.getNodeOPVAL(jIndex).equals(opval) ) {
					reenter --;
					if (reenter == 0) {
						handlerBlocks.put(iIndex, jIndex);
						break;
					}
				}
				// end - modified
			}
			if ( !handlerBlocks.containsKey(iIndex) ) 
				handlerBlocks.put(iIndex, null);
    	}
    }
    
    
    /**
     * for now, in fact, including including RPC, socket           #may need to differentiate
     */
    public void extractRPCHandlerInfo() {
        // threadpool's event - ThdEnter & ThdExit
    	// Get all MsgProcEnter&MsgProcExit nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.MsgProcEnter.name(), LogType.MsgProcExit.name());
    
    	// Handle lock codes
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(LogType.MsgProcEnter.name()) ) continue;
    		
    		String opval = hbg.getNodeOPVAL(iIndex);
			int reenter = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				// modified: bug fix
				if ( hbg.getNodeOPTY(jIndex).equals(LogType.MsgProcEnter.name()) && hbg.getNodeOPVAL(jIndex).equals(opval) )  
					reenter ++;
				if ( hbg.getNodeOPTY(jIndex).equals(LogType.MsgProcExit.name()) && hbg.getNodeOPVAL(jIndex).equals(opval) ) {
					reenter --;
					if (reenter == 0) {
						rpcHandlerBlocks.put(iIndex, jIndex);
						break;
					}
				}
				// end - modified
			}
			if ( !rpcHandlerBlocks.containsKey(iIndex) ) 
				rpcHandlerBlocks.put(iIndex, null);
    	}
    }
    
    
    
    /***********************************************************************
     * Compute more useful information
     **********************************************************************/
    
    public void computeTargetCodeInfo() {
    	//for now just one sink
    	Sink sink = new Sink();
    	for (int beginindex: targetCodeBlocks.keySet() ) {
    		if ( targetCodeBlocks.get(beginindex) == null ) continue;
    		int endindex = targetCodeBlocks.get(beginindex);
    		
    		SinkInstance sinkInstance = new SinkInstance(beginindex, endindex);
    		sink.addInstance(sinkInstance);
    	}
    	this.sinks.add(sink);
    }
    
	/**
	 * //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
	 */
	public void computeHandlerInfo() {
		List<Integer> list = new ArrayList<>( handlerBlocks.keySet() );
		
		int numHandlers = 0;
		for (int i = 0; i < list.size(); i++) {     //note: cann't add "if (handlerBlocks.get(list.get(i)) == null) continue;", this will cause inaccurate
			if ( i>0 && !hbg.isSameThread(list.get(i), list.get(i-1)) ) {
				if (numHandlers > 1) {  //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
					handlerThreads.put(i-numHandlers, i-1);
					System.out.println("numHandlers = " + numHandlers + ", " + hbg.getNodePIDTID(list.get(i-1)));
				}
				numHandlers = 0; 
			}
			if ( i == list.size()-1 ) {
				if (numHandlers > 1) {  //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
					handlerThreads.put(i-numHandlers, i-1);
					System.out.println("numHandlers = " + numHandlers + ", " + hbg.getNodePIDTID(list.get(i-1)));
				}
				numHandlers = 0;
			}
			numHandlers ++;
		}
	}
	
	
	/**
	 * //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
	 */
	public void computeRPCHandlerInfo() {
		List<Integer> list = new ArrayList<>( rpcHandlerBlocks.keySet() );
		
		int numHandlers = 0;
		for (int i = 0; i < list.size(); i++) {     //note: cann't add "if (handlerBlocks.get(list.get(i)) == null) continue;", this will cause inaccurate
			if ( i>0 && !hbg.isSameThread(list.get(i), list.get(i-1)) ) {
				if (numHandlers > 1) {  //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
					rpcHandlerThreads.put(i-numHandlers, i-1);
					System.out.println("numHandlers = " + numHandlers + ", " + hbg.getNodePIDTID(list.get(i-1)));
				}
				numHandlers = 0; 
			}
			if ( i == list.size()-1 ) {
				if (numHandlers > 1) {  //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
					rpcHandlerThreads.put(i-numHandlers, i-1);
					System.out.println("numHandlers = " + numHandlers + ", " + hbg.getNodePIDTID(list.get(i-1)));
				}
				numHandlers = 0;
			}
			numHandlers ++;
		}
	}
    
}
