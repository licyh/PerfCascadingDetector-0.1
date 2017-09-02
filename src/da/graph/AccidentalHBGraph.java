package da.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import LogClass.LogType;
import da.tagging.JobTagger;


public class AccidentalHBGraph {
	
	HappensBeforeGraph hbg;
	LogInfoExtractor logInfo;
    //Added by JX
    public HashMap<String, ArrayList<Integer> > accurateLockmemref;  	//New: pid+opval0 -> set of nodes
    
    HashMap<String, ArrayList<Integer> > lockmemref;     	  	//JX: record all LockRequire/LockRelease. 
    HashMap<String, ArrayList<Integer> > lockmemrefType;  		//for test: lock mem addr -> [_1sync(ojb), _2sync method, _3lock];   the number of every kind of lock
    HashMap<String, ArrayList<Integer> > dotlockmemref;   		// same as above, but only type "_3" ie, xxx.lock
    public HashMap<String, String[]> rwlockmatch;               		// "pid"+"hashcode" -> "R/W", "pid"+"correspondinghashcode", "pid"+"superobjhashcode" 
    
    //ArrayList<ArrayList<Pair>> lockrelationedge;      //adjcent list of edges for lock relationship graph
    //ArrayList<ArrayList<Pair>> lockrelationbackedge;  //adjcent list of backward edges for tracing back
    
	
    //for validity test
    boolean DO_HBCONCURRENT = true;      //default should be true;
    //boolean DO_HBCONCURRENT = false;      //default should be true;
    
    
	public AccidentalHBGraph(HappensBeforeGraph hbg) {
		this.hbg = hbg;
		// extract Target, (EventHandler), Lock, Loop logs
		this.logInfo = new LogInfoExtractor( this.hbg, this);
		
		//lock-related
		this.accurateLockmemref = new HashMap<String, ArrayList<Integer> >();
        
        this.lockmemref = new HashMap<String , ArrayList<Integer>>();
        this.lockmemrefType = new HashMap<String , ArrayList<Integer>>();
        this.dotlockmemref = new HashMap<String, ArrayList<Integer> >();  
        this.rwlockmatch = new HashMap<String, String[]>();    
        
		
	}

	
	public LogInfoExtractor getLogInfoExtractor() {
		return this.logInfo;
	}
	
	
	public boolean isReadLock(int index) {
		String pidhashcode = hbg.getNodePIDOPVAL0(index);
		if ( !rwlockmatch.containsKey(pidhashcode) )
			return false;
		String rwType = rwlockmatch.get(pidhashcode)[0];   // [0] means "R" or "W"
		if (rwType.equals("R"))
			return true;
		return false;
	}

	
	public boolean isWriteLock(int index) {
		String pidhashcode = hbg.getNodePIDOPVAL0(index);
		if ( !rwlockmatch.containsKey(pidhashcode) )
			return false;
		String rwType = rwlockmatch.get(pidhashcode)[0];   // [0] means "R" or "W"
		if (rwType.equals("W"))
			return true;
		return false;
	}
	
	
	public boolean isReadOrWriteLock(int index) {
		if (isReadLock(index) || isWriteLock(index))
			return true;
		return false;
	}

	
	public boolean isRWLockPair(int index1, int index2) {
		if (isReadLock(index1) && isWriteLock(index2)
				|| isWriteLock(index1) && isReadLock(index2) ) {
			String pidhashcode1 = hbg.getNodePIDOPVAL0(index1);
			String pidhashcode2 = hbg.getNodePIDOPVAL0(index2);
			return rwlockmatch.get(pidhashcode1)[2].equals( rwlockmatch.get(pidhashcode2)[2] );         // [2] means "pid"+"superobjhashcode"	
		}
		else
			return false;
	}
	
	
	public boolean isSameLock(int index1, int index2) {
		String pidhashcode1 = hbg.getNodePIDOPVAL0(index1);
		String pidhashcode2 = hbg.getNodePIDOPVAL0(index2);
    	if ( pidhashcode1.equals(pidhashcode2) )
    		return true;
    	return false;
    }
	
	
    public boolean isRelevantLock(int index1, int index2) {
		String pidhashcode1 = hbg.getNodePIDOPVAL0(index1);
		String pidhashcode2 = hbg.getNodePIDOPVAL0(index2);
		// same hashcode: include single lock, R/R, W/W
    	if ( pidhashcode1.equals(pidhashcode2) )
    		return true;
    	// R/W
    	if ( isRWLockPair(index1, index2) )
    		return true;
    	return false;
    }
	
    
    
    
    
    
    /*****************************************************************************************************
     * Core
     *****************************************************************************************************/
    
    //Added by JX - analyze all locks
    public void buildLockmemref() {
    	System.out.println("\nJX - lock memory address analysis");
    	
    	int totalLockRequires = 0;
    	int typesOfTotalLockRequires[] = {0, 0, 0, 0};
    	int totalRWLockCreates = 0;
    	
    	// traverse all nodes to find 'lock-related' nodes
    	for ( int i = 0; i < hbg.nList.size(); i++) { 		
    		String opty = hbg.getNodeOPTY( i );
    		String opval = hbg.getNodeOPVAL( i );
    		String pid = hbg.getNodePID( i );

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
    			String pidopval0 = hbg.getNodePIDOPVAL0( i );
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
    				ArrayList<Integer> list = new ArrayList<Integer>(hbg.nList.size());
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
			System.out.println("JX - freq - " + entry.getKey() + " : " + entry.getValue().size() + " - " + hbg.lastCallstack(entry.getValue().get(0)));
		}
		// traverse newAccurateLockmemref
		//for (String memaddr: newAccurateLockmemref.keySet()) {
        //}
		
		for (String memaddr: dotlockmemref.keySet()) { 
			ArrayList<Integer> list = dotlockmemref.get(memaddr);
			System.out.println("JX - dotlock - " + memaddr + " : " + list.size() + " : " + hbg.lastCallstack(list.get(0)) );
			
			for (int index: list) {
        		if ( !isReadOrWriteLock(index) )
        			System.out.println("JX - ERROR???(that's FINE if generalobj.lock()) - " + hbg.lastCallstack(index) ); 
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
        			System.out.println( hbg.lastCallstack( list.get(i) ) );
        		}
        	}
            
            /*

        		
            */
        }
        System.out.println("#Crossing1/2/3LockTypes: " + "1-" + tmp[1] + " 2-" + tmp[2] + " 3-" + tmp[3] );
        System.out.println("N12 = " + N12 + " N13 = " + N13 + "  N23 = " + N23);
    }
    
	
    
    public boolean isContentionResource(int index) {
    	String type = hbg.getNodeOPTY(index);
    	if ( type.equals( LogType.LockRequire.name() )
    			|| type.equals( LogType.EventProcEnter.name() )
    			// part of the followings
    			//|| type.equals( LogType.MsgProcEnter.name() )
    			//|| type.equals( LogType.ThdEnter.name() )    				
    			) {
    		return true;
    	}
    	return false;
    }
    
    
    //newly added
    /**
     * CRCode - Lock(ie,PIDOPVAL0) or Queue(ie,PIDTID) which contention resources belong to  
     */
    public String getCRCode(int index) {
    	if ( hbg.getNodeOPTY(index).equals(LogType.LockRequire.name()) ) {
    		return hbg.getNodePIDOPVAL0(index);
    	}
    	else if ( hbg.getNodeOPTY(index).equals(LogType.EventProcEnter.name()) ) {
    		return hbg.getNodePIDTID(index);
    	}
    	return "XXX";
    }
    
    
    
    
    /**
     * Queue-related 
     */
    public void getContentionResources() {
    	
    }
    
    // ThdEnter, EventProcEnter, MsgProcEnter(RPC, socket)
    public Set<Integer> getHandlerCRs(int index) {
    	if ( hbg.getNodeOPTY(index).equals(LogType.ThdEnter.name()) ) {
    		return getXXXFrom(index, logInfo.getHandlerBlocks(), logInfo.getHandlerThreads());
    	}
    	else if ( hbg.getNodeOPTY(index).equals(LogType.EventProcEnter.name()) ) {
    		return getXXXFrom(index, logInfo.getEventHandlerBlocks(), logInfo.getEventHandlerThreads());
    	}
    	else if ( hbg.getNodeOPTY(index).equals(LogType.MsgProcEnter.name()) ) {
    		
    	}
    	else if ( hbg.getNodeOPTY(index).equals(LogType.EventHandlerBegin.name()) ) {
    		//TODO
    	}
    	return new HashSet<Integer>();
    }
    
 
    public Set<Integer> getXXXFrom(int index, LinkedHashMap<Integer, Integer> blocks, LinkedHashMap<Integer, Integer> threads) {
    	Set<Integer> results = new HashSet<Integer>();
    	String pidtid = hbg.getNodePIDTID(index);
		List<Integer> list = new ArrayList<>( blocks.keySet() );
		
		for (Entry<Integer, Integer> thread: threads.entrySet()) {
			int beginPos = thread.getKey();
			int endPos = thread.getValue();
			System.out.println("JX - DEBUG - getXXXFrom: node: " + hbg.getPrintedIdentity( list.get(beginPos)  ));
			if ( hbg.getNodePIDTID( list.get(beginPos) ).equals(pidtid) ) {
				for (int i = beginPos; i <= endPos; i++) {
					int beginIndex = list.get(i);
					if (blocks.get(beginIndex) == null) continue;
					System.out.println("JX - DEBUG - getXXXFrom: enter: " + hbg.getPrintedIdentity( beginIndex  ));
					int endIndex = blocks.get(beginIndex);
					
					if ( DO_HBCONCURRENT && !hbg.isConcurrent(index, beginIndex) ) continue; 
					results.add(beginIndex);
				}
				break;
			}
		}
		return results;
    }
    
	
}
