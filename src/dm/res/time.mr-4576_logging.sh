Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

java.lang.NullPointerException
	at dm.util.MethodUtil.setMethod(Unknown Source)
	at dm.util.MethodUtil.<init>(Unknown Source)
	at dm.transformers.Transformers.transformClassForCodeSnippets(Unknown Source)
	at dm.MapReduceTransformer.transformClass(Unknown Source)
	at dm.Transformer.transformClass(Unknown Source)
	at dm.Transformer.transform(Unknown Source)
	at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:424)
	at com.sun.security.auth.module.UnixLoginModule.login(UnixLoginModule.java:124)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at javax.security.auth.login.LoginContext.invoke(LoginContext.java:762)
	at javax.security.auth.login.LoginContext.access$000(LoginContext.java:203)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:690)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.login.LoginContext.invokePriv(LoginContext.java:687)
	at javax.security.auth.login.LoginContext.login(LoginContext.java:595)
	at org.apache.hadoop.security.UserGroupInformation.getLoginUser(UserGroupInformation.java:433)
	at org.apache.hadoop.security.UserGroupInformation.getCurrentUser(UserGroupInformation.java:414)
	at org.apache.hadoop.fs.FileSystem$Cache$Key.<init>(FileSystem.java:1494)
	at org.apache.hadoop.fs.FileSystem$Cache.get(FileSystem.java:1395)
	at org.apache.hadoop.fs.FileSystem.get(FileSystem.java:254)
	at org.apache.hadoop.fs.FileSystem.getLocal(FileSystem.java:225)
	at org.apache.hadoop.util.GenericOptionsParser.validateFiles(GenericOptionsParser.java:374)
	at org.apache.hadoop.util.GenericOptionsParser.processGeneralOptions(GenericOptionsParser.java:287)
	at org.apache.hadoop.util.GenericOptionsParser.parseGeneralOptions(GenericOptionsParser.java:413)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:164)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:147)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:53)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:68)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:139)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:64)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:156)
JX - WARN - 1&0 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 3&2 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 5&4 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 1&0 for org.apache.hadoop.mapred.Counters.makeCompactString()Ljava/lang/String;
17/09/09 06:43:34 INFO input.FileInputFormat: Total input paths to process : 1
JX - WARN - 1&0 for org.apache.hadoop.mapred.QueueManager.checkDeprecation(Lorg/apache/hadoop/conf/Configuration;)V
17/09/09 06:43:35 INFO mapred.JobClient: Running job: job_201709090643_0001
17/09/09 06:43:36 INFO mapred.JobClient:  map 0% reduce 0%
Command terminated by signal 2
real 542.46
user 23.50
sys 13.57
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

java.lang.NullPointerException
	at dm.util.MethodUtil.setMethod(Unknown Source)
	at dm.util.MethodUtil.<init>(Unknown Source)
	at dm.transformers.Transformers.transformClassForCodeSnippets(Unknown Source)
	at dm.MapReduceTransformer.transformClass(Unknown Source)
	at dm.Transformer.transformClass(Unknown Source)
	at dm.Transformer.transform(Unknown Source)
	at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:424)
	at com.sun.security.auth.module.UnixLoginModule.login(UnixLoginModule.java:124)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at javax.security.auth.login.LoginContext.invoke(LoginContext.java:762)
	at javax.security.auth.login.LoginContext.access$000(LoginContext.java:203)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:690)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.login.LoginContext.invokePriv(LoginContext.java:687)
	at javax.security.auth.login.LoginContext.login(LoginContext.java:595)
	at org.apache.hadoop.security.UserGroupInformation.getLoginUser(UserGroupInformation.java:433)
	at org.apache.hadoop.security.UserGroupInformation.getCurrentUser(UserGroupInformation.java:414)
	at org.apache.hadoop.fs.FileSystem$Cache$Key.<init>(FileSystem.java:1494)
	at org.apache.hadoop.fs.FileSystem$Cache.get(FileSystem.java:1395)
	at org.apache.hadoop.fs.FileSystem.get(FileSystem.java:254)
	at org.apache.hadoop.fs.FileSystem.getLocal(FileSystem.java:225)
	at org.apache.hadoop.util.GenericOptionsParser.validateFiles(GenericOptionsParser.java:374)
	at org.apache.hadoop.util.GenericOptionsParser.processGeneralOptions(GenericOptionsParser.java:287)
	at org.apache.hadoop.util.GenericOptionsParser.parseGeneralOptions(GenericOptionsParser.java:413)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:164)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:147)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:53)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:68)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:139)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:64)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:156)
JX - WARN - 1&0 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 3&2 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 5&4 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 1&0 for org.apache.hadoop.mapred.Counters.makeCompactString()Ljava/lang/String;
17/09/09 07:01:54 INFO input.FileInputFormat: Total input paths to process : 1
JX - WARN - 1&0 for org.apache.hadoop.mapred.QueueManager.checkDeprecation(Lorg/apache/hadoop/conf/Configuration;)V
17/09/09 07:01:55 INFO mapred.JobClient: Running job: job_201709090701_0001
17/09/09 07:01:56 INFO mapred.JobClient:  map 0% reduce 0%
Command terminated by signal 2
real 133.55
user 19.28
sys 9.06
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

java.lang.NullPointerException
	at dm.util.MethodUtil.setMethod(Unknown Source)
	at dm.util.MethodUtil.<init>(Unknown Source)
	at dm.transformers.Transformers.transformClassForCodeSnippets(Unknown Source)
	at dm.MapReduceTransformer.transformClass(Unknown Source)
	at dm.Transformer.transformClass(Unknown Source)
	at dm.Transformer.transform(Unknown Source)
	at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:424)
	at com.sun.security.auth.module.UnixLoginModule.login(UnixLoginModule.java:124)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at javax.security.auth.login.LoginContext.invoke(LoginContext.java:762)
	at javax.security.auth.login.LoginContext.access$000(LoginContext.java:203)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:690)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.login.LoginContext.invokePriv(LoginContext.java:687)
	at javax.security.auth.login.LoginContext.login(LoginContext.java:595)
	at org.apache.hadoop.security.UserGroupInformation.getLoginUser(UserGroupInformation.java:433)
	at org.apache.hadoop.security.UserGroupInformation.getCurrentUser(UserGroupInformation.java:414)
	at org.apache.hadoop.fs.FileSystem$Cache$Key.<init>(FileSystem.java:1494)
	at org.apache.hadoop.fs.FileSystem$Cache.get(FileSystem.java:1395)
	at org.apache.hadoop.fs.FileSystem.get(FileSystem.java:254)
	at org.apache.hadoop.fs.FileSystem.getLocal(FileSystem.java:225)
	at org.apache.hadoop.util.GenericOptionsParser.validateFiles(GenericOptionsParser.java:374)
	at org.apache.hadoop.util.GenericOptionsParser.processGeneralOptions(GenericOptionsParser.java:287)
	at org.apache.hadoop.util.GenericOptionsParser.parseGeneralOptions(GenericOptionsParser.java:413)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:164)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:147)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:53)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:68)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:139)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:64)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:156)
JX - WARN - 1&0 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 3&2 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 5&4 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 1&0 for org.apache.hadoop.mapred.Counters.makeCompactString()Ljava/lang/String;
17/09/09 07:07:50 INFO input.FileInputFormat: Total input paths to process : 1
JX - WARN - 1&0 for org.apache.hadoop.mapred.QueueManager.checkDeprecation(Lorg/apache/hadoop/conf/Configuration;)V
17/09/09 07:07:51 INFO mapred.JobClient: Running job: job_201709090707_0001
17/09/09 07:07:52 INFO mapred.JobClient:  map 0% reduce 0%
Command terminated by signal 2
real 132.50
user 19.39
sys 9.71
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

java.lang.NullPointerException
	at dm.util.MethodUtil.setMethod(Unknown Source)
	at dm.util.MethodUtil.<init>(Unknown Source)
	at dm.transformers.Transformers.transformClassForCodeSnippets(Unknown Source)
	at dm.MapReduceTransformer.transformClass(Unknown Source)
	at dm.Transformer.transformClass(Unknown Source)
	at dm.Transformer.transform(Unknown Source)
	at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:424)
	at com.sun.security.auth.module.UnixLoginModule.login(UnixLoginModule.java:124)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at javax.security.auth.login.LoginContext.invoke(LoginContext.java:762)
	at javax.security.auth.login.LoginContext.access$000(LoginContext.java:203)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:690)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.login.LoginContext.invokePriv(LoginContext.java:687)
	at javax.security.auth.login.LoginContext.login(LoginContext.java:595)
	at org.apache.hadoop.security.UserGroupInformation.getLoginUser(UserGroupInformation.java:433)
	at org.apache.hadoop.security.UserGroupInformation.getCurrentUser(UserGroupInformation.java:414)
	at org.apache.hadoop.fs.FileSystem$Cache$Key.<init>(FileSystem.java:1494)
	at org.apache.hadoop.fs.FileSystem$Cache.get(FileSystem.java:1395)
	at org.apache.hadoop.fs.FileSystem.get(FileSystem.java:254)
	at org.apache.hadoop.fs.FileSystem.getLocal(FileSystem.java:225)
	at org.apache.hadoop.util.GenericOptionsParser.validateFiles(GenericOptionsParser.java:374)
	at org.apache.hadoop.util.GenericOptionsParser.processGeneralOptions(GenericOptionsParser.java:287)
	at org.apache.hadoop.util.GenericOptionsParser.parseGeneralOptions(GenericOptionsParser.java:413)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:164)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:147)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:53)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:68)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:139)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:64)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:156)
JX - WARN - 1&0 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 3&2 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 5&4 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 1&0 for org.apache.hadoop.mapred.Counters.makeCompactString()Ljava/lang/String;
17/09/09 07:13:31 INFO input.FileInputFormat: Total input paths to process : 1
JX - WARN - 1&0 for org.apache.hadoop.mapred.QueueManager.checkDeprecation(Lorg/apache/hadoop/conf/Configuration;)V
17/09/09 07:13:31 INFO mapred.JobClient: Running job: job_201709090713_0001
17/09/09 07:13:32 INFO mapred.JobClient:  map 0% reduce 0%
Command terminated by signal 2
real 269.00
user 20.38
sys 9.61
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

java.lang.NullPointerException
	at dm.util.MethodUtil.setMethod(Unknown Source)
	at dm.util.MethodUtil.<init>(Unknown Source)
	at dm.transformers.Transformers.transformClassForCodeSnippets(Unknown Source)
	at dm.MapReduceTransformer.transformClass(Unknown Source)
	at dm.Transformer.transformClass(Unknown Source)
	at dm.Transformer.transform(Unknown Source)
	at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:424)
	at com.sun.security.auth.module.UnixLoginModule.login(UnixLoginModule.java:124)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at javax.security.auth.login.LoginContext.invoke(LoginContext.java:762)
	at javax.security.auth.login.LoginContext.access$000(LoginContext.java:203)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:690)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.login.LoginContext.invokePriv(LoginContext.java:687)
	at javax.security.auth.login.LoginContext.login(LoginContext.java:595)
	at org.apache.hadoop.security.UserGroupInformation.getLoginUser(UserGroupInformation.java:433)
	at org.apache.hadoop.security.UserGroupInformation.getCurrentUser(UserGroupInformation.java:414)
	at org.apache.hadoop.fs.FileSystem$Cache$Key.<init>(FileSystem.java:1494)
	at org.apache.hadoop.fs.FileSystem$Cache.get(FileSystem.java:1395)
	at org.apache.hadoop.fs.FileSystem.get(FileSystem.java:254)
	at org.apache.hadoop.fs.FileSystem.getLocal(FileSystem.java:225)
	at org.apache.hadoop.util.GenericOptionsParser.validateFiles(GenericOptionsParser.java:374)
	at org.apache.hadoop.util.GenericOptionsParser.processGeneralOptions(GenericOptionsParser.java:287)
	at org.apache.hadoop.util.GenericOptionsParser.parseGeneralOptions(GenericOptionsParser.java:413)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:164)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:147)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:53)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:68)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:139)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:64)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:156)
JX - WARN - 1&0 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 3&2 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 5&4 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 1&0 for org.apache.hadoop.mapred.Counters.makeCompactString()Ljava/lang/String;
17/09/09 07:22:24 INFO input.FileInputFormat: Total input paths to process : 1
JX - WARN - 1&0 for org.apache.hadoop.mapred.QueueManager.checkDeprecation(Lorg/apache/hadoop/conf/Configuration;)V
17/09/09 07:22:25 INFO mapred.JobClient: Running job: job_201709090722_0001
17/09/09 07:22:26 INFO mapred.JobClient:  map 0% reduce 0%
17/09/09 07:22:56 INFO mapred.JobClient:  map 100% reduce 0%
17/09/09 07:23:20 INFO mapred.JobClient:  map 100% reduce 100%
17/09/09 07:23:32 INFO mapred.JobClient: Job complete: job_201709090722_0001
17/09/09 07:23:32 INFO mapred.JobClient: Counters: 29
17/09/09 07:23:32 INFO mapred.JobClient:   Job Counters 
17/09/09 07:23:32 INFO mapred.JobClient:     Launched reduce tasks=1
17/09/09 07:23:32 INFO mapred.JobClient:     SLOTS_MILLIS_MAPS=38317
17/09/09 07:23:32 INFO mapred.JobClient:     Total time spent by all reduces waiting after reserving slots (ms)=0
17/09/09 07:23:32 INFO mapred.JobClient:     Total time spent by all maps waiting after reserving slots (ms)=0
17/09/09 07:23:32 INFO mapred.JobClient:     Launched map tasks=1
17/09/09 07:23:32 INFO mapred.JobClient:     Data-local map tasks=1
17/09/09 07:23:32 INFO mapred.JobClient:     SLOTS_MILLIS_REDUCES=21001
17/09/09 07:23:32 INFO mapred.JobClient:   File Output Format Counters 
17/09/09 07:23:32 INFO mapred.JobClient:     Bytes Written=1447
17/09/09 07:23:32 INFO mapred.JobClient:   FileSystemCounters
17/09/09 07:23:32 INFO mapred.JobClient:     FILE_BYTES_READ=2397
17/09/09 07:23:32 INFO mapred.JobClient:     HDFS_BYTES_READ=1501
17/09/09 07:23:32 INFO mapred.JobClient:     FILE_BYTES_WRITTEN=49617
17/09/09 07:23:32 INFO mapred.JobClient:     HDFS_BYTES_WRITTEN=1447
17/09/09 07:23:32 INFO mapred.JobClient:   File Input Format Counters 
17/09/09 07:23:32 INFO mapred.JobClient:     Bytes Read=1386
17/09/09 07:23:32 INFO mapred.JobClient:   Map-Reduce Framework
17/09/09 07:23:32 INFO mapred.JobClient:     Map output materialized bytes=2397
17/09/09 07:23:32 INFO mapred.JobClient:     Map input records=18
17/09/09 07:23:32 INFO mapred.JobClient:     Reduce shuffle bytes=0
17/09/09 07:23:32 INFO mapred.JobClient:     Spilled Records=472
17/09/09 07:23:32 INFO mapred.JobClient:     Map output bytes=2479
17/09/09 07:23:32 INFO mapred.JobClient:     Total committed heap usage (bytes)=403177472
17/09/09 07:23:32 INFO mapred.JobClient:     CPU time spent (ms)=25450
17/09/09 07:23:32 INFO mapred.JobClient:     Combine input records=329
17/09/09 07:23:32 INFO mapred.JobClient:     SPLIT_RAW_BYTES=115
17/09/09 07:23:32 INFO mapred.JobClient:     Reduce input records=236
17/09/09 07:23:32 INFO mapred.JobClient:     Reduce input groups=236
17/09/09 07:23:32 INFO mapred.JobClient:     Combine output records=236
17/09/09 07:23:32 INFO mapred.JobClient:     Physical memory (bytes) snapshot=458227712
17/09/09 07:23:32 INFO mapred.JobClient:     Reduce output records=236
17/09/09 07:23:32 INFO mapred.JobClient:     Virtual memory (bytes) snapshot=5962530816
17/09/09 07:23:32 INFO mapred.JobClient:     Map output records=329
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

real 91.55
user 17.48
sys 7.62
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

java.lang.NullPointerException
	at dm.util.MethodUtil.setMethod(Unknown Source)
	at dm.util.MethodUtil.<init>(Unknown Source)
	at dm.transformers.Transformers.transformClassForCodeSnippets(Unknown Source)
	at dm.MapReduceTransformer.transformClass(Unknown Source)
	at dm.Transformer.transformClass(Unknown Source)
	at dm.Transformer.transform(Unknown Source)
	at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:424)
	at com.sun.security.auth.module.UnixLoginModule.login(UnixLoginModule.java:124)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at javax.security.auth.login.LoginContext.invoke(LoginContext.java:762)
	at javax.security.auth.login.LoginContext.access$000(LoginContext.java:203)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:690)
	at javax.security.auth.login.LoginContext$4.run(LoginContext.java:688)
	at java.security.AccessController.doPrivileged(Native Method)
	at javax.security.auth.login.LoginContext.invokePriv(LoginContext.java:687)
	at javax.security.auth.login.LoginContext.login(LoginContext.java:595)
	at org.apache.hadoop.security.UserGroupInformation.getLoginUser(UserGroupInformation.java:433)
	at org.apache.hadoop.security.UserGroupInformation.getCurrentUser(UserGroupInformation.java:414)
	at org.apache.hadoop.fs.FileSystem$Cache$Key.<init>(FileSystem.java:1494)
	at org.apache.hadoop.fs.FileSystem$Cache.get(FileSystem.java:1395)
	at org.apache.hadoop.fs.FileSystem.get(FileSystem.java:254)
	at org.apache.hadoop.fs.FileSystem.getLocal(FileSystem.java:225)
	at org.apache.hadoop.util.GenericOptionsParser.validateFiles(GenericOptionsParser.java:374)
	at org.apache.hadoop.util.GenericOptionsParser.processGeneralOptions(GenericOptionsParser.java:287)
	at org.apache.hadoop.util.GenericOptionsParser.parseGeneralOptions(GenericOptionsParser.java:413)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:164)
	at org.apache.hadoop.util.GenericOptionsParser.<init>(GenericOptionsParser.java:147)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:53)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:68)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:139)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:64)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:156)
JX - WARN - 1&0 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 3&2 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 5&4 for org.apache.hadoop.mapred.JobTracker.markCompletedJob(Lorg/apache/hadoop/mapred/JobInProgress;)V
JX - WARN - 1&0 for org.apache.hadoop.mapred.Counters.makeCompactString()Ljava/lang/String;
17/09/09 07:27:52 INFO input.FileInputFormat: Total input paths to process : 1
JX - WARN - 1&0 for org.apache.hadoop.mapred.QueueManager.checkDeprecation(Lorg/apache/hadoop/conf/Configuration;)V
17/09/09 07:27:53 INFO mapred.JobClient: Running job: job_201709090727_0001
17/09/09 07:27:54 INFO mapred.JobClient:  map 0% reduce 0%
17/09/09 07:28:23 INFO mapred.JobClient:  map 100% reduce 0%
17/09/09 07:28:49 INFO mapred.JobClient:  map 100% reduce 100%
17/09/09 07:29:00 INFO mapred.JobClient: Job complete: job_201709090727_0001
17/09/09 07:29:00 INFO mapred.JobClient: Counters: 29
17/09/09 07:29:00 INFO mapred.JobClient:   Job Counters 
17/09/09 07:29:00 INFO mapred.JobClient:     Launched reduce tasks=1
17/09/09 07:29:00 INFO mapred.JobClient:     SLOTS_MILLIS_MAPS=38363
17/09/09 07:29:00 INFO mapred.JobClient:     Total time spent by all reduces waiting after reserving slots (ms)=0
17/09/09 07:29:00 INFO mapred.JobClient:     Total time spent by all maps waiting after reserving slots (ms)=0
17/09/09 07:29:00 INFO mapred.JobClient:     Launched map tasks=1
17/09/09 07:29:00 INFO mapred.JobClient:     Data-local map tasks=1
17/09/09 07:29:00 INFO mapred.JobClient:     SLOTS_MILLIS_REDUCES=20644
17/09/09 07:29:00 INFO mapred.JobClient:   File Output Format Counters 
17/09/09 07:29:00 INFO mapred.JobClient:     Bytes Written=1447
17/09/09 07:29:00 INFO mapred.JobClient:   FileSystemCounters
17/09/09 07:29:00 INFO mapred.JobClient:     FILE_BYTES_READ=2397
17/09/09 07:29:00 INFO mapred.JobClient:     HDFS_BYTES_READ=1501
17/09/09 07:29:00 INFO mapred.JobClient:     FILE_BYTES_WRITTEN=49617
17/09/09 07:29:00 INFO mapred.JobClient:     HDFS_BYTES_WRITTEN=1447
17/09/09 07:29:00 INFO mapred.JobClient:   File Input Format Counters 
17/09/09 07:29:00 INFO mapred.JobClient:     Bytes Read=1386
17/09/09 07:29:00 INFO mapred.JobClient:   Map-Reduce Framework
17/09/09 07:29:00 INFO mapred.JobClient:     Map output materialized bytes=2397
17/09/09 07:29:00 INFO mapred.JobClient:     Map input records=18
17/09/09 07:29:00 INFO mapred.JobClient:     Reduce shuffle bytes=0
17/09/09 07:29:00 INFO mapred.JobClient:     Spilled Records=472
17/09/09 07:29:00 INFO mapred.JobClient:     Map output bytes=2479
17/09/09 07:29:00 INFO mapred.JobClient:     Total committed heap usage (bytes)=396886016
17/09/09 07:29:00 INFO mapred.JobClient:     CPU time spent (ms)=22130
17/09/09 07:29:00 INFO mapred.JobClient:     Combine input records=329
17/09/09 07:29:00 INFO mapred.JobClient:     SPLIT_RAW_BYTES=115
17/09/09 07:29:00 INFO mapred.JobClient:     Reduce input records=236
17/09/09 07:29:00 INFO mapred.JobClient:     Reduce input groups=236
17/09/09 07:29:00 INFO mapred.JobClient:     Combine output records=236
17/09/09 07:29:00 INFO mapred.JobClient:     Physical memory (bytes) snapshot=458645504
17/09/09 07:29:00 INFO mapred.JobClient:     Reduce output records=236
17/09/09 07:29:00 INFO mapred.JobClient:     Virtual memory (bytes) snapshot=5962530816
17/09/09 07:29:00 INFO mapred.JobClient:     Map output records=329
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

real 94.90
user 18.99
sys 9.07
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

17/09/09 07:32:38 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 07:32:39 INFO mapred.JobClient: Running job: job_201709090732_0001
17/09/09 07:32:40 INFO mapred.JobClient:  map 0% reduce 0%
17/09/09 07:33:10 INFO mapred.JobClient:  map 100% reduce 0%
17/09/09 07:33:34 INFO mapred.JobClient:  map 100% reduce 100%
17/09/09 07:33:45 INFO mapred.JobClient: Job complete: job_201709090732_0001
17/09/09 07:33:45 INFO mapred.JobClient: Counters: 29
17/09/09 07:33:45 INFO mapred.JobClient:   Job Counters 
17/09/09 07:33:45 INFO mapred.JobClient:     Launched reduce tasks=1
17/09/09 07:33:45 INFO mapred.JobClient:     SLOTS_MILLIS_MAPS=38124
17/09/09 07:33:45 INFO mapred.JobClient:     Total time spent by all reduces waiting after reserving slots (ms)=0
17/09/09 07:33:45 INFO mapred.JobClient:     Total time spent by all maps waiting after reserving slots (ms)=0
17/09/09 07:33:45 INFO mapred.JobClient:     Launched map tasks=1
17/09/09 07:33:45 INFO mapred.JobClient:     Data-local map tasks=1
17/09/09 07:33:45 INFO mapred.JobClient:     SLOTS_MILLIS_REDUCES=20498
17/09/09 07:33:45 INFO mapred.JobClient:   File Output Format Counters 
17/09/09 07:33:45 INFO mapred.JobClient:     Bytes Written=1447
17/09/09 07:33:45 INFO mapred.JobClient:   FileSystemCounters
17/09/09 07:33:45 INFO mapred.JobClient:     FILE_BYTES_READ=2397
17/09/09 07:33:45 INFO mapred.JobClient:     HDFS_BYTES_READ=1501
17/09/09 07:33:45 INFO mapred.JobClient:     FILE_BYTES_WRITTEN=49615
17/09/09 07:33:45 INFO mapred.JobClient:     HDFS_BYTES_WRITTEN=1447
17/09/09 07:33:45 INFO mapred.JobClient:   File Input Format Counters 
17/09/09 07:33:45 INFO mapred.JobClient:     Bytes Read=1386
17/09/09 07:33:45 INFO mapred.JobClient:   Map-Reduce Framework
17/09/09 07:33:45 INFO mapred.JobClient:     Map output materialized bytes=2397
17/09/09 07:33:45 INFO mapred.JobClient:     Map input records=18
17/09/09 07:33:45 INFO mapred.JobClient:     Reduce shuffle bytes=0
17/09/09 07:33:45 INFO mapred.JobClient:     Spilled Records=472
17/09/09 07:33:45 INFO mapred.JobClient:     Map output bytes=2479
17/09/09 07:33:45 INFO mapred.JobClient:     Total committed heap usage (bytes)=402653184
17/09/09 07:33:45 INFO mapred.JobClient:     CPU time spent (ms)=22920
17/09/09 07:33:45 INFO mapred.JobClient:     Combine input records=329
17/09/09 07:33:45 INFO mapred.JobClient:     SPLIT_RAW_BYTES=115
17/09/09 07:33:45 INFO mapred.JobClient:     Reduce input records=236
17/09/09 07:33:45 INFO mapred.JobClient:     Reduce input groups=236
17/09/09 07:33:45 INFO mapred.JobClient:     Combine output records=236
17/09/09 07:33:45 INFO mapred.JobClient:     Physical memory (bytes) snapshot=452083712
17/09/09 07:33:45 INFO mapred.JobClient:     Reduce output records=236
17/09/09 07:33:45 INFO mapred.JobClient:     Virtual memory (bytes) snapshot=5962530816
17/09/09 07:33:45 INFO mapred.JobClient:     Map output records=329
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

real 82.84
user 5.12
sys 3.53
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

17/09/09 07:35:54 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 07:35:55 INFO mapred.JobClient: Running job: job_201709090735_0001
17/09/09 07:35:56 INFO mapred.JobClient:  map 0% reduce 0%
17/09/09 07:36:27 INFO mapred.JobClient:  map 100% reduce 0%
17/09/09 07:36:51 INFO mapred.JobClient:  map 100% reduce 100%
17/09/09 07:37:02 INFO mapred.JobClient: Job complete: job_201709090735_0001
17/09/09 07:37:02 INFO mapred.JobClient: Counters: 29
17/09/09 07:37:02 INFO mapred.JobClient:   Job Counters 
17/09/09 07:37:02 INFO mapred.JobClient:     Launched reduce tasks=1
17/09/09 07:37:02 INFO mapred.JobClient:     SLOTS_MILLIS_MAPS=38161
17/09/09 07:37:02 INFO mapred.JobClient:     Total time spent by all reduces waiting after reserving slots (ms)=0
17/09/09 07:37:02 INFO mapred.JobClient:     Total time spent by all maps waiting after reserving slots (ms)=0
17/09/09 07:37:02 INFO mapred.JobClient:     Launched map tasks=1
17/09/09 07:37:02 INFO mapred.JobClient:     Data-local map tasks=1
17/09/09 07:37:02 INFO mapred.JobClient:     SLOTS_MILLIS_REDUCES=20670
17/09/09 07:37:02 INFO mapred.JobClient:   File Output Format Counters 
17/09/09 07:37:02 INFO mapred.JobClient:     Bytes Written=1447
17/09/09 07:37:02 INFO mapred.JobClient:   FileSystemCounters
17/09/09 07:37:02 INFO mapred.JobClient:     FILE_BYTES_READ=2397
17/09/09 07:37:02 INFO mapred.JobClient:     HDFS_BYTES_READ=1501
17/09/09 07:37:02 INFO mapred.JobClient:     FILE_BYTES_WRITTEN=49617
17/09/09 07:37:02 INFO mapred.JobClient:     HDFS_BYTES_WRITTEN=1447
17/09/09 07:37:02 INFO mapred.JobClient:   File Input Format Counters 
17/09/09 07:37:02 INFO mapred.JobClient:     Bytes Read=1386
17/09/09 07:37:02 INFO mapred.JobClient:   Map-Reduce Framework
17/09/09 07:37:02 INFO mapred.JobClient:     Map output materialized bytes=2397
17/09/09 07:37:02 INFO mapred.JobClient:     Map input records=18
17/09/09 07:37:02 INFO mapred.JobClient:     Reduce shuffle bytes=2397
17/09/09 07:37:02 INFO mapred.JobClient:     Spilled Records=472
17/09/09 07:37:02 INFO mapred.JobClient:     Map output bytes=2479
17/09/09 07:37:02 INFO mapred.JobClient:     Total committed heap usage (bytes)=402653184
17/09/09 07:37:02 INFO mapred.JobClient:     CPU time spent (ms)=21060
17/09/09 07:37:02 INFO mapred.JobClient:     Combine input records=329
17/09/09 07:37:02 INFO mapred.JobClient:     SPLIT_RAW_BYTES=115
17/09/09 07:37:02 INFO mapred.JobClient:     Reduce input records=236
17/09/09 07:37:02 INFO mapred.JobClient:     Reduce input groups=236
17/09/09 07:37:02 INFO mapred.JobClient:     Combine output records=236
17/09/09 07:37:02 INFO mapred.JobClient:     Physical memory (bytes) snapshot=451424256
17/09/09 07:37:02 INFO mapred.JobClient:     Reduce output records=236
17/09/09 07:37:02 INFO mapred.JobClient:     Virtual memory (bytes) snapshot=5962530816
17/09/09 07:37:02 INFO mapred.JobClient:     Map output records=329
Warning: $HADOOP_HOME is deprecated.

Warning: $HADOOP_HOME is deprecated.

real 83.84
user 5.15
sys 4.58
