nohup: appending output to ‘nohup.out’
17/09/09 05:06:37 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:06:37 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:06:37 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:06:37 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:06:38 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:06:38 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:06:39 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:06:39 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:06:39 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:06:39 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:06:39 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:06:39 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:06:40 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:06:40 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:06:40 INFO mapreduce.Job: Running job: job_201709090506_0002
17/09/09 05:06:40 INFO mapreduce.Job: Running job: job_201709090506_0001
17/09/09 05:06:41 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:06:41 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:07:01 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:07:01 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:07:11 INFO mapreduce.Job:  map 100% reduce 100%
17/09/09 05:07:14 INFO mapreduce.Job: Job complete: job_201709090506_0002
17/09/09 05:07:14 INFO mapreduce.Job: Counters: 33
	FileInputFormatCounters
		BYTES_READ=13836
	FileSystemCounters
		FILE_BYTES_READ=20127
		FILE_BYTES_WRITTEN=40286
		HDFS_BYTES_READ=13951
		HDFS_BYTES_WRITTEN=12625
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	Job Counters 
		Data-local map tasks=1
		Total time spent by all maps waiting after reserving slots (ms)=0
		Total time spent by all reduces waiting after reserving slots (ms)=0
		SLOTS_MILLIS_MAPS=11978
		SLOTS_MILLIS_REDUCES=7202
		Launched map tasks=1
		Launched reduce tasks=1
	Map-Reduce Framework
		Combine input records=3308
		Combine output records=1884
		Failed Shuffles=0
		GC time elapsed (ms)=0
		Map input records=180
		Map output bytes=25074
		Map output records=3308
		Merged Map outputs=1
		Reduce input groups=1884
		Reduce input records=1884
		Reduce output records=1884
		Reduce shuffle bytes=20127
		Shuffled Maps =1
		Spilled Records=3768
		SPLIT_RAW_BYTES=115
17/09/09 05:07:15 INFO mapreduce.Job:  map 100% reduce 100%
java.io.IOException: Call to /11.11.2.51:9001 failed on local exception: java.io.EOFException
	at org.apache.hadoop.ipc.Client.wrapException(Client.java:940)
	at org.apache.hadoop.ipc.Client.call(Client.java:908)
	at org.apache.hadoop.ipc.WritableRpcEngine$Invoker.invoke(WritableRpcEngine.java:198)
	at com.sun.proxy.$Proxy1.getTaskCompletionEvents(Unknown Source)
	at org.apache.hadoop.mapreduce.Job.getTaskCompletionEvents(Job.java:460)
	at org.apache.hadoop.mapreduce.Job.monitorAndPrintJob(Job.java:1025)
	at org.apache.hadoop.mapreduce.Job.waitForCompletion(Job.java:979)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:84)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:72)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:144)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:68)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:192)
Caused by: java.io.EOFException
	at java.io.DataInputStream.readInt(DataInputStream.java:392)
	at org.apache.hadoop.ipc.Client$Connection.receiveResponse(Client.java:664)
	at org.apache.hadoop.ipc.Client$Connection.run(Client.java:602)
real 56.09
user 12.31
sys 9.75
nohup: appending output to ‘nohup.out’
17/09/09 05:11:05 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:11:05 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:11:06 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:11:06 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:11:06 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:11:06 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:11:08 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:11:08 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:11:08 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:11:08 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:11:09 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:11:09 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:11:09 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:11:09 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:11:09 INFO mapreduce.Job: Running job: job_201709090511_0002
17/09/09 05:11:09 INFO mapreduce.Job: Running job: job_201709090511_0001
17/09/09 05:11:10 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:11:10 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:11:27 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:11:31 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:11:38 INFO mapreduce.Job:  map 100% reduce 100%
17/09/09 05:11:40 INFO mapreduce.Job: Job complete: job_201709090511_0002
17/09/09 05:11:40 INFO mapreduce.Job: Counters: 33
	FileInputFormatCounters
		BYTES_READ=1386
	FileSystemCounters
		FILE_BYTES_READ=2368
		FILE_BYTES_WRITTEN=4768
		HDFS_BYTES_READ=1500
		HDFS_BYTES_WRITTEN=1435
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	Job Counters 
		Data-local map tasks=1
		Total time spent by all maps waiting after reserving slots (ms)=0
		Total time spent by all reduces waiting after reserving slots (ms)=0
		SLOTS_MILLIS_MAPS=10158
		SLOTS_MILLIS_REDUCES=7071
		Launched map tasks=1
		Launched reduce tasks=1
	Map-Reduce Framework
		Combine input records=320
		Combine output records=232
		Failed Shuffles=0
		GC time elapsed (ms)=0
		Map input records=18
		Map output bytes=2428
		Map output records=320
		Merged Map outputs=1
		Reduce input groups=232
		Reduce input records=232
		Reduce output records=232
		Reduce shuffle bytes=2368
		Shuffled Maps =1
		Spilled Records=464
		SPLIT_RAW_BYTES=114
17/09/09 05:11:41 INFO mapreduce.Job:  map 100% reduce 100%
17/09/09 05:11:44 INFO mapreduce.Job: Job complete: job_201709090511_0001
17/09/09 05:11:44 INFO mapreduce.Job: Counters: 33
	FileInputFormatCounters
		BYTES_READ=13836
	FileSystemCounters
		FILE_BYTES_READ=20127
		FILE_BYTES_WRITTEN=40286
		HDFS_BYTES_READ=13951
		HDFS_BYTES_WRITTEN=12625
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	Job Counters 
		Data-local map tasks=1
		Total time spent by all maps waiting after reserving slots (ms)=0
		Total time spent by all reduces waiting after reserving slots (ms)=0
		SLOTS_MILLIS_MAPS=12402
		SLOTS_MILLIS_REDUCES=6980
		Launched map tasks=1
		Launched reduce tasks=1
	Map-Reduce Framework
		Combine input records=3308
		Combine output records=1884
		Failed Shuffles=0
		GC time elapsed (ms)=0
		Map input records=180
		Map output bytes=25074
		Map output records=3308
		Merged Map outputs=1
		Reduce input groups=1884
		Reduce input records=1884
		Reduce output records=1884
		Reduce shuffle bytes=20127
		Shuffled Maps =1
		Spilled Records=3768
		SPLIT_RAW_BYTES=115
real 57.83
user 12.39
sys 8.78
nohup: appending output to ‘nohup.out’
17/09/09 05:24:45 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:24:45 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:24:45 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:24:45 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:24:45 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:24:45 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:24:46 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:24:46 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:24:46 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:24:46 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:24:46 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:24:46 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:24:46 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:24:46 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:24:46 INFO mapreduce.Job: Running job: job_201709090524_0001
17/09/09 05:24:46 INFO mapreduce.Job: Running job: job_201709090524_0002
17/09/09 05:24:47 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:24:47 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:25:02 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:25:05 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:25:08 INFO mapreduce.Job:  map 100% reduce 100%
17/09/09 05:25:11 INFO mapreduce.Job: Job complete: job_201709090524_0001
17/09/09 05:25:11 INFO mapreduce.Job: Counters: 33
	FileInputFormatCounters
		BYTES_READ=1386
	FileSystemCounters
		FILE_BYTES_READ=2368
		FILE_BYTES_WRITTEN=4768
		HDFS_BYTES_READ=1500
		HDFS_BYTES_WRITTEN=1435
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	Job Counters 
		Data-local map tasks=1
		Total time spent by all maps waiting after reserving slots (ms)=0
		Total time spent by all reduces waiting after reserving slots (ms)=0
		SLOTS_MILLIS_MAPS=10668
		SLOTS_MILLIS_REDUCES=3945
		Launched map tasks=1
		Launched reduce tasks=1
	Map-Reduce Framework
		Combine input records=320
		Combine output records=232
		Failed Shuffles=0
		GC time elapsed (ms)=0
		Map input records=18
		Map output bytes=2428
		Map output records=320
		Merged Map outputs=1
		Reduce input groups=232
		Reduce input records=232
		Reduce output records=232
		Reduce shuffle bytes=2368
		Shuffled Maps =1
		Spilled Records=464
		SPLIT_RAW_BYTES=114
17/09/09 05:25:15 INFO mapreduce.Job:  map 100% reduce 100%
17/09/09 05:25:17 INFO mapreduce.Job: Job complete: job_201709090524_0002
17/09/09 05:25:17 INFO mapreduce.Job: Counters: 33
	FileInputFormatCounters
		BYTES_READ=13836
	FileSystemCounters
		FILE_BYTES_READ=20127
		FILE_BYTES_WRITTEN=40286
		HDFS_BYTES_READ=13951
		HDFS_BYTES_WRITTEN=12625
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	Job Counters 
		Data-local map tasks=1
		Total time spent by all maps waiting after reserving slots (ms)=0
		Total time spent by all reduces waiting after reserving slots (ms)=0
		SLOTS_MILLIS_MAPS=12362
		SLOTS_MILLIS_REDUCES=7062
		Launched map tasks=1
		Launched reduce tasks=1
	Map-Reduce Framework
		Combine input records=3308
		Combine output records=1884
		Failed Shuffles=0
		GC time elapsed (ms)=0
		Map input records=180
		Map output bytes=25074
		Map output records=3308
		Merged Map outputs=1
		Reduce input groups=1884
		Reduce input records=1884
		Reduce output records=1884
		Reduce shuffle bytes=20127
		Shuffled Maps =1
		Spilled Records=3768
		SPLIT_RAW_BYTES=115
real 49.99
user 10.19
sys 6.53
nohup: appending output to ‘nohup.out’
17/09/09 05:26:54 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:26:54 INFO security.Groups: Group mapping impl=org.apache.hadoop.security.ShellBasedUnixGroupsMapping; cacheTimeout=300000
17/09/09 05:26:55 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:26:55 WARN conf.Configuration: mapred.task.id is deprecated. Instead, use mapreduce.task.attempt.id
17/09/09 05:26:55 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:26:55 WARN conf.Configuration: mapred.used.genericoptionsparser is deprecated. Instead, use mapreduce.client.genericoptionsparser.used
17/09/09 05:26:55 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:26:55 INFO input.FileInputFormat: Total input paths to process : 1
17/09/09 05:26:56 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:26:56 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:26:56 WARN conf.Configuration: mapred.map.tasks is deprecated. Instead, use mapreduce.job.maps
17/09/09 05:26:56 INFO mapreduce.JobSubmitter: number of splits:1
17/09/09 05:26:56 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:26:56 INFO mapreduce.JobSubmitter: adding the following namenodes' delegation tokens:null
17/09/09 05:26:56 INFO mapreduce.Job: Running job: job_201709090526_0001
17/09/09 05:26:56 INFO mapreduce.Job: Running job: job_201709090526_0002
17/09/09 05:26:57 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:26:57 INFO mapreduce.Job:  map 0% reduce 0%
17/09/09 05:27:12 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:27:15 INFO mapreduce.Job:  map 100% reduce 0%
17/09/09 05:27:18 INFO mapreduce.Job:  map 100% reduce 100%
17/09/09 05:27:20 INFO mapreduce.Job: Job complete: job_201709090526_0002
17/09/09 05:27:20 INFO mapreduce.Job: Counters: 33
	FileInputFormatCounters
		BYTES_READ=13836
	FileSystemCounters
		FILE_BYTES_READ=20127
		FILE_BYTES_WRITTEN=40286
		HDFS_BYTES_READ=13951
		HDFS_BYTES_WRITTEN=12625
	Shuffle Errors
		BAD_ID=0
		CONNECTION=0
		IO_ERROR=0
		WRONG_LENGTH=0
		WRONG_MAP=0
		WRONG_REDUCE=0
	Job Counters 
		Data-local map tasks=1
		Total time spent by all maps waiting after reserving slots (ms)=0
		Total time spent by all reduces waiting after reserving slots (ms)=0
		SLOTS_MILLIS_MAPS=10817
		SLOTS_MILLIS_REDUCES=4041
		Launched map tasks=1
		Launched reduce tasks=1
	Map-Reduce Framework
		Combine input records=3308
		Combine output records=1884
		Failed Shuffles=0
		GC time elapsed (ms)=0
		Map input records=180
		Map output bytes=25074
		Map output records=3308
		Merged Map outputs=1
		Reduce input groups=1884
		Reduce input records=1884
		Reduce output records=1884
		Reduce shuffle bytes=20127
		Shuffled Maps =1
		Spilled Records=3768
		SPLIT_RAW_BYTES=115
java.io.IOException: Call to /11.11.2.51:9001 failed on local exception: java.io.EOFException
	at org.apache.hadoop.ipc.Client.wrapException(Client.java:940)
	at org.apache.hadoop.ipc.Client.call(Client.java:908)
	at org.apache.hadoop.ipc.WritableRpcEngine$Invoker.invoke(WritableRpcEngine.java:198)
	at com.sun.proxy.$Proxy1.getTaskCompletionEvents(Unknown Source)
	at org.apache.hadoop.mapreduce.Job.getTaskCompletionEvents(Job.java:460)
	at org.apache.hadoop.mapreduce.Job.monitorAndPrintJob(Job.java:1025)
	at org.apache.hadoop.mapreduce.Job.waitForCompletion(Job.java:979)
	at org.apache.hadoop.examples.WordCount.main(WordCount.java:84)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ProgramDriver$ProgramDescription.invoke(ProgramDriver.java:72)
	at org.apache.hadoop.util.ProgramDriver.driver(ProgramDriver.java:144)
	at org.apache.hadoop.examples.ExampleDriver.main(ExampleDriver.java:68)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.RunJar.main(RunJar.java:192)
Caused by: java.io.EOFException
	at java.io.DataInputStream.readInt(DataInputStream.java:392)
	at org.apache.hadoop.ipc.Client$Connection.receiveResponse(Client.java:664)
	at org.apache.hadoop.ipc.Client$Connection.run(Client.java:602)
real 43.95
user 10.34
sys 5.96
