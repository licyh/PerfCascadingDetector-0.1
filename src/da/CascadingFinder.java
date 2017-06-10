package da;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.text.TextFileWriter;

public class CascadingFinder {

	String projectDir;
	String packageDir = "src/da";
	HappensBeforeGraph gb;
	AccidentalGraph ag;
	
    //In .traverseTargetCodes()
    ArrayList<Integer> alltargetitems;    				//all nodes of all TargetCodeBegin & TargetCodeEnd snippets
    //ArrayList<Integer> targetcodeLoops;  				//never used, just ps for time-consuming loops?
    ArrayList<Integer> targetcodeLocks;   				//set of lock nodes for a single target code snippet
    BitSet traversedNodes;                      		//tmp var. set of all nodes for a single target code snippet
    
    ArrayList<Integer> allloopitems = new ArrayList<Integer>();
    LinkedHashMap<Integer, Integer> targetblocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> lockblocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
    LinkedHashMap<Integer, Integer> loopblocks = new LinkedHashMap<Integer, Integer>();   // beginIndex -> endIndex
	
    // for results
    boolean ONLY_LOCK_RELATED_BUGS = true;                  //default
    int CASCADING_LEVEL = 10;                               //minimum:2; default:3;
    @SuppressWarnings("unchecked")
	HashMap<Integer, Integer>[] predNodes = new HashMap[ CASCADING_LEVEL + 1 ];  //record cascading paths, for different threads
    @SuppressWarnings("unchecked")
	HashMap<Integer, Integer>[] upNodes   = new HashMap[ CASCADING_LEVEL + 1 ];  //record cascading paths, for the same thread
    Set<LoopBug> bugpool = new HashSet<LoopBug>();          //dynamic loop instances, only one bug pool for whole code snippets
    Set<Integer> bugnodeset = new HashSet<Integer>();       //HappensBeforeGraph's node index set for bugpool 
    
    // for output
    String simplebugpoolFilename = "output/simple_bugpool.txt";
    String medianbugpoolFilename = "output/median_bugpool.txt";
    String simplechainbugpoolFilename = "output/simplechain_bugpool.txt";
    String medianchainbugpoolFilename = "output/medianchain_bugpool.txt";
    Set<String> simplebugpool = new TreeSet<String>();
	Set<String> medianbugpool = new TreeSet<String>();
	Set<String> simplechainbugpool = new TreeSet<String>();
	Set<String> medianchainbugpool = new TreeSet<String>();
	
	
	CascadingFinder(String projectDir, HappensBeforeGraph graphBuilder, AccidentalGraph ag) {
		this.projectDir = projectDir;
		this.gb = graphBuilder;
		this.ag = ag;
		
        this.alltargetitems = new ArrayList<Integer>();    //all nodes of all TargetCodeBegin & TargetCodeEnd snippets
        //targetcodeLoops = new ArrayList<Integer>();                              //never used, just ps for time-consuming loops?
        this.targetcodeLocks = new ArrayList<Integer>();   //set of lock nodes for a single target code snippet
        this.traversedNodes = new BitSet();                //tmp var. set of all nodes for a single target code snippet
        
        for (int i = 1; i <= CASCADING_LEVEL; i++) {
        	this.predNodes[i] = new HashMap<Integer, Integer>();
        	this.upNodes[i]   = new HashMap<Integer, Integer>();
        }
        //this.predNodes = new int[this.gb.nList.size()];
	}
	
	
	public void doWork() {
		// extract Target, Lock, Loop logs
		extractTargetLockLoopInfo();
        // traverseTargetCodes
		traverseTargetCodes();
    	// print the results
		try {
			printResultsOfTraverseTargetCodes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    			String opval = gb.getNodeOPVAL(i);
    			int reenter = 1;
    			for (int j = i+1; j < gb.nList.size(); j++) {
    				if ( !gb.getNodePIDTID(j).equals(pidtid) ) break;
    				// modified: bug fix
    				if ( gb.getNodeOPTY(j).equals("LockRequire") && gb.getNodeOPVAL(j).equals(opval) )  
    					reenter ++;
    				if ( gb.getNodeOPTY(j).equals("LockRelease") && gb.getNodeOPVAL(j).equals(opval) ) {
    					reenter --;
    					if (reenter == 0) {
    						lockblocks.put(i, j);
    						break;
    					}
    				}
    				// end - modified
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
    					// System.out.println("JX - WARN - " + "couldn't find LoopEND for LoopBegin " + i + " its index = " + iindex);
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
    	}
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
    		if ( ONLY_LOCK_RELATED_BUGS ) {
    			//Do Nothing
    		} else {
    			addToBugPool( x, 1 );
    		}
    	}

        List<Pair> list = gb.edge.get(x);
        for (Pair pair: list) {
        	int y = pair.destination;
        	if ( !traversedNodes.get(y) && gb.reachbitset.get(y).get(endIndex) )
        		dfsTraversing( y, endIndex );
        }
    }
    
    
    // 2. findLockRelatedBugs
    int tmpflag = 0;  //just for test
    /** JX - findLockRelatedBugs - */    
    public void findLockRelatedBugs( List<Integer> firstbatchLocks ) {
    	System.out.println( "JX - findLockRelatedBugs" );
    	if ( firstbatchLocks.size() <= 0 ) {
    		System.out.println( "JX - CascadingBugDetection - there is no first batch of locks, finished normally!" );
    		return;
    	}
		// Debugging - print all firt batch of locks' names
		if (tmpflag == 0) {
			for (int index: firstbatchLocks)
				System.out.println( "including lock - " 
						+ "Node"+index + ":" + gb.getNodePIDTID(index) + ":" + gb.getNodeOPVAL(index) + gb.lastCallstack_2(index) );
		}
		
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
            if ( tmpflag == 0 && curCascadingLevel == 2 ) {
    			for (int index: nextbatchLocks)
    				System.out.println( "including lock - " 
    						+ "Node"+index + ":" + gb.getNodePIDTID(index) + ":" + gb.getNodeOPVAL(index) + gb.lastCallstack_2(index) );
            }
                
            // Find affected locks based on 1 in the same thread
    		curbatchLocks = findNextbatchLocksInSameThread( nextbatchLocks, curCascadingLevel );
    		System.out.println("batch #" + (tmpbatch+1) + ":#intermediate locks=" + curbatchLocks.size()  );
    		// Debugging
            if ( tmpflag == 0 && curCascadingLevel == 2 ) {
    			for (int index: curbatchLocks)
    				System.out.println( "including lock - " 
    						+ "Node"+index + ":" + gb.getNodePIDTID(index) + ":" + gb.getNodeOPVAL(index) + gb.lastCallstack_2(index) );
    			tmpflag = 1;
            }
    		
        	if ( curbatchLocks.size() <= 0 ) {
    			System.out.println( "JX - CascadingBugDetection - finished normally" );
    			break;
    		}
    		curCascadingLevel ++;
    	}
    	
    	System.out.println( "JX - CascadingBugDetection - finished with CASCADING_LEVEL = " + CASCADING_LEVEL );
    }
    
    
    // JX - Find affected locks in different threads
    public Set<Integer> findNextbatchLocksInDiffThreads( Set<Integer> batchLocks, int curCascadingLevel ) {
    	//ArrayList<Integer> nextbatchLocks = new ArrayList<Integer>();
    	Set<Integer> nextbatchLocks = new TreeSet<Integer>();
    	for (int lockindex: batchLocks) {
    		String pidopval0 = gb.getNodePIDOPVAL0(lockindex);
    		// 1. if not R lock; cuz R will not affect R, but a general obj.lock can affect the obj itself
    		if ( !ag.isReadOrWriteLock(lockindex).equals("R") ) {
    			ArrayList<Integer> list = ag.accurateLockmemref.get( pidopval0 );
	    		for (int index: list) {
	    			if (lockindex == index) continue;
	                if ( gb.isFlippedorder(lockindex, index) ) {
	                	nextbatchLocks.add( index );
	                	predNodes[curCascadingLevel].put(index, lockindex); 
	                }
	    		}
    		}
    		// 2. if R/W lock
    		if ( !ag.isReadOrWriteLock(lockindex).equals("null") ) {
    			String correspondingPidopval0 = ag.rwlockmatch.get( pidopval0 )[1];
    			ArrayList<Integer> list = ag.accurateLockmemref.get( correspondingPidopval0 );   //or using dotlockmemref.get( xx )
    			for (int index: list) {
    				if ( gb.isFlippedorder(lockindex, index) ) {
    					nextbatchLocks.add( index );
    					predNodes[curCascadingLevel].put(index, lockindex);
    				}
    			}
    		}
    	}
    	return nextbatchLocks;
    }
    
    
    // JX - Find affected locks based on 'findNextbatchLocksInDiffThreads' in the same thread
    public Set<Integer> findNextbatchLocksInSameThread( Set<Integer> batchLocks, int curCascadingLevel ) {
    	Set<Integer> nextbatchLocks = new TreeSet<Integer>();
		for (int index: batchLocks) {
			int beginIndex = index;
			if ( lockblocks.get(beginIndex) == null ) 
				continue;
			int endIndex = lockblocks.get( beginIndex );
			
			String pidopval0 = gb.getNodePIDOPVAL0( index );
			int loopflag = 0;
			for (int k = beginIndex; k <= endIndex; k++) {         /////////JXXXXXXXXXXX - here seems a big bug, I didn't find into RPC or method call
				// TODO
				if ( gb.getNodeOPTY(k).equals("LoopBegin") ) {
					loopflag = 1;
		    		// add to bug pool
					upNodes[curCascadingLevel].put(k, index);
					addToBugPool( k, curCascadingLevel );
				}
				if ( gb.getNodeOPTY(k).equals("LockRequire") ) {
					if ( !gb.getNodePIDOPVAL0(k).equals(pidopval0) ) {  // yes, it's right
						nextbatchLocks.add( k );
						upNodes[curCascadingLevel].put(k, index);
						//jx: it seems no need to check if the LockReuire has LockRelease or not
					}
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
    	//jx: had better commented this when #targetcode is large or #loopbug is large
    	//System.out.println( loopbug );
    }
    
	
    public void printResultsOfTraverseTargetCodes() throws IOException {
    	System.out.println("\nJX - Results of traverseTargetCodes");
    	
    	// real bug pool
    	System.out.print("\nbugpool - " + "has " + bugpool.size() + " dynamic loop instances");
    	Set<String> tmpset = new HashSet<String>();
    	for (LoopBug loopbug: bugpool) {
    		int nodeIndex = loopbug.nodeIndex;
    		int cascadingLevel = loopbug.cascadingLevel;
    		bugnodeset.add( nodeIndex );
    		medianchainbugpool.add( "CL" + cascadingLevel + ": " + fullCallstacksOfCascadingChain(loopbug) );
    		simplechainbugpool.add( "CL" + cascadingLevel + ": " + lastCallstacksOfCascadingChain(loopbug) );
    		medianbugpool.add( "CL" + cascadingLevel + ": " + gb.fullCallstack(nodeIndex) );
    		simplebugpool.add( "CL" + cascadingLevel + ": " + gb.lastCallstack(nodeIndex) );
    		tmpset.add( gb.lastCallstack(nodeIndex) );
    	}
    	System.out.println(", ie, representing " + bugnodeset.size() + " nodes out of total " + gb.nList.size() + " nodes");

    	// bug pools - 
        // write to file & print
    	medianchainbugpoolFilename = Paths.get(projectDir, packageDir, medianchainbugpoolFilename).toString();
    	simplechainbugpoolFilename = Paths.get(projectDir, packageDir, simplechainbugpoolFilename).toString();
    	medianbugpoolFilename = Paths.get(projectDir, packageDir, medianbugpoolFilename).toString();
    	simplebugpoolFilename = Paths.get(projectDir, packageDir, simplebugpoolFilename).toString();
    	System.out.println( "\nwrite to files - " );
    	System.out.println( "\t" + medianchainbugpoolFilename );
    	System.out.println( "\t" + simplechainbugpoolFilename );
    	System.out.println( "\t" + medianbugpoolFilename );
    	System.out.println( "\t" + simplebugpoolFilename );
		
    	TextFileWriter mcWriter = new TextFileWriter( medianchainbugpoolFilename );
    	TextFileWriter scWriter = new TextFileWriter( simplechainbugpoolFilename );
    	TextFileWriter mWriter = new TextFileWriter( medianbugpoolFilename );
    	TextFileWriter sWriter = new TextFileWriter( simplebugpoolFilename );

    	System.out.println("\nmedianchainbugpool(whole chain's fullcallstacks) - " + "has " + medianchainbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	mcWriter.writeLine( medianchainbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String chainfullcallstacks: medianchainbugpool) {
    		//System.out.println( chainfullcallstacks );
    		mcWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nsimplechainbugpool(whole chain's lastcallstacks) - " + "has " + simplechainbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	scWriter.writeLine( simplechainbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String chainfullcallstacks: simplechainbugpool) {
    		System.out.println( chainfullcallstacks );
    		scWriter.writeLine( chainfullcallstacks );
    	}
    	
    	System.out.println("\nmedianbugpool(loop's fullcallstack) - " + "has " + medianbugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")" );
    	mWriter.writeLine( medianbugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String fullcallstack: medianbugpool) {
    		//System.out.println( fullcallstack );
    		mWriter.writeLine( fullcallstack );
    	}
    	
    	System.out.println("\nsimplebugpool(loop's lastcallstack) - " + "has " + simplebugpool.size() + " loops (#static codepoints=" + tmpset.size() + ")");
    	sWriter.writeLine( simplebugpool.size() + " (#static codepoints=" + tmpset.size() + ")" );
    	for (String lastcallstack: simplebugpool) {
    		System.out.println( lastcallstack );
    		sWriter.writeLine( lastcallstack );
    	}
    	
    	mcWriter.close();
    	scWriter.close();
    	mWriter.close();
    	sWriter.close();
    }
    
    public String fullCallstacksOfCascadingChain(LoopBug loopbug) {
    	String result = "";
    	for (int nodeindex: loopbug.cascadingChain) {
    		result += gb.fullCallstack(nodeindex) + "|";
    		// for DEBUG
    		//result += gb.getNodePIDTID(nodeindex) + ":" + gb.fullCallstack(nodeindex) + "|";
	        //result += gb.getNodePIDTID(nodeindex)+":"+nodeindex + ":" + gb.fullCallstack(nodeindex) + "|";
    	}
    	return result;
    }
    
    public String lastCallstacksOfCascadingChain(LoopBug loopbug) {
    	String result = "";
    	for (int nodeindex: loopbug.cascadingChain) {
    		result += gb.lastCallstack(nodeindex) + "|";
    		// for DEBUG
    		//result += gb.getNodePIDTID(nodeindex) + ":" + gb.lastCallstack(nodeindex) + "|";
    	}
    	return result;
    }
    

    
    
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // SubClasses
    /////////////////////////////////////////////////////////////////////////////////////////////////
    
    class LoopBug { 
    	
    	int nodeIndex;
    	int cascadingLevel;
    	LinkedList<Integer> cascadingChain;
    	
    	LoopBug(int nodeIndex) {
    		this.nodeIndex = nodeIndex;
    		this.cascadingLevel = 1;
    		this.cascadingChain = new LinkedList<Integer>();
    	}
    	
    	LoopBug(int nodeIndex, int cascadingLevel) {
    		this.nodeIndex = nodeIndex;
    		this.cascadingLevel = cascadingLevel;
    		this.cascadingChain = new LinkedList<Integer>();
    	}
    	
		
		//useless now
    	@Override
    	public int hashCode() {
    		int result = 17;
    		//result = 31 * result + nodeIndex;
    		//result = 31 * result + cascadingLevel;
    		//result = 31 * result + cascadingChain.hashCode();         //this one has some problem!!!   or maybe the below equals' problem
    		//return result;
    		return Objects.hash( nodeIndex, cascadingLevel, cascadingChain );
    	}
    	
    	//useless now
    	@Override
    	public boolean equals(Object o) {
    		if ( this == o )
    			return true;
    		if ( o == null || this.getClass() != o.getClass() )
    			return false;
    		LoopBug other = (LoopBug) o;
    		return nodeIndex == other.nodeIndex
    				&& cascadingLevel == other.cascadingLevel
    				&& ( cascadingChain == other.cascadingChain || (cascadingChain!=null && cascadingChain.equals(other.cascadingChain)) );
    				
    	}
    	
    	@Override
    	public String toString() {
    		String str = "BugLoop - cascadingLevel=" + cascadingLevel + " - " + gb.lastCallstack(nodeIndex);
    		return str;
    	}

    }
}

















