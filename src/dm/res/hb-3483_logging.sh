
cd ~
#start-dfs.sh
start-hbase.sh
sleep 3s
jps
sleep 2s


expect << EOF
set timeout -1

spawn hbase shell

expect "hbase(main):"
send "status 'detailed'\r"

expect "hbase(main):"
send "put 'test', 'r2', 'data:2', 'v2'\r"

expect "hbase(main):"
send "exit\r"

expect eof

EOF




echo "waiting for Xs to end"
sleep 2s

#stop-dfs.sh
stop-hbase.sh
sleep 2s
jps

echo "JX - INFO - DM's logging finished!!!"
