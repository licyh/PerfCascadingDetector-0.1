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

public class Sink {

	List<SinkInstance> instances;

	
	String projectDir;
	HappensBeforeGraph hbg;
	AccidentalHBGraph ag;
	LogInfoExtractor logInfo;
	
    //used for pruning 
    //HashMap<Integer, Integer>[] curNodes = new HashMap[ CASCADING_LEVEL + 1 ];
    HashMap<Integer, HashSet<String>> outerLocks = new HashMap<Integer, HashSet<String>>(); // lock -> ourter locks
    
    //Results
    BugPool bugPool;

    
    public Sink() {
		this.instances = new ArrayList<SinkInstance>();
	}
	
	public void addInstance(SinkInstance instance) {
		this.instances.add(instance);
	}
    
	public void setEnv(String projectDir, HappensBeforeGraph hbg, AccidentalHBGraph ag, LogInfoExtractor logInfo) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		this.ag = ag;
		this.logInfo = logInfo;

        this.bugPool = new BugPool(this.projectDir, this.hbg);
	}
	
	public void doWork() {
		// prepare
		prepare();
        // traverseTargetCodes
		handleSink();
    	// print the results
		bugPool.printResults();
	}
	
	
	
	
	/******************************************************************************
	 * Core
	 ******************************************************************************/
	
	public void prepare() {
		
		for (int lockIndex: logInfo.getLockBlocks().keySet()) {
			if (logInfo.getLockBlocks().get(lockIndex) == null) continue;
			int lockBegin = lockIndex;
			int lockEnd = logInfo.getLockBlocks().get(lockIndex);
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
     * ie, 
     * Note: may involve two types of sink code snippets
     * 		- TargetCodeBegin & TargetCodeEnd
     * 		- tmp: (not here) EventHandlerBegin & EventHandlerEnd,  this should also be changed to  TargetCodeBegin & TargetCodeEnd
     */
    public void handleSink() {
    	System.out.println("\nJX - traverseTargetCodes - including all TARGET CODE snippets");
    	
    	int numofsnippets = 0;
    	
    	for (SinkInstance instance: this.instances) {
    		System.out.println( "\nTarget Code Snippet #" + (++numofsnippets) + instance  );
    		instance.setEnv(this.projectDir, this.hbg, this.ag, this.ag.getLogInfoExtractor());
    		instance.setBugPool(this.bugPool);
    		instance.setOuterLocks(this.outerLocks);
    		instance.handleSinkInstance( instance.getBeginIndex(), instance.getEndIndex() );
    	}
    	
    }
    
}









