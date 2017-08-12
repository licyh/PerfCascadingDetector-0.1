package da.cascading;

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

import com.text.TextFileWriter;

import LogClass.LogType;
import da.graph.AccidentalHBGraph;
import da.graph.HappensBeforeGraph;
import da.graph.LogInfoExtractor;
import da.graph.Pair;

public class LockCase {

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
    ArrayList<Integer> targetcodeLocks;   				//set of lock nodes for a single target code snippet
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

    
	
	public LockCase(String projectDir, HappensBeforeGraph hbg, AccidentalHBGraph ag, LogInfoExtractor logInfo) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		this.ag = ag;
		this.logInfo = logInfo;
		
		this.targetCodeBlocks = logInfo.getTargetCodeBlocks(); 
	    this.eventHandlerBlocks = logInfo.getEventHandlerBlocks(); 
	    this.lockBlocks = logInfo.getLockBlocks();
	    this.loopBlocks = logInfo.getLoopBlocks();
        
        //targetcodeLoops = new ArrayList<Integer>();                              //never used, just ps for time-consuming loops?
        this.targetcodeLocks = new ArrayList<Integer>();   //set of lock nodes for a single target code snippet
        this.traversedNodes = new BitSet();                //tmp var. set of all nodes for a single target code snippet
        
        this.bugPool = new BugPool(this.projectDir, this.hbg);
        //this.predNodes = new int[this.gb.nList.size()];
        for (int i = 1; i <= CASCADING_LEVEL; i++) {
        	this.predNodes[i] = new HashMap<Integer, Integer>();
        	this.upNodes[i]   = new HashMap<Integer, Integer>();
        }
	}
	
	
	public void doWork() {
		// prepare
		prepare();
        // traverseTargetCodes
		traverseTargetCodes();
    	// print the results
		bugPool.printResults();
	}
	
	
	
	
	/******************************************************************************
	 * Core
	 ******************************************************************************/
	
	
	public void prepare() {
		
		for (int lockIndex: lockBlocks.keySet()) {
			if (lockBlocks.get(lockIndex) == null) continue;
			int lockBegin = lockIndex;
			int lockEnd = lockBlocks.get(lockIndex);
			for (int x = lockBegin+1; x < lockEnd; x++) {
				if ( hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()) 
					 && logInfo.lockContains(lockIndex, x) 
						) {
					
					if ( !outerLocks.containsKey(x) ) {
						HashSet<String> set = new HashSet<String>();
						outerLocks.put(x, set);
					}
					HashSet<String> set = outerLocks.get(x);
					set.add( hbg.getNodePIDOPVAL0(lockIndex) );
					
				}
			}
		}
	}
 
    
    
    /**  
     * JX - traverseTargetCodes - Traversing target code snippets
     * Note: the core
     */
    public void traverseTargetCodes() {
    	System.out.println("\nJX - traverseTargetCodes - including all TARGET CODE snippets");
    	
    	int numofsnippets = 0;
    	// traverse every pair of TargetCodeBegin & TargetCodeEnd
    	for (int beginindex: targetCodeBlocks.keySet() ) {
    		if ( targetCodeBlocks.get(beginindex) == null )
    			continue;
    		int endindex = targetCodeBlocks.get(beginindex);
    		System.out.println( "\nTarget Code Snippet #" + (++numofsnippets) + ": (" + beginindex + " to " + endindex + ")"  );
    		// Step 1 of 2 - firstRoundTraversing, ie, bugs that inside the executed code, also get locks for Step 2
    		findImmediateBugs( beginindex, endindex );
    		// Step 2 of 2
        	findLockRelatedBugs( targetcodeLocks );
    	}
    }
    
    
    // 1. findImmediateBugs
    
    public void findImmediateBugs(int beginIndex, int endIndex) {
    	boolean isClosedCycle = hbg.getReachSet().get(beginIndex).get(endIndex);
   
    	// traversing for getting ImmediateBugs & Locks inside the executed code 
    	targetcodeLocks.clear();
    	traversedNodes.clear();
    	
    	if ( isClosedCycle ) {
    		dfsTraversing( beginIndex, 1, endIndex );  //modified for ca-6744
    	}
    	else {
    		System.out.println("JX - WARN - " + "couldn't reach from " + beginIndex + " to " + endIndex);
        	//tmply add, only for ca-6744
        	scanAndDfs(beginIndex, endIndex);
    	}
    	
    	// analyzing Locks that are inside the executed code
    	Set<String> setofinvolvingthreads = new HashSet<String>();
    	for (int index: targetcodeLocks) {
    		setofinvolvingthreads.add( hbg.getNodePIDTID(index) );
    	}
    	System.out.println("this snippet includes " + traversedNodes.cardinality()
    						+ " nodes, #firstbatchLocks = " + targetcodeLocks.size() + ", #involving threads = " + setofinvolvingthreads);
    }
    
    
    /**
     * @param direction   1(--->) or -1 (<---)
     */
    public void dfsTraversing( int x, int direction, int endIndex ) {
    	traversedNodes.set( x );
    	if ( hbg.getNodeOPTY(x).equals("LockRequire") ) {
    		targetcodeLocks.add( x );
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
        		dfsTraversing( y, direction, endIndex );
        }
        
    	// for needed in the future
        for (Pair pair: hbg.getBackEdge().get(x)) {
        	int y = pair.destination;
        	
        	//debug
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) 
        			&& hbg.getNodeOPTY(y).equals(LogType.ThdEnter.toString()) ) {
        		System.out.println("JX - debug - LockCase: ThdEnter " + hbg.getPrintedIdentity(y) );
        	}
        	
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) 
        			&& !hbg.getNodeOPTY(y).equals(LogType.ThdEnter.toString())
        			)
        		dfsTraversing( y, -1, endIndex );
        }
    }
    
    //tmp
    public void scanAndDfs( int beginIndex, int endIndex ) {
    	for (int i = beginIndex; i <= endIndex; i++) {
    		dfsTraversing(i, 1, i);
    	}
    }
    

    
    
    
    
    // 2. findLockRelatedBugs
    
    int tmpflag = 0;  //just for test
    /** JX - findLockRelatedBugs - */    
    public void findLockRelatedBugs( List<Integer> firstbatchLocks ) {
    	System.out.println( "JX - INFO - findLockRelatedBugs" );
    	if ( firstbatchLocks.size() <= 0 ) {
    		System.out.println( "JX - INFO - CascadingBugDetection - there is no first batch of locks, finished normally!" );
    		return;
    	}
    	
		// Debugging - print all firt batch of locks' names
		if (tmpflag == 0)
			printLocks(firstbatchLocks);
		
    	Set<Integer> curbatchLocks = new TreeSet<Integer>( firstbatchLocks );
    	int curCascadingLevel = 2;   //this is the minimum level for lock-related cascading bugs
        for (int i = 1; i <= CASCADING_LEVEL; i++) {
        	predNodes[i].clear();
        	upNodes[i].clear();
        }
    	int tmpbatch = 0;
    	
    	while ( curCascadingLevel <= CASCADING_LEVEL ) {
    		// Find affected locks in different threads
    		Set<Integer> nextbatchLocks = findNextbatchLocksInDiffThreads( curbatchLocks, curCascadingLevel );
            System.out.println("batch #" + (++tmpbatch) + ":#locks=" + curbatchLocks.size() + " <--- #" + tmpbatch + ".5:#locks=" + nextbatchLocks.size() );
        
            // Debugging
            if ( tmpflag == 0 && curCascadingLevel == 2 )
    			printLocks(nextbatchLocks);
                
            // Find affected locks based on 1 in the same thread
    		curbatchLocks = findNextbatchLocksInSameThread( nextbatchLocks, curCascadingLevel );
    		System.out.println("batch #" + (tmpbatch+1) + ":#intermediate locks=" + curbatchLocks.size()  );
    		
    		// Debugging
            if ( tmpflag == 0 && curCascadingLevel == 2 ) {
            	printLocks(curbatchLocks);
    			tmpflag = 1;
            }
    		
        	if ( curbatchLocks.size() <= 0 ) {
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
    
    
    
    // JX - Find affected locks in different threads
    public Set<Integer> findNextbatchLocksInDiffThreads( Set<Integer> batchLocks, int curCascadingLevel ) {
    	//ArrayList<Integer> nextbatchLocks = new ArrayList<Integer>();
    	Set<Integer> nextbatchLocks = new TreeSet<Integer>();
    	for (int lockindex: batchLocks) {
    		String pidopval0 = hbg.getNodePIDOPVAL0(lockindex); 
    		// 1. if not R lock; cuz R will not affect R, but a general obj.lock can affect the obj itself
    		if ( !ag.isReadLock(lockindex) ) {                
    			ArrayList<Integer> list = ag.accurateLockmemref.get( pidopval0 );
	    		for (int index: list) {
	    			if (lockindex == index) continue;  
	                if ( hbg.isFlippedorder(lockindex, index) ) {
	                	nextbatchLocks.add( index );
	                	predNodes[curCascadingLevel].put(index, lockindex); 
	                }
	    		}
    		}                                                
    		// 2. if R/W lock
    		if ( ag.isReadOrWriteLock(lockindex) ) {          
    			String correspondingPidopval0 = ag.rwlockmatch.get( pidopval0 )[1];
    			ArrayList<Integer> list = ag.accurateLockmemref.get( correspondingPidopval0 );  //or using dotlockmemref.get( xx )
    			if (list != null)  //needed
    			for (int index: list) {                                                                 
    				if ( hbg.isFlippedorder(lockindex, index) ) {      
    					nextbatchLocks.add( index );            
    					predNodes[curCascadingLevel].put(index, lockindex); 
    				}                                    
    			}
    		}  
    	} 
    	return nextbatchLocks;
    }
    
    
    // JX - Find affected locks based on 'findNextbatchLocksInDiffThreads' in the same thread
    // New: find inner (bug) loops and locks, may across multiple threads
    public Set<Integer> findNextbatchLocksInSameThread( Set<Integer> batchLocks, int curCascadingLevel ) {
    	Set<Integer> nextbatchLocks = new TreeSet<Integer>();
		for (int index: batchLocks) {
			int beginIndex = index;
			if ( lockBlocks.get(beginIndex) == null ) continue;
			int endIndex = lockBlocks.get( beginIndex );
			
			// for obtaining outerlocks & dfs
			Set<String> outerlocks = null;
	    	traversedNodes.clear();
	    	
	    	// dfs from beginIndex
			dfsForInnerLoopsAndLocks(beginIndex,  beginIndex, endIndex, outerlocks, curCascadingLevel, nextbatchLocks);
		}
		return nextbatchLocks;
    }
    
    
    public void dfsForInnerLoopsAndLocks( int x,  int beginIndex, int endIndex, Set<String> outerlocks, int curCascadingLevel, Set<Integer> nextbatchLocks ) {
    	traversedNodes.set( x );
    	
    	//for testing
		if (hbg.getNodeOPTY(x).equals(LogType.MsgSending.name())) {
			System.out.println("JX - DEBUG - MsgSending: " + hbg.getPrintedIdentity(x));
		}
    	
    	// check Lock
    	if ( hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()) ) {
			if ( !ag.isRelevantLock(beginIndex, x) ) {                         // prune "lock beginIndex=x -> lock x"
				if (outerlocks == null) outerlocks = obtainOuterLocks(beginIndex, endIndex);
				if ( !outerlocks.contains(hbg.getNodePIDOPVAL0(x)) ) {         // prune "lock x -> lock beginIndex -> lock x"
					nextbatchLocks.add( x );
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
			addLoopBug( x, curCascadingLevel );
    	}

        List<Pair> list = hbg.getEdge().get(x);
        for (Pair pair: list) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && hbg.getReachSet().get(y).get(endIndex) ) {
        		dfsForInnerLoopsAndLocks( y,  beginIndex, endIndex, outerlocks, curCascadingLevel, nextbatchLocks  );
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
    	int cascadingLevel = loopbug.cascadingLevel;
    	
    	HashSet<String> own = new HashSet<String>();
    	for (int x: loopbug.cascadingChain) {
    		if (hbg.getNodeOPTY(x).equals(LogType.LockRequire.name()))
    			own.add( hbg.getNodePIDOPVAL0(x) );
    	}
    	//System.out.println("JX - DEBUG - checkChain: own.size()=" + own.size());
    	for (int i = 0; i<loopbug.cascadingChain.size(); i++) {
    		int x = loopbug.cascadingChain.get(i);
    		if (i%2==1 || i==loopbug.cascadingChain.size()-1) {
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
    		loopbug.cascadingChain.add( nodeIndex );
    	}
    	else if ( cascadingLevel >= 2 ) { // Lock-related loop bug
        	loopbug.cascadingChain.add( nodeIndex );
        	int tmp = nodeIndex;
    		for (int i=cascadingLevel; i>=2; i--) {
    			tmp = upNodes[i].get(tmp);
    			loopbug.cascadingChain.add( tmp );
    			tmp = predNodes[i].get(tmp);
    			loopbug.cascadingChain.add( tmp );
    		}
    	}
    	
    	//added for checking chain, ie, false positive prunning
    	if ( cascadingLevel>=2 && !checkChain(loopbug) ) return;
    	
    	bugPool.addLoopBug( loopbug );
    	//jx: had better commented this when #targetcode is large or #loopbug is large
    	//System.out.println( loopbug );
    }
    
}






//String pidopval0 = hbg.getNodePIDOPVAL0( index );
//int loopflag = 0;

/*
for (int k = beginIndex; k <= endIndex; k++) {         /////////JXXXXXXXXXXX - here seems a big bug, I didn't find into RPC or method call
	
	// check Lock
	if ( hbg.getNodeOPTY(k).equals(LogType.LockRequire.name()) ) {
		if ( !ag.isRelevantLock(index, k) ) {  // yes, it's right
			if (outerlocks == null) outerlocks = obtainOuterLocks(beginIndex, endIndex);
			if ( !outerlocks.contains(hbg.getNodePIDOPVAL0(k)) ) {   
				nextbatchLocks.add( k );
				bugPool.upNodes[curCascadingLevel].put(k, index);
				//jx: it seems no need to check if the LockReuire has LockRelease or not
			}
		}
	}
	
	// check Loop
	if ( hbg.getNodeOPTY(k).equals(LogType.LoopBegin.name()) ) {
		//loopflag = 1;
		// add to bug pool
		bugPool.upNodes[curCascadingLevel].put(k, index);
		bugPool.addLoopBug( k, curCascadingLevel );
	}
	
}
*/










