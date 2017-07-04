package da.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import LogClass.LogType;



public class LogInfoExtractor {
	
	HappensBeforeGraph hbg;
	
    LinkedHashMap<Integer, Integer> targetCodeBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> eventHandlerBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> lockBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> loopBlocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    
	
    
    public LogInfoExtractor(HappensBeforeGraph hbg) {
    	this.hbg = hbg;
    	doWork();
    }
    
    public void doWork() {
    	System.out.println("JX - INFO - LogInfoExtractor: doWork...");
    	extractLogInfo();
    }
    
    

    public LinkedHashMap<Integer, Integer> getTargetCodeBlocks() {
    	return this.targetCodeBlocks;
    }
    
    public LinkedHashMap<Integer, Integer> getEventHandlerBlocks() {
    	return this.eventHandlerBlocks;
    }
    
    public LinkedHashMap<Integer, Integer> getLockBlocks() {
    	return this.lockBlocks;
    }
    
    public LinkedHashMap<Integer, Integer> getLoopBlocks() {
    	return this.loopBlocks;
    }
    
    
	
    public void extractLogInfo() {
        
        extractTargetCodeInfo();
        extractEventHandlerInfo();
        extractLockInfo();
        extractLoopInfo();
    	 	
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
    
    
    public void extractTargetCodeInfo() {
    	// find out all Target Code nodes (Beging & End) first
    	ArrayList<Integer> items = new ArrayList<Integer>();    //all nodes of all TargetCodeBegin & TargetCodeEnd snippets
    	for (int i = 0; i < hbg.getNodeList().size(); i++) {
    		String opty = hbg.getNodeOPTY(i);
    		if ( opty.equals("TargetCodeBegin") || opty.equals("TargetCodeEnd") ) {
    			items.add( i );
    		}
    	}
    	
    	// Handle target codes: alltargetitems -> targetCodeBlocks: get targetCodeBlocks by TargetCodeBegin & TargetCodeEnd    	
    	for (int i = 0; i < items.size(); i++) {
    		//print for debug
    		//System.out.println("JX - INFO - i=" + i + " - index=" + items.get(i) + " - " + hbg.getNodeOPTY(items.get(i)));
    		//end-print
    		int iindex = items.get(i);
    		if ( hbg.getNodeOPTY( iindex ).equals("TargetCodeBegin") ) {
    			String pidtid = hbg.getNodePIDTID( iindex );
    			int flag = 1;
    			for (int j = i+1; j < items.size(); j++) {
    				int jindex = items.get(j);
    				if ( !hbg.getNodePIDTID( jindex ).equals(pidtid) ) {
    					System.out.println("JX - WARN - " + "couldn't find TargetCodeEND for TargetCodeBegin " + i + " its index = " + iindex);
    					break;
    				}
    				if ( hbg.getNodeOPTY( jindex ).equals("TargetCodeBegin") ) flag ++;
    				else flag --;
    				if (flag == 0) {
    					targetCodeBlocks.put( iindex, jindex );
    					break;
    				}
    			}
    			if ( !targetCodeBlocks.containsKey( iindex ) )
    				targetCodeBlocks.put( iindex, null );
    		}
    	}
    }
    
    
    public void extractEventHandlerInfo() {
    	// scan all nodes
    	ArrayList<Integer> items = new ArrayList<Integer>();
    	for (int i = 0; i < hbg.getNodeList().size(); i++) {
    		String opty = hbg.getNodeOPTY(i);
    		if ( opty.equals(LogType.EventHandlerBegin.name()) || opty.equals(LogType.EventHandlerEnd.name()) ) {
    			items.add( i );
    		}
    	}
    	
    	// Handle loop codes
    	for (int i = 0; i < items.size(); i++) {
    		int iindex = items.get(i);
    		if ( hbg.getNodeOPTY( iindex ).equals(LogType.EventHandlerBegin.name()) ) {
    			String pidtid = hbg.getNodePIDTID( iindex );
    			int flag = 1;
    			for (int j = i+1; j < items.size(); j++) {
    				int jindex = items.get(j);
    				if ( !hbg.getNodePIDTID( jindex ).equals(pidtid) ) {
    					System.out.println("JX - WARN - " + "couldn't find EventHandlerEND for EventHandlerBegin " + i + " its index = " + iindex);
    					break;
    				}
    				if ( hbg.getNodeOPTY( jindex ).equals(LogType.EventHandlerBegin.name()) ) flag ++;
    				else flag --;
    				if (flag == 0) {
    					eventHandlerBlocks.put( iindex, jindex );
    					break;
    				}
    			}
    			if ( !eventHandlerBlocks.containsKey( iindex ) )
    				eventHandlerBlocks.put( iindex, null );
    		}
    	}
    }
    
    
    public void extractLockInfo() {
    	// scan all nodes
    	for (int i = 0; i < hbg.getNodeList().size(); i++) {
    		String opty = hbg.getNodeOPTY(i);

    		// find out all Lock code blocks
    		if ( opty.equals("LockRequire") ) {
    			String pidtid = hbg.getNodePIDTID(i);
    			String opval = hbg.getNodeOPVAL(i);
    			int reenter = 1;
    			for (int j = i+1; j < hbg.getNodeList().size(); j++) {
    				if ( !hbg.getNodePIDTID(j).equals(pidtid) ) break;
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
  
    }


    public void extractLoopInfo() {
    	// scan all nodes
    	ArrayList<Integer> items = new ArrayList<Integer>();
    	for (int i = 0; i < hbg.getNodeList().size(); i++) {
    		String opty = hbg.getNodeOPTY(i);
    		if ( opty.equals("LoopBegin") || opty.equals("LoopEnd") ) {
    			items.add( i );
    		}
    	}
    	
    	// Handle loop codes
    	for (int i = 0; i < items.size(); i++) {
    		int iindex = items.get(i);
    		if ( hbg.getNodeOPTY( iindex ).equals("LoopBegin") ) {
    			String pidtid = hbg.getNodePIDTID( iindex );
    			int flag = 1;
    			for (int j = i+1; j < items.size(); j++) {
    				int jindex = items.get(j);
    				if ( !hbg.getNodePIDTID( jindex ).equals(pidtid) ) {
    					// System.out.println("JX - WARN - " + "couldn't find LoopEND for LoopBegin " + i + " its index = " + iindex);
    					break;
    				}
    				if ( hbg.getNodeOPTY( jindex ).equals("LoopBegin") ) flag ++;
    				else flag --;
    				if (flag == 0) {
    					loopBlocks.put( iindex, jindex );
    					break;
    				}
    			}
    			if ( !loopBlocks.containsKey( iindex ) )
    				loopBlocks.put( iindex, null );
    		}
    	}
    }

    
}
