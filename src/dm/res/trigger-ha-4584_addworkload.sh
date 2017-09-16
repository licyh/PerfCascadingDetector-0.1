cd ~
stop-dfs.sh
sleep 5s


# Start hadoop to clean up data
echo "JX - INFO - clean up HDFS data (need ensure started)"
start-dfs.sh
echo "JX - INFO - sleep 30s.."
sleep 50s
hadoop fs -lsr /
echo "JX - INFO - write a big file 10G?"
hadoop fs -mkdir /workload
hadoop fs -put ~/input/10G.file /workload/10G.file.new
hadoop fs -lsr /
echo "JX - INFO - sleep 30s.."
sleep 50s
hadoop fs -lsr /


# Stop hadoop so that NOT under logging
echo "JX - INFO - stop hadoop .."
stop-dfs.sh
if [ $? -ne 0 ]; then
  echo "stop-all.sh error"
  exit
fi

