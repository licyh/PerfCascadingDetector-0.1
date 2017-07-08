package sa.lockloop;

import java.util.BitSet;
import java.util.HashMap;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;



/**
 * this is the object for containing all of results
 */
public class CGNodeList extends HashMap<Integer, CGNodeInfo> {

	CallGraph cg;
	/** used for each work that needs, so it maybe need to clear before every use */
	BitSet traversedNodes;
	
	
		
	
	public CGNodeList(CallGraph cg) {
		this.cg = cg;
		// others
		this.traversedNodes = new BitSet();
		this.traversedNodes.clear();
	}
	
	
	public BitSet getTraversedNodes() {
		return this.traversedNodes;
	}
	

	
	public CGNodeInfo forceGet(int cgNodeId) {
		if ( !this.containsKey(cgNodeId) )
			this.put(cgNodeId, new CGNodeInfo(cg.getNode(cgNodeId)) );		
		return this.get(cgNodeId);
	}
	
	
	public CGNodeInfo forceGet(CGNode cgNode) {
		int cgNodeId = cgNode.getGraphNodeId();
		return forceGet(cgNodeId);
	}
	
	
	
	
	

	

	@Override
	public String toString() {
		String str = "CGNodeList: " + "#nodes=" + this.size();
		return str;
	}
	
	
	
}
