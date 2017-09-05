# used after ca-6744_logging_end_sh2_node2.sh 
# JX - used in another shell, becuase "ca-6744_logging_begin.sh" need represent a shell
echo "JX - INFO - stop cassandra .."

jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
sleep 3s
jps

