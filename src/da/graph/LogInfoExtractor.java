package da.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import LogClass.LogType;
import da.cascading.core.Sink;
import da.cascading.core.SinkInstance;



public class LogInfoExtractor {
	
	HappensBeforeGraph hbg;
	AccidentalHBGraph ag;
	
    LinkedHashMap<Integer, Integer> targetCodeBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> lockBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> loopBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    //added for CA
    LinkedHashMap<Integer, Integer> handlerBlocks = new LinkedHashMap<Integer, Integer>(); 		  // beginIndex -> endIndex : for Events handled by thread pool imho
    LinkedHashMap<Integer, Integer> eventHandlerBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex : for xxx
    LinkedHashMap<Integer, Integer> rpcHandlerBlocks = new LinkedHashMap<Integer, Integer>();     // beginIndex -> endIndex : for RPC/Socket
    
    //computed
    List<Sink> sinks = new ArrayList<Sink>();
    LinkedHashMap<Integer, Integer> handlerThreads = new LinkedHashMap<Integer, Integer>();    //No.handlerBlock->No.handlerBlock    //for threadpool#submit/execute
    LinkedHashMap<Integer, Integer> eventHandlerThreads = new LinkedHashMap<Integer, Integer>();
    LinkedHashMap<Integer, Integer> rpcHandlerThreads = new LinkedHashMap<Integer, Integer>();
   
    HashMap<Integer, HashSet<String>> outerResources = new HashMap<Integer, HashSet<String>>();    // lock -> ourter locks
    
    
    
    public LogInfoExtractor(HappensBeforeGraph hbg, AccidentalHBGraph ag) {
    	this.hbg = hbg;
    	this.ag = ag;
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
    //more computing
    public LinkedHashMap<Integer, Integer> getEventHandlerThreads() {
    	return this.eventHandlerThreads;
    }
    
    public LinkedHashMap<Integer, Integer> getRPCHandlerBlocks() {
    	return this.rpcHandlerBlocks;
    }
    //more computing
    public LinkedHashMap<Integer, Integer> getRPCHandlerThreads() {
    	return this.rpcHandlerThreads;
    }
    
    
    public HashMap<Integer, HashSet<String>> getOuterResources() {
    	return this.outerResources;
    }
    
    
    
    
    /**
     * check if a contains b
     * @param a - the begin index of lock A or eventhandlerA
     * @param b - the begin index of lock B
     */    
    public boolean crContainsLock(int a, int b) {
    	if ( !hbg.isSameThread(a, b) ) return false; // is it necessary?
    	if (lockBlocks.get(b) == null) return false;
    	
    	if ( hbg.getNodeOPTY(a).equals(LogType.LockRequire.name()) ) {
    		if (lockBlocks.get(a) == null) return false;
    		if (a < b && lockBlocks.get(a) > lockBlocks.get(b)) return true;
    	}
    	else if ( hbg.getNodeOPTY(a).equals(LogType.EventProcEnter.name()) ) {
    		if (eventHandlerBlocks.get(a) == null) return false;
    		if (a < b && eventHandlerBlocks.get(a) > lockBlocks.get(b)) return true;
    	}
    	else {
    		//TODO
    	}
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
    	computeSinkInfo();
    	computeHandlerInfo();
    	computeEventHandlerInfo();
    	computeRPCHandlerInfo();
    	
    	//more
    	computeOuterResources();
    }
    
    
    /***********************************************************************
     * Extract useful information
     **********************************************************************/
    
    
    public void extractTargetCodeInfo() {
    	// Get all TargetCodeBegin&TargetCodeEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.TargetCodeBegin.name(), LogType.TargetCodeEnd.name());
    	// Handle target codes
    	//extractBlockInfo_weak(LogType.TargetCodeBegin.name(), LogType.TargetCodeEnd.name(), items, targetCodeBlocks);
    	extractBlockInfo(LogType.TargetCodeBegin.name(), LogType.TargetCodeEnd.name(), items, targetCodeBlocks);
    }
        
    
    public void extractLockInfo() {
    	// Get all EventHandlerBegin&EventHandlerEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.LockRequire.name(), LogType.LockRelease.name());
    	// Handle lock codes
    	extractBlockInfo(LogType.LockRequire.name(), LogType.LockRelease.name(), items, lockBlocks);
    }


    public void extractLoopInfo() {
    	// Get all LoopBegin&LoopEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.LoopBegin.name(), LogType.LoopEnd.name());
    	// Handle loop codes
    	extractBlockInfo_weak(LogType.LoopBegin.name(), LogType.LoopEnd.name(), items, loopBlocks);
    }
    
    
    /**
     * only for threadpool's thread now.
     */
    public void extractHandlerInfo() {
        // threadpool's event - ThdEnter & ThdExit
    	// Get all ThdEnter&ThdExit nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.ThdEnter.name(), LogType.ThdExit.name());
    	// Handle lock codes
    	extractBlockInfo(LogType.ThdEnter.name(), LogType.ThdExit.name(), items, handlerBlocks);
    }

    
/*    
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
*/  

    
    public void extractEventHandlerInfo() {
    	// Get all EventHandlerBegin&EventHandlerEnd nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.EventProcEnter.name(), LogType.EventProcExit.name());
    	// Handle event handler codes
    	extractBlockInfo_weak(LogType.EventProcEnter.name(), LogType.EventProcExit.name(), items, eventHandlerBlocks);
    }
    
    
    /**
     * for now, in fact, including including RPC, socket           #may need to differentiate
     */
    public void extractRPCHandlerInfo() {
    	// Get all MsgProcEnter&MsgProcExit nodes
    	ArrayList<Integer> items = getTypedNodes(LogType.MsgProcEnter.name(), LogType.MsgProcExit.name());
    	// Handle rpc handler codes
    	extractBlockInfo(LogType.MsgProcEnter.name(), LogType.MsgProcExit.name(), items, rpcHandlerBlocks);
    }
    
    
    
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
    
    
    
    
    public void extractBlockInfo_weak(String typeEnter, String typeExit, ArrayList<Integer> items, LinkedHashMap<Integer, Integer> blocks) {
    	
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(typeEnter) ) continue;
    		
			int flag = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				if ( hbg.getNodeOPTY( jIndex ).equals(typeEnter) ) flag ++;
				else flag --;
				if (flag == 0) {
					blocks.put( iIndex, jIndex );
					break;
				}
			}
			if ( !blocks.containsKey( iIndex ) )
				blocks.put( iIndex, null );
    	}
    }

    
    /**
     * Handle block code based on own typed items
     */
    public void extractBlockInfo(String typeEnter, String typeExit, ArrayList<Integer> items, LinkedHashMap<Integer, Integer> blocks) {
    
    	for (int i = 0; i < items.size(); i++) {
    		int iIndex = items.get(i);
    		if ( !hbg.getNodeOPTY( iIndex ).equals(typeEnter) ) continue;
    		
    		String opval = hbg.getNodeOPVAL(iIndex);
			int reenter = 1;
			for (int j = i+1; j < items.size(); j++) {
				int jIndex = items.get(j);
				if ( !hbg.isSameThread(jIndex, iIndex) ) break;
				// modified: bug fix
				if ( hbg.getNodeOPTY(jIndex).equals(typeEnter) && hbg.getNodeOPVAL(jIndex).equals(opval) )  
					reenter ++;
				if ( hbg.getNodeOPTY(jIndex).equals(typeExit) && hbg.getNodeOPVAL(jIndex).equals(opval) ) {
					reenter --;
					if (reenter == 0) {
						blocks.put(iIndex, jIndex);
						break;
					}
				}
				// end - modified
			}
			if ( !blocks.containsKey(iIndex) ) 
				blocks.put(iIndex, null);
    	}
    }

    
    
    /***********************************************************************
     * Compute more useful information
     **********************************************************************/
    
    public void computeSinkInfo() {
    	for (int beginindex: targetCodeBlocks.keySet() ) {
    		if ( targetCodeBlocks.get(beginindex) == null ) continue;
    		int endindex = targetCodeBlocks.get(beginindex);
    		
    		System.out.println("JX - DEBUG - loc 1");
    		SinkInstance sinkInstance = new SinkInstance(beginindex, endindex);
    		Sink targetSink = null;
    		System.out.println("JX - DEBUG - loc 2");
    		for (Sink sink: this.sinks) 
    			if ( sink.getID().equals( hbg.getNodeOPVAL(beginindex) ) ) {
    				System.out.println("JX - DEBUG - loc 2.1");
    				targetSink = sink;
    				break;
    			}
    		System.out.println("JX - DEBUG - loc 3");
    		if ( targetSink == null ) {
    			System.out.println("JX - DEBUG - loc 3.1");
    			targetSink = new Sink();
    			this.sinks.add(targetSink);
    		}
    		System.out.println("JX - DEBUG - loc 4");
    		targetSink.addInstance(sinkInstance);
    		System.out.println("JX - DEBUG - loc 5");
    	}
    }
    
        
	/**
	 * //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
	 */
	public void computeHandlerInfo() {
		computeThreadInfo(handlerBlocks, handlerThreads);
	}

	
	public void computeEventHandlerInfo() {
		computeThreadInfo(eventHandlerBlocks, eventHandlerThreads);
	}
	
	
	public void computeRPCHandlerInfo() {
		computeThreadInfo(rpcHandlerBlocks, rpcHandlerThreads);
	}
    
	
	/**
	 * //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
	 */
	public void computeThreadInfo(LinkedHashMap<Integer, Integer> blocks, LinkedHashMap<Integer, Integer> threads) {
		List<Integer> list = new ArrayList<>( blocks.keySet() );
		
		int numHandlers = 0;
		for (int i = 0; i < list.size(); i++) {     //note: cann't add "if (handlerBlocks.get(list.get(i)) == null) continue;", this will cause inaccurate
			if ( i>0 && !hbg.isSameThread(list.get(i), list.get(i-1)) ) {
				if (numHandlers > 1) {  //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
					threads.put(i-numHandlers, i-1);
					System.out.println("numHandlers = " + numHandlers + ", " + hbg.getNodePIDTID(list.get(i-1)));
				}
				numHandlers = 0; 
			}
			if ( i == list.size()-1 ) {
				if (numHandlers > 1) {  //note: we think if there are 2 or more thdenter&thdexit in one thread's log, then it is a handler thread
					threads.put(i-numHandlers, i-1);
					System.out.println("numHandlers = " + numHandlers + ", " + hbg.getNodePIDTID(list.get(i-1)));
				}
				numHandlers = 0;
			}
			numHandlers ++;
		}
	}
	
	
	
    public void computeOuterResources() {
    	// for locks - check their inner locks
    	resolveInnerLocks( lockBlocks );
		// for event handlers - check their inner locks
    	resolveInnerLocks( eventHandlerBlocks );
    }
    
    public void resolveInnerLocks(LinkedHashMap<Integer, Integer> crBlocks) {
		for (int crIndex: crBlocks.keySet()) {
			if (crBlocks.get(crIndex) == null) continue;
			int crBegin = crIndex;
			int crEnd = crBlocks.get(crIndex);
			
			for (int x = crBegin+1; x < crEnd; x++) {
				if ( hbg.getNodeOPTY(x).equals(LogType.LockRequire.name())  //ie, inner locks  //it seems we do not need to check inner EventHandler etc. 
					 && crContainsLock(crIndex, x) ) {
					
					if ( !outerResources.containsKey(x) ) {
						HashSet<String> set = new HashSet<String>();
						outerResources.put(x, set);
					}
					HashSet<String> set = outerResources.get(x);
					set.add( ag.getCRCode(crIndex) );
				}
			}
		}
    }
    
    
   
}
