
cd ~
#start-dfs.sh
hadoop-daemon.sh start jobtracker 
hadoop-daemon.sh start tasktracker
sleep 3s
jps
sleep 2s


hadoop jar ~/hadoop/install/hadoop-1.0.0/hadoop-examples-1.0.0.jar wordcount -files hdfs://11.11.1.111:9000/sidefiles/1K.sidefile input/1K.file output/1K.file


echo "waiting for 2s to end"
sleep 2s

#stop-dfs.sh
hadoop-daemon.sh stop tasktracker
hadoop-daemon.sh stop jobtracker
sleep 2s
jps

echo "JX - INFO - DM's logging finished!!!"
