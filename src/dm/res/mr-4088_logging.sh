
cd ~
#start-dfs.sh
hadoop-daemon.sh start jobtracker 
hadoop-daemon.sh start tasktracker
sleep 3s
jps
sleep 2s

nohup hadoop jar ~/hadoop/install/hadoop-0.20.205.0/hadoop-examples-0.20.205.0.jar wordcount input/1K.file output/1K.file &
hadoop jar ~/hadoop/install/hadoop-0.20.205.0/hadoop-examples-0.20.205.0.jar wordcount input/10K.file output/10K.file

echo "waiting for 5s to end"
sleep 5s

#stop-dfs.sh
hadoop-daemon.sh stop tasktracker
hadoop-daemon.sh stop jobtracker
sleep 2s
jps

echo "JX - INFO - DM's logging finished!!!"
