package sa.wala;

import java.util.Iterator;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

public class IRUtil {

	  public static int getSSAIndexBySSA(SSAInstruction[] ssas, SSAInstruction ssa) {
		    int index = -1;
		    for (int i=0; i < ssas.length; i++)
		      if (ssas[i] != null)
		        if (ssas[i].equals(ssa)) { 
		          index = i; 
		          break; 
		        }
		    return index;
		  }
	  
	  public static int getSourceLineNumberFromSSA(SSAInstruction ssa, SSAInstruction[] ssas, IBytecodeMethod bytecodemethod) {
	      int index = getSSAIndexBySSA(ssas, ssa); 
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
	  
	  
		public static int getSourceLineNumberFromBB(ISSABasicBlock bb, SSAInstruction[] ssas, IBytecodeMethod bytecodemethod) {
		      for (Iterator<SSAInstruction> it = bb.iterator(); it.hasNext(); ) {
		          SSAInstruction ssa = it.next();
		          int index = getSSAIndexBySSA(ssas, ssa); 
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
		      } 
			  return -1;
		}
	  
	  
}
