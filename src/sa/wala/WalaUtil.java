package sa.wala;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

public class WalaUtil {
	
	// useless useless useless useless NOW NOW NOW 
	

	public static String containMethod(IClass c, String methodSelector) {
	    for (IMethod im : c.getDeclaredMethods()) {
	      if ( im.getSelector().toString().equals(methodSelector) ) {
	    	return im.getSignature();
	      }
	    }
	    return null;
	}

	public static String format(String str) {
		String rt = str;
		if (rt.startsWith("L")) {
			rt = rt.substring(1);
		}
		rt = rt.replaceAll("/", ".");
		return rt;
	}
	
}
