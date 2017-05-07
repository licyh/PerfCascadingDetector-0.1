package dt.da.loop;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;


public class LoopInfo {

	int count = 0;      // the number of instances
	int tccount = 0;    // the number of instances that contain RPCs or IOs
	List<Node> tcs = new ArrayList<Node>();     // time-consuming operations, ie RPCs or IOs
	
	public LoopInfo() {
		
	}
}





////////////////////////////////////////////////////////////////////////////////////////
//Inner Classes
////////////////////////////////////////////////////////////////////////////////////////

/*
* For a Loop that may have many instances, to record the Loop's Time-consuming info, including
* 		1. the count of loop instances
* 		2. TC operations like RPC, IO for each instance
*/