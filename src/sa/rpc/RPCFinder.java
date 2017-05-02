package sa.rpc;

import java.nio.file.Path;
import java.nio.file.Paths;

import sa.wala.WalaAnalyzer;

public class RPCFinder {

	Path dirpath;
	String systemName = "MapReduce";   //by default;
	
	WalaAnalyzer walaAnalyzer;
	
	
	public static void main(String[] args) {
		
		new RPCFinder( args[0] );
	}
	
	public RPCFinder(String jarDir) {
		this.dirpath = Paths.get(jarDir);
		this.walaAnalyzer = new WalaAnalyzer( this.dirpath );
		doWork();
	}
	
	public void doWork() {
		findRPCs();
		
	}
		
	public void findRPCs() {
		System.out.println("JX - INFO - findRPCs...");
		
		switch ( systemName ) {
			case "MapReduce":
				MRrpc mrrpc = new MRrpc(walaAnalyzer.cha, dirpath.toString());
				mrrpc.doWork();
				HDrpc hdrpc2 = new HDrpc(walaAnalyzer.cha, dirpath.toString());  
				hdrpc2.doWork();
				break;
			case "HDFS":
				HDrpc hdrpc = new HDrpc(walaAnalyzer.cha, dirpath.toString());  
				hdrpc.doWork();
				break;
			case "HBase":
				break;
			default:
				break;
		}
	}
	
	
}
