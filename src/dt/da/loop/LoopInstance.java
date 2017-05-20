package dt.da.loop;

import com.xml.XMLNodeList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;


public class LoopInstance {
	String identity;
	int beginIndex;
	int endIndex;
	int iterations;
	
	int ioIterations = 0;                       //how many iterations containing IOs or RPCs
	List<Node> ios = new ArrayList<Node>();
	
	public LoopInstance(String identity, int beginIndex, int endIndex, int iterations) {
		this.identity = identity;
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
		this.iterations = iterations;
	}
	
	public String getIdentity() {
		return this.identity;
	}
	
	public void doWork(XMLNodeList nodelist) {
		int realIterations = 0;
		int iosForNewIteration = -1;
		for (int i = beginIndex; i <= endIndex; i++) {
			String opty = nodelist.getNodeOPTY(i);
			String opval = nodelist.getNodeOPVAL(i);
			if (opty.equals("LoopCenter") && opval.equals(identity)) {
				realIterations ++;
				if (iosForNewIteration > 0)
					ioIterations ++;
				iosForNewIteration = 0;
			}
			else if (opty.equals("IO") || opty.equals("RPC")) {
				ios.add( nodelist.get(i) );
				iosForNewIteration ++;
			}
		}
		if (realIterations != iterations) {
			System.out.println("JX - ERROR - realIterations != iterations, " + realIterations + " != " + iterations );
		}
	}
	
	public Set<String> getIoIdentityStrs(XMLNodeList nodelist) {
		Set<String> strs = new HashSet<String>();
		for (Node node: ios) {
			strs.add( nodelist.getNodeOPVAL(node) );
		}
		return strs;
	}
	
}
