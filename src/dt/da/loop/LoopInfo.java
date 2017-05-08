package dt.da.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Node;

import dt.da.xml.XMLNodeList;


public class LoopInfo {

	String identity;
	int count = 0;      // the number of instances
	List<LoopInstance> instances = new ArrayList<LoopInstance>();
	
	Set<String> ioIdentityStrs = new TreeSet<String>();   //IOs or RPCs
	
	public LoopInfo(String identity) {
		this.identity = identity;
	}
	
	public String toString() {
		return  this.identity;
	}
	
	public void add(LoopInstance instance) {
		this.count ++;
		this.instances.add(instance);
	}
	
	public void doWork(XMLNodeList nodelist) {
		for (LoopInstance instance: instances) {
			instance.doWork(nodelist);
			ioIdentityStrs.addAll( instance.getIoIdentityStrs(nodelist) );
		}
	}
	
	public Set<String> getIoIdentityStrs() {
		return this.ioIdentityStrs;
	}
	
	public void printIoIdentityStrs() {
		System.out.println("#ioIdentityStrs=" + ioIdentityStrs.size());
		int index = 0;
		for (String str: ioIdentityStrs)
			System.out.println(++index + ": " + str);
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