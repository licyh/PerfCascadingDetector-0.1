
cd ~
start-dfs.sh
sleep 20s
start-yarn.sh
sleep 3s
jps
sleep 2s

hadoop jar ~/hadoop/install/hadoop-0.23.3/share/hadoop/mapreduce/hadoop-mapreduce-examples-0.23.3.jar wordcount -Dmapred.reduce.tasks=1 input/1K.file output/1K.file-1red

echo "waiting for 5s to end"
sleep 3s

#stop-dfs.sh
stop-yarn.sh
stop-dfs.sh
sleep 3s
jps

echo "JX - INFO - DM's logging finished!!!"
