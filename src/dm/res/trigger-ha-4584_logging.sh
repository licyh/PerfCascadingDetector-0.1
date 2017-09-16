
cd ~
stop-dfs.sh
sleep 5s



start-dfs.sh
sleep 15s

:<<note
hadoop fs -put ~/input/10K.file input
sleep 20s

hadoop fs -put ~/input/100K.file input
sleep 20s
note

hadoop fs -put ~/input/10K.file input

echo "waiting for 50s to end"
sleep 50s


:<<note
stop-dfs.sh
note


echo "JX - INFO - DM's logging finished!!!"
