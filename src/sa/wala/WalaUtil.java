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

	
	/**
	 * Eg, change "Lorg/apache/hadoop/mapreduce/Mapper$Context" to "org.apache.hadoop.mapreduce.Mapper$Context"
	 */
	public static String formatClassName(String className) {
		String formalClassName = className;
		if (formalClassName.startsWith("L"))
			formalClassName = formalClassName.substring(1);
		formalClassName = formalClassName.replaceAll("/", ".");
		return formalClassName;
	}
	
}
