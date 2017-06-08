package sa.rpc;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.benchmark.Benchmarks;
import com.ibm.wala.ipa.cha.ClassHierarchy;

import sa.wala.WalaAnalyzer;

public class RPCFinder {

	Path jarDirPath;
	String systemName;             //by default;
	
	WalaAnalyzer walaAnalyzer;
	
	
	/**
	 * @param args - dir of jars
	 */
	public static void main(String[] args) {		
		new RPCFinder( args[0] );
	}
	
	public RPCFinder(String jarDir) {
		this.jarDirPath = Paths.get(jarDir);
		this.systemName = Benchmarks.resolveSystem(jarDir);
		this.walaAnalyzer = new WalaAnalyzer( this.jarDirPath );
		doWork();
	}
	
	public void doWork() {
		System.out.println("JX - INFO - RPCFinder.doWork");
		findRPCs();
	}
	

		
	public void findRPCs() {
		ClassHierarchy cha = walaAnalyzer.getClassHierarchy();
		
		switch ( systemName ) {
			case Benchmarks.MR:
				MRrpc mrrpc = new MRrpc(cha, jarDirPath.toString());
				mrrpc.doWork();
				//HDrpc hdrpc2 = new HDrpc(cha, jarDirPath.toString());  
				//hdrpc2.doWork();
				break;
			case Benchmarks.HD:
				HDrpc hdrpc = new HDrpc(cha, jarDirPath.toString());  
				hdrpc.doWork();
				break;
			case Benchmarks.HB:
				break;
			default:
				break;
		}
		
		System.out.println("JX - INFO - findRPCs finished. the results are written out into " + jarDirPath);
	}
	
	
}
