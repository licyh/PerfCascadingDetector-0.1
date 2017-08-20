/**
 * Created by guangpu on 3/21/16.
 */
package da.graph;
import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import org.apache.commons.io.comparator.NameFileComparator;

import org.w3c.dom.*;

import com.text.TextFileReader;


/*
// JX - this is form class '_DM_Log'
public enum OPTYPE {
    LockRequire, 		//require lock    //JX:
    LockRelease, 		//release lock    //JX:
    RWLockCreate,                         //JX:
    
    ThdCreate,  		//create thread        //JX: including "(servers') handler threads" for processing requests/events. NO need including listeners, they are already hidden by RPC-MsgSending/MsgProcEnter/MsgProcExit
    ThdEnter,           //enter thread
    ThdExit,    		//exit thread
    ThdJoin,    		//join thread
    MsgSending, 		//msg sending(rpc/sendSocket)   //JX: yes, only RPC/Socket. The general method calls don't belong to here, they are already included in logging 
    MsgProcEnter, 		//msg handler enter    //JX: this is just a unit for handling a RPC/Socket request. No a handler thread.
    MsgProcExit, 		//msg handler exit
    EventCreate, 		//event create         //JX: like .put/.submit for generating and submit a Event. Note: like method "handle()", but actually this is a 'Enter' in MapReduce, 'GenericEventHandler'-related.
    EventProcEnter, 	//event handler enter  //JX: this is just a unit for handling a event. Not a handler thread
    EventProcExit, 		//event handler exit   
    ProcessCreate, 		//process create
    HeapRead,   		//read a heap var
    HeapWrite,  		//write a heap var
};
*/


public class HappensBeforeGraph {

	//Class - thread ID
    class IdPair implements Comparable<IdPair>{
        int pid;
        int tid;
        public IdPair(int p, int t) {
            this.pid = p;
            this.tid = t;
        }
        public String toString(){
            return "["+pid+","+tid+"]";
        }
        public int compareTo(IdPair idPair){
            Integer Tid = this.tid;
            return Tid.compareTo(idPair.tid);
        }

    }

    class EgPair {
        int source;
        int destination;
        public EgPair( int f, int t){
            source = f;
            destination = t;
        }
    }

    String xmldir;                                    // the directory containing log files to process
    Document inputdoc;				      // xml input variable	
    Document outputdoc ;			      // xml output file
    Element  docroot;

    ArrayList<Node> nList;                            //node list of all the file, JX - each node is 'Node' in XML doc
    ArrayList<ArrayList<Pair>> edge;                  //adjcent list of edges of the happen before graph
    ArrayList<ArrayList<Pair>> backedge;              //adjcent list of backward edges for tracing back
    ArrayList<HashSet<Integer>> reach;                //reach[i] = set of reachable node form Node I
    HashMap <String, ArrayList<Integer> >memref;      //JX: record HeapRead/HeapWrite. ie, memref[addr] = set of nodes which read or write memory location addr
    
    ArrayList<IdPair> idplist;                        //JX: pid/tid for all nodes. ie, idplist[i] includes the pid and tid of node i
    HashMap <IdPair, ArrayList<Integer>> ptidref;     //ptidref(idpair) includes the node in ptid from the beginning to end
    HashMap <String, ArrayList<Integer>> typeref;     //add the same optype to a same list

    HashMap <String, ArrayList<Integer>> hashMsgSending;     //hashMsg(key) is a list of nodes whose OPVAL is the key and type is msgsending
    HashMap <String, ArrayList<Integer>> hashMsgProcEnter;   //hashNsgProcEnter(key) is a list of nodes whose OPVAL is the key and type is msgProcEnter

    HashMap <IdPair, ArrayList<Integer>> ptidsyncref; //ptidsyncref(idpair, i) means ith sync node in idpair thread
    //sync node includes MsgSending MsgProcEnter EventProcEnter ThdCreate ThdEnter
    //
    ArrayList<HashMap <IdPair, Integer>> vectorclock; // vectorclock of one node is a hashmap <ptid,int>... 
    ArrayList<ArrayList<EgPair>> syncedges;           // sync edges set, rpc call event handle...

    ArrayList<BitSet> reachbitset;                    // 
    ArrayList<Integer> eventend;                      // i is a event start and eventend[i] is its end
    ArrayList<Integer> eventcaller;		              // i is a event start and eventcaller[i] is its caller	
    HashMap <IdPair, ArrayList<Integer>> ptideventref; //
    ArrayList<Integer> emlink;                         // event msg back tracking
    ArrayList<Integer> emlink2;                        // event msg back tracking version 2
    int [] flag;
    ArrayList<Integer> root;                    //JX-unheaded nodes?      //(Wrong)haopeng note: unheaded threads 
    int esum;
    HashMap <IdPair, Integer> outputlist; 		//for simple output #JX:yes
    HashMap <IdPair, Integer> outputlist2;		//for median output #JX:yes
    HashMap <String, Integer> identity;			//group the same operation

    ArrayList<Integer> msender; 		//where is the message sender
    ArrayList<Integer> mexit;			//where is the message exit
    ArrayList<Integer> eexit;			//where is the event exit
					//where is the event enqueue [eventcaller]
    ArrayList<Integer> locktrace;
    int eedgesum;
    int medgesum;
    int tedgesum; 
    ArrayList<Integer> zkcreatelist;
    int special4637 = 0;
    int getcurdotrans = 0;
    int twodotrans = 0;
    int unigetstate = 0;
    int unitwotrans = 0;
    int uni3=0;
    int uni4=0;
    boolean mr = false;
    boolean hb = false;	
    boolean hd = false;
    
    boolean samethread(int x, int y) {
    	IdPair ipx = idplist.get(x);
    	IdPair ipy = idplist.get(y);
    	if (ipx.pid != ipy.pid) return false;
    	if (ipx.tid != ipy.tid) return false;
    	return true;
    }
    
    
    public String getTargetDir() {
    	return this.xmldir;
    }
    
    
    public HappensBeforeGraph(String xmldirctory) {
        xmldir = xmldirctory;
        
	    if (xmldir.contains("MR") || xmldir.contains("mr")) mr = true;
	    if (xmldir.contains("HB") || xmldir.contains("hb")) hb = true;
	    if (xmldir.contains("HD") || xmldir.contains("hd") || xmldir.contains("HA") || xmldir.contains("ha")) hd = true;
        esum = 0;
        nList = new ArrayList<Node>();
        edge  = new ArrayList<ArrayList<Pair>>();
        backedge  = new ArrayList<ArrayList<Pair>>();
        root = new ArrayList<Integer>();
        idplist = new ArrayList<IdPair>();  
        ptidref = new HashMap<IdPair,ArrayList<Integer>>();
        ptideventref = new HashMap<IdPair,ArrayList<Integer>>();
        typeref = new HashMap<String,ArrayList<Integer>>();
        memref = new HashMap<String , ArrayList<Integer>>(nList.size());
        
        hashMsgSending = new HashMap<String, ArrayList<Integer>>();
        hashMsgProcEnter = new HashMap<String, ArrayList<Integer>>();
        syncedges = new ArrayList<ArrayList<EgPair>>(50);
        
        
        /*
        syncedges.add(new ArrayList<EgPair>());
        syncedges.add(new ArrayList<EgPair>());
        syncedges.add(new ArrayList<EgPair>());
        syncedges.add(new ArrayList<EgPair>());
 		syncedges.set(10, new ArrayList<EgPair>());
 		syncedges.set(20, new ArrayList<EgPair>());
 		syncedges.set(30, new ArrayList<EgPair>());
        */
        outputlist  = new HashMap<IdPair, Integer>();  //JX - for simple output
        outputlist2 = new HashMap<IdPair, Integer>();  //JX - for median output
        identity = new HashMap<String, Integer>();  //"type+class+method+line" -> freq
        emlink   = new ArrayList<Integer>();
        emlink2  = new ArrayList<Integer>();
        
        // Get all threads' log files
        File [] xmlfiles = new File(xmldir).listFiles();
        if (xmlfiles == null) {
        	System.out.println("JX - ERROR - None of log files to handle");
        	return;
        }
        // Nothing, just sort files by filename
        Arrays.sort(xmlfiles, new Comparator(){
	    	@Override
		    public int compare(Object f1, Object f2) {
		        String fileName1 = ((File) f1).getName();
		        String fileName2 = ((File) f1).getName();
		        int fileId11 = Integer.parseInt(fileName1.split("-")[0]);
		        int fileId12 = Integer.parseInt(fileName1.split("-")[1]);
		        int fileId21 = Integer.parseInt(fileName2.split("-")[0]);
		        int fileId22 = Integer.parseInt(fileName2.split("-")[1]);
		        if (fileId11 == fileId21) return fileId12-fileId22;
		        	return fileId11 - fileId21;
       	    }
        });
        //Collections.sort(xmlfiles);
        
		msender = new ArrayList<Integer>();
		mexit = new ArrayList<Integer>();
		eexit = new ArrayList<Integer>();
		zkcreatelist = new ArrayList<Integer>();
		locktrace = new ArrayList<Integer>();
		eedgesum = tedgesum = medgesum = 0;
		
        int index = 0;
        IdPair idPair;
        	
    	// JX - traverse all thread files
        for (File xml : xmlfiles) {
        	Stack<Integer> stack = new Stack<Integer>();
        	Stack<Integer> lockstack = new Stack<Integer>();
            String xmlfilename = xml.getName();
            //System.out.println(xmlfilename);
            String [] ptid = xmlfilename.split("-");
            idPair = new IdPair(Integer.parseInt(ptid[0]), Integer.parseInt(ptid[1]));  //JX - get file's pid and tid
            ArrayList<Integer> idlist = new ArrayList<Integer>();        //JX - pid/tid for a thread's nodes; for all nodes across all threads, see 'idplist' 
            ArrayList<Integer> ideventlist = new ArrayList<Integer>();   //?ideventlist
            //System.out.println(ptid[0] + " ~ " + ptid[1]);
            //File xml = new File(xmlfile);
            //System.out.println(xml.toString());
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                inputdoc = db.parse(xml);
                //System.out.println(document);
            } catch (Exception e) {
                System.out.println("XML file load error, graphbuilder construction failed");
                e.printStackTrace();
            }
            inputdoc.getDocumentElement().normalize();
            //System.out.println("Root element :" + document.getDocumentElement().getNodeName());

            NodeList NList = inputdoc.getElementsByTagName("Operation");   //get all operations/nodes at a thread
            int msgheader = -1;   
            int msgflag   = 1;
            int curmsg = -1;

            // JX - deal with a single thread file's operations
            for (int i = 0; i < NList.getLength(); i++) {
                Node nNode = NList.item(i);        //get a node/operation
                //System.out.println("\nCurrent Element :" + nNode.getNodeName());
                //	    System.out.println(ptid[0]+"-"+ptid[1]+" mheader = "+mheader+ " mflag = "+ mflag);
                if ( goodnode(nNode) == false ) continue;  //JX - little filter
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    //System.out.println("++++ index=" + index + " "+ nNode);
                    Element eElement = (Element) nNode;
                    String tp = eElement.getElementsByTagName("OPTY").item(0).getTextContent();
                    String tval = eElement.getElementsByTagName("OPVAL").item(0).getTextContent();
                    /*	
					if (i > 0){
					    Node pn = nList.get(index-1);
					    Element pe = (Element) pn;
					    String ptp = pe.getElementsByTagName("OPTY").item(0).getTextContent();
		                            String pval = pe.getElementsByTagName("OPVAL").item(0).getTextContent();        
					    if (ptp.equals(tp) && pval.equals(tval)) continue;
					    if (i > 2){
						 
		                        //	System.out.println("++++ i =" + i + " i>2");
			      		        pn = nList.get(index-2);
		                                pe = (Element) pn;
		                                ptp = pe.getElementsByTagName("OPTY").item(0).getTextContent();
		                                pval = pe.getElementsByTagName("OPVAL").item(0).getTextContent();
		                                if (ptp.equals(tp) && pval.equals(tval)) continue;
					    }
					}
					*/
                    //System.out.println("++++ index=" + index + " "+ nNode);
                    nList.add(nNode);                   //JX - Node, will be <OPINFO>/<Operation> at 'base' file 
					String idstr = getIdentity(nNode);  //id/signature for an node/operation, not IdPair-pid/tid
					if (identity.keySet().contains(idstr)){
					    int freq = identity.get(idstr);
					    identity.put(idstr,freq+1);
					}else
					    identity.put(idstr,1);
					emlink.add(-1);  //?
					mexit.add(-1);   //?
					eexit.add(-1);   //?
					msender.add(-1); //?
					if (stack.empty()){
					    emlink2.add(-1);
					} else{
					    emlink2.add(stack.peek());
					}
					//if (index == 2994) System.out.println("2994's lock = "+lockstack.peek());
					/* commented by JX
					if (lockstack.empty()){
					    locktrace.add(-1);
					}  else {
					    locktrace.add(lockstack.peek());
					}
					*/
					
                    idplist.add(idPair);
                    edge.add(new ArrayList<Pair>());
                    backedge.add(new ArrayList<Pair>());
                    
                    // Modified by JX
                    if ( !tp.equals("ThdEnter")                   // for reused threads!! IMPO!!!
                    	&& !tp.equals("MsgProcEnter")               //not msg/rpc handler enter 
                    	&& !tp.equals("EventProcEnter") ) {       //not event handler enter 
                    	if ( i > 0 ) {
                    		//newly added //should for all, bug this change is for only ca-6744
                    		if ( getNodePIDTID(index-1).equals(getNodePIDTID(index)) )
                    		addedge(index-1, index);			  //ie, 0->1->2->3->4->5
                    	}
                    	if ( msgflag > 0 )
                    		msgheader = index;
                    }
                    /*
                    if ((i > 0) && (!tp.equals("ThdEnter"))                      //not enter thread          
                    			&& (!tp.equals("MsgProcEnter"))                  //not msg handler enter    
                				&& (!tp.equals("EventProcEnter")) ) {             //not event handler enter   
		                addedge(index-1,index);                                 //ie, 0->1->2->3->4->5
					    if (msgflag > 0) {
					    	mheader = index;                                    //how to determine the mheader?
					    	//System.out.println("set mheader = "+index);
					    }
		            }
		            */
                    
					if (tp.equals("MsgProcEnter")){
					    msgflag = -1;
					    if (curmsg > -1)
					    	mexit.set(curmsg,index-1);
					    //System.out.println("Form "+curmsg + " to "+ index+"-1 is a msg");
					    curmsg = index;
					    //System.out.println("set mflag = -1");
					    if (msgheader > -1) {
					    	addedge(msgheader,index);
					    	//System.out.println(index+ "is pluged to "+ mheader);
					    } else {
					    	//System.out.println(index+ "is a no program header msg from "+ptid[0]+"-"+ptid[1]);
					    }
					}
					
                    if (typeref.get(tp) == null)                          //JX - summarize types
                        typeref.put(tp,new ArrayList<Integer>());
                    typeref.get(tp).add(index);
                    idlist.add(index);
		
					//////////////////////////////////////////////
                    if (tp.equals("EventProcEnter")){
                    	//stack.push(index);
                        String val = eElement.getElementsByTagName("OPVAL").item(0).getTextContent();
                        if (!val.contains("GenericEventHandler")){  //JX - normal EventProcEnter 
                            ideventlist.add(index);
					        stack.push(index);
					    }                              
					    else addedge(index-1,index); //JX - special EventProcEnter, actually it is only for MapReduce's "handle()", this put/submit is 'Enter(maybe many)' not 'create'
                    }
					if (tp.equals("EventProcExit")){
					    String val = eElement.getElementsByTagName("OPVAL").item(0).getTextContent();
					    if (!val.contains("GenericEventHandler"))
					    stack.pop();
					}
					//////////////////////////////////////////////
		
					
					//////////////////////////////////////////////
					/* commented by JX
					if (tp.equals("LockRequire")){
					    lockstack.push(index);
					}
					if (tp.equals("LockRelease")){                                         //JX - ????? a mistake??
					    lockstack.pop();
					    //Actually the structure should not be a stack.
					}
					*/
					
					/*if (index == 2985) {
					    System.out.println("2985's lock = "+lockstack);
					    System.out.println("2985's type = "+tp);
					}
					if (index == 2994) System.out.println("2994's lock = "+lockstack);*/
					//////////////////////////////////////////////			
					if (tp.equals("HeapWrite")){
		   			    Element esx = (Element) eElement.getElementsByTagName("Stacks").item(0);
					    Element sx = (Element) esx.getElementsByTagName("Stack").item(0);
					    String xmethod = sx.getElementsByTagName("Method").item(0).getTextContent();
					    if (xmethod.equals("createNonSequential"))
						zkcreatelist.add(index);
					}			                        
                    /*if (index == 18686) {
                        System.out.println("18686 eventput = "+ stack.peek());
                       //System.out.println("pid = " + idPair.pid +" tid = " +idPair.tid);
                    }*/
                    index ++;  //jx - should be each node/operation's ID
		            //System.out.println("Author : " + eElement.getElementsByTagName("author").item(0).getTextContent());
	            }
                
			    if (curmsg > -1) mexit.set(curmsg,index-1);
            } //end-for-each thread file's-nodes
            
            
            //JX - still for each thread file
            ptidref.put(idPair,idlist);                      //JX - only nodes/operations belonging to this thread
            ptideventref.put(idPair,ideventlist);
            /*if ((idPair.pid == 31943) &&(idPair.tid == 49)) {
                System.out.println("####"+ ptidref.get(new IdPair(31943,49)));
                gSendingystem.out.println("####"+ ptidref.get(idPair));
            }*/
        } //End-for-all thread files
        
        
    	//JX - just added - maybe need 'LockReqire' and 'LockRelease'        #this is moved out from above loop
    	if (typeref.get("LockRequire") == null)                                             
    		typeref.put("LockRequire", new ArrayList<Integer>());
    	if (typeref.get("LockRelease") == null)
    		typeref.put("LockRelease", new ArrayList<Integer>());
    	if (typeref.get("RWLockCreate") == null)
    		typeref.put("RWLockCreate", new ArrayList<Integer>());
    	if (typeref.get("MsgSending") == null)                                             
            typeref.put("MsgSending",new ArrayList<Integer>());
        if (typeref.get("MsgProcEnter") == null)
            typeref.put("MsgProcEnter",new ArrayList<Integer>());
        if (typeref.get("ThdCreate") == null)
            typeref.put("ThdCreate",new ArrayList<Integer>());
        if (typeref.get("ThdEnter") == null)
            typeref.put("ThdEnter",new ArrayList<Integer>());
        if (typeref.get("ThdJoin") == null)
            typeref.put("ThdJoin",new ArrayList<Integer>());
        if (typeref.get("ThdExit") == null)
            typeref.put("ThdExit",new ArrayList<Integer>());
        if (typeref.get("MsgProcExit") == null)
            typeref.put("MsgProcExit",new ArrayList<Integer>());
        if (typeref.get("EventProcEnter") == null)
            typeref.put("EventProcEnter",new ArrayList<Integer>());
        if (typeref.get("EventProcExit") == null)
            typeref.put("EventProcExit",new ArrayList<Integer>());
        if (typeref.get("ProcessCreate") == null)	
            typeref.put("ProcessCreate",new ArrayList<Integer>());
        if (typeref.get("EventCreate") == null)	
            typeref.put("EventCreate",new ArrayList<Integer>());
    
        System.out.println("JX - initialization");
        System.out.println("#totalNodes = " + nList.size() + ", including the following");  //JX: verified, equal to $index
        System.out.println("LockRequire: " + typeref.get("LockRequire").size());
        System.out.println("LockRelease: " + typeref.get("LockRelease").size());
        System.out.println("RWLockCreate: " + typeref.get("RWLockCreate").size());
	    for (String type : typeref.keySet())    
	    	if ( !type.equals("LockRequire") && !type.equals("LockRelease") && !type.equals("RWLockCreate"))
            	System.out.println(type + " : " + typeref.get(type).size());
        System.out.println( esum + " basic edges are added in the initialization.");
	    
	    //System.out.print("zkcreate = ");
	    //System.out.println(zkcreatelist);
        createbasexml(); //JX - generate the 'base' file including all nodes/operations
        createemlink();  //?
    } //end-for-Construction
    
    
    //added by JX
    public ArrayList<Node> getNodeList() {
    	return this.nList;
    }
    
    public ArrayList<ArrayList<Pair>> getEdge() {
    	return this.edge;
    }
    
    public ArrayList<ArrayList<Pair>> getBackEdge() {
    	return this.backedge;
    }
    
    public ArrayList<BitSet> getReachSet() {
    	return this.reachbitset;
    }
    
    
    public String getIdentity(Node node){
    	Element ex = (Element) node;
        String optyx = ex.getElementsByTagName("OPTY").item(0).getTextContent();
        Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
        String slenx = esx.getAttribute("Len");
        String id = optyx;
        for (int i = 0 ; i < Integer.parseInt(slenx); i++ ){
            Element sx = (Element) esx.getElementsByTagName("Stack").item(i);
            String xclass = sx.getElementsByTagName("Class").item(0).getTextContent();
            String xmethod= sx.getElementsByTagName("Method").item(0).getTextContent(); 
            String xline  = sx.getElementsByTagName("Line").item(0).getTextContent();
            id = id + xclass + xmethod + xline;
        }
        return id;
    }
    
    //Added by JX
    public String getWholeIdentity(Node node){
		Element e = (Element) node;
		String opty = e.getElementsByTagName("OPTY").item(0).getTextContent();
		String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
		String pid = e.getElementsByTagName("PID").item(0).getTextContent();
		String tid = e.getElementsByTagName("TID").item(0).getTextContent();
	    Element stacks = (Element) e.getElementsByTagName("Stacks").item(0);
	    String len = stacks.getAttribute("Len");
		String id = opty +"-"+ opval +"-"+ pid +"-"+ tid;
		for (int i = 0 ; i < Integer.parseInt( len ); i++ ){
	        Element stack = (Element) stacks.getElementsByTagName("Stack").item(i);
	        String xclass = stack.getElementsByTagName("Class").item(0).getTextContent();
		    String xmethod= stack.getElementsByTagName("Method").item(0).getTextContent(); 
		    String xline  = stack.getElementsByTagName("Line").item(0).getTextContent();
		    id = id +"-"+ xclass +"-"+ xmethod +"-"+ xline;
	  	}
		return id;
    }    

    //added by JX
    // only for printing or debugging
    public String getPrintedIdentity(int index) {
    	return "Node" + index + " " + getNodeOPTY(index) + " " + getNodeOPVAL(index) + " " + getNodePIDTID(index) + " "
    		   + lastCallstack_2(index);
    }
    

        public String fullCallstack(int i){
            Node ni = nList.get(i);
            Element ei = (Element) ni;
            String sti = "";
            int ind = 0;
            Element esi = (Element) ei.getElementsByTagName("Stacks").item(0);
            int slenx = Integer.parseInt(esi.getAttribute("Len"));
            while (ind < slenx){                                                                                                                                                                                          
                Element si  = (Element) esi.getElementsByTagName("Stack").item(ind);                                                                                                                                      
                // commented by JX                                                                                                                                                                                        
                /*                                                                                                                                                                                                        
                if (si.getElementsByTagName("Line").item(0).getTextContent().equals("-1")){                                                                                                                               
                    ind++;                                                                                                                                                                                                
                    continue;                                                                                                                                                                                             
                }                                                                                                                                                                                                         
                */                                                                                                                                                                                                        
                sti = sti                                                                                                                                                                                                 
                    + si.getElementsByTagName("Class").item(0).getTextContent()  + "-"                                                                                                                                    
                    + si.getElementsByTagName("Method").item(0).getTextContent() + "-"                                                                                                                                    
                    + si.getElementsByTagName("Line").item(0).getTextContent() + ";";                                                                                                                                     
                ind++;                                                                                                                                                                                                    
            }                                                                                                                                                                                                             
            return sti;                                                                                                                                                                                                   
        }


    public String lastCallstack(int index) {
	    Node ni = nList.get(index);
	    Element ei = (Element) ni;
	    String sti = "";
	    int ind = 0;
	    Element esi = (Element) ei.getElementsByTagName("Stacks").item(0);  
	    int stackLength = esi.getElementsByTagName("Stack").getLength();
	    for (int i = 0; i < stackLength; i++) {
	        Element si  = (Element) esi.getElementsByTagName("Stack").item(i);
                // commented by JX
	        if (si.getElementsByTagName("Line").item(0).getTextContent().equals("-1") || si.getElementsByTagName("Line").item(0).getTextContent().equals("-2"))
	            continue;
	        // Modified by JX
	        sti = si.getElementsByTagName("Class").item(0).getTextContent() + "-"       //jx-modified: " "->"-"
	        		+ si.getElementsByTagName("Method").item(0).getTextContent() + "-"     //jx-modified: " "->"-"
	        		+ si.getElementsByTagName("Line").item(0).getTextContent() + ";";
	        break;
	    }
	    return sti;		
    }



    public String lastCallstack_2(int index) {
            Node ni = nList.get(index);
            Element ei = (Element) ni;
            String sti = "";
            int ind = 0;
            Element esi = (Element) ei.getElementsByTagName("Stacks").item(0);
            int stackLength = esi.getElementsByTagName("Stack").getLength();
            for (int i = 0; i < Math.min(2, stackLength); i++) {
                Element si  = (Element) esi.getElementsByTagName("Stack").item(i);
                sti += si.getElementsByTagName("Class").item(0).getTextContent() + "-"       //jx-modified: " "->"-"
                                + si.getElementsByTagName("Method").item(0).getTextContent() + "-"     //jx-modified: " "->"-"
                                + si.getElementsByTagName("Line").item(0).getTextContent() + ";";
            }
            return sti;
    }





	//JX - compare whole call stacks of two nodes/operations
	public boolean lastCallstackEqual(int x, int y){
	    Node nx = nList.get(x);
	    Node ny = nList.get(y);
	    Element ex = (Element) nx;
	    Element ey = (Element) ny;
	
	    String pidx = ex.getElementsByTagName("PID").item(0).getTextContent();
	    String tidx = ex.getElementsByTagName("TID").item(0).getTextContent();
	    String optyx = ex.getElementsByTagName("OPTY").item(0).getTextContent();
	    String opvalx = ex.getElementsByTagName("OPVAL").item(0).getTextContent();
	    Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
	    String slenx = esx.getAttribute("Len");
	
	    String pidy = ey.getElementsByTagName("PID").item(0).getTextContent();
	    String tidy = ey.getElementsByTagName("TID").item(0).getTextContent();
	    String optyy = ey.getElementsByTagName("OPTY").item(0).getTextContent();
	    String opvaly = ey.getElementsByTagName("OPVAL").item(0).getTextContent();
	    Element esy = (Element) ey.getElementsByTagName("Stacks").item(0);
	    String sleny = esy.getAttribute("Len");
	    //if ((!pidx.equals(pidy))||(!tidx.equals(tidy))||(!optyx.equals(optyy))||(!opvalx.equals(opvaly))
	    if (!optyx.equals(optyy)
	    		// ||(!slenx.equals(sleny))
		)
	    return false;
	    //for (int i = 0 ; i < Integer.parseInt(slenx); i++ ){
	    for (int i = 0 ; i < 1; i++ ){  //jx - yes, 1
	        Element sx = (Element) esx.getElementsByTagName("Stack").item(i);
	        Element sy = (Element) esy.getElementsByTagName("Stack").item(i);
	
	        String xclass = sx.getElementsByTagName("Class").item(0).getTextContent();
	        String xmethod = sx.getElementsByTagName("Method").item(0).getTextContent();
	        String xlen = sx.getElementsByTagName("Line").item(0).getTextContent();
	        String yclass = sy.getElementsByTagName("Class").item(0).getTextContent();
	        String ymethod = sy.getElementsByTagName("Method").item(0).getTextContent();
	        String ylen = sy.getElementsByTagName("Line").item(0).getTextContent();
	
	        if ((!xclass.equals(yclass))||(!xmethod.equals(ymethod))||(!xlen.equals(ylen)))
	            return false;
	    }
	    return true;
	}

	public boolean wholeCallstackEqual(int x, int y){
	    Node nx = nList.get(x);
	    Node ny = nList.get(y);
	    Element ex = (Element) nx;
	    Element ey = (Element) ny;
	
	    String pidx = ex.getElementsByTagName("PID").item(0).getTextContent();
	    String tidx = ex.getElementsByTagName("TID").item(0).getTextContent();
	    String optyx = ex.getElementsByTagName("OPTY").item(0).getTextContent();
	    String opvalx = ex.getElementsByTagName("OPVAL").item(0).getTextContent();
	    Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
	    String slenx = esx.getAttribute("Len");
	
	    String pidy = ey.getElementsByTagName("PID").item(0).getTextContent();
	    String tidy = ey.getElementsByTagName("TID").item(0).getTextContent();
	    String optyy = ey.getElementsByTagName("OPTY").item(0).getTextContent();
	    String opvaly = ey.getElementsByTagName("OPVAL").item(0).getTextContent();
	    Element esy = (Element) ey.getElementsByTagName("Stacks").item(0);
	    String sleny = esy.getAttribute("Len");
	    //if ((!pidx.equals(pidy))||(!tidx.equals(tidy))||(!optyx.equals(optyy))||(!opvalx.equals(opvaly))
	    if (!optyx.equals(optyy)
	            ||(!slenx.equals(sleny))
	            )
	        return false;
	    for (int i = 0 ; i < Integer.parseInt(slenx); i++ ){
	    //for (int i = 0 ; i < 1; i++ ){
	        Element sx = (Element) esx.getElementsByTagName("Stack").item(i);
	        Element sy = (Element) esy.getElementsByTagName("Stack").item(i);
	
	        String xclass = sx.getElementsByTagName("Class").item(0).getTextContent();
	        String xmethod = sx.getElementsByTagName("Method").item(0).getTextContent();
	        String xlen = sx.getElementsByTagName("Line").item(0).getTextContent();
	        String yclass = sy.getElementsByTagName("Class").item(0).getTextContent();
	        String ymethod = sy.getElementsByTagName("Method").item(0).getTextContent();
	        String ylen = sy.getElementsByTagName("Line").item(0).getTextContent();
	
	        if ((!xclass.equals(yclass))||(!xmethod.equals(ymethod))||(!xlen.equals(ylen)))
	            return false;
	
	    }
	
	    return true;
	
	}

	/*
	    public void buildtreepic() {
	
	        String gexfile = xmldir+".gexf";
	
	        try {
	            PrintWriter writer = new PrintWriter(gexfile, "UTF-8");
	            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	            writer.println("<gexf xmlns:viz=\"http:///www.gexf.net/1.1draft/viz\" version=\"1.1\" xmlns=\"http://www.gexf.net/1.1draft\">");
	            writer.println("<meta lastmodifieddate=\"2010-03-03+23:44\">");
	            writer.println("<creator>Gephi 0.9</creator>");
	            writer.println("</meta>");
	            writer.println("<graph defaultedgetype=\"directed\" idtype=\"string\" type=\"static\">");
	            writer.println("<nodes count=\""+nList.size()+"\">");
	            for (int i = 0 ; i < nList.size(); i++){
	                Node node = nList.get(i);
	                Element eElement = (Element) node;
	                writer.println("<node id=\"" + i + "\" label=\"" + eElement.getElementsByTagName("OPTY").item(0).getTextContent() + "\"/>");
	            }
	            writer.println("</nodes>");
	            writer.println("<edges count=\""+ esum +"\">");
	            int eindex = 0;
	            for (int i = 0; i < nList.size(); i++){
	                ArrayList<Pair> list = edge.get(i);
	                //for(int j = 0 ; j < list.size(); j++){
	                for (Pair tj : list){
	                    //System.out.println("gexf : " + i + " j = "+ j);
	                    //Pair tj = list.get(j);
	                    writer.println("<edge id=\""+ eindex +"\" source=\""+ i +"\" target=\""+ tj.destination +"\" weight=\""+ tj.otype+"\"/>");
	                    eindex ++;
	                }
	            }
	            writer.println("</edges>");
	            writer.println("</graph>");
	            writer.println("</gexf>");
	            writer.close();
	        } catch (Exception e){
	            e.printStackTrace();
	        }
	    }
	*/
	    
	    
	
	
	public boolean isSameThread(int index1, int index2) {
		return getNodePIDTID(index1).equals( getNodePIDTID(index2) );
	}
	
	public boolean isSameValue(int index1, int index2) {
		return getNodeOPVAL(index1).equals( getNodeOPVAL(index2) );
	}
	
	
	
	
    //Added by JX
    public String getNodeOPTY(int index) {
    	return getNodeOPTY( nList.get(index) );
    }
    public String getNodeOPTY(Node node) {
    	Element e = (Element) node;
    	String opty = e.getElementsByTagName("OPTY").item(0).getTextContent();
    	return opty;
    }

	public String getNodePID(int index) {
		return getNodePID( nList.get(index) );
	}
	public String getNodePID(Node node) {
		Element e = (Element) node;
		String pid = e.getElementsByTagName("PID").item(0).getTextContent();
		return pid;
	}

    public String getNodeTID(int index) {
        return getNodeTID( nList.get(index) );
    }
    public String getNodeTID(Node node) {
        Element e = (Element) node;
        String tid = e.getElementsByTagName("TID").item(0).getTextContent();
        return tid;
    }

	public String getNodePIDTID(int index) {
		return getNodePIDTID( nList.get(index) );
	}
	public String getNodePIDTID(Node node) {
		Element e = (Element) node;
		String pid = e.getElementsByTagName("PID").item(0).getTextContent();
		String tid = e.getElementsByTagName("TID").item(0).getTextContent();
		return pid+"-"+tid;
	}

	public String getNodeOPVAL(int index) {
		return getNodeOPVAL( nList.get(index) );
	}
	public String getNodeOPVAL(Node node) {
		Element e = (Element) node;
		String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
		return opval;
	}
	
	
	// jx: for "ThdCreate" generated by "threadpoolxx.submit(xx)", the value is "thread hashcode_furture hashcode"
	public String getNodeOPVAL_0(int index) {   
		return getNodeOPVAL_0( nList.get(index) ); 
	}
	public String getNodeOPVAL_0(Node node) {   
		Element e = (Element) node;
		String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
		return opval.split("_")[0]; 
	}
	
	
	public String getNodeOPVAL_n(int index) {   
		return getNodeOPVAL_n( nList.get(index) ); 
	}
	public String getNodeOPVAL_n(Node node) {   
		Element e = (Element) node;
		String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
		return opval.split("_")[ opval.split("_").length-1 ]; 
	}
	

	// return "PID"+"OPVAL0" for 'lock' nodes, especially for r/w locks
	public String getNodePIDOPVAL0(int index) {   
		return getNodePIDOPVAL0( nList.get(index) ); 
	}
	public String getNodePIDOPVAL0(Node node) {   
		Element e = (Element) node;
		String pid = e.getElementsByTagName("PID").item(0).getTextContent();
		String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
		return pid + opval.split("_")[0]; 
	}


	public boolean goodnode(Node node){
        Element ex = (Element) node;
		try {
	        String optyx = ex.getElementsByTagName("OPTY").item(0).getTextContent();
	        Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
	        String slenx = esx.getAttribute("Len");
	        //	if (! optyx.equals("MsgProcEnter")) return true;
	        String opvx = ex.getElementsByTagName("OPVAL").item(0).getTextContent();
	        //	if (opvx.contains("NodeChildrenChanged")) return false;
	        for (int i = 0 ; i < Integer.parseInt(slenx); i++ ){
	        //for (int i = 0 ; i < 1; i++ ){
	            Element sx = (Element) esx.getElementsByTagName("Stack").item(i);
	            String xclass = sx.getElementsByTagName("Class").item(0).getTextContent();
			    //if (xclass.contains("FileSnap")) return false;
			    /*
			    if (i == 0){
				String xlinum = sx.getElementsByTagName("Line").item(0).getTextContent();        if (optyx.equals("HeapRead") && xlinum.equals("-1")) return false;
				String xmethod = sx.getElementsByTagName("Method").item(0).getTextContent();     
				if (optyx.equals("HeapRead") && xmethod.equals("getCurrentState")) {
				Element s2 = (Element) esx.getElementsByTagName("Stack").item(1);
				String string = s2.getElementsByTagName("Line").item(0).getTextContent();
				if (string.equals("-1")) return false;
				}
			    } */ 
			    if (xclass.contains("FileTxnLog") && optyx.equals("MsgProcEnter")) return false;	
			    if (xclass.contains("FileSnap") && optyx.equals("MsgProcEnter")) return false;	
			    if (xclass.contains("FileTxnSnapLog") && optyx.equals("MsgProcEnter")) return false;	
	        }
		} catch (Exception e){
		    return false;
		}
	    return true;
    }
    
    public void createemlink(){
    	//emlink = new ArrayList<Integer>(nList.size());
		for ( int i = 0 ; i < nList.size(); i++) {
		    if (backedge.get(i).size() == 0) {
				Stack<Integer> stack = new Stack<Integer>();
				stack.push(-1);
				int cur = -1;
				int j = i;
				//emlink.set(j,-1);
				while (!edge.get(j).isEmpty()){
				    if (emlink.get(j) == -1){
		    	         emlink.set(j,stack.peek());
					}
	
			        Node node = nList.get(j);
	                Element e = (Element) node;
	                String opval= e.getElementsByTagName("OPVAL").item(0).getTextContent();
			        String opty = e.getElementsByTagName("OPTY").item(0).getTextContent();
			        if (opty.equals("ThdEnter")|| opty.equals("MsgProcEnter") || opty.equals("EventProcEnter")){
				    stack.push(j);
			        }
			        //only works well for hadoop
			    	if ((opty.equals("ThdExit")||opty.equals("MsgProcExit")||opty.equals("EventProcExit")) && (stack.peek() != -1)){
					    Node nodex = nList.get(stack.peek());
					    Element ex = (Element) nodex;
					    if (ex.getElementsByTagName("OPVAL").item(0).getTextContent().equals(opval)){
					        int xx=stack.pop();
							if (opty.equals("EventProcExit")){
							    eexit.set(xx,j);
							}
					    }
			    	} 
					if (opty.equals("EventProcExit") && (stack.peek() != -1)){
					    Node nodex = nList.get(stack.peek());
		                            Element ex = (Element) nodex;
		                            String st1=ex.getElementsByTagName("OPVAL").item(0).getTextContent();
					    String st2 = st1.split("!")[0];
					    String st3 = opval.split("!")[0];
					    if (st2.equals(st3)){
		                                int xx = stack.pop();
						eexit.set(xx,j);
					    }
					}	
					j = edge.get(j).get(0).destination;
				}            
            }
		}
    }
    
    public void createbasexml(){
    	File dir = new File(xmldir+"result");
        dir.mkdir();
        DocumentBuilderFactory documentBuilderFactory;
        DocumentBuilder documentBuilder;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            outputdoc = documentBuilder.newDocument();
            docroot = outputdoc.createElement("OPINFOS");
            outputdoc.appendChild(docroot);
            for (int i = 0 ; i < nList.size(); i++){
            	Node node = nList.get(i);
            	Element e1 = (Element) node;
            	Element opinfo = outputdoc.createElement("OPINFO");
            	Attr attr = outputdoc.createAttribute("ID");
            	attr.setValue(Integer.toString(i));
            	opinfo.setAttributeNode(attr);
            	opinfo.appendChild(outputdoc.importNode(node,true));
            	docroot.appendChild(opinfo);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(outputdoc);
            File wf = new File(dir, "base");
            if (!wf.exists())
                wf.createNewFile();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(wf);
            transformer.transform(source, result);
        } catch (Exception e) {
            System.out.println("Cannot write a base xml file");
            e.printStackTrace();
            return ;
        }
    }
    
    public void msgcodestat(ArrayList<Integer> list){
        for (int x : list) {
            Node node = nList.get(x);
            Element e1 = (Element) node;
            if (hashMsgSending.get(e1.getElementsByTagName("OPVAL").item(0).getTextContent()) == null) {
                hashMsgSending.put(e1.getElementsByTagName("OPVAL").item(0).getTextContent(), new ArrayList<Integer>());
            }
            hashMsgSending.get(e1.getElementsByTagName("OPVAL").item(0).getTextContent()).add(x);
        }
    }
    
    
    public void buildsyncgraph() {
    	System.out.println("\nJX - buildsyncgraph - Adding edges to build Happen-Before graph");
    	
    	//eventremovethreadorder();
        eventcaller = new ArrayList<Integer>(nList.size());
        for (int i = 0 ; i < nList.size(); i++)
            eventcaller.add(-1);
        if (typeref.get("MsgSending") != null) {
            msgcodestat(typeref.get("MsgSending"));
            //msgcodestat(typeref.get("MsgProcEnter"));
            //msgcodestat(typeref.get("MsgProcExit"));
            
            int hashsum = 0;
            for (String st : hashMsgSending.keySet()) {
                if (hashMsgSending.get(st).size() > 2) {
                    hashsum += hashMsgSending.get(st).size();
                    //int xx = hashMsg.get(st).get(0);
                    //int yy = hashMsg.get(st).get(1);
                    //System.out.println(st + " : " + hashMsg.get(st).size() +" "+ idplist.get(xx).pid + "-"+idplist.get(xx).tid
                    //+ " " + idplist.get(yy).pid+"-"+idplist.get(yy).tid);
                    //System.out.println(st + " : " + hashMsg.get(st).size());

               }else{
		    hashsum++;
	       }
            }
            //Commented by JX
            //System.out.println("TOTAL = " + hashsum);
            //end-Commented
        }
        if (typeref.get("MsgProcEnter") != null){
            ArrayList<Integer> list = typeref.get("MsgProcEnter");
            for (int x : list) {
                Node node = nList.get(x);
                Element e1 = (Element) node;
                if (hashMsgProcEnter.get(e1.getElementsByTagName("OPVAL").item(0).getTextContent()) == null) {
                    hashMsgProcEnter.put(e1.getElementsByTagName("OPVAL").item(0).getTextContent(), new ArrayList<Integer>());
                }
                hashMsgProcEnter.get(e1.getElementsByTagName("OPVAL").item(0).getTextContent()).add(x);
            }
        }

        /***** JX - 1. find out relation of Thread Creation -> Thread Enter *****/
        
        int threadcreaterel = 0;                            //relation number
        for( int i: typeref.get("ThdCreate") ) {            //create thread
            // introducing rule
            String tid = getNodeOPVAL_0( i );           //jx: modify from getNodeOPVAL to getNodeOPVAL_0
            int f = 0;
            for (int j: typeref.get("ThdEnter")) {
            	String opvalstr = getNodeOPVAL( j );
                if ( opvalstr.equals("-") ) continue;   //jx: main threads 
                String [] opval = opvalstr.split("/");
                if ( (opval[0].equals(tid) || opval[1].equals(tid))
                        && (idplist.get(i).pid == idplist.get(j).pid) ) {
				    if (f > 0 ) {
						System.out.println("INFO - "+tid+" find multi children");
						break;
					} 
                    threadcreaterel ++;
                    f++;
                    //System.out.println("Thread owner relation " + i +":" + idplist.get(i).pid+"-"+idplist.get(i).tid + " create " + j +":"+idplist.get(j).pid+"-"+idplist.get(j).tid);
                    addedge(i,j,1);
                    System.out.println("JX - DEBUG - thread: " + getNodePIDTID(i)+":"+i + " creates "+ getNodePIDTID(j)+":"+j + "thread");
                }
            } //end-inner-for
        } //end-outer-for
        System.out.println(threadcreaterel + " thread creation->enter relation added, expect " + typeref.get("ThdCreate").size());
       
        /* plug no creator thread to its direct parent */     //JX - for no-source threads like those in ThreadPool
        for ( int i : typeref.get("ThdEnter") ) {
            if ((backedge.get(i).size()==0)&&(i>0)) {
				int xx = idplist.get(i-1).tid;                //JX - idplist contains all nodes
				int yy = idplist.get(i-1).pid;
				int x  = idplist.get(i).tid;
				int y  = idplist.get(i).pid;
				if ((x == xx) &&(y == yy)) {
					System.out.println(i+" is no creator thread, plug it to its direct parent");
					addedge(i-1,i,1);                        //like a ThreadPool: t1 <- t2 <- t3
				}
            }
        }
        
        
        /**
         * added by JX, map "thread info" to "future hashcode" if exists "xxx.submit"
         */
        Map<String, String> futureGet = new HashMap<String,String>();
        for( int i: typeref.get("ThdCreate") ) {            //create thread
            String thdHash = getNodeOPVAL_0( i );           //jx: modify from getNodeOPVAL to getNodeOPVAL_0
            String futureHash = getNodeOPVAL_n( i );
            if ( !thdHash.equals(futureHash) ) {
            	futureGet.put(thdHash, futureHash);
            }
        } 
        
        
        /** 10. build thread join with a thread end */
        int threadjoinrel = 0;
        for (int i : typeref.get("ThdJoin")){
            int test = -1;
            int pid = idplist.get(i).pid;
            String opval = getNodeOPVAL(i);            //jx: tid could be tid or future's hashcode
            for (int j : typeref.get("ThdExit")){
                IdPair idPair = idplist.get(j);
                String futureHash = null;
                if (getNodeOPVAL(j).split("/").length > 1) 
                    futureHash = futureGet.get( getNodeOPVAL(j).split("/")[1] );
                
                if ( (pid == idPair.pid)&&( opval.equals(Integer.toString(idPair.tid)) )
                		|| (pid == idPair.pid)&&(futureHash!=null&&opval.equals(futureHash) )           //added by jx for "future.get"
                		){
                    addedge(j,i,10); //
                    //System.out.println(j + " join to "+ i);
                    test = j;
                    threadjoinrel++;
                    break;
                }
            }
            if (test >= 0) {
                //System.out.println(idplist.get(test) + " join to "+ idplist.get(i));
            }
            else
                System.out.println("Cannot find "+pid+"-"+opval +"'s ThdExit");
        }
        System.out.println(threadjoinrel + " thread join relation added, expect " +typeref.get("ThdJoin").size());


        /***** JX - 2. find out EventCreate->EventProcEnter,ie, Event process call and callee relation *****/
        
        int genevent = 0;
        int notfound = 0;
        //if (typeref.get("EventProcEnter")!=null){
        //System.out.println("eventPE = " + typeref.get("EventProcEnter"));
        for (int i : typeref.get("EventProcEnter")) {

            Node node = nList.get(i);
            //System.out.println("++++ i=" + i + " "+ node);
            Element e1 = (Element) node;
            String es1 = e1.getElementsByTagName("OPVAL").item(0).getTextContent();
            //System.out.println(es1);
            String hscode = "";
            for (int ch = 0; (ch < es1.length())&&(es1.charAt(ch) >= '0') &&(es1.charAt(ch) <= '9') ; ch++){
                hscode = hscode + es1.charAt(ch);
            }
            //System.out.println("event hashcode = " + hscode);
            if (es1.contains("GenericEventHandler")) {               //this is actually a 'EventCreate' for MapReduce's 'handle()' 
                int callee = -1;
                int calleedepth = 999999;
                for (int j : typeref.get("EventProcEnter")) {
                    Node node2= nList.get(j);
                    //System.out.println("----- " + j +" " + node2);
                    Element e2 = (Element) node2;
                    String opval2 = e2.getElementsByTagName("OPVAL").item(0).getTextContent();
                    if ((i != j) && opval2.contains(hscode) 
                    		//&& opval2.contains("!")
                    		){
                        //&&(calleedepth >depth)){
                        //calleedepth = depth;
                        callee = j;
                        //if (i == 3100) System.out.println("                            E-Part 3100->"+j);
                        addedge(i,callee,2);
                        eventcaller.set(callee,i);
                        //break;
                    }
                }
                genevent++;
				/*
		   		if (callee == -1){
		   		    for (int j : typeref.get("EventProcEnter")){
		                    Node node2= nList.get(j);
		                    //System.out.println("----- " + j +" " + node2);
		                    Element e2 = (Element) node2;
		                    String opval2 = e2.getElementsByTagName("OPVAL").item(0).getTextContent();
		                    if ((i != j) && opval2.contains(hscode)){
		                            //&&(calleedepth >depth)){
		                        //calleedepth = depth;
		                        callee = j;
		                        break;
		                    }
		                }
				}*/
                if (callee == -1) {
                    //System.out.println(genevent+" : " + i+" -> "+ j);
                	//addedge(i,callee,2);
                	//eventcaller.set(callee,i);
                	//}else{
                    notfound ++;
                    System.out.println(e1.getElementsByTagName("OPVAL").item(0).getTextContent());
                    //System.out.println(i+"'s callee is not found");
                }
            } //end-outer-if
        } //end-outer-for
        
        for (int i : typeref.get("EventCreate")){    //for normal EventCreate
            Node node = nList.get(i);
            Element e1 = (Element) node;
            String es1 = e1.getElementsByTagName("OPVAL").item(0).getTextContent();
            String hscode = "";
            for (int ch = 0; (ch < es1.length())&&(es1.charAt(ch) >= '0') &&(es1.charAt(ch) <= '9') ; ch++){
                hscode = hscode + es1.charAt(ch);
            }
            //if (es1.contains("GenericEventHandler")) {
                int callee = -1;
                int calleedepth = 999999;
                for (int j : typeref.get("EventProcEnter")){
                    Node node2= nList.get(j);
                    Element e2 = (Element) node2;
                    String opval2 = e2.getElementsByTagName("OPVAL").item(0).getTextContent();
                    if ((i != j) && opval2.contains(hscode)&& (!opval2.contains("GenericEventHandler"))
                                                                ){
                        callee = j;
                        addedge(i,callee,2);
                        eventcaller.set(callee,i);
                        //System.out.println(i + " is event putter of "+ callee);
                    }
                }
                genevent++;
                if (callee == -1) {
                    notfound ++;
                    System.out.println(e1.getElementsByTagName("OPVAL").item(0).getTextContent());
                }
            //}

        }
        System.out.println(notfound +" in "+genevent+ " is not found in event part");
	
        /**
         * JX - 3. find out MsgSending->MsgProcEnter. 
         * Ie, build the Msg caller and callee relation
         */
        notfound = 0;
        int genmsg = 0;
        HashMap<IdPair,ArrayList<Integer>> count = new HashMap<IdPair, ArrayList<Integer>>();
        IdPair sender;
        ArrayList<IdPair> receiver = new ArrayList<IdPair>();
        for (String st : hashMsgSending.keySet()) {
        	// jx - multi senders for a same hashcode - not use for now
            if (hashMsgSending.get(st).size() > 1) {
                System.out.println(st+" has multi senders");
                count.clear();
                sender = null;
                receiver.clear();
                ArrayList<Integer> list = hashMsgSending.get(st);
                for (Integer j : list) {
                    if (count.get(idplist.get(j))== null){
                        count.put(idplist.get(j),new ArrayList<Integer>());
                        count.get(idplist.get(j)).add(j);
                    }
                    else count.get(idplist.get(j)).add(j);
                }
                int multisender = 0;
                for (IdPair idPair : count.keySet()) {
                    if (count.get(idPair).size() == 1) receiver.add(idPair);
                    else{
                        if (sender == null) sender = idPair;
                        else {
                           // System.out.println(st + " cannot be processed multi-sender : " + sender + " + "+ idPair);
                           // multisender = 1;
                           // break;
                            if (count.get(idPair).size() > count.get(sender).size()) {
                                receiver.add(sender);
                                sender = idPair;
                            }
                        }
                    }
                }
                if (multisender == 1) continue;
                if (sender == null) {
                    sender = receiver.get(0);
                    receiver.remove(0);
                }
                //System.out.println("sender = " + sender);
                //System.out.println("count map = " + count);
                int receiversum = 0;
                for (IdPair idPair :receiver){
                    receiversum += count.get(idPair).size();
                }
                if (receiversum == 0){
                    //handle the multi invocation of v1
                    if (hashMsgProcEnter.get(st) == null){
                        System.out.println(st+" No any receiver");
                        continue;
                    }
                    if (count.get(sender).size()!= hashMsgProcEnter.get(st).size()) {
                        System.out.println(st + " MR-V1  sender does not match receiver :" + count.get(sender).size() + " VS " + hashMsgProcEnter.get(st).size());
                        continue;
                    }
                    for ( int i = 0; i < count.get(sender).size()-1; i++)
                     if (idplist.get(hashMsgProcEnter.get(st).get(i)).pid!=idplist.get(hashMsgProcEnter.get(st).get(i+1)).pid){
                         System.out.println(st +" MR-V1 receiver is not in the same process ");
                         continue;
                     }
                    Collections.sort(count.get(sender));
                    Collections.sort(hashMsgProcEnter.get(st));
                    int loop = 0;
                    while (loop < count.get(sender).size()){
                        int xx = count.get(sender).get(loop);
                        int yy = hashMsgSending.get(st).get(loop);
                        if (!buildMsgSync(xx,yy))  notfound++;
                        genmsg++;
                        loop ++;
                    }
                    continue;
                }
                if (count.get(sender).size() != receiversum) {
                    System.out.println(st + " cannot be processed sender does not match receiver :" + count.get(sender).size() + " VS " + receiversum);
                    continue;
                }
                for (int i = 0 ; i < receiver.size()-1; i++){
                    if (receiver.get(i).pid != receiver.get(i+1).pid) {
                        System.out.println(st +" cannot be processed receiver is not in the same process ");
                    }
                }
                Collections.sort(count.get(sender));
                Collections.sort(receiver);
                int loop = 0;
                int receiveindex = 0;
                int receiveoffset = 0;
                while (loop < receiversum) {
                    int xx = count.get(sender).get(loop);
                    //int yy = count.get(receiver.get(loop)).get(0);
                    int yy = count.get(receiver.get(receiveindex)).get(receiveoffset);
                    if (receiveoffset == count.get(receiver.get(receiveindex)).size()-1) {
                        receiveindex ++;
                        receiveoffset = 0;
                    }else{
                        receiveoffset ++;
                    }
                    //System.out.println(st +" : "+xx+"->"+yy);
                    if (!buildMsgSync(xx,yy))  notfound++;
                    genmsg++;
                    loop ++;
                }
            }
            // jx - single sender(msg sending) - normal situation
            else {
                int xx = hashMsgSending.get(st).get(0);
                int sum = 0;
                int yy =-1;
                if (st.startsWith("ZK") == false){
	                for (int y : typeref.get("MsgProcEnter")){
        	            Node node = nList.get(y);
                	    Element element = (Element) node;
	                    if (element.getElementsByTagName("OPVAL").item(0).getTextContent().equals(st)){
        	                if (sum == 0) {
                	            yy = y;
                	            if (y == 36) System.out.println("36 is matched to "+xx);
                        	    sum ++;
	                        }else {
        	                    System.out.println(st + " more than one receiver : " + yy + " " + y);
                	            continue;
                       		}
	                    }
        	        }
	                // jx: have matched msg enter
    	            if (sum > 0) {
        	            genmsg++;
                	    addedge(xx, yy, 3);
                    	//System.out.println(st +" : "+xx+"->"+yy);
                	    // commented by JX
					    //if (mr || hb){
						int xt = xx+1;
						int yt = -1;
						int sumt =0;
						if (samethread(xt,xx)){
							for (int y : typeref.get("MsgProcExit")){
				                            Node node = nList.get(y);
			        	                    Element element = (Element) node;
			                	            if (element.getElementsByTagName("OPVAL").item(0).getTextContent().equals(st)){
			                        	        if (sumt == 0) {
			                                	    yt = y;
			                               	 	    sumt ++;
			                                	}else {
			                              // 		     System.out.println(st + " more than one exit: " + yt+ " " + y);
			                                    		continue;
			                                	}
			                            	    }
							}
							if (sumt > 0){
								addedge(yt, xt, 3);
								genmsg++;
			                               	//	System.out.println(st + " exit " + yt + " to "+ xt);
							}
		                }
					    //}
		            } else {
		                    	//System.out.println(st + " message has no receiver ");
		            }
				} else {
				    /*
				    System.out.println(xx+" Processing "+st);
				    String [] opval = ZKSplit(st);
				    String t;
				    if (opval[1].equals("setData")) 
		 			t = "NodeDataChanged";
				    else if (opval[1].equals("delete"))
					t = "NodeDeleted";
				    else if (opval[1].equals("create"))
					t = "NodeCreated";
				    else {
					System.out.println(st + " contains a unknown opval!");
					continue;
				    }
				    Long xtime = Long.parseLong(opval[0]);
				    Long ctime = (long)-1;
				    int tempy;
				    for (int y : typeref.get("MsgProcEnter")){
		                        Node node = nList.get(y);
		                        Element element = (Element) node;
					String yval = element.getElementsByTagName("OPVAL").item(0).getTextContent();
					if (yval.startsWith("ZK")== false) continue;
					String [] opval2 = ZKSplit(yval);
					if (opval2.length < 2 ) continue;
					System.out.println(y + " "  +opval2[0] + " "+ opval2[1]+ " "+ opval2[2]);
		                        if (opval2[2].equals(opval[2]) && opval2[1].equals(t)){
					    Long ytime = Long.parseLong(opval2[0]);
					System.out.println(y + " "  +opval2[0] + " "+ opval2[1]+ " "+ opval2[2]);
		            		    if ((ytime > xtime) && ((ytime < ctime) || (ctime == -1))){
						yy = y;
						ctime = ytime;
					    }
		                        }
		                    }
				    if (ctime > 0) {
		                            genmsg++;
		                            addedge(xx, yy, 3);
		                        //System.out.println(st +" : "+xx+"->"+yy);
		                        }else {
		                        System.out.println(st + " message has no receiver ");
		                        }		
				}
				
		                sum = 0;
		                yy = -1;
		                for (int y : typeref.get("MsgProcExit")){
		                    Node node = nList.get(y);
		                    Element element = (Element) node;
		                    if ((element.getElementsByTagName("OPVAL").item(0).getTextContent().equals(st))&&
		                            ((idplist.get(y).tid != idplist.get(xx).tid)||(idplist.get(y).pid != idplist.get(xx).pid))){
		                        if (sum == 0) {
		                            yy = y;
		                            sum ++;
		                        }else {
		                            System.out.println(st + " more than one end in receiver : " + yy + " " + y);
		                            continue;
		                        }
		                    }
		                }
		                if (sum > 0) {
		                    genmsg++;
		                    int xt = xx+1;
		                    if ((idplist.get(xt).pid == idplist.get(xx).pid)&&(idplist.get(xt).tid == idplist.get(xx).tid)) {
		                        addedge(yy, xt, 30);
		                        //System.out.println(st + " : " + yy + "->" + xt);
		                    }
		                }else {
		                    System.out.println(st + " message has no callback ");
		                }
				****/
		
				}
            }
        }
        if (hb) {
            System.out.println("HB msg Special process begins");
		    for (int x : typeref.get("MsgProcEnter")){
			Node nodex = nList.get(x);
	                Element elementx = (Element) nodex;
			String st = elementx.getElementsByTagName("OPVAL").item(0).getTextContent();
			if (st.startsWith("ZK") == false) continue;
			//System.out.println(st);
			String [] opval = ZKSplit(st);
			if (opval.length < 3) continue; 
	                String t="";
	                if (opval[1].equals("NodeDataChanged")) 
	                    t = "setData";
	                else if (opval[1].equals("NodeDeleted"))
	                    t = "delete";
	                else if (opval[1].equals("NodeCreated"))
	                    t = "create";
	
			Long xtime = Long.parseLong(opval[0]);
	                Long ctime = (long)-1;
			int yy=-1;
			for (int y :typeref.get("MsgSending") ){
				Node node = nList.get(y);
	                        Element element = (Element) node;
	                        String yval = element.getElementsByTagName("OPVAL").item(0).getTextContent();
				if (yval.startsWith("ZK") == false) continue;
				String [] opval2 = ZKSplit(yval);
				if (opval2[2].equals(opval[2]) && opval2[1].equals(t)){
	                            Long ytime = Long.parseLong(opval2[0]);
	                        //System.out.println(y + " "  +opval2[0] + " "+ opval2[1]+ " "+ opval2[2]);
	                            if ((ytime < xtime) && ((ytime > ctime) || (ctime == -1))){
	                                yy = y;
	                                ctime = ytime;
	                           }
				}
			}
			if (ctime > 0) {
	                            genmsg++;
	                            addedge(yy, x, 3);
	                        System.out.println("ZK Special : "+x+"->"+yy);
	                        }else {
	                        System.out.println(st + " message has no sender ");
	               }
		    }
		}
        System.out.println(notfound + " is not found in " +genmsg+ " in msg part");

        /*
        flag = new int[nList.size()];
        for (int i = 0 ; i < nList.size(); i++){
            flag[i] = 1;
        }
        for (int i = 0 ; i < nList.size(); i++){
            for (Pair pair : edge.get(i)) {
                flag[pair.destination] = 0;
            }
        }
        for (int i = 0 ; i < nList.size(); i++){
            if (flag[i] == 1){
                if (root < 0) root = i;
                else System.out.println("Too many roots");
            }
        }*/
        
        // JX - handle ProcessCreate
        for (int i = 0; i < nList.size(); i++) {
            if (backedge.get(i).size() == 0) {
				int pidi = idplist.get(i).pid;
				int pclink = 0;
				for (int j : typeref.get("ProcessCreate")) {
				    Node npc = nList.get(j);
				    Element epc = (Element) npc;
		  		    String st = epc.getElementsByTagName("OPVAL").item(0).getTextContent();
				    if (pidi == Integer.parseInt(st)) {
						pclink = 1;
						addedge(j, i);
						System.out.println(j + " creates process " + st + " : " + i);
						break;
				    } 
				}
				if (pclink == 1) continue;
		        Node node = nList.get(i);
		        Element e = (Element) node;
				if (e.getElementsByTagName("OPTY").item(0).getTextContent().equals("MsgProcEnter")) {	
					Node nn1 = nList.get(i-1);
					Element en1 = (Element) nn1; //Warn less conditions
					if (en1.getElementsByTagName("OPTY").item(0).getTextContent().equals("MsgProcEnter")) {
						addedge(i-1,i);
						continue;
					}
				}
                if (e.getElementsByTagName("OPTY").item(0).getTextContent().equals("EventProcEnter")) {
                	addedge(i-1,i);
                	continue;
                }
                root.add(i);
                if (!e.getElementsByTagName("OPTY").item(0).getTextContent().equals("ThdEnter")) {
                    System.out.println("There exits a thread starting with " + i + "-" + e.getElementsByTagName("OPTY").item(0).getTextContent());
                }
		    }
        }
        System.out.println("Number of Roots is " + root.size());
        System.out.println("Total added Edge num is " + esum + "\n");
        //Collections.sort(root);
        //System.out.println("Roots is " + root);
    } //End-buildsyncgraph
    
    
    
    //jx: for DEBUGGING and Cascading project
    public void addEdgesManually() {


        for (int i = 0; i < nList.size(); i++) {
             if ( getNodeOPTY(i).equals("ThdExit") ) {
                 System.out.println("JX - DEBUG - " + i + " : " + getNodeOPTY(i) + " " + getNodePID(i)+":"+getNodeTID(i) );
             }
        }

    	
    	for (int i = 0; i < nList.size(); i++) {
    		String nodeLastCallStr = lastCallstack(i);
                if (nodeLastCallStr == null) continue;


                /*
                CL3: org.apache.hadoop.io.IOUtils-copyBytes-69;|org.apache.hadoop.filecache.TrackerDistributedCacheManager-getLocalCache-187;|org.apache.hadoop.filecache.TrackerDistributedCacheManager$CacheStatus-decRefCount-595;|org.apache.hadoop.mapred.TaskTracker$1-run-430;|org.apache.hadoop.mapred.TaskTracker-transmitHeartBeat-1821;|
                */
    		if ( nodeLastCallStr.equals("org.apache.hadoop.filecache.TrackerDistributedCacheManager-getLocalCache-187;") 
    				|| nodeLastCallStr.equals("org.apache.hadoop.filecache.TrackerDistributedCacheManager$CacheStatus-decRefCount-595;")
    				) {
    			System.out.println("JX - DEBUG - " + i + " : " + nodeLastCallStr + " " + getNodePID(i)+":"+getNodeTID(i)  );
    		}
               /*JX - rjob: purgeJob after all map/reduce tasks
CL5~10: org.apache.hadoop.io.IOUtils-copyBytes-69;|org.apache.hadoop.filecache.TrackerDistributedCacheManager-getLocalCache-187;|org.apache.hadoop.filecache.TrackerDistributedCacheManager$BaseDirManager-checkAndCleanup-993;|org.apache.hadoop.filecache.TrackerDistributedCacheManager$BaseDirManager-checkAndCleanup-981;|org.apache.hadoop.filecache.TrackerDistributedCacheManager$CacheStatus-decRefCount-594;|org.apache.hadoop.mapred.TaskTracker-purgeJob-2017;|org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;|sun.reflect.NativeMethodAccessorImpl-invoke-57;|org.apache.hadoop.mapred.TaskTracker-transmitHeartBeat-1821;|
               */
                if ( nodeLastCallStr.equals("org.apache.hadoop.mapred.TaskTracker-purgeJob-2017;") 
                                || nodeLastCallStr.equals("org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;")
                                || nodeLastCallStr.equals("org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914;")
                                ) {
                        System.out.println("JX - DEBUG - " + i + " : " + nodeLastCallStr + " " + getNodePID(i)+":"+getNodeTID(i) );
                }

/*
               CL6: org.apache.hadoop.io.IOUtils-copyBytes-69;|org.apache.hadoop.filecache.TrackerDistributedCacheManager-getLocalCache-187;|org.apache.hadoop.filecache.TrackerDistributedCacheManager$BaseDirManager-checkAndCleanup-993;|org.apache.hadoop.filecache.TrackerDistributedCacheManager$BaseDirManager-checkAndCleanup-981;|org.apache.hadoop.filecache.TrackerDistributedCacheManager$CacheStatus-decRefCount-594;|org.apache.hadoop.mapred.TaskTracker-purgeJob-2017;|org.apache.hadoop.mapred.TaskTracker-addTaskToJob-496;|org.apache.hadoop.mapred.TaskTracker-addTaskToJob-487;|org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3498;|sun.reflect.NativeMethodAccessorImpl-invoke-57;|org.apache.hadoop.mapred.TaskTracker-transmitHeartBeat-1821;|
*/
               if ( nodeLastCallStr.equals("org.apache.hadoop.mapred.TaskTracker-addTaskToJob-496;") ) 
                       System.out.println("JX - DEBUG - " + i + " : " + nodeLastCallStr + " " + getNodePID(i)+":"+getNodeTID(i) );


    	}
/*
JX - DEBUG - 26244 : org.apache.hadoop.filecache.TrackerDistributedCacheManager-getLocalCache-187;
JX - DEBUG - 29322 : org.apache.hadoop.filecache.TrackerDistributedCacheManager$CacheStatus-decRefCount-595;
*/
        addedge(26244, 29322);

/*
JX - DEBUG - 3642 : org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;
JX - DEBUG - 3655 : org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;
JX - DEBUG - 29320 : org.apache.hadoop.mapred.TaskTracker-purgeJob-2017;
JX - DEBUG - 40278 : org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;
JX - DEBUG - 40293 : org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;
JX - DEBUG - 40421 : org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;
JX - DEBUG - 40436 : org.apache.hadoop.mapred.TaskTracker-getMapCompletionEvents-3501;
*/
       addedge(40278, 29320);
       addedge(40293, 29320);
       addedge(40421, 29320);
       addedge(40436, 29320);
       addedge(3642, 29320);
       addedge(3655, 29320);


/*
JX - DEBUG - 38460 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38465 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38470 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38489 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38508 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38527 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38546 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38559 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38572 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38585 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38604 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38617 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38636 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
JX - DEBUG - 38655 : org.apache.hadoop.mapred.TaskTracker$MapEventsFetcherThread-reducesInShuffle-914; 9822:29
*/
      	addedge(38655, 29320);



/*
JX - DEBUG - 21680 : org.apache.hadoop.mapred.TaskTracker-addTaskToJob-496; 9822:66
JX - DEBUG - 26109 : org.apache.hadoop.mapred.TaskTracker-addTaskToJob-496; 9822:39
JX - DEBUG - 34013 : org.apache.hadoop.mapred.TaskTracker-addTaskToJob-496; 9822:57
JX - DEBUG - 34802 : org.apache.hadoop.mapred.TaskTracker-addTaskToJob-496; 9822:48
*/

        addedge(21680, 29320);
        addedge(26109, 29320);
        addedge(34013, 29320);
        addedge(34802, 29320);
        


    //	while(true);
    }
    
    
    
    public String [] ZKSplit(String st){
	int i;
	String [] s = new String[3];
	try {
	//s[0] = "22222";s[1] = "tttttt";s[2] = "333333333";
        String [] pi = st.split("/");
	if (pi.length <2) {
		//System.out.println(st + " is too short");
		return pi;
	}
	String path = pi[1];
	for (i =2 ; i < pi.length; i++)
		path = path + "/"+ pi[i];
        String time = "";
	i = 2;
	while ( (pi[0].charAt(i) <= '9') &&(pi[0].charAt(i)>='0')){
		time = time + pi[0].charAt(i);
		i++;
	}
	s[0] = time;
	s[1] = pi[0].substring(2+time.length());
	s[2] = path;
	} catch (Exception e){
		System.out.println(st + " cannot be splitted in ZKSplit");
		e.printStackTrace();
	}
	return s;
    }
    
    public boolean buildMsgSync(int xx, int yy){
        if (xx == yy) return true;
        int f = 0;
        Node node1 = nList.get(xx);
        Element e1 = (Element) node1;
                /*int pid1 = idplist.get(xx).pid;
                int tid1 = idplist.get(xx).tid;
                System.out.println("pid = "+ pid1 +" tid = "+tid1);*/
        for (int x : ptidref.get(idplist.get(yy))) {
            Node node2 = nList.get(x);
            Element e2 = (Element) node2;
            if ((x != yy) && (e2.getElementsByTagName("OPVAL").item(0).getTextContent().equals(
                    e1.getElementsByTagName("OPVAL").item(0).getTextContent()
            )) && (e2.getElementsByTagName("OPTY").item(0).getTextContent().equals("MsgProcEnter"))) {
                f=1;
                yy = x;
                break;
            }
        }
        Node node3 = nList.get(yy);
        Element e3 = (Element) node3;
        for (int x : ptidref.get(idplist.get(xx))) {
            Node node2 = nList.get(x);
            Element e2 = (Element) node2;
            if ((x != xx) && (e2.getElementsByTagName("OPVAL").item(0).getTextContent().equals(
                    e3.getElementsByTagName("OPVAL").item(0).getTextContent()
            )) && (e2.getElementsByTagName("OPTY").item(0).getTextContent().equals("MsgProcEnter"))) {
                f=2;
                xx = x;
                break;
            }
        }


        if (f == 1) {
            addedge(yy, xx, 3);
            //System.out.println(yy + "->" + xx);
        } else {
            if (f == 0) {
                System.out.println("Cannot find Msg caller and callee relation between "+xx + " and " + yy);
                return false;
            } else {
                addedge(xx, yy, 3);
                //System.out.println(xx + "->" + yy);
            }
        }
        return true;
    }
    
    
    public void addedge(int from , int to){
        Pair p1 = new Pair(from,2);
        Pair p2 = new Pair(to,2);
        edge.get(from).add(p2);
        backedge.get(to).add(p1);
        esum ++;
    }
    
    
    public void addedge(int from , int to, int type){
        // type:  1 -> thread creation and enter,		10 -> thread join, 
    	//        2 -> Eventhandle caller and callee,	20 -> event call back (no use) 
    	//        3 -> Msgsender and Msgreceiver,		30 -> rpc call back
        if ((type == 1)||(type == 2)||(type == 3)){
            //if (emlink.get(to ) != -1)
        	//System.out.println(to + " has msg/event/thd parent already: " + emlink.get(to) + " , be set to " + from);
        	emlink.set(to,from);     
        }
		if (type == 3) 
		    msender.set(to,from);
		if (type == 1) tedgesum++;   //ThdCreate->ThdEnter number
		if (type == 2) eedgesum++;   //EventCreate->EventProcEnter number
		if (type == 3) medgesum++;   //MsgSending->MsgProcEnter nmuber
		//syncedges.get(type).add(new EgPair(from,to));
        addedge(from , to);
    }
    
/*
    public void buildtreepic() {

        String gexfile = xmldir+".gexf";

        try {
            PrintWriter writer = new PrintWriter(gexfile, "UTF-8");
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<gexf xmlns:viz=\"http:///www.gexf.net/1.1draft/viz\" version=\"1.1\" xmlns=\"http://www.gexf.net/1.1draft\">");
            writer.println("<meta lastmodifieddate=\"2010-03-03+23:44\">");
            writer.println("<creator>Gephi 0.9</creator>");
            writer.println("</meta>");
            writer.println("<graph defaultedgetype=\"directed\" idtype=\"string\" type=\"static\">");
            writer.println("<nodes count=\""+nList.size()+"\">");
            for (int i = 0 ; i < nList.size(); i++){
                Node node = nList.get(i);
                Element eElement = (Element) node;
                writer.println("<node id=\"" + i + "\" label=\"" + eElement.getElementsByTagName("OPTY").item(0).getTextContent() + "\"/>");
            }
            writer.println("</nodes>");
            writer.println("<edges count=\""+ esum +"\">");
            int eindex = 0;
            for (int i = 0; i < nList.size(); i++){
                ArrayList<Pair> list = edge.get(i);
                //for(int j = 0 ; j < list.size(); j++){
                for (Pair tj : list){
                    //System.out.println("gexf : " + i + " j = "+ j);
                    //Pair tj = list.get(j);
                    writer.println("<edge id=\""+ eindex +"\" source=\""+ i +"\" target=\""+ tj.destination +"\" weight=\""+ tj.otype+"\"/>");
                    eindex ++;
                }
            }
            writer.println("</edges>");
            writer.println("</graph>");
            writer.println("</gexf>");
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
*/
    
    
    public void buildmemref(){
    	
        for ( int i = 0 ; i< nList.size(); i++){
            Node node2 = nList.get(i);
            Element e2 = (Element) node2;
            if ((e2.getElementsByTagName("OPTY").item(0).getTextContent().equals("HeapWrite") ||
                    (e2.getElementsByTagName("OPTY").item(0).getTextContent().equals("HeapRead")))){
                String address = e2.getElementsByTagName("OPVAL").item(0).getTextContent();
                ArrayList<Integer> list = memref.get(address);
                if (list  == null){
                    list = new ArrayList<Integer>(nList.size());
                    memref.put(address, list);
                }
                list.add(i);
            }
        }
        //
        //System.out.println("HashMap :" + memref);
        //
    }

    public void buildreachset(){
        reach = new ArrayList<HashSet<Integer>>(nList.size());
        flag = new int[nList.size()];
        HashSet<Integer> treach;
        for (int i=0; i < nList.size(); i++) {
            treach = new HashSet<Integer>(nList.size());
            reach.add(treach);
            flag[i] = 1;
        }
        for (int x : root)
            travel(x);
        // print for debug
        //for (int i=0; i<nList.size(); i++) {
        //    System.out.println(reach.get(i));
        //}
        //
    }

    public void travel(int x){
        flag[x] = 0;
        for(Pair pair : edge.get(x)){

            if (flag[pair.destination] == 1) travel(pair.destination);
            //reach.get(x).addAll(reach.get(pair.destination));
            reach.get(x).add(pair.destination);
        }
        reach.get(x).add(x);
        //flag[x] = 0;
    }
    boolean canreach(int source, int target){
        if (source == target) return true;
        for (int x : reach.get(source))
            if (canreach(x,target)) return true;
        return false;
    }

    public void bindtoevent(){

    }
    /******** traditional vector clock implementation *****/
    public void findconcurrent(){
        initfile();
        int consum = 0;
        //System.out.println("Memory location is "+memref.keySet());
        for (String st : memref.keySet()){
            ArrayList<Integer> list = memref.get(st);
            if (list.size() > 1) {
               // System.out.println("Memory location " + st + " : " + list.size() + "   " + consum + " before analysis");//+" "+list);
            }
            //consum += list.size() * (list.size() -1) /2;
            if (!st.contains("c")) {
                for (int i = 0; i < list.size(); i++)
                    for (int j = i + 1; j < list.size(); j++)
                        if ((concurrent(list.get(i), list.get(j)))) {
                            //if ((typeref.get("HeapWrite").contains(i)) ||(typeref.get("HeapWrite").contains(j)))
                            Node node1 = nList.get(list.get(i));
                            Node node2 = nList.get(list.get(j));
                            Element e1 = (Element) node1;
                            Element e2 = (Element) node2;
                            if (e1.getElementsByTagName("OPTY").item(0).getTextContent().equals("HeapWrite")
                                    ||(e2.getElementsByTagName("OPTY").item(0).getTextContent().equals("HeapWrite"))) {

                                consum++;
                                //writefile(list.get(i), list.get(j));
                                /*
                                if ((typeref.get("HeapWrite").contains(i)) ||(typeref.get("HeapWrite").contains(j))) {
                                }else{
                                    System.out.println(typeref.get("HeapWrite"));
                                    System.out.println("i = "+ list.get(i) + " j = "+ list.get(j));

                                }*/

                            }
                            //System.out.println(consum + ": " + list.get(i) + "  " + list.get(j));
                        }
            }
        }
        System.out.println("Concurrent number is at most " + consum);
        //flushfile(consum);
    }

    public void buildvectorclock(){
        vectorclock = new ArrayList<HashMap <IdPair, Integer>>();
        ptidsyncref = new HashMap <IdPair, ArrayList<Integer>>();

        for (int i=0 ; i<nList.size(); i++) {
            vectorclock.add(new HashMap<IdPair, Integer>());
            for (IdPair idPair : ptidref.keySet()){
                vectorclock.get(i).put(idPair, 0);
            }
        }
        ArrayList<Integer> degree = new ArrayList<Integer>();
        ArrayList<Integer> regree = new ArrayList<Integer>();
        for (int i=0 ; i < nList.size(); i++) {
            degree.add(backedge.get(i).size());
            regree.add(0);
        }
        // init the thread internal order

        for (IdPair idpair : ptidref.keySet()){
            int index = 0;
            ptidsyncref.put(idpair, new ArrayList<Integer>());
            ArrayList<Integer> list = ptidref.get(idpair);
            for ( int j : list){
                Node node = nList.get(j);
                Element element = (Element) node;
                //sync node includes MsgSending MsgProcEnter EventProcEnter ThdCreate ThdEnter
                /*if ((element.getElementsByTagName("OPTY").item(0).getTextContent().equals("MsgSending"))||(element.getElementsByTagName("OPTY").item(0).getTextContent().equals("ThdCreate"))
                    ||(element.getElementsByTagName("OPTY").item(0).getTextContent().equals("MsgProcEnter"))||(element.getElementsByTagName("OPTY").item(0).getTextContent().equals("ThdEnter"))
                    ||(element.getElementsByTagName("OPTY").item(0).getTextContent().equals("EventProcEnter")))*/{
                    index ++;
                    ptidsyncref.get(idpair).add(j);
                }
                vectorclock.get(j).put(idpair,index);
            }
        }

        ArrayList<Integer> toposort = new ArrayList<Integer>();
        toposort.addAll(root);
        int [] vcf = new int [nList.size()];
	for ( int xx= 0; xx < nList.size(); xx++) vcf[xx]= 0;
        int sumn = 0;
        while (!toposort.isEmpty()){
            int x = toposort.get(0);
	    vcf[x] ++;
            toposort.remove(0);
            sumn++;
            for (Pair j : edge.get(x)){
                mergevector(j.destination,x);
                regree.set(j.destination,regree.get(j.destination) + 1);
                if (regree.get(j.destination) == degree.get(j.destination)) toposort.add(j.destination);
            }
        }
	
        System.out.println(sumn + " in " + nList.size() + " vectorclock is computed");
	for (int i =0 ; i < nList.size(); i++) 
	    if ((vcf[i] == 0)&& (backedge.get(i).size()==0)) System.out.print( i + " " );
	    //System.out.print(vcf[i] + " ");
//        System.out.println(vectorclock);
    }

    public void mergevector(int t, int s){
        for (IdPair idPair : ptidref.keySet()){
            int max = vectorclock.get(t).get(idPair);
            if (max < vectorclock.get(s).get(idPair)) max = vectorclock.get(s).get(idPair);
            vectorclock.get(t).put(idPair,max);
        }

    }

    public boolean concurrent(int t, int s){
        //System.out.println("computing " + vectorclock.get(t) +" and "+ vectorclock.get(s));
        int sum = 0;
        int flag = 0;
        for (IdPair idPair : ptidref.keySet()){
            int x = vectorclock.get(t).get(idPair) - vectorclock.get(s).get(idPair);
            if (flag * x < 0 ) return true;
            if (x != 0) {
                flag = x;
            }
            //System.out.println(x+" + "+flag + " + "+sum);
            sum += x;
        }

        if (sum == 0) return true;
        return false;
    }

    /******** used for output a xml file ****/
    public void writefile(int x, int y, int freq){
        Node nx = nList.get(x);
        Node ny = nList.get(y);
        Element ex = (Element) nx;
        Element ey = (Element) ny;
        Element opair = outputdoc.createElement("OperationPair");
        opair.appendChild(outputdoc.importNode(nx,true));
        opair.appendChild(outputdoc.importNode(ny,true));
	Attr attr = outputdoc.createAttribute("freq");
        attr.setValue(Integer.toString(freq));
        opair.setAttributeNode(attr);
        docroot.appendChild(opair);
    }
    	
    public void initfile(){
        DocumentBuilderFactory documentBuilderFactory;
        DocumentBuilder documentBuilder;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            outputdoc = documentBuilder.newDocument();
        }catch (ParserConfigurationException e){
            System.out.println("Cannot write output to a xml file");
            return ;
        }
        docroot = outputdoc.createElement("OperationPairs");
        outputdoc.appendChild(docroot);
        return ;
    }

    public void flushfile(int sum){
        Attr attr = outputdoc.createAttribute("Len");
        attr.setValue(Integer.toString(sum));
        docroot.setAttributeNode(attr);
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(outputdoc);
            File wf = new File(xmldir+"-result");
            if (!wf.exists())
                wf.createNewFile();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(wf);
            transformer.transform(source, result);
        } catch (Exception e){
            e.printStackTrace();
            System.out.println(xmldir+"-result"+" cannot be written");
        }

    }

    /******** iteration and bit-indicator for flipped order detection *******/
    public void  eventremovethreadorder() {
        /*
        for (int i = 0 ; i < nList.size(); i++){
            Node node = nList.get(i);
            Element e = (Element) node;
            String  value = e.getElementsByTagName("OPVAL").item(0).getTextContent();
            if ((e.getElementsByTagName("OPTY").item(0).getTextContent().equals("EventProcEnter"))&&(!value.contains("GenericEventHandler"))){
                int j = i-1;
                Node node2 = nList.get(j);
                Element e2 = (Element) node2;
                if ((!e2.getElementsByTagName("OPTY").item(0).getTextContent().equals("EventProcEnter")) &&
                        (idplist.get(i).pid == idplist.get(j).pid) &&(idplist.get(i).tid== idplist.get(j).tid)){
                    removeedge(j,i);
                    //System.out.println(j + "->"+ i + " is removed from graph");
                }
            }
        }*/
	int sumremove =0;
        for (int i : typeref.get("EventProcEnter")) {
            if (eventcaller.get(i) >= 0) {
                int j = i - 1;
                if ((idplist.get(i).pid == idplist.get(j).pid) && (idplist.get(i).tid == idplist.get(j).tid)) {
                    removeedge(j, i);
		    sumremove ++;
                    //System.out.println(j + "->"+ i + " is removed from graph");
                }
            }
        }
        System.out.println(esum + " edges remains in graph, " + sumremove +" is removed");
    }
    public void removeedge(int x, int y){
         // remove edge x -> y
        int f =0;
        ArrayList<Pair> list= edge.get(x);
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).destination == y){
                list.remove(i);
                esum--;
                f = 1;
                ArrayList<Pair> l2 = backedge.get(y);
                for (int j =0; j < l2.size(); j++){
                    if (l2.get(j).destination== x){
                        l2.remove(j);
                        return;
                    }
                }
                System.out.println("Dont find backedge from " + y + " to "+x + ", when removeing");
                break;
            }
        if (f ==0)
            System.out.println("Dont find edge from " + x + " to "+y + ", when removeing");
    }
    
    
    //Added & Modified by JX
    public void buildReachSet() {
    	//eventremovethreadorder();
        eventend = new ArrayList<Integer>(nList.size());
        for (int i = 0 ; i < nList.size(); i++)
            eventend.add(-1);
        int callersum = 0;
        for (int i = 0; i < nList.size(); i++)
            if (eventcaller.get(i) > -1 ) callersum++;
        //Commented by JX
        //System.out.println("Caller sum is " + callersum);
        //end-Commented
        for (int i : typeref.get("EventProcEnter")) {
            Node node = nList.get(i);
            Element e = (Element) node;
            String val = e.getElementsByTagName("OPVAL").item(0).getTextContent();
            int f = 0;
            for (int j : typeref.get("EventProcExit")){
                Node node2 = nList.get(j);
                Element e2 = (Element) node2;
		if (val.contains("!"))
		{
		 String [] valt = e2.getElementsByTagName("OPVAL").item(0).getTextContent().split("!");
		 String [] vals = val.split("!");
		 if ((valt[0] == vals[0])&&(valt[2] == vals[2])){
		    eventend.set(i,j);
                    f = 1;
                    break;
		 }
		}else if (e2.getElementsByTagName("OPVAL").item(0).getTextContent().equals(val)){
                    eventend.set(i,j);
                    f = 1;
                    break;
                }
            }
            if (f == 0) {
            	//System.out.println("One event : " + i + " misses its end ");
            }
        }
        
        // JX - compute reachability matrix
        System.out.println("\nJX - compute reachability matrix");
        reachbitset = new ArrayList<BitSet>(nList.size());
        for (int i =0 ; i< nList.size(); i++) {
            reachbitset.add(new BitSet(nList.size()));
        }
        int loop =0;
        do {
            System.out.println(loop + " time iteration for event atomic edges. esum = " + esum);
            computereachbitset();
            loop++;
            System.out.println("compute bit set finished");
        } while (addeventatomicedge()&&(loop<20));

    }
    
    
    //Modified by JX
    public boolean isConcurrent(int x, int y) {
    	return isFlippedorder(x, y);
    }
    
    /**
     * isFlippedorder - ie, concurrent or not
     */
    public boolean isFlippedorder(int x, int y) {
		IdPair ip1 = idplist.get(x);
		IdPair ip2 = idplist.get(y);
		if ( ip1.pid != ip2.pid ) return false;         //JX- don't consider diff processes
		if ( ip1.tid == ip2.tid ) return false;         //should be IMPO
	    if (reachbitset.get(x).get(y)) return false;    //JX- can reach
	    if (reachbitset.get(y).get(x)) return false;    //JX- can reach
	    return true;
    }
    //End-Added
    
    /** Commented out by JX
    public boolean flippedorder(int x, int y, String st) {
		IdPair ip1 = idplist.get(x);
		IdPair ip2 = idplist.get(y);
		//if (! ((ip1.pid == ip2.pid) && (ip1.tid != ip2.tid))) return false;  
		if ((ip1.pid != ip2.pid) && (!st.contains("hbase")) ) return false;  //JX- don't consider diff processes  
	    if (reachbitset.get(x).get(y)) return false;    //JX- can reach
	    if (reachbitset.get(y).get(x)) return false;    //JX- can reach
	    return true;
    }
    */
    
    
    
    
    
    
    /*******************************************************************
     * Query  //added by JX 
     *******************************************************************/
    
    
    /**
     * before doing ReachSet
     */
    /*
	public void queryHBEdge(int x, int y) {
		edge.get(x).get
	}
	*/
	
	
	

	public void queryHappensBeforeRelations(String path) {
		try {
		File file = new File(xmldir + "result/suggestion");
		file.mkdir();
		
		TextFileReader br = new TextFileReader(path);
		//BufferedReader br = new BufferedReader(new FileReader(path));
		    String line = "";
		    while ((line = br.readLine())!=null){
			String [] eles = line.split("\\s+");
			int x = Integer.parseInt(eles[0]);
			int y = Integer.parseInt(eles[1]);
			/* commented by JX
			if ((!reachbitset.get(x).get(y)) && (!reachbitset.get(y).get(x))) {
			    String str = suggestion(x,y);
			    writesuggestion(x,y,str);
			    String [] strs = str.split("!");
	    		    String [] basic = strs[0].split("@");
			    String [] adv   = strs[1].split("@");
	                    System.out.println("str = "+str);
	                    System.out.println("Basic x : " + basic[0]);
	                    System.out.println("Basic y : " + basic[1]);
	                    System.out.println("Advance x : " + adv[0]);
	                    System.out.println("Advance y : " + adv[1]);
			    System.out.println(x + " & " + y + " have no HB relation");
			    continue;
		        }
		    */
			if (reachbitset.get(x).get(y))
				System.out.println(x + " -> "+ y );
			if (reachbitset.get(y).get(x))
				System.out.println(x + " <- "+ y );
		}
		} catch (Exception e){
			System.out.println("Query file open error");
			e.printStackTrace();
		}
	}


	public void writesuggestion(int x, int y, String str){
	        try{
	            File file = new File(xmldir + "result/suggestion"+"/"+x+"-"+y);
			    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			    String s1 = str.split("!")[0];
		            String []s2 = s1.split("@");
			    String []s21 = s2[0].split("&");
			    String []s22 = s2[1].split("&");
			    bw.write(s21[0] + "\n");
			    bw.write(s21[1] + "\n");
			    bw.write("java.lang.Thread-getStackTrace-1589;" + s21[2] + "\n");
			    bw.write(s22[0] + "\n");
			    bw.write(s22[1] + "\n");
			    bw.write("java.lang.Thread-getStackTrace-1589;" + s22[2] + "\n");
		            //bw.write(str);
			    bw.close();
		    }catch (Exception e){
			    e.printStackTrace();
	        }
	   }


	/**
     * JX - Core
     */
    public void findflippedorder(){
        
        //JX - ??
        initfile();

        //JX - prepare 3 output files - complex, median, simple
        BufferedWriter cout = null;
        BufferedWriter sout = null;
        BufferedWriter mout = null;
        //prepare others
        BufferedWriter cdout = null;
        BufferedWriter sdout = null;
        BufferedWriter mdout = null;
        try {
        	File complexfile = new File(xmldir+"result", "complex");  //JX:complete
        	File simplefile =  new File(xmldir+"result", "simple");   //JX: the same last level of call stacks
        	File medianfile = new File(xmldir+"result", "median");    //JX: the same calls tacks
            cout = new BufferedWriter(new FileWriter(complexfile));
            sout = new BufferedWriter(new FileWriter(simplefile));
            mout = new BufferedWriter(new FileWriter(medianfile));
            // prepare others
            File complexdetailedfile =  new File(xmldir+"result", "complex-detailed");
            File simpledetailedfile =  new File(xmldir+"result", "simple-detailed");
            File mediandetailedfile =  new File(xmldir+"result", "median-detailed");
            cdout = new BufferedWriter(new FileWriter(complexdetailedfile));
            sdout = new BufferedWriter(new FileWriter(simpledetailedfile));
            mdout = new BufferedWriter(new FileWriter(mediandetailedfile));
        } catch (Exception e){}

        int totalsum = 0;
        //System.out.println("Memory location is "+memref.keySet());
        
        //Added by JX
        System.out.println("\nJX - Results of Flipped Locks");
        /*
        // JX - get mutual effect by R & W , ie, flipped R/W pairs.   PS - this is fine, because the number of R/W locks is small usually. 
        for (String memaddr1: dotlockmemref.keySet()) {
        	ArrayList<Integer> list1 = dotlockmemref.get(memaddr1);
        	for (int i = 0; i < list1.size(); i++) {
        		int index1 = list1.get(i);
        		// jx - if exists, means SingleObject.lock()? or some R/W locks haven't been recorded?    For now, haven't taken place
        		if ( isReadOrWriteLock(index1).equals("null") )
        			System.out.println("JX - ERROR??? - " + lastCallstack(index1) ); 
        		// get one R & one W to check if they are flipped
        		if (isReadOrWriteLock(index1).equals("R")) {
        			for (String memaddr2: dotlockmemref.keySet()) {
        				ArrayList<Integer> list2 = dotlockmemref.get(memaddr2);
        	        	for (int j = 0; j < list2.size(); j++) {
        	        		int index2 = list2.get(j);
        	        		if (isReadOrWriteLock(index2).equals("W")) {
        	        			// related ReadLock & WriteLock
        	        			if ( isRelatedLocks(index1, index2) ) {
        	        				// is flippedorder?
        	        				if ( isFlippedorder(index1, index2) ) {
        	        					totalsum++;
        	        					writeaddrlist(index1, index2);    //JX - for simple
        	                            writeaddrlist2(index1, index2);   //JX - for median
        	                            writetext(cout, index1, index2, 1, 0);  //'1' means freq=1, added by JX
        	                            writetextDetailed(cdout, index1, index2, 1); //Added by JX
        	        				}
        	        			}
        	        		}
        	        	}
        			}
        		}
        	}
        }
        */
        System.out.println("After getting R & W pairs, #pairs in 'complex'(complete): " + totalsum );
        
        //find mutual effect by the same lock
        /*
        System.out.println("#lockmemaddr = " + lockmemref.size());
        for (String memaddr : lockmemref.keySet()) {
            ArrayList<Integer> list = lockmemref.get(memaddr);
            System.out.println("memaddr " + memaddr + " has " + list.size() + " locks");
            int lsum = 0;
            for (int i = 0; i < list.size(); i++) {
            	int index1 = list.get(i);
                for (int j = i + 1; j < list.size(); j++) {
                	int index2 = list.get(j);
                	if ( isReadOrWriteLock(index1).equals("R") && isReadOrWriteLock(index2).equals("R") ) continue;  //filter R & R
                    if ( flippedorder(index1, index2) ) {
                        //if ((typeref.get("HeapWrite").contains(i)) ||(typeref.get("HeapWrite").contains(j)))
                        totalsum++;
                        lsum++;
                        writeaddrlist(index1, index2);    //JX - for simple
                        writeaddrlist2(index1, index2);   //JX - for median
                        writetext(cout, index1, index2, 1, 0);  //'1' means freq=1, added by JX
                        writetextDetailed(cdout, index1, index2, 1); //Added by JX
                        //System.out.println("JX - " + totalsum + ": " + list.get(i) + "  " + list.get(j));
                    }
                }
            }
        }
        */
        System.out.println("#pairs in 'complex'(complete): " + totalsum);
        //End-Added
        
        /**
         * Commented out by JX
         * 
        for (String st : memref.keySet()){
            ArrayList<Integer> list = memref.get(st);
            int lsum = 0;
            //consum += list.size() * (list.size() -1) /2;
            //if (!st.contains("c")) {
            if (true) {
                for (int i = 0; i < list.size(); i++)
                    for (int j = i + 1; j < list.size(); j++)
                        if (flippedorder(list.get(i),list.get(j),st) && hbzkinit(list.get(i),list.get(j))) {
                            //if ((typeref.get("HeapWrite").contains(i)) ||(typeref.get("HeapWrite").contains(j)))
                            Node node1 = nList.get(list.get(i));
                            Node node2 = nList.get(list.get(j));
                            Element e1 = (Element) node1;
                            Element e2 = (Element) node2;
                            if (e1.getElementsByTagName("OPTY").item(0).getTextContent().equals("HeapWrite")
                                    ||(e2.getElementsByTagName("OPTY").item(0).getTextContent().equals("HeapWrite"))) {
                                consum++;
                                writeaddlist(list.get(i), list.get(j));
                                writeaddlist2(list.get(i), list.get(j));
                                writetext(cout,list.get(i), list.get(j),0);
                                lsum++;
                                //if ((typeref.get("HeapWrite").contains(i)) ||(typeref.get("HeapWrite").contains(j))) {
                                //}else{
                                //    System.out.println(typeref.get("HeapWrite"));
                                //    System.out.println("i = "+ list.get(i) + " j = "+ list.get(j));
                                //}
                            }
                            //System.out.println(consum + ": " + list.get(i) + "  " + list.get(j));
                        }
                if ((list.size() > 1)&&(lsum > 0)) {
                    //System.out.println("Memory location " + st + " : " + list.size() + "  give " + lsum);//+" "+list);
                }
            }
        }
        */
        
	    /*	
	    if (hb) {
		    int temptimer = 0;
		    System.out.println("HB special write parse begins");
		    for (int i :typeref.get("HeapWrite") ){
			temptimer ++;
			//if (temptimer % 500 == 0) 
			System.out.println(temptimer+" HB special write-write parsed.");
			for (int j : typeref.get("HeapWrite")){
			    if ((i!=j) && (hbspecial(i,j))){
				consum++;
	                        writeaddlist(i, j);
	                        writetext(cout,i, j);
				System.out.println(i + "  "+ j);
	                        //lsum++;
			    }
			}
			System.out.println(temptimer+" HB special write-read parsed.");
	                for (int j : typeref.get("HeapRead")){
	                    if ((i!=j) && (hbspecial(i,j))){
	                        consum++;
	                        writeaddlist(i, j);
	                        writetext(cout,i, j);
				System.out.println(i + "  "+ j);
	                        //lsum++;
	                    }
	                }
	
		    }
		}*/
		
        //JX - write to 'simple' by freq
        System.out.println("#pairs in 'simple': " + outputlist.keySet().size());
        HashMap <Integer, ArrayList<IdPair>> sortingout = new HashMap <Integer, ArrayList<IdPair>>();
		for (IdPair idPair : outputlist.keySet()) {
			//stasticalana(idPair.pid,idPair.tid, outputlist.get(idPair));
			//writefile(idPair.pid, idPair.tid, outputlist.get(idPair));
		    int freq = outputlist.get(idPair);
		    if (sortingout.get(freq) == null)
			sortingout.put(freq, new ArrayList<IdPair>());
		    sortingout.get(freq).add(idPair);
		    //System.out.print(outputlist.get(idPair) + " ");
	    }
		ArrayList<Integer> freqset = new ArrayList(sortingout.keySet());
		Collections.sort(freqset, Collections.reverseOrder());
		int remain = 0;
		int remainsum = 0;
	        int oldgetcstate;
	        int oldtwotrans;
	   	int olduni3;
		for (int freq : freqset) {
		    for (IdPair idPair : sortingout.get(freq)){
		    	oldgetcstate = unigetstate;
		    	oldtwotrans  = unitwotrans;
		    	olduni3      = uni3;
		        stasticalana(idPair.pid,idPair.tid, outputlist.get(idPair));
		        //if ((oldgetcstate == unigetstate)&&(oldtwotrans == unitwotrans)&&(olduni3 == uni3)){
		        writefile(idPair.pid, idPair.tid, outputlist.get(idPair));
				writetext(sout, idPair.pid, idPair.tid, outputlist.get(idPair), 0); //JX - real write
				writetextDetailed(sdout, idPair.pid, idPair.tid, outputlist.get(idPair)); //Added by JX
				remain += 1;
				remainsum += outputlist.get(idPair);
		        //}
			}
		}
		
		//JX - write to 'median'
		System.out.println("#pairs in 'median': " + outputlist2.keySet().size());
		for (IdPair idPair : outputlist2.keySet()){
		    writetext(mout, idPair.pid, idPair.tid, outputlist2.get(idPair), 1); //JX - real write
		    writetextDetailed(mdout, idPair.pid, idPair.tid, outputlist2.get(idPair)); //Added by JX
		}
		
		System.out.println("\nJX - Summary");
		System.out.println("#pairs for 'complex'=" + totalsum + " ,'simple'=" + outputlist.keySet().size() + " ,'median'=" + outputlist2.keySet().size());
		//System.out.println("getCurrentState + doTransition = " + getcurdotrans + " unique:"+unigetstate);
		//System.out.println("doTransition + doTransition = " + twodotrans + " unique:" + unitwotrans);
		//System.out.println("getheadroom + setheadroom unique:" + uni3);
		//System.out.println("remaining "+ remainsum+" unique:"+remain);
		System.out.println("Thread edge : "+ tedgesum +", Event edge : "+ eedgesum + ", Msg edge : "+ medgesum);
		//flushfile(outputlist.keySet().size());
		
		//Added by JX
		//build Lock Relationship Graph
		/*  ask Haopeng for edge or BitSet???
		for 
        Pair p1 = new Pair(from,2);
        Pair p2 = new Pair(to,2);
        edge.get(from).add(p2);
        backedge.get(to).add(p1);
        */
		
    }
    
    
public boolean hbspecial(int x, int y) {
	//special Identity and flipped order for HBase
	if (reachbitset.get(x).get(y) || reachbitset.get(y).get(x)) return false;
        Node nx = nList.get(x);
	Node ny = nList.get(y);
	Element ex = (Element) nx;
	Element ey = (Element) ny;
	String valx = ex.getElementsByTagName("OPVAL").item(0).getTextContent();
	String valy = ey.getElementsByTagName("OPVAL").item(0).getTextContent();
	String [] valxs = valx.split("/");
	String [] valys = valx.split("/");
	if (valxs.length * valys.length == 0) return false;
	if (valxs.length != valys.length) return false;
	for (int ii =1; ii < valxs.length; ii++)
		if (!valxs[ii].equals(valys[ii])) return false;
	return true;
}

public boolean hbzkinit(int x, int y){
        //special Identity and flipped order for HBase
        if (reachbitset.get(x).get(y) || reachbitset.get(y).get(x)) return false;
        Node nx = nList.get(x);
        Node ny = nList.get(y);
        Element ex = (Element) nx;
        Element ey = (Element) ny;
        Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
        Element sx = (Element) esx.getElementsByTagName("Stack").item(0);
        String xmethod = sx.getElementsByTagName("Method").item(0).getTextContent();
        Element esy = (Element) ey.getElementsByTagName("Stacks").item(0);
        Element sy = (Element) esy.getElementsByTagName("Stack").item(0);
        String ymethod = sy.getElementsByTagName("Method").item(0).getTextContent();
        if ((xmethod.equals("setData")|| xmethod.equals("getData"))&& zkcreatelist.contains(y)){
            int mat= 0;
            for (int j : zkcreatelist)
                if (reachbitset.get(j).get(x)) mat++;
            if (mat>=0) return false;
        }
        if ((ymethod.equals("setData")|| ymethod.equals("getData"))&& zkcreatelist.contains(x)){
            int mat= 0;
            for (int j : zkcreatelist)
                if (reachbitset.get(j).get(y)) mat++;
            if (mat>=0) return false;
        }

        return true;
}

public void writetext(BufferedWriter bf , int x ,int y, int freq, int pat){
	try{
	//   bf.write(y);
	//   bf.write(x);
	//   bf.newLine();
	   bf.write("IDs:" + String.valueOf(x) +"/"+ String.valueOf(y) +" freq:"+ String.valueOf(freq) +"\n");
	   bf.write(String.valueOf(x));
	   int j = emlink.get(x);
	   int len = 10;
	   while (j > -1){
	        bf.write("<-"+j);
		j = emlink.get(j);
		len --;
		if (len ==0) break;
	   }
	   bf.write("<-"+String.valueOf(emlink2.get(x)));
	   bf.write("\n");

           bf.write(String.valueOf(y));
           j = emlink.get(y);
           len = 10;
           while (j > -1){
                bf.write("<-"+j);
                j = emlink.get(j);
                len --;
		if (len == 0) break;
           }
	   bf.write("<-"+String.valueOf(emlink2.get(y)));
	   bf.write("\n");
//	   if (pat == 1){	
	   if (pat == 100){	
	       String[] st = suggestion(x,y).split("!");
	       bf.write(st[0]+"\n");
   	       bf.write(st[1]+"\n");
	   }
	   //IdPair ip1 = idplist.get(x);
           //IdPair ip2 = idplist.get(y);	
	   //bf.write(ip1.pid+"-"+ip1.tid+ " + " + ip2.pid+"-"+ip2.tid+"\n");
	   bf.flush();
	} catch (Exception e){}
}

//Added by JX
//detailed with callstacks
public void writetextDetailed(BufferedWriter bf , int x ,int y, int freq) {
	try{
		bf.write("IDs:" + String.valueOf(x) +"/"+ String.valueOf(y) +" freq:"+ String.valueOf(freq) +"\n");
		bf.write(String.valueOf(x));
		int j = emlink.get(x);
		int len = 10;
		while (j > -1){
	        bf.write("<-"+j);
	        j = emlink.get(j);
	        len --;
	        if (len ==0) break;
		}
		bf.write("<-"+String.valueOf(emlink2.get(x)));
		bf.write("\n");
		bf.write(String.valueOf(y));
		j = emlink.get(y);
		len = 10;
		while (j > -1) {
			bf.write("<-"+j);
			j = emlink.get(j);
			len --;
			if (len == 0) break;
		}
		bf.write("<-" + String.valueOf(emlink2.get(y)) + "\n");
	   
		//print detailed Callstacks
		Node node1 = nList.get( x );
		Node node2 = nList.get( y );
		String wholeidentity1 = getWholeIdentity( node1 );
		String wholeidentity2 = getWholeIdentity( node2 );
	    bf.write("Callstack of ID " + String.valueOf(x) + ":\n" + wholeidentity1 + "\n");
		bf.write("Callstack of ID " + String.valueOf(y) + ":\n" + wholeidentity2 + "\n");
		bf.write("\n");
		bf.flush();
	} catch (Exception e) {}
}


public String computeLock(int x, int y){
    
    int i = locktrace.get(x);
    int j = locktrace.get(y);
    ArrayList<Integer> lockx = new ArrayList<Integer>();
    ArrayList<Integer> locky = new ArrayList<Integer>();
    System.out.println("i = "+i+"; j = "+j); 
    while(i > 0){
	lockx.add(i);
	i = locktrace.get(i);
    }

    while(j > 0){
        locky.add(j);
        j = locktrace.get(j);
    }
    System.out.println(x +"'s lockset" + lockx); 
    System.out.println(y +"'s lockset" + locky); 
    for (i = lockx.size() -1; i >=0 ; i--){
	int ax = lockx.get(i);
	Node nx = nList.get(ax);
        Element ex = (Element) nx;
	String sxval = ex.getElementsByTagName("OPVAL").item(0).getTextContent();
	for (j = 0; j < locky.size(); j++){
	    int ay = locky.get(j);
	    Node ny = nList.get(ay);
	    Element ey = (Element) ny;
	    if (ey.getElementsByTagName("OPVAL").item(0).getTextContent().equals(sxval)) 
		return ax + " "+ ay;
	}
    }
    return "";

}

public String suggestion(int x, int y){

    int i = x;
    int j = y;
    Element ei = null;
    Element ej = null;
    
    while(true){
	System.out.println("   "+i + " "+ j);
	if ((i==-1)||(j ==-1)) break;

        String stemp = computeLock(i,j);
        System.out.println("Lock transfer's result "+ stemp);
        if (stemp!="") {
            String []ss = stemp.split(" ");
            i = Integer.parseInt(ss[0]);
            j = Integer.parseInt(ss[1]);
            continue;
        }

	Node ni = nList.get(i);
	ei      = (Element) ni;
//        Element esi = (Element) ei.getElementsByTagName("Stacks").item(0);
//	Element si  = (Element) esi.getElementsByTagName("Stack").item(0);
	String tidi = ei.getElementsByTagName("TID").item(0).getTextContent();

        Node nj = nList.get(j);
        ej      = (Element) nj;
//        Element esj = (Element) ej.getElementsByTagName("Stacks").item(0);
//        Element sj  = (Element) esj.getElementsByTagName("Stack").item(0);
	String tidj = ej.getElementsByTagName("TID").item(0).getTextContent();
	
	if (tidi.equals(tidj)){
	    i = emlink.get(emlink.get(i));
	    j = emlink.get(emlink.get(j));
	    continue;
	}else{
	    int ti = emlink.get(i);
	    int tj = emlink.get(j);
	    System.out.println("        "+i + " "+ j);
	    if ((ti == -1) && (tj == -1)) break;
	    if (typeref.get("MsgProcEnter").contains(ti) && typeref.get("MsgProcEnter").contains(tj)){ 
	    Node nx = nList.get(ti);
	    Node ny = nList.get(tj);
	    Element ex = (Element) nx;
	    Element ey = (Element) ny;
	    Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
    	    Element esy = (Element) ey.getElementsByTagName("Stacks").item(0);
	    Element sx = (Element) esx.getElementsByTagName("Stack").item(0);
	    Element sy = (Element) esy.getElementsByTagName("Stack").item(0);
	    String xclass = sx.getElementsByTagName("Class").item(0).getTextContent();
	    String yclass = sy.getElementsByTagName("Class").item(0).getTextContent();
	    if (xclass.equals(yclass)){
		i = emlink.get(ti);
		j = emlink.get(tj);
		continue;
	    }
	}
		
	}
	/*
	String stemp = computeLock(i,j);
	System.out.println("Lock transfer's result "+ stemp);
	if (stemp!="") {
	    String []ss = stemp.split(" ");
	    i = Integer.parseInt(ss[0]);
	    j = Integer.parseInt(ss[1]);
	    continue;
	}*/
	break;
    }
    if ((i==-1)||(j ==-1)) return "-1 No suggestion@No suggestion"
				+"!-1 No suggestion@No suggestion";
    //String sti = Integer.toString(i)+" "+lastCallstack(i);
    String sti = lastCallstack(i) + "&" + fullCallstack(i);
    //String stj = Integer.toString(j)+" "+lastCallstack(j);
    String stj = lastCallstack(j) + "&" + fullCallstack(j);
    /*
    int ind = 0;
    Element esi = (Element) ei.getElementsByTagName("Stacks").item(0);
    while (true){
	Element si  = (Element) esi.getElementsByTagName("Stack").item(ind);
	if (si.getElementsByTagName("Line").item(0).getTextContent().equals("-1")){
	    ind++;
	    continue;
	}
	sti = i +" "
	    + si.getElementsByTagName("Class").item(0).getTextContent()  + " "
	    + si.getElementsByTagName("Method").item(0).getTextContent() + " "
	    + si.getElementsByTagName("Line").item(0).getTextContent();
	break;
    }
    ind = 0;
    Element esj = (Element) ej.getElementsByTagName("Stacks").item(0);
    while(true){
        Element sj  = (Element) esj.getElementsByTagName("Stack").item(ind);
	if (sj.getElementsByTagName("Line").item(0).getTextContent().equals("-1")){
            ind++;
            continue;
        }
        stj = j + " " 
	    + sj.getElementsByTagName("Class").item(0).getTextContent()  + " "
            + sj.getElementsByTagName("Method").item(0).getTextContent() + " "
            + sj.getElementsByTagName("Line").item(0).getTextContent();
	break;
    }*/
    String ai = "#";
    String aj = "#";
    if ((emlink.get(i)>-1)&&(emlink.get(emlink.get(i)) > -1)){
        int ti = emlink.get(emlink.get(i));
        String idi = getIdentity(nList.get(i));
        String idti = getIdentity(nList.get(ti));
        int oi = identity.get(idi);
        int oti = identity.get(idti);
        ai = lastCallstack(ti) + " " + oti + " vs " + oi;
    }
    if ((emlink.get(j)>-1)&&(emlink.get(emlink.get(j)) > -1)){
        int tj = emlink.get(emlink.get(j));
        String idj = getIdentity(nList.get(j));
        String idtj = getIdentity(nList.get(tj));
        int oj = identity.get(idj);
        int otj = identity.get(idtj);
        aj = lastCallstack(tj) + " " + otj + " vs " + oj;  
    }
    return sti+"@"+stj+"!"+ai+"@"+aj;

}
public void stasticalana(int x, int y, int freq) {
    Node nx = nList.get(x);
    Node ny = nList.get(y);
    Element ex = (Element) nx;
    Element ey = (Element) ny;
    Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
    Element esy = (Element) ey.getElementsByTagName("Stacks").item(0);
    Element sx = (Element) esx.getElementsByTagName("Stack").item(0);
    Element sy = (Element) esy.getElementsByTagName("Stack").item(0);
    String xmethod = sx.getElementsByTagName("Method").item(0).getTextContent();	
    String ymethod = sy.getElementsByTagName("Method").item(0).getTextContent();
    if ((xmethod.equals("getCurrentState"))||(ymethod.equals("getCurrentState"))) { getcurdotrans += freq; unigetstate++;}
    if ((xmethod.equals("doTransition"))&&(ymethod.equals("doTransition"))) { twodotrans += freq; unitwotrans ++;}
    if ((xmethod.equals("setHeadroom"))||(ymethod.equals("setHeadroom"))) { uni3 ++;}
}

	//JX - for simple output
    public void writeaddrlist(int x, int y) {
        for (IdPair idPair : outputlist.keySet()) {
            int i = idPair.pid;
            int j = idPair.tid;
            if ((lastCallstackEqual(i,x)&&(lastCallstackEqual(j,y))) || ((lastCallstackEqual(i,y))&&(lastCallstackEqual(j,x)))) {
                int freq = outputlist.get(idPair);
                outputlist.put(idPair, freq+1);
                //System.out.println("Find duplicated pair");
                return ;
            }
        }
        IdPair idPair1 = new IdPair(x,y);
        outputlist.put(idPair1,1);
   }
    
    public boolean isSpecial4637(int x , int y ){
        int xx = emlink.get(x);
	int yy = emlink.get(y);
	if ((xx < 0) ||(yy <0)) return false;
	Node nx = nList.get(xx);
        Node ny = nList.get(yy);
        Element ex = (Element) nx;
        Element ey = (Element) ny;

        String optyx = ex.getElementsByTagName("OPTY").item(0).getTextContent();
        String opvalx = ex.getElementsByTagName("OPVAL").item(0).getTextContent();

        String optyy = ey.getElementsByTagName("OPTY").item(0).getTextContent();
        String opvaly = ey.getElementsByTagName("OPVAL").item(0).getTextContent();
 
	if (((opvalx.contains("TA_ASSIGNED!"))&&(opvaly.contains("TA_DIAGNOSTICS_UPDATE"))) 
		|| ((opvaly.contains("TA_ASSIGNED!"))&&(opvalx.contains("TA_DIAGNOSTICS_UPDATE"))))
		return true;
	return false;	
    }
    
    //JX - for median output
    public void writeaddrlist2(int x, int y){
	if (special4637 ==0 ){
             special4637 = 1;
	     if (isSpecial4637(x,y)){
		IdPair idPair1 = new IdPair(x,y);
	        outputlist2.put(idPair1,1);
	     }

        }
        for (IdPair idPair : outputlist2.keySet()){
            int i = idPair.pid;
            int j = idPair.tid;
            if ((wholeCallstackEqual(i,x)&&(wholeCallstackEqual(j,y)))||((wholeCallstackEqual(i,y))&&(wholeCallstackEqual(j,x)))){
                int freq = outputlist2.get(idPair);
                outputlist2.put(idPair,freq+1);
                return ;
            }
        }
        IdPair idPair1 = new IdPair(x,y);
        outputlist2.put(idPair1,1);

        //System.out.println("Find new pair");
    }

    
    public boolean addeventatomicedge(){
        int change = 0;
	int ec = 0;
	int mc = 0;
	/*
        for (IdPair idPair : ptideventref.keySet()){
            for (int i : ptideventref.get(idPair))
                for (int j : ptideventref.get(idPair)) {
                    if //((eventcaller.get(i) >= 0) && (eventcaller.get(j) >= 0) &&
                            ((eventend.get(i) >= 0) && (eventend.get(j) >= 0)) {
                        if ((i != j) && (!reachbitset.get(eventend.get(i)).get(j)) && (!reachbitset.get(eventend.get(j)).get(i)) &&
                                (eventcaller.get(i) >= 0) && (eventcaller.get(j) >= 0)) {
                            if (reachbitset.get(eventcaller.get(i)).get(eventcaller.get(j))) {
                                addedge(eventend.get(i), j);
                                //System.out.println("Event Atomic edge " + eventend.get(i) + "->" + j);
                                change++;ec++;
                            }
                            if (reachbitset.get(eventcaller.get(j)).get(eventcaller.get(i))) {
                                addedge(eventend.get(j), i);
                                //System.out.println("Event Atomic edge " + eventend.get(j) + "->" + i);
                                change++;ec++;
                            }
                        }
                    }
                }
        }
	*/
	for (int i :typeref.get("EventProcEnter") )
            for (int j :typeref.get("EventProcEnter") ){
		IdPair pi = idplist.get(i);
                IdPair pj = idplist.get(j);
                if ( ((pi.pid != pj.pid) ||(pi.tid != pj.tid)) ) continue;
		int sendi = eventcaller.get(i);
                int sendj = eventcaller.get(j);
                int exiti = eexit.get(i);
                int exitj = eexit.get(j);
		if ((sendi == -1) ||(sendj == -1) ||
		    (exiti == -1) ||(exitj == -1) || (i==j)) continue;
                if (reachbitset.get(exiti).get(j) || reachbitset.get(exitj).get(i) ) continue;
		if (reachbitset.get(sendi).get(sendj)){
                        addedge(exiti,j);
                        change++;ec++;eedgesum++;
//			System.out.println(i+ " ~~~~~ "+j);
                }
                if (reachbitset.get(sendj).get(sendi)){
                        addedge(exitj,i);
                        change++;ec++;eedgesum++;
                }
		
	}
	for (int i :typeref.get("MsgProcEnter") )
	    for (int j :typeref.get("MsgProcEnter") ){
		IdPair pi = idplist.get(i);
		IdPair pj = idplist.get(j);
		//if ( ((pi.pid != pj.pid) ||(pi.tid != pj.tid)) && (!mr) ) continue;
		if ( ((pi.pid != pj.pid) ||(pi.tid != pj.tid))) continue;
		//System.out.println("Loop for "+ i +" & "+ j);
		int sendi = msender.get(i);
		int sendj = msender.get(j);
		int exiti = mexit.get(i);
		int exitj = mexit.get(j);
		if ((sendi == -1) ||(sendj == -1)|| (i==j)) continue;
		//System.out.println("Loop for "+ i +" & "+ j);
		if (reachbitset.get(exiti).get(j) || reachbitset.get(exitj).get(i) ) continue;
//		System.out.println("Loop for "+ i +" & "+ j);
		if (mr){
                    Node nx = nList.get(i);
                    Node ny = nList.get(j);
                    Element ex = (Element) nx;
                    Element ey = (Element) ny;
                    Element esx = (Element) ex.getElementsByTagName("Stacks").item(0);
                    Element esy = (Element) ey.getElementsByTagName("Stacks").item(0);
                    Element sx = (Element) esx.getElementsByTagName("Stack").item(0);
                    Element sy = (Element) esy.getElementsByTagName("Stack").item(0);
                    String xclass = sx.getElementsByTagName("Class").item(0).getTextContent();
                    String yclass = sy.getElementsByTagName("Class").item(0).getTextContent();
                    if (!xclass.equals(yclass)) continue;
		    if (reachbitset.get(sendi).get(sendj)){
		    	addedge(exiti,j);
		    	change++;mc++;medgesum++;
		    }
            if (reachbitset.get(sendj).get(sendi)){
                addedge(exitj,i);
                change++;mc++;medgesum++;
            }
		}
		else{
		    if (reachbitset.get(sendi).get(sendj)){
                addedge(exiti,j);
                change++;mc++;medgesum++;
            }
            if (reachbitset.get(sendj).get(sendi)){
                addedge(exitj,i);
                change++;mc++;medgesum++;
            }
		}
	}
	System.out.println("msg change = "+mc + "; event change = "+ec);
        if (change > 0) return true;
        else     return  false;
    }
    
    public void computereachbitset() {
        flag = new int[nList.size()];
        for ( int i = 0 ; i < nList.size(); i++ ) {
            reachbitset.get(i).clear();
            flag[i] = 1;
        }
        BitSet bs = new BitSet(nList.size());
        bs.clear();
        //Node node = nList.get(4743);
        //Element e = (Element) node;
        //System.out.println("Cycle 4743 := "+ e.getElementsByTagName("OPTY").item(0).getTextContent()
        //        + e.getElementsByTagName("OPVAL").item(0).getTextContent());
        for (int i: root) {  //JX - from unheaded nodes??
            play(i,0);       //JX - I think it's DFS from all unheaded nodes
            bs.or(reachbitset.get(i));
            //System.out.println(i+" is finished");
        }
        boolean f = true;
        for (int i = 0 ; i < nList.size(); i++)
            f = f & bs.get(i);
        if (f) System.out.println("All vertices are covered");
        else System.out.println("Not all vertices are covered");
    }
    
    public void play(int x, int dep) {
        if (flag[x] == 0) return;
        flag[x] = 0;
        ArrayList<Pair> list = edge.get(x);
        //if (list.size() >1)
        //System.out.println(x + " : deep = "+dep+ " e="+list.size());
        for (Pair pair : list){
            play(pair.destination,dep+1);
            reachbitset.get(x).or(reachbitset.get(pair.destination));
        }
        reachbitset.get(x).set(x);
        //flag[x] = 0;
        //System.out.println(x + " is passed");
    }
    
    public void addspecialedges(String fin){
	try{
	    BufferedReader bf = new BufferedReader(new FileReader(new File(fin)));
	    String st;
	    while ((st = bf.readLine()) !=null){
		String [] sts = st.split(" ");
                addedge(Integer.parseInt(sts[0]),Integer.parseInt(sts[1]));
		System.out.println("Special edge : "+ sts[0] + " -> "+ sts[1]);
	    }
	} catch (Exception e){
	    e.printStackTrace();
            System.out.println("Special edges file error");
	}
	return ;
   } 
}
