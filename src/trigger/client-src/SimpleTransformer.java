import java.security.*;
import java.lang.instrument.*;
import java.util.*;
import javassist.*;
import java.io.*;

public class SimpleTransformer implements ClassFileTransformer {
    
    ArrayList<String> cname;
    ArrayList<String> mname;
    ArrayList<String>  lnum;  
    ArrayList<Integer> flag;
    String inst;
    public SimpleTransformer(String strtar){
	super();
	//CtClass.debugDump = "/home/hadoop/hadoop/dump";
	cname = new ArrayList<String>();
	mname = new ArrayList<String>();
	lnum  = new ArrayList<String>();
	flag = new ArrayList<Integer>();
	Properties configprop = new Properties();
	try {
		System.out.println("Loading the config information from "+strtar);
		FileInputStream configinputstream = new FileInputStream(strtar);
	        configprop.load(configinputstream);
            	configinputstream.close();
		String locations = configprop.getProperty("locations");
		String instruction = configprop.getProperty("instruction");
		FileInputStream flo = new FileInputStream(locations);
 
		BufferedReader br = new BufferedReader(new InputStreamReader(flo));
		String line;
 		while ((line = br.readLine()) != null){
			String [] str = line.split(" ");
			cname.add(str[0]);
			mname.add(str[1]);
			lnum.add( str[2]);
			flag.add( 1);
		}
		//String line1 = br.readLine();
		//String line2 = br.readLine();
		br.close();
		System.out.println("Insti File : "+instruction);
                FileInputStream fis = new FileInputStream(instruction);
		
                br = new BufferedReader(new InputStreamReader(fis));
		inst = "";
		while ((line = br.readLine()) != null){
			inst = inst + line;
	//		System.out.println("Inst: "+line);
                }

		/*
		String [] l1 = line1.split(" ");
		String [] l2 = line2.split(" ");
		cname = new String[2];
		mname = new String[2];
		lnum  = new String[2];
		cname[0] = l1[0]; cname[1] = l2[0];
		mname[0] = l1[1]; mname[1] = l2[1];
		lnum[0]  = l1[2]; lnum[1]  = l2[2];
		f1 = f0 = 1;
		*/
	} catch (Exception e){
		System.out.println("configure file load error");
	}
	System.out.println(cname.size() + " locations are loaded");
	System.out.println("cname = "+ cname);
	System.out.println("mname = "+ mname);
	System.out.println("lnum =  "+ lnum );
	System.out.println("instruction = "+inst);
	}

    public byte[] transform (ClassLoader loader, String className, Class redefiningClass, ProtectionDomain domain, byte[] bytes) throws IllegalClassFormatException {
//            System.out.println("Loading " + className +" from "+loader.toString());
	    return transformClass(redefiningClass, bytes);
    }
    
    private byte[] transformClass (Class classToTransform, byte[] b){
    
    ClassPool pool = ClassPool.getDefault();
    CtClass cl = null;
        try{
          cl = pool.makeClass(new java.io.ByteArrayInputStream(b));	
          CtBehavior[] methods = cl.getDeclaredBehaviors();
	  for (int k = 0; k< cname.size(); k++){
	  	if (cl.getName().equals(cname.get(k))){
			pool.importPackage("edu.uchicago.sg.dcbt.BlockingClient");
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(mname.get(k))){
					if (flag.get(k)>0){
						changeMethod(methods[i],Integer.parseInt(lnum.get(k)));
					   	System.out.println("location "+k + " is found.");
						flag.set(k,0);
					} else {
					   	System.out.println("location "+k + " is found multi-instances.");
					}
				}
			}
		  }
	  }
	/*
	  if (cl.getName().equals(cname[1])){
		pool.importPackage("edu.uchicago.sg.dcbt.BlockingClient");
                for (int i = 0; i < methods.length; i++) {
                        if (methods[i].getName().equals(mname[1])&& (f1>0)){
                                changeMethod(methods[i],Integer.parseInt(lnum[1]));
                                f1 = 0;
                        }
                }
          }*/
	  //out.flush();
          b = cl.toBytecode();
        }
        catch (Exception e) {
            e.printStackTrace();
            }
        finally {
            if (cl != null) {
                cl.detach();
            }
        }
        return b;    
    
    }
    
    private void changeMethod(CtBehavior method, int l) throws NotFoundException, CannotCompileException {
        	method.insertAt(l, true, inst);
        //	method.insertAt(l, true, "BlockingClient blockingClient = new BlockingClient(\"222\");");
        //	method.insertAt(l, true, "System.out.println(\"This Message is from JSsist\");");

	 }
    

}
