package com.benchmark;

public class Benchmarks {
	
	public static final String MR = "MapReduce";
	public static final String HD = "HDFS"; 
	public static final String HB = "HBase";
	
	
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
