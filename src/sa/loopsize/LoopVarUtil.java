package sa.loopsize;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.viz.PDFViewUtil;

public final class LoopVarUtil {
	public static boolean isApplicationMethod(CGNode f) {
  	  	IMethod m = f.getMethod();
  	  	ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
  	  	if ( classloader_ref.equals(ClassLoaderReference.Application) )
  	  		return true;
  	  	return false;
    }
    
    public static boolean isNativeMethod(CGNode f) {
  	  	IMethod m = f.getMethod();
  	  	if ( m.isNative() )
  	  		return true;
  	  	return false;
    }

    public static boolean isPrimordialMethod(CGNode f) {
  	  	IMethod m = f.getMethod();
  	  	ClassLoaderReference classloader_ref = m.getDeclaringClass().getClassLoader().getReference();
  	  	if ( classloader_ref.equals(ClassLoaderReference.Primordial) )
  	  		return true;
  	  	return false;
    }
    
    public static int getSSAIndexBySSA(SSAInstruction[] ssaSet, SSAInstruction ssa) {
        int index = -1;
        for (int i=0; i < ssaSet.length; i++)
          if (ssaSet[i] != null)
            if (ssaSet[i].equals(ssa)) { 
              index = i; 
              break; 
            }
        return index;
    }
    
    public static int getSourceLineNumberFromSSA(SSAInstruction ssa,IR methodIR) {
        IBytecodeMethod bytecodemethod = (IBytecodeMethod) methodIR.getMethod();
        SSAInstruction[] ssaSet = methodIR.getInstructions();
    	int index = getSSAIndexBySSA(ssaSet, ssa); 
        if (index != -1) {
  		try {
  			int bytecodeindex = bytecodemethod.getBytecodeIndex( index );
  			int sourcelinenum = bytecodemethod.getLineNumber( bytecodeindex );
              if (sourcelinenum != -1) 
              	  return sourcelinenum;
  		} catch (InvalidClassFileException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
        }
  	  return -1;
    }
    
    public static void printSSAInfo(String location, int lineNum){
    	System.out.println("the location of target instruction: " + " " + location + ":" + Integer.toString(lineNum));
    }
    
	public static void generateViewIR(IR ir, ClassHierarchy cha) throws WalaException {
		
	    final String dotExe = "/usr/bin/dot";
	    final String pdfExe = "/usr/bin/evince";
	    final String dotFile = "/home/nemo/outFig/temp.dt";
	    final String pdfFile = "/home/nemo/outFig/ir.pdf";
	    
	    /**
	     * JX: it seems "viewIR" is not suitable for some functions like "LeaseManager.checkLeases", 
	     * its Exception: failed to find <Application,Lorg/apache/hadoop/fs/UnresolvedLinkException>
	     */
	    
	    // Print IR's basic blocks and SSA instructions.    //JX: good, it includes variable names 
	    System.err.println(ir.toString());  
	    
	    // Preparing
	    Properties wp = null;
	    try {
	      wp = WalaProperties.loadProperties();
	      wp.putAll(WalaExamplesProperties.loadProperties());
	    } catch (WalaException e) {
	      e.printStackTrace();
	      Assertions.UNREACHABLE();
	    }
	    //String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
	    //String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
	    //String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
	    //String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);
	    if ( !Files.exists( Paths.get(dotExe)) ) 
	    	System.out.println("JX - ERROR - the software location of 'dot' is wrong");
	    if ( !Files.exists( Paths.get(pdfExe)) )
	    	System.out.println("JX - ERROR - the software location of 'pdfviewer' is wrong");
	   
	    // Generate IR ControlFlowGraph's SWT viewer
	    //SSACFG cfg = ir.getControlFlowGraph();
	    
	    // Generate IR PDF viewer
	    PDFViewUtil.ghostviewIR(cha, ir, pdfFile, dotFile, dotExe, pdfExe); //that is, psFile, dotFile, dotExe, gvExe, originally
	}	 
}
