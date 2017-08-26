
cd ~
#start-dfs.sh
#hadoop-daemon.sh start jobtracker             #wrong entrance for 0.21.0's logging 
#hadoop-daemon.sh start tasktracker
start-mapred.sh
sleep 3s
jps
sleep 2s


#nohup hadoop jar ~/hadoop/install/hadoop-0.20.204.0/hadoop-examples-0.20.204.0.jar wordcount -files hdfs://11.11.2.51:9000/sidefiles/1K.sidefile,input/10K.file input/1K.file output/1K.file &
#hadoop jar ~/hadoop/install/hadoop-0.20.204.0/hadoop-examples-0.20.204.0.jar wordcount -files hdfs://11.11.2.51:9000/sidefiles/10K.sidefile,input/1K.file input/10K.file output/10K.file

nohup hadoop jar ~/hadoop/install/hadoop-0.21.0/hadoop-mapred-examples-0.21.0.jar wordcount -files hdfs://11.11.2.51:9000/sidefiles/1K.sidefile,input/10K.file input/1K.file output/1K.file &
hadoop jar ~/hadoop/install/hadoop-0.21.0/hadoop-mapred-examples-0.21.0.jar wordcount -files hdfs://11.11.2.51:9000/sidefiles/10K.sidefile,input/1K.file input/10K.file output/10K.file



echo "waiting for 2s to end"
sleep 2s

#stop-dfs.sh
hadoop-daemon.sh stop tasktracker
hadoop-daemon.sh stop jobtracker
sleep 2s
jps

echo "JX - INFO - DM's logging finished!!!"
