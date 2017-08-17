package com.benchmark;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Benchmarks {
	
	public static final String MR = "MapReduce";
	public static final String HD = "HDFS"; 
	public static final String HB = "HBase";
	public static final String CA = "Cassandra";
	
	
	/**
	 * resolve bug id by a input path. A bug id is like mr-4576, ha-4584
	 * @param pathStr
	 */
	public static String resolveBugId(String pathStr) {
		// check every path component
		Path path = Paths.get( pathStr );
		for (int i = 0; i < path.getNameCount(); i++) {
			String ele = path.getName(i).toString();
			//System.out.println("JX - DEBUG - ele:" + ele);
			if ( ele.matches( "[a-z]{2}-[0-9]*" ) ) {  //ie, mr-4576
				return ele;
			}
		}
		// check by directly searching
		Pattern pattern = Pattern.compile( "[a-z]{2}-[0-9]*" );
		Matcher matcher = pattern.matcher( pathStr );
		while ( matcher.find() ) {
			return matcher.group();
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
		//added
		else if ( lowerPathStr.contains("ha") )
			systemName = HD;
		else if ( lowerPathStr.contains("ca") || lowerPathStr.contains("cassandra") ) {
			systemName = CA;
		}
		return systemName;
	}
			
}
