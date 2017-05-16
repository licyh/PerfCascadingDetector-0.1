package com.benchmark;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Benchmarks {
	
	public static final String MR = "MapReduce";
	public static final String HD = "HDFS"; 
	public static final String HB = "HBase";
	
	
	
	public static String resolveBugId(String pathStr) {
		Path path = Paths.get( pathStr );
		for (int i = 0; i < path.getNameCount(); i++) {
			String ele = path.getName(i).toString();
			//System.out.println("JX - DEBUG - ele:" + ele);
			if ( ele.matches( "[a-z]{2}-[0-9]*" ) ) {  //ie, mr-4576
				return ele;
			}
		}
		return null;
	}
	
	
	public static String resolveSystem(String pathStr) {
		String systemName = "";
		String lowerPathStr = pathStr.toLowerCase();
		if ( lowerPathStr.contains("mr") || lowerPathStr.contains("mapreduce") )
			systemName = MR;
		else if ( lowerPathStr.contains("hd") || lowerPathStr.contains("hdfs") )
			systemName = HD;
		else if ( lowerPathStr.contains("hb") || lowerPathStr.contains("hbase") )
			systemName = HB;
		return systemName;
	}
			
}
