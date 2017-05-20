package com.xml;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLUtil {

	
	public static Document readXMLFile(File xmlfile) {
		Document document = null;
    	try {
    		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    		DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(xmlfile);
            //System.out.println(document);
        } catch (Exception e) {
            System.out.println("JX - ERROR - XML file load error, graphbuilder construction failed");
            e.printStackTrace();
        }
    	document.getDocumentElement().normalize();
        return document;
	}
	
    
	
}
