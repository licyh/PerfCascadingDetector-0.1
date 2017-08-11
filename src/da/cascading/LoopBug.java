package da.cascading;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;


public class LoopBug { 
	
	int nodeIndex;
	int cascadingLevel;
	ArrayList<Integer> cascadingChain;
	
	LoopBug(int nodeIndex) {
		this.nodeIndex = nodeIndex;
		this.cascadingLevel = 1;
		this.cascadingChain = new ArrayList<Integer>();
	}
	
	LoopBug(int nodeIndex, int cascadingLevel) {
		this.nodeIndex = nodeIndex;
		this.cascadingLevel = cascadingLevel;
		this.cascadingChain = new ArrayList<Integer>();
	}
	
	
	//useless now
	@Override
	public int hashCode() {
		int result = 17;
		//result = 31 * result + nodeIndex;
		//result = 31 * result + cascadingLevel;
		//result = 31 * result + cascadingChain.hashCode();         //this one has some problem!!!   or maybe the below equals' problem
		//return result;
		return Objects.hash( nodeIndex, cascadingLevel, cascadingChain );
	}
	
	//useless now
	@Override
	public boolean equals(Object o) {
		if ( this == o )
			return true;
		if ( o == null || this.getClass() != o.getClass() )
			return false;
		LoopBug other = (LoopBug) o;
		return nodeIndex == other.nodeIndex
				&& cascadingLevel == other.cascadingLevel
				&& ( cascadingChain == other.cascadingChain || (cascadingChain!=null && cascadingChain.equals(other.cascadingChain)) );
				
	}
	
	@Override
	public String toString() {
		//String str = "BugLoop - cascadingLevel=" + cascadingLevel + " - " + hbg.lastCallstack(nodeIndex);
		String str = "BugLoop - cascadingLevel=" + cascadingLevel + " - lasCasllstck....";
		return str;
	}

}
