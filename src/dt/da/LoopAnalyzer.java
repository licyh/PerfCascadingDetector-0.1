package dt.da;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LoopAnalyzer {
	
	String xmlDir;    
	ArrayList<Node> nList;                            //node list of all the file, JX - each node is 'Node' in XML doc
	
	
	public LoopAnalyzer(String xmlDir) {
		this.xmlDir = xmlDir;
        this.nList = new ArrayList<Node>();
	}
	
	public void doWork() {
		// read xml log files
		readWholeXMLDir();
		// scan loops
		scan();
	}

	public void readWholeXMLDir() {
        // Get all threads' log files
        File [] xmlfiles = new File( xmlDir ).listFiles();
        if (xmlfiles == null) {
        	System.out.println("JX - ERROR - None of log files to handle");
        	return;
        }
        
        Document inputdoc = null;				      // xml input variable	
        // JX - traverse all thread files
        for (File xml : xmlfiles) {
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
            NodeList NList = inputdoc.getElementsByTagName("Operation");   //get all operations/nodes at a thread
            
        	// JX - deal with a single thread file's operations
        	for (int i = 0; i < NList.getLength(); i++) {
        		Node nNode = NList.item(i);        //get a node/operation
        		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
        			nList.add(nNode);                   //JX - Node, will be <OPINFO>/<Operation> at 'base' file 
        		}
        	}
        }//outer-for
	}
	
	
	// jx: node signature -> xx
	// PUT "72 23638 LoopCenter org.apache.hadoop.yarn.server.api.records.impl.pb.NodeStatusPBImpl.initContainers()V_loop0" -> ++count
	// REMOVE when meeting "72 23638 LoopPrint org.apache.hadoop.yarn.server.api.records.impl.pb.NodeStatusPBImpl.initContainers()V_loop0_11"
	Map<String, LoopTCInfo> loopNodes = new HashMap<String, LoopTCInfo>();
	
	public void scan() {
		
		for (int i = 0; i < nList.size(); i++) {
			String opty = getNodeOPTY(i);
			
			LoopTCInfo looptci = null;
			// LoopCenter
			if ( opty.equals("LoopCenter") ) {
				String sig = getNodeSignature(i);
				if ( !loopNodes.containsKey(sig) ) {
					LoopTCInfo looptci = new LoopTCInfo();
					loopNodes.put(sig, looptci);
				}
				looptci = loopNodes.get( sig );
				looptci.count ++;
				looptci
				
			}
			// MsgSending
			else if ( opty.equals("MsgSending") || opty.equals("IO") ) {
				
			}
			// LoopPrint
			else if ( opty.equals("LoopPrint") ) {
				
			}
			
		}
		
	}
	
	
	
	
	
	/////////////////////////////////////////////////////////////////////////////////
	// Basic Methods
	/////////////////////////////////////////////////////////////////////////////////
	
	public String getNodeSignature(int index) {
		String signature = getNodeOPTY(index) 
				+ getNodePID(index) 
				+ getNodeTID(index) 
				+ getNodeOPVAL(index);
		return signature;
	}
	
	/*
	 * Node architecture:
	 * TID  PID  OPTY OPVAL
	 * CALLSTACKS
	 * 
	 * PS - a log looks like
	 *******************************************************************************************************************
	 * 72 23638 LoopPrint org.apache.hadoop.yarn.server.api.records.impl.pb.NodeStatusPBImpl.initContainers()V_loop0_11                                                                                                          
	 * org.apache.hadoop.yarn.server.api.records.impl.pb.NodeStatusPBImpl initContainers 244
	 * org.apache.hadoop.yarn.server.api.records.impl.pb.NodeStatusPBImpl getContainersStatuses 205
	 * ...
	 * org.apache.hadoop.ipc.Server$Handler run 1522
	 ******************************************************************************************************************* 
	 */
	
	
    public String getNodeOPTY(int index) {
    	Node node = nList.get( index );
    	Element e = (Element) node;
    	String opty = e.getElementsByTagName("OPTY").item(0).getTextContent();
    	return opty;
    }
    
    public String getNodePID(int index) {
    	Node node = nList.get( index );
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("PID").item(0).getTextContent();
    	return pid;
    }
    
    public String getNodeTID(int index) {
    	Node node = nList.get( index );
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("TID").item(0).getTextContent();
    	return pid;
    }

    public String getNodeOPVAL(int index) {
    	Node node = nList.get( index );
    	Element e = (Element) node;
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval;
    }

    public String getNodePIDTID(int index) {
    	Node node = nList.get( index );
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("PID").item(0).getTextContent();
    	String tid = e.getElementsByTagName("TID").item(0).getTextContent();
    	return pid+tid;
    }
    
    // return "PID"+"OPVAL0" for 'lock' nodes, especially for r/w locks
    public String getNodePIDOPVAL0(int index) {   
    	Node node = nList.get( index );
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("PID").item(0).getTextContent();
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return pid + opval.split("_")[0]; 
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////////////////
    // Inner Classes
    ////////////////////////////////////////////////////////////////////////////////////////
    
    /*
     * For a Loop that may have many instances, to record the Loop's Time-consuming info, including
     * 		1. the count of loop instances
     * 		2. TC operations like RPC, IO for each instance
     */
    class LoopTCInfo {
    	int count = 0;      // the number of instances
    	int tccount = 0;    // the number of instances that contain RPCs or IOs
    	List<Node> tcs = new ArrayList<Node>();     // time-consuming operations, ie RPCs or IOs
    	
    	public LoopTCInfo() {
    		
    	}
    }
    
}
