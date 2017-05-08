package dt.da;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dt.da.loop.LoopInfo;
import dt.da.loop.LoopInstance;
import dt.da.xml.XMLNodeList;
import dt.da.xml.XMLUtil;

public class LoopAnalyzer {
	
	String xmlDir;    
	XMLNodeList nodelist;
	List<LoopInstance> instances = new ArrayList<LoopInstance>();
	TreeMap<String, LoopInfo> loops = new TreeMap<String, LoopInfo>();
	
	
	public LoopAnalyzer(String xmlDir) {
		this.xmlDir = xmlDir;
		this.nodelist = new XMLNodeList();
        //doWork();
	}
	
	public void doWork() {
		// read xml log files
		scanXMLDir();
		analyzeLoops();
	}

	public void scanXMLDir() {
        // Get all threads' log files
        File[] xmlfiles = new File( xmlDir ).listFiles();
        if (xmlfiles == null) {
        	System.out.println("JX - ERROR - None of log files to handle");
        	return;
        }
        	
        // Traverse all xml files
        for (File xmlfile: xmlfiles) {
        	Document doc = XMLUtil.readXMLFile(xmlfile);
            NodeList nodes = doc.getElementsByTagName("Operation");   //get all operations/nodes at a thread
        	for (int i = 0; i < nodes.getLength(); i++) {
        		Node node = nodes.item(i);        					 //get a node/operation
        		if (node.getNodeType() == Node.ELEMENT_NODE) {
        			nodelist.add(node);                   				     //JX - Node, will be <OPINFO>/<Operation> at 'base' file 
        		}
        	}
        }//outer-for
        System.out.println("JX - INFO - " + nodelist.toString());
	}
	
	
	public void analyzeLoops() {
		getLoopInstances();
		getLoopInfos();
		analyzeLoopInfos();
	}
	
	
	public void getLoopInstances() {
		
		String prevPidtid = "xxx";
		LinkedList<Integer> stack = new LinkedList<Integer>();  //tmp
		
		// Get all loop(begin, end)
		for (int i = 0; i < nodelist.size(); i++) {
			String opty = nodelist.getNodeOPTY(i);
		
			// check if a new file
			String pidtid = nodelist.getNodePIDTID(i);
			if (!pidtid.equals(prevPidtid)) {
				if (!stack.isEmpty()) {
					System.out.println("JX - WARN - !stack.isEmpty(), that should be the beginning of the thread without exiting correctly"
							+ ", maybe all of them are 'while(xx){ wait; }', I varified the most");
					/*
					System.out.println("JX - DEBUG - i=" + i + " stack.num=" + stack.size() );
					for (int k = 0; k < stack.size(); k++)
						System.out.println("JX - DEBUG - stack(" + k + "): " + nodelist.getNodeOPVAL(stack.get(k)) );
					*/
                    stack.clear();    		//better, but actually no need
				}
                prevPidtid = pidtid;
			}
			
			if (opty.equals("LoopBegin")) {
				stack.push(i);
				//System.out.println("JX - DEBUG - add: " + nodelist.getNodeOPVAL(i));
			}
			else if (opty.equals("LoopEnd")) {
				//System.out.println("JX - DEBUG - end: " + nodelist.getNodeOPVAL(i));
				int beginIndex = stack.pop();  //LoopBegin
				int endIndex = i;              //LoopEnd
				if ( nodelist.getNodeOPVAL(beginIndex).equals( nodelist.getNodeOPVAL012(endIndex) )) {
					String identity = nodelist.getNodeOPVAL(beginIndex);
					//System.out.println("JX - DEBUG - *" + nodelist.getNodeOPVAL(endIndex) + "*" );
					//System.out.println("JX - DEBUG - *" + nodelist.getNodeOPVAL3(endIndex) + "*" );
					int iterations = Integer.parseInt( nodelist.getNodeOPVALn(endIndex) );
					LoopInstance instance = new LoopInstance(identity, beginIndex, endIndex, iterations);
					instances.add(instance);
				}
				else {
					System.out.println("JX - ERROR - should not happen: loop begin NOT MATCHES loop end");
				}
			}
		}
		
		System.out.println("JX - INFO - num of loop instances = " + instances.size());
		
	}
	
	
	public void getLoopInfos() {
		for (LoopInstance instance: instances) {
			String identity = instance.getIdentity();
			if (!loops.containsKey(identity)) {
				LoopInfo loop = new LoopInfo(identity);
				loops.put(identity, loop);
			}
			LoopInfo loop = loops.get(identity);
			loop.add(instance);
		}
		System.out.println("JX - INFO - " + "num of loops(physical) = " + loops.size());
	}
	
	
	public void analyzeLoopInfos() {
		int count = 0;
		for (LoopInfo loop: loops.values()) {
			loop.doWork(nodelist);
			if (loop.getIoIdentityStrs().size() > 0) {
				count ++;
				System.out.println("IO/RPC Loop - " + loop);
				loop.printIoIdentityStrs();
			}
		}
		System.out.println("JX - INFO - " + "num of loops(physical) including IO/RPC = " + count);
	}
    
}
