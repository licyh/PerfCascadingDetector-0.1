
nodetool ring
sleep 1s

nodetool clean
sleep 1s

nodetool flush
sleep 3s

# Stop cassandra node2
echo "JX - INFO - stop cassandra node 2.."
jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
sleep 1s
jps
echo "JX - INFO - cassandra node2 DM finished."

