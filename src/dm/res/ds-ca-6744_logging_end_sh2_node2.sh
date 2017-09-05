
#add in sh1
sleep 15s


echo "JX - INFO - waiting of cassandra node2 to start .."
sleep 40s

nodetool ring

sleep 30s

nodetool ring


echo "JX - INFO - do cleanup & flush on cassandra node 2.."
nodetool cleanup
#sleep 3s

#nodetool flush
sleep 5s

# Stop cassandra node2
echo "JX - INFO - stop cassandra node 2.."
jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
sleep 1s
jps
echo "JX - INFO - cassandra node2 DM finished."

