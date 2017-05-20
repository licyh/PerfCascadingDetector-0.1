package com.xml;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLNodeList extends ArrayList {

	ArrayList<Node> nodelist;
	
	
	public XMLNodeList() {
		this.nodelist = new ArrayList<Node>();
	}
	
	@Override
	public int size() {
		return nodelist.size();
	}
	
	public void add(Node node) {
		nodelist.add(node);
	}
	
	@Override
	public Node get(int index) {
		return nodelist.get(index);
	}
	
	@Override
	public String toString() {
		String str = "XMLNodeList: " + "#nodes=" + nodelist.size();
		return str;
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
	
    public String getNodePID(int index) {
    	return getNodePID( nodelist.get(index) );
    }
    public String getNodePID(Node node) {
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("PID").item(0).getTextContent();
    	return pid;
    }
    
    
    public String getNodeTID(int index) {
    	return getNodeTID( nodelist.get(index) );
    }
    public String getNodeTID(Node node) {
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("TID").item(0).getTextContent();
    	return pid;
    }
    
    
    public String getNodeOPTY(int index) {
    	return getNodeOPTY( nodelist.get(index) );
    }
	public String getNodeOPTY(Node node) {
    	Element e = (Element) node;
    	String opty = e.getElementsByTagName("OPTY").item(0).getTextContent();
    	return opty;
    }
   
	
    public String getNodeOPVAL(int index) {
    	return getNodeOPVAL( nodelist.get(index) );
    }
    public String getNodeOPVAL(Node node) {
    	Element e = (Element) node;
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval;
    }
    
    
    // added
    public String getNodePIDTID(int index) {
    	return getNodePIDTID( nodelist.get(index) );
    }
    public String getNodePIDTID(Node node) {
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("PID").item(0).getTextContent();
    	String tid = e.getElementsByTagName("TID").item(0).getTextContent();
    	return pid+tid;
    }
    
    
    public String getNodeOPVAL0(int index) {   
    	return getNodeOPVAL0( nodelist.get(index) );
    }
    public String getNodeOPVAL0(Node node) {   
    	Element e = (Element) node;
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval.split("_")[0]; 
    }
    
    
    public String getNodeOPVAL1(int index) {   
    	return getNodeOPVAL1( nodelist.get(index) );
    }
    public String getNodeOPVAL1(Node node) {   
    	Element e = (Element) node;
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval.split("_")[1]; 
    }
    
    
    public String getNodeOPVAL2(int index) {   
    	return getNodeOPVAL2( nodelist.get(index) );
    }
    public String getNodeOPVAL2(Node node) {   
    	Element e = (Element) node;
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval.split("_")[2]; 
    }
    
    
    public String getNodeOPVAL3(int index) {   
    	return getNodeOPVAL3( nodelist.get(index) );
    }
    public String getNodeOPVAL3(Node node) {   
    	Element e = (Element) node;
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval.split("_")[3]; 
    }
   
    // "0n_1" means 0~n-1
    public String getNodeOPVAL0n_1(int index) {   
    	return getNodeOPVAL0n_1( nodelist.get(index) );
    }
    public String getNodeOPVAL0n_1(Node node) {   
    	Element e = (Element) node;
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval.substring(0, opval.lastIndexOf('_')); 
    }
    
    
    public String getNodeOPVALn(int index) {   
    	return getNodeOPVALn( nodelist.get(index) );
    }
    public String getNodeOPVALn(Node node) {   
    	Element e = (Element) node;   
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return opval.substring(opval.lastIndexOf('_')+1); 
    }
    
    
    // return "PID"+"OPVAL0" for 'lock' nodes, especially for r/w locks
    public String getNodePIDOPVAL0(Node node) {   
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("PID").item(0).getTextContent();
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return pid + opval.split("_")[0]; 
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
	
	

    
    // return "PID"+"OPVAL0" for 'lock' nodes, especially for r/w locks
    public String getNodePIDOPVAL0(int index) {   
    	Node node = nodelist.get( index );
    	Element e = (Element) node;
    	String pid = e.getElementsByTagName("PID").item(0).getTextContent();
    	String opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
    	return pid + opval.split("_")[0]; 
    }
    
}
