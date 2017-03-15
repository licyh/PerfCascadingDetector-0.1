package da;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CascadingFinder {

	GraphBuilder gb;
    //In .traverseTargetCodes()
    ArrayList<Integer> alltargetitems;    				//all nodes of all TargetCodeBegin & TargetCodeEnd snippets
    //ArrayList<Integer> targetcodeLoops;  				//never used, just ps for time-consuming loops?
    ArrayList<Integer> targetcodeLocks;   				//set of lock nodes for a single target code snippet
    BitSet traversedNodes;                      		//tmp var. set of all nodes for a single target code snippet
    int[] predNodes; 
    //End-Added
    
    ArrayList<Integer> allloopitems = new ArrayList<Integer>();
    LinkedHashMap<Integer, Integer> targetblocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> lockblocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> loopblocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
	
    int CASCADING_LEVEL = 10;  //minimum:2; default:3;
	//Set<Integer> bugpool = new TreeSet<Integer>();        //for now, only one bug pool for whole code snippets
    Set<LoopBug> bugpool = new TreeSet<LoopBug>();          //for now, only one bug pool for whole code snippets
	Set<String> simplebugpool = new TreeSet<String>();
	Set<String> medianbugpool = new TreeSet<String>();
	//Set<String> complexbugpool = new TreeSet<String>();
    
	CascadingFinder(GraphBuilder graphBuilder) {
		this.gb = graphBuilder;
		
        this.alltargetitems = new ArrayList<Integer>();    //all nodes of all TargetCodeBegin & TargetCodeEnd snippets
        //targetcodeLoops = new ArrayList<Integer>();                              //never used, just ps for time-consuming loops?
        this.targetcodeLocks = new ArrayList<Integer>();   //set of lock nodes for a single target code snippet
        this.traversedNodes = new BitSet();                //tmp var. set of all nodes for a single target code snippet
        this.predNodes = new int[this.gb.nList.size()];
	}
	
	
	public void doWork() {
		// extract Target, Lock, Loop logs
		extractTargetLockLoopInfo();
        // traverseTargetCodes
		traverseTargetCodes();
    	// print the results
		printResultsOfTraverseTargetCodes();
	}
	
    /**
     * JX - extractTargetLockLoopInfo
     */
    public void extractTargetLockLoopInfo() {
    	System.out.println("\nJX - extractTargetLockLoopInfo");
    	
    	// init, No need actually
    	targetblocks.clear();
    	lockblocks.clear();
    	loopblocks.clear();
    	
    	// scan all nodes
    	for (int i = 0; i < gb.nList.size(); i++) {
    		String opty = gb.getNodeOPTY(i);
    		// find out all Target Code nodes/blocks
    		if ( opty.equals("TargetCodeBegin") || opty.equals("TargetCodeEnd") ) {
    			alltargetitems.add( i );
    		}
    		// find out all Lock code blocks
    		else if ( opty.equals("LockRequire") ) {
    			String pidtid = gb.getNodePIDTID(i);
    			String pidopval0 = gb.getNodePIDOPVAL0(i);
    			for (int j = i+1; j < gb.nList.size(); j++) {
    				if ( !gb.getNodePIDTID(j).equals(pidtid) ) break;
    				if ( gb.getNodeOPTY(j).equals("LockRelease") && gb.getNodePIDOPVAL0(j).equals(pidopval0) ) {
    					lockblocks.put(i, j);
    					break;
    				}
    			}
    			if ( !lockblocks.containsKey(i) ) 
    				lockblocks.put(i, null);
    		}
    		// find out all Loop code blocks
    		// TODO
    		else if ( opty.equals("LoopBegin") || opty.equals("LoopEnd") ) {
    			allloopitems.add( i );
    		}
    	}
    	
    	// Handle target codes: alltargetitems -> targetblocks: get targetblocks by TargetCodeBegin & TargetCodeEnd    	
    	for (int i = 0; i < alltargetitems.size(); i++) {
    		//print for debug
    		System.out.println("JX - i=" + i + " - index=" + alltargetitems.get(i) + " - " + gb.getNodeOPTY(alltargetitems.get(i)));
    		//end-print
    		int iindex = alltargetitems.get(i);
    		if ( gb.getNodeOPTY( iindex ).equals("TargetCodeBegin") ) {
    			String pidtid = gb.getNodePIDTID( iindex );
    			int flag = 1;
    			for (int j = i+1; j < alltargetitems.size(); j++) {
    				int jindex = alltargetitems.get(j);
    				if ( !gb.getNodePIDTID( jindex ).equals(pidtid) ) {
    					System.out.println("JX - WARN - " + "couldn't find TargetCodeEND for TargetCodeBegin " + i + " its index = " + iindex);
    					break;
    				}
    				if ( gb.getNodeOPTY( jindex ).equals("TargetCodeBegin") ) flag ++;
    				else flag --;
    				if (flag == 0) {
    					targetblocks.put( iindex, jindex );
    					break;
    				}
    			}
    			if ( !targetblocks.containsKey( iindex ) )
    				targetblocks.put( iindex, null );
    		}
    	}
    	
    	// Handle lock codes: already handled in 'scan' phase
    	
    	// Handle loop codes
    	for (int i = 0; i < allloopitems.size(); i++) {
    		int iindex = allloopitems.get(i);
    		if ( gb.getNodeOPTY( iindex ).equals("LoopBegin") ) {
    			String pidtid = gb.getNodePIDTID( iindex );
    			int flag = 1;
    			for (int j = i+1; j < allloopitems.size(); j++) {
    				int jindex = allloopitems.get(j);
    				if ( !gb.getNodePIDTID( jindex ).equals(pidtid) ) {
    					System.out.println("JX - WARN - " + "couldn't find LoopEND for LoopBegin " + i + " its index = " + iindex);
    					break;
    				}
    				if ( gb.getNodeOPTY( jindex ).equals("LoopBegin") ) flag ++;
    				else flag --;
    				if (flag == 0) {
    					loopblocks.put( iindex, jindex );
    					break;
    				}
    			}
    			if ( !loopblocks.containsKey( iindex ) )
    				loopblocks.put( iindex, null );
    		}
    	}
    	
    	// for test
    	Set<String> tmpset = new HashSet<String>();
    	int ntargetsInSourceCode = 0;
    	int nlocksInSourceCode = 0;
    	int nloopsInSourceCode = 0;
    	
    	tmpset.clear();
    	for (Integer index: targetblocks.keySet()) {
    		tmpset.add( gb.lastCallstack(index) );
    	}
    	ntargetsInSourceCode = tmpset.size();
    	tmpset.clear();
    	for (Integer index: lockblocks.keySet()) {
    		tmpset.add( gb.lastCallstack(index) );
    	}
    	nlocksInSourceCode = tmpset.size();
    	tmpset.clear();
    	for (Integer index: loopblocks.keySet()) {
    		tmpset.add( gb.lastCallstack(index) );
    	}
    	nloopsInSourceCode = tmpset.size();
    			
    	// for test
    	System.out.println("#targetblocks = " + targetblocks.size() + " -> ntargetsInSourceCode=" + ntargetsInSourceCode);
    	System.out.println("#lockblocks = " + lockblocks.size() + " -> nlocksInSourceCode=" + nlocksInSourceCode);
    	System.out.println("#loopblocks = " + loopblocks.size() + " -> nloopsInSourceCode=" + nloopsInSourceCode);
    	
    	
    	// build the relationship between locks and loops
    	// JX - it seems NO NEED
    	
    }
    
    
    /**  
     * JX - traverseTargetCodes - Traversing target code snippets
     * Note: the core
     */
    public void traverseTargetCodes() {
    	System.out.println("\nJX - traverseTargetCodes - including all TARGET CODE snippets");
    	
    	int numofsnippets = 0;
    	// traverse every pair of TargetCodeBegin & TargetCodeEnd
    	for (int beginindex: targetblocks.keySet() ) {
    		if ( targetblocks.get(beginindex) == null )
    			continue;
    		int endindex = targetblocks.get(beginindex);
    		System.out.println( "\nTarget Code Snippet #" + (++numofsnippets) + ": (" + beginindex + " to " + endindex + ")"  );
    		// Step 1 of 2 - firstRoundTraversing, ie, bugs that inside the executed code, also get locks for Step 2
    		findImmediateBugs( beginindex, endindex );
    		// Step 2 of 2
        	findLockRelatedBugs( targetcodeLocks );
    	}
    }
    
    
    // 1. findImmediateBugs
    int tmpflag = 0;  //just for test
    public void findImmediateBugs(int beginIndex, int endIndex) {
    	if ( !gb.reachbitset.get(beginIndex).get(endIndex) ) {
    		System.out.println("JX - ERROR - " + "couldn't reach from " + beginIndex + " to " + endIndex);
    		return;
    	}
    	
    	// traversing for getting ImmediateBugs & Locks inside the executed code 
    	targetcodeLocks.clear();
    	traversedNodes.clear();
    	dfsTraversing( beginIndex, endIndex );
    	
    	// analyzing Locks that are inside the executed code
    	Set<String> setofinvolvingthreads = new HashSet<String>();
    	for (int index: targetcodeLocks) {
    		setofinvolvingthreads.add( gb.getNodePIDTID(index) );
    		// for test, print all locks' names
    		if (tmpflag == 0 ) {
    			System.out.println( "including lock - " + gb.getNodePIDTID(index) + " " + gb.lastCallstack(index) );
    		}
    	}
    	tmpflag = 1;
    	System.out.println("this snippet includes " + traversedNodes.cardinality()
    						+ " nodes, #firstbatchLocks = " + targetcodeLocks.size() + ", #involving threads = " + setofinvolvingthreads);
    }
    
    
    public void dfsTraversing( int x, int endIndex ) {
    	traversedNodes.set( x );
    	if ( gb.getNodeOPTY(x).equals("LockRequire") ) {
    		targetcodeLocks.add( x );
    	}
    	// TODO - for Loop - suspected bugs
    	if ( gb.getNodeOPTY(x).equals("LoopBegin") ) {
    		// add to bug pool
			addToBugPool( x, 1 );
    	}

        List<Pair> list = gb.edge.get(x);
        for (Pair pair: list) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && gb.reachbitset.get(y).get(endIndex) )
        		dfsTraversing( y, endIndex );
        }
    }
    
    
    // 2. findLockRelatedBugs
    /** JX - findLockRelatedBugs - */    
    public void findLockRelatedBugs( List<Integer> firstbatchLocks ) {
    	System.out.println( "JX - findLockRelatedBugs" );
    	if ( firstbatchLocks.size() <= 0 ) {
    		System.out.println( "JX - CascadingBugDetection - there is no first batch of locks, finished normally!" );
    		return;
    	}
    	Set<Integer> curbatchLocks = new TreeSet<Integer>( firstbatchLocks );
    	int cascadingLevel = 2;   //this is the minimum level for lock-related cascading bugs
    	int tmpbatch = 0;
    	
    	while ( cascadingLevel <= CASCADING_LEVEL ) {
    		// Find affected locks in different threads
    		Set<Integer> nextbatchLocks = findNextbatchLocksInDiffThreads( curbatchLocks );
    		System.out.println("batch #" + (++tmpbatch) + ":#locks=" + curbatchLocks.size() + " <--- #" + tmpbatch + ".5:#locks=" + nextbatchLocks.size() );
    		// Find affected locks based on 1 in the same thread
    		curbatchLocks = findNextbatchLocksInSameThread( nextbatchLocks, cascadingLevel );
    		System.out.println("batch #" + (tmpbatch+1) + ":#intermediate locks=" + curbatchLocks.size()  );
    		if ( curbatchLocks.size() <= 0 ) {
    			System.out.println( "JX - CascadingBugDetection - finished normally" );
    			break;
    		}
    		cascadingLevel ++;
    	}
    	
    	System.out.println( "JX - CascadingBugDetection - finished with CASCADING_LEVEL = " + CASCADING_LEVEL );
    }
    
    
    // JX - Find affected locks in different threads
    public Set<Integer> findNextbatchLocksInDiffThreads( Set<Integer> batchLocks ) {
    	//ArrayList<Integer> nextbatchLocks = new ArrayList<Integer>();
    	Set<Integer> nextbatchLocks = new TreeSet<Integer>();
    	for (int lockindex: batchLocks) {
    		String pidopval0 = gb.getNodePIDOPVAL0(lockindex);
    		// 1. if not R lock; cuz R will not affect R, but a general obj.lock can affect the obj itself
    		if ( !gb.isReadOrWriteLock(lockindex).equals("R") ) {
    			ArrayList<Integer> list = gb.accurateLockmemref.get( pidopval0 );
	    		for (int index: list) {
	    			if (lockindex == index) continue;
	                if ( gb.flippedorder(lockindex, index) ) {
	                	nextbatchLocks.add( index );
	                	predNodes[index] = lockindex;
	                }
	    		}
    		}
    		// 2. if R/W lock
    		if ( !gb.isReadOrWriteLock(lockindex).equals("null") ) {
    			String correspondingPidopval0 = gb.rwlockmatch.get( pidopval0 )[1];
    			ArrayList<Integer> list = gb.accurateLockmemref.get( correspondingPidopval0 );   //or using dotlockmemref.get( xx )
    			for (int index: list) {
    				if ( gb.flippedorder(lockindex, index) ) {
    					nextbatchLocks.add( index );
    					predNodes[index] = lockindex;
    				}
    			}
    		}
    	}
    	return nextbatchLocks;
    }
    
    
    // JX - Find affected locks based on 'findNextbatchLocksInDiffThreads' in the same thread
    public Set<Integer> findNextbatchLocksInSameThread( Set<Integer> batchLocks, int cascadingLevel ) {
    	Set<Integer> nextbatchLocks = new TreeSet<Integer>();
		for (int index: batchLocks) {
			int beginIndex = index;
			if ( lockblocks.get(beginIndex) == null ) 
				continue;
			int endIndex = lockblocks.get( beginIndex );
			
			String pidopval0 = gb.getNodePIDOPVAL0( index );
			int loopflag = 0;
			for (int k = beginIndex; k <= endIndex; k++) {
				// TODO
				if ( gb.getNodeOPTY(k).equals("LoopBegin") ) {
					loopflag = 1;
		    		// add to bug pool
					addToBugPool( k, cascadingLevel );
				}
				if ( gb.getNodeOPTY(k).equals("LockRequire") ) {
					if ( !gb.getNodePIDOPVAL0(k).equals(pidopval0) )
						nextbatchLocks.add( k );
						predNodes[k] = index;
					//jx: it seems no need to check if the LockReuire has LockRelease or not
				}
			}
		}
		return nextbatchLocks;
    }
    
    
    public void addToBugPool( int nodeIndex, int cascadingLevel ) {
    	// add to bug pool
    	LoopBug loopbug = new LoopBug( nodeIndex, cascadingLevel );
    	bugpool.add( loopbug );	
    	// get cascading lock chain
    	if ( cascadingLevel == 1 ) { // Immediate loop bug
    		loopbug.lockchain.add( nodeIndex );
    	}
    	else if ( cascadingLevel >= 2 ) { // Lock-related loop bug
        	int tmp = nodeIndex;
        	while ( tmp != 0 ) {
        		loopbug.lockchain.add( tmp );
        		tmp = predNodes[ tmp ];
        	}
    	}
    	System.out.println( loopbug );
    }
    
	
    public void printResultsOfTraverseTargetCodes() {
    	System.out.println("\nJX - Results of traverseTargetCodes");
    	
    	System.out.println("\nbugpool - " + "has " + bugpool.size() + " nodes");
    	for (LoopBug loopbug: bugpool) {
    		int nodeIndex = loopbug.nodeIndex;
    		simplebugpool.add( gb.lastCallstack(nodeIndex) );
    		medianbugpool.add( gb.fullCallstack(nodeIndex) );
    	}
    	System.out.println("\nmedianbugpool - " + "has " + medianbugpool.size() + " loops");
    	for (String fullcallstack: medianbugpool) {
    		System.out.println( fullcallstack );
    	}
    	System.out.println("\nsimplebugpool - " + "has " + simplebugpool.size() + " loops");
    	for (String lastcallstack: simplebugpool) {
    		System.out.println( lastcallstack );
    	}
    	
    }
	
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // SubClasses
    /////////////////////////////////////////////////////////////////////////////////////////////////
    
    class LoopBug implements Comparable<LoopBug> {
    	
    	int nodeIndex;
    	int cascadingLevel;
    	LinkedList<Integer> lockchain;
    	
    	LoopBug(int nodeIndex) {
    		this.nodeIndex = nodeIndex;
    		this.cascadingLevel = 1;
    		this.lockchain = new LinkedList<Integer>();
    	}
    	
    	LoopBug(int nodeIndex, int cascadingLevel) {
    		this.nodeIndex = nodeIndex;
    		this.cascadingLevel = cascadingLevel;
    		this.lockchain = new LinkedList<Integer>();
    	}

		@Override
		public int compareTo(LoopBug arg0) {
			// TODO Auto-generated method stub
			return 0;
		}
		
    	@Override
    	public int hashCode() {
    		int result = 17;
    		result = 31 * result + nodeIndex;
    		result = 31 * result + lockchain.hashCode();
    		return result;
    	}
    	
    	@Override
    	public boolean equals(Object obj) {
    		if ( this == obj )
    			return true;
    		if ( obj == null || this.getClass() != obj.getClass() )
    			return false;
    		LoopBug other = (LoopBug) obj;
    		return nodeIndex == other.nodeIndex &&
    				( lockchain == other.lockchain || (lockchain!=null && lockchain.equals(other.lockchain)) );
    	}
    	
    	@Override
    	public String toString() {
    		String str = "BugLoop - cascadingLevel=" + cascadingLevel + " - " + gb.lastCallstack(nodeIndex);
    		return str;
    	}

    }
}

















