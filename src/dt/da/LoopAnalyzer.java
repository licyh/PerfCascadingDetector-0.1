package dt.da;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dt.da.loop.LoopInstance;
import dt.da.xml.XMLNodeList;
import dt.da.xml.XMLUtil;

public class LoopAnalyzer {
	
	String xmlDir;    
	XMLNodeList nodelist;
	
	
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
	}
	
	
	LinkedList<Integer> stack = new LinkedList<Integer>();
	
	public void analyzeLoops() {
		
		String prevPidtid = "xxx";
		
		// Get all loop(begin, end)
		for (int i = 0; i < nodelist.size(); i++) {
			String opty = nodelist.getNodeOPTY(i);
		
			// check if a new file
			String pidtid = nodelist.getNodePIDTID(i);
			if (!pidtid.equals(prevPidtid)) {
				if (!stack.isEmpty()) {
					System.out.println("JX - ERROR/WARN - !stack.isEmpty()");
                    stack.clear();
					//return;
				}
                prevPidtid = pidtid;
			}
			
			if (opty.equals("LoopBegin")) {
				stack.push(i);
				System.out.println("JX - DEBUG - add: " + nodelist.getNodeOPVAL(i));
			}
			else if (opty.equals("LoopEnd")) {
				System.out.println("JX - DEBUG - end: " + nodelist.getNodeOPVAL(i));
				LoopInstance instance = new LoopInstance();
				int beginIndex = stack.pop();
				int endIndex = i;
				
				if ( nodelist.getNodeOPVAL(beginIndex).equals( nodelist.getNodeOPVAL012(endIndex) )) {
					instance.identity = nodelist.getNodeOPVAL(beginIndex);
					instance.beginIndex = beginIndex;
					instance.endIndex = endIndex;
				}
				else {
					System.out.println("JX - ERROR - loop begin NOT MATCHES loop end");
				}
			}
			else {
				
			}
		}
		
		
	}
	
    
}
