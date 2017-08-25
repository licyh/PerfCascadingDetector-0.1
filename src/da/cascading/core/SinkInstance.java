package da.cascading.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.benchmark.Benchmarks;
import com.text.TextFileWriter;

import LogClass.LogType;
import da.cascading.BugPool;
import da.cascading.LoopBug;
import da.graph.AccidentalHBGraph;
import da.graph.HappensBeforeGraph;
import da.graph.LogInfoExtractor;
import da.graph.Pair;

public class SinkInstance {

	List<SinkInstance> instances;

	int beginIndex;
	int endIndex;
	
	public SinkInstance(int beginIndex, int endIndex) {
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
	}
	
	public int getBeginIndex() {
		return this.beginIndex;
	}
	
	public int getEndIndex() {
		return this.endIndex;
	}
	
	public String toString() {
		return "SinkInstance(" + beginIndex + " to " + endIndex + ")";
	}
	
	String projectDir;
	HappensBeforeGraph hbg;
	AccidentalHBGraph ag;
	LogInfoExtractor logInfo;
	
	// from LogInfoExtractor
    LinkedHashMap<Integer, Integer> targetCodeBlocks;   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> eventHandlerBlocks;   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> lockBlocks;   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> loopBlocks;   // beginIndex -> endIndex
	
    //In .traverseTargetCodes()
    //ArrayList<Integer> targetcodeLoops;  				//never used, just ps for time-consuming loops?
    
    BitSet traversedNodes;                      		//tmp var. set of all nodes for a single target code snippet

    //used for pruning 
    //HashMap<Integer, Integer>[] curNodes = new HashMap[ CASCADING_LEVEL + 1 ];
    HashMap<Integer, HashSet<String>> outerLocks = new HashMap<Integer, HashSet<String>>(); // lock -> ourter locks
    

    //Results
    BugPool bugPool;
    
    boolean ONLY_LOCK_RELATED_BUGS = true;                  //default
    int CASCADING_LEVEL = 10;                               //minimum:2; default:3;
    // for cascading chains
    @SuppressWarnings("unchecked")
	HashMap<Integer, Integer>[] predNodes = new HashMap[ CASCADING_LEVEL + 1 ];  //record cascading paths, for different threads
    @SuppressWarnings("unchecked")
	HashMap<Integer, Integer>[] upNodes   = new HashMap[ CASCADING_LEVEL + 1 ];  //record cascading paths, for the same thread

    
	public void setEnv(String projectDir, HappensBeforeGraph hbg, AccidentalHBGraph ag, LogInfoExtractor logInfo) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		this.ag = ag;
		this.logInfo = logInfo;
		
		this.targetCodeBlocks = logInfo.getTargetCodeBlocks(); 
	    this.eventHandlerBlocks = logInfo.getEventHandlerBlocks(); 
	    this.lockBlocks = logInfo.getLockBlocks();
	    this.loopBlocks = logInfo.getLoopBlocks();
        
        //targetcodeLoops = new ArrayList<Integer>();                              //never used, just ps for time-consuming loops?
        this.traversedNodes = new BitSet();                //tmp var. set of all nodes for a single target code snippet
        
        //this.bugPool = new BugPool(this.projectDir, this.hbg);
        //this.predNodes = new int[this.gb.nList.size()];
        for (int i = 1; i <= CASCADING_LEVEL; i++) {
        	this.predNodes[i] = new HashMap<Integer, Integer>();
        	this.upNodes[i]   = new HashMap<Integer, Integer>();
        }
	}
	
	public void setBugPool(BugPool bugPool) {
		this.bugPool = bugPool;
	}
	
	public void setOuterLocks( HashMap<Integer, HashSet<String>> outerLocks ) {
		this.outerLocks = outerLocks;
	}
	
	
	/*
	public void doWork() {
		// prepare
		prepare();
        // traverseTargetCodes
		handleSink();
    	// print the results
		bugPool.printResults();
	}
	*/
	
	
	
	/******************************************************************************
	 * Core
	 ******************************************************************************/
	
	

    
    
    /**  
     * JX - traverseTargetCodes - Traversing target code snippets
     * ie, 
     * Note: may involve two types of sink code snippets
     * 		- TargetCodeBegin & TargetCodeEnd
     * 		- tmp: (not here) EventHandlerBegin & EventHandlerEnd,  this should also be changed to  TargetCodeBegin & TargetCodeEnd
     */
	/*
    public void handleSink() {
    	System.out.println("\nJX - traverseTargetCodes - including all TARGET CODE snippets");
    	
    	int numofsnippets = 0;
    	// traverse every pair of TargetCodeBegin & TargetCodeEnd
    	
    	for (int beginindex: targetCodeBlocks.keySet() ) {
    		if ( targetCodeBlocks.get(beginindex) == null )
    			continue;
    		int endindex = targetCodeBlocks.get(beginindex);
    		System.out.println( "\nTarget Code Snippet #" + (++numofsnippets) + ": (" + beginindex + " to " + endindex + ")"  );
    		handleSinkInstance( beginindex, endindex );
    	}
    }
    */
    
    
    int tmpxx = 0;
    public void handleSinkInstance(int beginIndex, int endIndex) {
    	// Step 1 - also can find Non-cascaded loops (ie, immediate loops) in the Sink if needed
    	ArrayList<Integer> crs = identifyContentionResources( beginIndex, endIndex );
    	
		// Debugging - print all firt batch of locks' names
		//if (tmpxx == 0) {
			printLocks(crs);
			//tmpxx = 1;
		//}
    	
		// Step 2 - cascading chain 
    	startCascadingChainAnalysis( crs );
    }
    
    
    public ArrayList<Integer> identifyContentionResources(int beginIndex, int endIndex) {
    	ArrayList<Integer> resources = new ArrayList<Integer>();   //set of lock nodes for a single target code snippet
    	
    	// traversing for getting ImmediateBugs & Locks inside the executed code 
    	traversedNodes.clear();
    	  	
		//tmply add, only for ca-6744
    	if ( Benchmarks.resolveBugId(hbg.getTargetDir()) != null
    			&& Benchmarks.resolveBugId(hbg.getTargetDir()).equals("ca-6744")) {
    		//System.out.print("JX - DEBUG - LockCass: enter ca-6744");
        	scanAndDfs(beginIndex, endIndex, resources);	
    	}
    	else {
    		/*
    		boolean isClosedCycle = hbg.getReachSet().get(beginIndex).get(endIndex);
        	if ( isClosedCycle ) {
        	*/
        		dfsTraversing( beginIndex, 1, endIndex, resources);  //modified for ca-6744
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
    public void dfsTraversing( int x, int direction, int endIndex, ArrayList<Integer> resources) {
    	traversedNodes.set( x );
    	if ( hbg.getNodeOPTY(x).equals("LockRequire") ) {
    		resources.add( x );
    	}
    	// TODO - for Loop - suspected bugs
    	if ( hbg.getNodeOPTY(x).equals("LoopBegin") ) {
    		// add to bug pool
    		if ( ONLY_LOCK_RELATED_BUGS ) {
    			//Do Nothing
    		} else {
    			addLoopBug( x, 1 );
    		}
    	}

    	if (direction == 1)
        for (Pair pair: hbg.getEdge().get(x)) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) )
        		dfsTraversing( y, direction, endIndex, resources );
        }
        
    }
    
    
    //tmp
    public void scanAndDfs( int beginIndex, int endIndex, ArrayList<Integer> resources ) {
    	for (int i = beginIndex; i <= endIndex; i++) {
    		dfsTraversing2(i, 1, i, resources);
    	}
    }
    

    /**
     * @param direction   1(--->) or -1 (<---)
     */
    public void dfsTraversing2( int x, int direction, int endIndex, ArrayList<Integer> resources) {
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
        		dfsTraversing2( y, direction, endIndex, resources );
        }
       
        
    	// for needed in the future
        for (Pair pair: hbg.getBackEdge().get(x)) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) ) {
        		dfsTraversing2( y, -1, endIndex, resources );
        	}
        }
    }
    
    
    
    /****
     * Cascading chain analysis
     */
    
    int tmpflag = 0;  //just for test
    /** JX - findLockRelatedBugs - */    
    public void startCascadingChainAnalysis( List<Integer> firstResources ) {
    	System.out.println( "JX - INFO - findLockRelatedBugs" );
    	if ( firstResources.size() <= 0 ) {
    		System.out.println( "JX - INFO - CascadingBugDetection - there is no first batch of locks, finished normally!" );
    		return;
    	}

		
    	Set<Integer> currentResources = new TreeSet<Integer>( firstResources );
    	int curCascadingLevel = 2;   //this is the minimum level for lock-related cascading bugs
        for (int i = 1; i <= CASCADING_LEVEL; i++) {
        	predNodes[i].clear();
        	upNodes[i].clear();
        }
    	int tmpbatch = 0;
    	
    	while ( curCascadingLevel <= CASCADING_LEVEL ) {
    		// Find affected locks in different threads
    		Set<Integer> nextResources = findContendingResources( currentResources, curCascadingLevel );
            System.out.println("batch #" + (++tmpbatch) + ":#locks=" + currentResources.size() + " <--- #" + tmpbatch + ".5:#locks=" + nextResources.size() );
        
            // Debugging
            if ( tmpflag == 0 && curCascadingLevel == 2 )
    			printLocks(nextResources);
                
            // Find affected locks based on 1 in the same thread
    		currentResources = findInnerResourcesAndLoops( nextResources, curCascadingLevel );
    		System.out.println("batch #" + (tmpbatch+1) + ":#intermediate locks=" + currentResources.size()  );
    		
    		// Debugging
            if ( tmpflag == 0 && curCascadingLevel == 2 ) {
            	printLocks(currentResources);
    			tmpflag = 1;
            }
    		
        	if ( currentResources.size() <= 0 ) {
    			System.out.println( "JX - INFO - CascadingBugDetection - finished normally" );
    			break;
    		}
    		curCascadingLevel ++;
    	}
    	
    	System.out.println( "JX - INFO - CascadingBugDetection - finished with CASCADING_LEVEL = " + CASCADING_LEVEL );
    }
    
    
    // for Debugging
    public void printLocks(Collection<Integer> locks) { 
		for (int index: locks)
			System.out.println( "including lock - " + hbg.getPrintedIdentity(index) );
    }
    
    
    
    /*****************************************************************************
     * JX - findContendingResources: Find affected locks in different threads
     *****************************************************************************/
    public Set<Integer> findContendingResources( Set<Integer> resources, int curCascadingLevel ) {
    	//ArrayList<Integer> nextbatchLocks = new ArrayList<Integer>();
    	Set<Integer> nextResources = new TreeSet<Integer>();
    	for (int resIndex: resources) {  
    		if ( hbg.getNodeOPTY(resIndex).equals(LogType.LockRequire.name()) ) {
    			String pidopval0 = hbg.getNodePIDOPVAL0(resIndex); 
        		// 1. if not R lock; cuz R will not affect R, but a general obj.lock can affect the obj itself
        		if ( !ag.isReadLock(resIndex) ) {                
        			ArrayList<Integer> list = ag.accurateLockmemref.get( pidopval0 );
    	    		for (int index: list) {
    	    			if (resIndex == index) continue;  
    	                if ( hbg.isFlippedorder(resIndex, index) ) {
    	                	nextResources.add( index );
    	                	predNodes[curCascadingLevel].put(index, resIndex); 
    	                }
    	    		}
        		}                                                
        		// 2. if R/W lock
        		if ( ag.isReadOrWriteLock(resIndex) ) {          
        			String correspondingPidopval0 = ag.rwlockmatch.get( pidopval0 )[1];
        			ArrayList<Integer> list = ag.accurateLockmemref.get( correspondingPidopval0 );  //or using dotlockmemref.get( xx )
        			if (list != null)  //needed
        			for (int index: list) {                                                                 
        				if ( hbg.isFlippedorder(resIndex, index) ) {      
        					nextResources.add( index );            
        					predNodes[curCascadingLevel].put(index, resIndex); 
        				}                                    
        			}
        		}	
    		}
    		// only for ca-6744 now, fine only one operation at tid 29  but another two MsgProcEnter node1pid-tid247 &  node2pid-tid147
    		else if ( hbg.getNodeOPTY(resIndex).equals(LogType.ThdEnter.name())
    		    	&& Benchmarks.resolveBugId(hbg.getTargetDir()) != null
        			&& Benchmarks.resolveBugId(hbg.getTargetDir()).equals("ca-6744")
    				) {
    			//TODO
    			for (int index: ag.getHandlerCRs(resIndex)) {
    				nextResources.add( index );
    				predNodes[curCascadingLevel].put(index, resIndex);
    			}
    		}
    	} 
    	return nextResources;
    }
    
    
    
    /******************************************************************************************
     * findInnerResourcesAndLoops
     * JX - Find affected locks based on 'findNextbatchLocksInDiffThreads' in the same thread
     ******************************************************************************************/
    // New: find inner (bug) loops and locks, may across multiple threads
    public Set<Integer> findInnerResourcesAndLoops( Set<Integer> resources, int curCascadingLevel ) {
    	Set<Integer> nextResources = new TreeSet<Integer>();
		for (int index: resources) {
			int beginIndex = index;
			int endIndex = -1;
			if ( hbg.getNodeOPTY(index).equals(LogType.LockRequire.name()) ) {
				if ( lockBlocks.get(beginIndex) == null ) continue;
				endIndex = lockBlocks.get( beginIndex );
			}
			// for ca-6744 now
			else if ( hbg.getNodeOPTY(index).equals(LogType.ThdEnter.name()) ) {
				if ( logInfo.getHandlerBlocks().get(beginIndex) == null ) continue;
				endIndex = logInfo.getHandlerBlocks().get(beginIndex);
			}
			else {
				//ie, endIndex == -1
				continue;
			}
			
			// for obtaining outerlocks & dfs
			Set<String> outerlocks = null;          //should not be used for queue-related???
			BitSet traversedNodes = new BitSet(); 
	    	traversedNodes.clear();
			dfsForInnerResourcesAndLoops(beginIndex,  beginIndex, endIndex, outerlocks, curCascadingLevel, nextResources, traversedNodes);
		}
		return nextResources;
    }
    
    
    public void dfsForInnerResourcesAndLoops( int x,  int beginIndex, int endIndex, Set<String> outerlocks, int curCascadingLevel, Set<Integer> nextResources, BitSet traversedNodes ) {
    	//for testing
    	/*
		if (hbg.getNodeOPTY(x).equals(LogType.MsgSending.name())) {
			System.out.println("JX - DEBUG - MsgSending: " + hbg.getPrintedIdentity(x));
		}
		*/
    	
    	traversedNodes.set( x );
    	
    	// check Lock
    	if ( hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()) ) {
			if ( !ag.isRelevantLock(beginIndex, x) ) {                         // prune "lock beginIndex=x -> lock x"
				if (outerlocks == null) outerlocks = obtainOuterLocks(beginIndex, endIndex);
				if ( !outerlocks.contains(hbg.getNodePIDOPVAL0(x)) ) {         // prune "lock x -> lock beginIndex -> lock x"
					nextResources.add( x );
					upNodes[curCascadingLevel].put(x, beginIndex);
					//jx: it seems no need to check if the LockReuire has LockRelease or not
				}
			}
    	}
    	
    	// check Loop
    	if ( hbg.getNodeOPTY(x).equals(LogType.LoopBegin.name()) ) {
    		// add to bug pool
			upNodes[curCascadingLevel].put(x, beginIndex);
			System.out.println("JX - DEBUG - addLoopBug..");
			if ( Benchmarks.resolveBugId(hbg.getTargetDir()) != null 
					&& Benchmarks.resolveBugId(hbg.getTargetDir()).equals("ca-6744")
					&& curCascadingLevel == 2)
				bugPool.addLoopBug( x );
			else
				addLoopBug( x, curCascadingLevel );
    	}

        List<Pair> list = hbg.getEdge().get(x);
        for (Pair pair: list) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) ) {
        		dfsForInnerResourcesAndLoops( y,  beginIndex, endIndex, outerlocks, curCascadingLevel, nextResources, traversedNodes  );
        	}
        }
    }  
    
    
    /**
     * obtain all of outer-layer locks for the current lock block of (beginIndex,endIndex)
     * Return - Set<"PIDOPVAL0">
     */
    public Set<String> obtainOuterLocks(int beginIndex, int endIndex) {
		// Note: if this part takes much time, then change to "for (int i = index-1; i >= index-20; i--) {"
		//get its outer locks, to avoid "(lockA - )lockB<index> - lockA<k> - uA-uB-uA"
		Set<String> outerlocks = new HashSet<String>();
    	if ( !hbg.getNodeOPTY(beginIndex).equals(LogType.LockRequire.name()) ) 
    		return outerlocks;
		
		for (int i = beginIndex-1; i >= 0; i--) {
			if ( !hbg.isSameThread(i, beginIndex) ) break;
			if ( hbg.getNodeOPTY(i).equals("LockRequire") ) {
				if (lockBlocks.get(i) != null && lockBlocks.get(i) > endIndex) {
					outerlocks.add( hbg.getNodePIDOPVAL0(i) );
				}
			}
		}
		return outerlocks;
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
    			if (!outerLocks.containsKey(x)) continue;
    			
    			for (String each: outerLocks.get(x)) {
    				if (each.equals( hbg.getNodePIDOPVAL0(x) )) continue;
    				if (own.contains(each))
    					return false;
    			}
    		}
    	}
    	return true;
    }
    
    
    
    public void addLoopBug( int nodeIndex, int cascadingLevel ) {
    	// add to bug pool
    	LoopBug loopbug = new LoopBug( nodeIndex, cascadingLevel );
  	
    	// get cascading lock chain
    	if ( cascadingLevel == 1 ) { // Immediate loop bug
    		loopbug.getCascadingChain().add( nodeIndex );
    	}
    	else if ( cascadingLevel >= 2 ) { // Lock-related loop bug
        	loopbug.getCascadingChain().add( nodeIndex );
        	int tmp = nodeIndex;
    		for (int i=cascadingLevel; i>=2; i--) {
    			tmp = upNodes[i].get(tmp);
    			loopbug.getCascadingChain().add( tmp );
    			tmp = predNodes[i].get(tmp);
    			loopbug.getCascadingChain().add( tmp );
    		}
    	}
    	
    	//added for checking chain, ie, false positive prunning
    	if ( cascadingLevel>=2 && !checkChain(loopbug) ) return;
    	
    	bugPool.addLoopBug( loopbug );
    	//jx: had better commented this when #targetcode is large or #loopbug is large
    	//System.out.println( loopbug );
    }
    
}









