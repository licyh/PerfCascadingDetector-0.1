package dt.spoon.checkers;

import java.util.ArrayList;
import java.util.List;

import com.text.TextFileReader;


public class RPCChecker implements Checker {

	TextFileReader reader;
	
	List<String> rpcClasses = new ArrayList<String>();
	List<String> rpcIfaces = new ArrayList<String>();
	List<String> rpcMethods = new ArrayList<String>();
	
	
	public RPCChecker(String filename) {
		this.reader = new TextFileReader( filename );
		this.reader.readFile();
		getRPCs();
	}
	
	public void getRPCs() {
		for (String[] strs: reader.splitstrs) {
			strs[0] = strs[0].substring(0, strs[0].indexOf('('));   //class.method
			strs[1] = strs[1].substring(0, strs[1].indexOf('('));   //iface.method
			rpcClasses.add( strs[0].substring(0, strs[0].lastIndexOf('.')) );
			rpcIfaces.add( strs[1].substring(0, strs[1].lastIndexOf('.')) );
			rpcMethods.add( strs[1].substring(strs[1].lastIndexOf('.')+1) );
		}
	}
	
	@Override
	public boolean isTarget(String sig) {
		String iface = sig.substring(0, sig.lastIndexOf('.'));
		String method = sig.substring(sig.lastIndexOf('.')+1);
		
		// java.lang.reflect.Method.invoke
		if (iface.equals("java.lang.reflect.Method") && method.equals("invoke"))
			return true;
		
		// common rpc
		for (int i = 0; i < rpcClasses.size(); i++) {
			if ( rpcIfaces.get(i).equals(iface) && rpcMethods.get(i).equals(method) )
				return true;
		}
		return false;
	}
	
}
