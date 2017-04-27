package dt.spoon;

import java.util.Arrays;
import java.util.List;

public class BlackList {
	
	
	
	static List<String> javaFileStrs = Arrays.asList(
			// Exception by my modified code regarding to RPC - but will not be executed 
			"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-common/src/main/java/org/apache/hadoop/mapred/LocalJobRunner.java"
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/src/java/org/apache/hadoop/mapred/TaskTracker.java"
			//,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/src/java/org/apache/hadoop/mapred/Child.java"
			
			// Special Grammar like reflect & generics 
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapred/pipes/PipesNonJavaInputFormat.java"
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/lib/partition/InputSampler.java"
			
			// Can't find the extended Class that from Ant & only used by Ant 
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/record/compiler/ant/RccTask.java"
			
			// java.lang.RuntimeException: inconsistent compilation unit
			// 1. Empty files, declared types are []
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/webapp/AggregatedLogsBlock.java"
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-nodemanager/src/main/java/org/apache/hadoop/yarn/server/nodemanager/webapp/AggregatedLogsPage.java"
			// 2. declared types are [ class A { public static class B extend A {} } }???
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/fs/shell/Touchz.java"
		);
	
	
	
	static boolean isBlack(String absoluteFilename) {
		if (javaFileStrs.contains( absoluteFilename ))
			return true;
		return false;
	}
	
}
