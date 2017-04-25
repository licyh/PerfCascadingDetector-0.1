package dt.spoon;

import java.util.Arrays;
import java.util.List;

public class BlackList {
	
	static List<String> javaFileStrs = Arrays.asList(
			"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-common/src/main/java/org/apache/hadoop/mapred/LocalJobRunner.java"
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapred/pipes/PipesNonJavaInputFormat.java"
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-core/src/main/java/org/apache/hadoop/mapreduce/lib/partition/InputSampler.java"
			//,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-mapreduce-project/src/java/org/apache/hadoop/mapred/Child.java"
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/record/compiler/ant/RccTask.java"
			,"/home/vagrant/spoontest/hadoop-0.23.3-src/hadoop-common-project/hadoop-common/src/main/java/org/apache/hadoop/util/UTF8ByteArrayUtils.java"
		);
	
	static boolean isBlack(String absoluteFilename) {
		if (javaFileStrs.contains( absoluteFilename ))
			return true;
		return false;
	}
	
}
