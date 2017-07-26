
cd ~
cassandra
jps
sleep 5s
nodetool ring
sleep 2s

#nohup xxxxx &
:<<note
echo "waiting for 10s to end"
sleep 20s

#stop-dfs.sh
echo "stoping cassandra node ..."
jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
#nodetool drain
sleep 2s
jps
note
echo "JX - INFO - DM's logging finished!!!"
