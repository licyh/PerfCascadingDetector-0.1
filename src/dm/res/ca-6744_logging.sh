
cd ~
cassandra
jps
sleep 3s
jps
sleep 2s

#nohup xxxxx &

echo "waiting for 10s to end"
sleep 20s

#stop-dfs.sh
echo "stoping cassandra node ..."
jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
#nodetool drain
sleep 2s
jps

echo "JX - INFO - DM's logging finished!!!"
