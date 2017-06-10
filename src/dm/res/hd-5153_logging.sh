
cd ~
start-dfs.sh
sleep 15s

hadoop fs -put ~/input/10K.file input

echo "waiting for 50s to end"
sleep 50s

stop-dfs.sh

echo "JX - INFO - DM's logging finished!!!"
