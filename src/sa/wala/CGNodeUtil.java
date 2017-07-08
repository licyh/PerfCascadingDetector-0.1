package sa.wala;

import com.ibm.wala.ipa.callgraph.CGNode;



public class CGNodeUtil {
	
	
	public static String getMethodName(CGNode cgNode) {
		// this is JX-standard ~
		int index = cgNode.getMethod().getSignature().indexOf('(');
		return cgNode.getMethod().getSignature().substring(0, index);
	}
	
	public static String getMethodShortName(CGNode cgNode) {
		return cgNode.getMethod().getName().toString();
	}
	
}
