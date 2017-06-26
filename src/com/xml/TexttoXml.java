package com.xml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by guangpu on 4/6/16.
 */
public class TexttoXml {

    public static void main (String[] argv){

    System.out.println("JX - INFO - TexttoXml begin ...");
	// argv[1] [2] = null? Event? Msg? => EmptyNode
	if (argv.length < 3) {
           System.out.println("Too less argv:dir n/m/e n/m/e ");
	   return ; 
	}
        File[] textfiles = new File(argv[0]).listFiles();
        Path tardir = Paths.get(argv[0]+"-xml");
        if (!Files.exists(tardir))
            try {
                Files.createDirectories(tardir);
            }catch (Exception e){
                System.out.println("Cannot create " + tardir);
            }
        for(File text : textfiles){
            String textname = text.getName();
            String [] xmlname = textname.split("-");
            BufferedReader buffer;
            String line;
            DocumentBuilderFactory documentBuilderFactory;
            DocumentBuilder documentBuilder;
            Document doc ;
            try {
                documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilder = documentBuilderFactory.newDocumentBuilder();
                doc = documentBuilder.newDocument();
            }catch (ParserConfigurationException e){
                System.out.println("Parser Engine for xml failed");
                return ;
            }
            Element root = doc.createElement("Operations");
            doc.appendChild(root);
            Element element = null;
            try{
                buffer = new BufferedReader(new FileReader(text));
                int depth = 0;
                int stacklen = 0;
                Element stacks = null;
                while ((line = buffer.readLine()) != null){
                    //System.out.println("parsering "+line);
                    String [] item = line.split(" ");
                    depth ++;
                    if (item.length >3) {
                        if (element != null) {
                            if (stacklen > 0) {
                                Attr attr = doc.createAttribute("Len");
                                attr.setValue(Integer.toString(stacklen));
                                stacks.setAttributeNode(attr);
                                element.appendChild(stacks);
                            }
                            stacklen = 0;
                            root.appendChild(element);
                            // jx: commented by JX
                            //System.out.println(element + " added");
                        }
                        element = doc.createElement("Operation");
                        Element tid = doc.createElement("TID");
                        tid.appendChild(doc.createTextNode(item[0]));
                        Element pid = doc.createElement("PID");
                        pid.appendChild(doc.createTextNode(item[1]));
                        Element typeop = doc.createElement("OPTY");
			if (item[2].contains(argv[1])||item[2].contains(argv[2]))	
                            typeop.appendChild(doc.createTextNode("EmptyNode"));
			else
                            typeop.appendChild(doc.createTextNode(item[2]));
                        Element opval = doc.createElement("OPVAL");
			String opvalue = item[3];
			String mdval = "";
			String [] opvalues = opvalue.split("/");
			if ((opvalues.length > 2)&& (item[2].equals("HeapRead")||item[2].equals("HeapWrite"))){
			    if (opvalues[1].equals("hbase")){
			        for (int i = 1; i < opvalues.length; i++)
				    mdval = mdval + opvalues[i];
                        	    opval.appendChild(doc.createTextNode(mdval));
				}
			}else{
                            opval.appendChild(doc.createTextNode(item[3]));
			}
			
                        //Element localtime = doc.createElement("LTIME");
                        //localtime.appendChild(doc.createTextNode(item[4]));
                        element.appendChild(pid);
                        element.appendChild(tid);
                        element.appendChild(typeop);
                        element.appendChild(opval);
                        //element.appendChild(localtime);
                        stacks = doc.createElement("Stacks");
                    }else{
                        if (item.length < 3) {
                            System.out.println(xmlname+ " : "+ depth +" format error");
                            break;
                        }
                        Element stack = doc.createElement("Stack");
                        Element classname = doc.createElement("Class");
                        classname.appendChild(doc.createTextNode(item[0]));
                        Element methodname = doc.createElement("Method");
                        methodname.appendChild(doc.createTextNode(item[1]));
                        Element linenum = doc.createElement("Line");
                        linenum.appendChild(doc.createTextNode(item[2]));
                        Attr attr = doc.createAttribute("id");
                        attr.setValue(Integer.toString(stacklen));
                        stack.setAttributeNode(attr);
                        stack.appendChild(classname);
                        stack.appendChild(methodname);
                        stack.appendChild(linenum);
                        stacks.appendChild(stack);
                        stacklen++;
                    }

                }
                if (element != null) {
                    if (stacklen > 0) {
                        Attr attr = doc.createAttribute("Len");
                        attr.setValue(Integer.toString(stacklen));
                        stacks.setAttributeNode(attr);
                        element.appendChild(stacks);
                    }
                    root.appendChild(element);
                    // jx: commented by JX
                    //System.out.println(element + " added");
                }
            }catch (Exception e){
		e.printStackTrace();
                System.out.println(textname + " is not found in the directory");
                return ;
            }




            try {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                File wf = new File(tardir + "/" + textname);
                if (!wf.exists())
                    wf.createNewFile();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                StreamResult result = new StreamResult(wf);
                transformer.transform(source, result);
            } catch (Exception e){
                e.printStackTrace();
                System.out.println(tardir + "/" + textname+" cannot be written");
            }

        }


    }
}
