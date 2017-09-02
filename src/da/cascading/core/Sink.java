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
import da.tagging.JobTagger;

public class Sink {

	String ID;
	List<SinkInstance> instances;

	
	String projectDir;
	HappensBeforeGraph hbg;
	AccidentalHBGraph ag;
	LogInfoExtractor logInfo;
	JobTagger jt;
	
    //
    CascadingUtil cascadingUtil;
    //Results
    BugPool bugPool;

    
    public Sink() {
		this.instances = new ArrayList<SinkInstance>();
	}
	
    public void setID(String ID) {
    	this.ID = ID;
    }
    
    public String getID() {
    	return this.ID;
    }
    
	public void addInstance(SinkInstance instance) {
		this.instances.add(instance);
	}
	
	public String toString() {
		return "ID" + this.ID + "-" + "#instances" + this.instances.size();
	}
	
    
	public void setEnv(String projectDir, HappensBeforeGraph hbg, AccidentalHBGraph ag, LogInfoExtractor logInfo,
						JobTagger jt) {
		this.projectDir = projectDir;
		this.hbg = hbg;
		this.ag = ag;
		this.logInfo = logInfo;
        this.jt = jt;
	}
	
	
	public void doWork() {
		this.cascadingUtil = new CascadingUtil(this.projectDir, this.hbg, this.ag, this.ag.getLogInfoExtractor());
		this.bugPool = new BugPool(this.projectDir, this.hbg);
		handleSink();               // traverseTargetCodes
		bugPool.printResults(true);     // print the results
		//bugPool.printJobIdentity();
	}
	
	
	
	
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
    public void handleSink() {
    	System.out.println("\nJX - INFO - Sink: handleSink, ie, all TARGET CODE snippets for a sink");
    	
    	int numofsnippets = 0;
    	
    	for (SinkInstance instance: this.instances) {
    		System.out.println( "\n#" + (++numofsnippets) + "-" + instance );
    		instance.setEnv(this.projectDir, this.hbg, this.ag, this.ag.getLogInfoExtractor(), this.jt);
    		instance.setCascadingUtil(this.cascadingUtil);
    		instance.setBugPool(this.bugPool);
    		instance.doWork();
    		//for DEBUG
    		//break;
    	}
    	
    }
    
}









