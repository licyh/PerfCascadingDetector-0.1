package da;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import da.GraphBuilder.IdPair;

public class AccidentalGraph {
	
	GraphBuilder gb;
    //Added by JX
    HashMap<String, ArrayList<Integer> > accurateLockmemref;  	//New: pid+opval0 -> set of nodes
    
    HashMap<String, ArrayList<Integer> > lockmemref;     	  	//JX: record all LockRequire/LockRelease. 
    HashMap<String, ArrayList<Integer> > lockmemrefType;  		//for test: lock mem addr -> [_1sync(ojb), _2sync method, _3lock];   the number of every kind of lock
    HashMap<String, ArrayList<Integer> > dotlockmemref;   		// same as above, but only type "_3" ie, xxx.lock
    HashMap<String, String[]> rwlockmatch;               		// "pid"+"hashcode" -> "R/W", "pid"+"correspondinghashcode", "pid"+"superobjhashcode" 
    
    //ArrayList<ArrayList<Pair>> lockrelationedge;      //adjcent list of edges for lock relationship graph
    //ArrayList<ArrayList<Pair>> lockrelationbackedge;  //adjcent list of backward edges for tracing back
    
	
	AccidentalGraph(GraphBuilder graphBuilder) {
		this.gb = graphBuilder;
        accurateLockmemref = new HashMap<String, ArrayList<Integer> >();
        
        lockmemref = new HashMap<String , ArrayList<Integer>>();
        lockmemrefType = new HashMap<String , ArrayList<Integer>>();
        dotlockmemref = new HashMap<String, ArrayList<Integer> >();  
        rwlockmatch = new HashMap<String, String[]>();               
	}

	public String isReadOrWriteLock(int index) {
		String pidhashcode = gb.getNodePIDOPVAL0(index);
		if ( rwlockmatch.containsKey(pidhashcode) ) {
			return rwlockmatch.get(pidhashcode)[0];   // [0] means "R" or "W"
		}
		else {
			return "null";
		}
	}

	public boolean isRelatedLocks(int index1, int index2) {
		String pidhashcode1 = gb.getNodePIDOPVAL0(index1);
		String pidhashcode2 = gb.getNodePIDOPVAL0(index2);
		return rwlockmatch.get(pidhashcode1)[2].equals( rwlockmatch.get(pidhashcode2)[2] );         // [1] means "pid"+"superobjhashcode"
	}
	
    //Added by JX - analyze all locks
    public void buildLockmemref() {
    	System.out.println("\nJX - lock memory address analysis");
    	
    	int totalLockRequires = 0;
    	int typesOfTotalLockRequires[] = {0, 0, 0, 0};
    	int totalRWLockCreates = 0;
    	
    	// traverse all nodes to find 'lock-related' nodes
    	for ( int i = 0; i < gb.nList.size(); i++) { 		
    		String opty = gb.getNodeOPTY( i );
    		String opval = gb.getNodeOPVAL( i );
    		String pid = gb.getNodePID( i );

    		// get all lock memory addresses
    		if ( opty.equals("LockRequire") 
    				//|| opty.equals("LockRelease") 
    				) {
    			totalLockRequires ++;
    			
    			String [] opvalarray = opval.split("_");  // _1 _2 _3
    			String memaddr = opvalarray[0];
    			int locktype = Integer.valueOf( opvalarray[1] );
    			typesOfTotalLockRequires[ locktype ] ++;
    			
    			// build accurateLockmemref: all locks, use the key of 'pid+opval0'
    			String pidopval0 = gb.getNodePIDOPVAL0( i );
    			if (accurateLockmemref.get(pidopval0) == null) {
    				ArrayList<Integer> list = new ArrayList<Integer>();
    				accurateLockmemref.put(pidopval0, list);
    				ArrayList<Integer> typelist = new ArrayList<Integer>(4);  // [0, _1sync(ojb), _2sync method, _3lock];   the number of every kind of lock
    				typelist.add(new Integer(0));typelist.add(new Integer(0));typelist.add(new Integer(0)); typelist.add(new Integer(0));
    				lockmemrefType.put(pidopval0, typelist);
    			} 
    			accurateLockmemref.get(pidopval0).add( i );
    			lockmemrefType.get(pidopval0).set( locktype, new Integer(lockmemrefType.get(pidopval0).get(locktype).intValue() + 1) );
    			
    			// build lockmemref: all locks, including 3 types
    			if (lockmemref.get(memaddr) == null) {
    				ArrayList<Integer> list = new ArrayList<Integer>(gb.nList.size());
    				lockmemref.put(memaddr, list);
    			} 
    			lockmemref.get(memaddr).add( i );
    			
    			// build dotlockmemref: only get type "_3" ie xxx.lock
    			if (locktype == 3) {
        			if (dotlockmemref.get(pidopval0) == null) {
        				ArrayList<Integer> list = new ArrayList<Integer>();
        				dotlockmemref.put(pidopval0, list);
        			} 
    				dotlockmemref.get(pidopval0).add( i );
    			}
    		}
    		    		
    		// get all rwlock matches, that is, 1 ReadWriteLock -> 1 Readlock + 1 Writelock
    		if ( opty.equals("RWLockCreate") ) {
    			totalRWLockCreates ++;
    			String [] opvalarray = opval.split("\\|");  //JX - "|" needs to be written as "\\|"
    	        String pidopval0;
    	        // ReadLock
    	        pidopval0 = pid + opvalarray[1]; 
    	        if (rwlockmatch.get(pidopval0) == null) {
    	        	String[] strs = new String[3];
    	        	strs[0] = "R";
    	        	strs[1] = pid + opvalarray[2];
    	        	strs[2] = pid + opvalarray[0];
    	        	rwlockmatch.put(pidopval0, strs);    // "pid"+"hashcode" -> "R/W", "pid"+"correspondinghashcode", "pid"+"superobjhashcode"
    	        } else {
    	        	System.out.println("JX - ERROR - " + "NOT rwlockmemref.get(str) == null - R");
    	        }
    	        // WriteLock
    	        pidopval0 = pid + opvalarray[2]; 
    	        if (rwlockmatch.get(pidopval0) == null) {
    	        	String[] strs = new String[3];
    	        	strs[0] = "W";
    	        	strs[1] = pid + opvalarray[1];
    	        	strs[2] = pid + opvalarray[0];
    	        	rwlockmatch.put(pidopval0, strs);    // "pid"+"hashcode" -> "R/W", "pid"+"correspondinghashcode", "pid"+"superobjhashcode"
    	        } else { 
    	        	System.out.println("JX - ERROR - " + "NOT rwlockmemref.get(str) == null - W");
    	        }
    		}
    	}
    	System.out.println("#totalRWLockCreates = " + totalRWLockCreates);
    	System.out.println("#totalLockRequires = " + totalLockRequires);
    	System.out.println("#_1sync(obj):"+typesOfTotalLockRequires[1] + "  #_2syncMethod:"+typesOfTotalLockRequires[2] + "  #_3lock:"+typesOfTotalLockRequires[3] );
    	
    	
    	System.out.println("#total accurateLockmemref = " + accurateLockmemref.size());
        System.out.println("#total lockmemaddr = " + lockmemref.size());
        System.out.println("#total dotlockmemaddr = " + dotlockmemref.size());
        // count the frequence of every accurateLock   #for filtering if needed
		List< Map.Entry<String, ArrayList<Integer>> > tmplist = new LinkedList< Map.Entry<String, ArrayList<Integer>> >( accurateLockmemref.entrySet() ); 
		Collections.sort( tmplist, new Comparator<Map.Entry<String, ArrayList<Integer>>>() {
			public int compare(Map.Entry<String, ArrayList<Integer>> o1, Map.Entry<String, ArrayList<Integer>> o2) {
				return o2.getValue().size() - o1.getValue().size();
			}
		});
		Map<String, ArrayList<Integer>> newAccurateLockmemref = new LinkedHashMap<String, ArrayList<Integer>>();
		int tmpi = 0;
		for (Map.Entry<String, ArrayList<Integer>> entry: tmplist) {
			newAccurateLockmemref.put(entry.getKey(), entry.getValue());
			if (++tmpi <= 10)
			System.out.println("JX - freq - " + entry.getKey() + " : " + entry.getValue().size() + " - " + gb.lastCallstack(entry.getValue().get(0)));
		}
		// traverse newAccurateLockmemref
		//for (String memaddr: newAccurateLockmemref.keySet()) {
        //}
		
		for (String memaddr: dotlockmemref.keySet()) { 
			ArrayList<Integer> list = dotlockmemref.get(memaddr);
			System.out.println("JX - dotlock - " + memaddr + " : " + list.size() + " : " + gb.lastCallstack(list.get(0)) );
			
			for (int index: list) {
        		if ( isReadOrWriteLock(index).equals("null") )
        			System.out.println("JX - ERROR???(that's FINE if generalobj.lock()) - " + gb.lastCallstack(index) ); 
			}
		}
		
        //for Debug
        int tmp[] = {0, 0, 0, 0};
        int N12 = 0;
        int N13 = 0; 
        int N23 = 0;
        for (String memaddr : accurateLockmemref.keySet()) {
            ArrayList<Integer> list = accurateLockmemref.get(memaddr);
            ArrayList<Integer> typelist = lockmemrefType.get( memaddr );
            /*
            System.out.println("addr " + memaddr + " has " + list.size() + " locks" 
            		+ "\t_1sync(obj)="+typelist.get(1) + "\t_2syncMethod="+typelist.get(2) + "\t_3lock="+typelist.get(3));
            */
        	int numTypes = 0;
        	if ( typelist.get(1) > 0 ) numTypes ++;
        	if ( typelist.get(2) > 0 ) numTypes ++;
        	if ( typelist.get(3) > 0 ) numTypes ++;
        	tmp[ numTypes ] ++;

        	if ( typelist.get(1) > 0 && typelist.get(2) > 0 ) {
        		N12++;
        	}
        	
        	if ( typelist.get(1) > 0 && typelist.get(3) > 0 ) {
        		N13++;
        	}
        	if ( typelist.get(2) > 0 && typelist.get(3) > 0 ) {
        		N23++;
        		System.out.println("--------------------------------------------------------------------------------------------------------------" );
        		for (int i = 0; i < list.size(); i++) {
        			System.out.println( gb.lastCallstack( list.get(i) ) );
        		}
        	}
            
            /*

        		
            */
        }
        System.out.println("#Crossing1/2/3LockTypes: " + "1-" + tmp[1] + " 2-" + tmp[2] + " 3-" + tmp[3] );
        System.out.println("N12 = " + N12 + " N13 = " + N13 + "  N23 = " + N23);
    }
    
    //end-Added
	
    

    
	
}
