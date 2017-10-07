

echo -e "\n JX - start HBase - ..."
start-hbase.sh
echo -e "start success - sleep seconds"
sleep 10s #5s
echo -e "start success - sleep seconds again"
sleep 10s
echo -e "JX - start HBase - end\n"

expect << EOF
set timeout -1

spawn hbase shell

expect "hbase(main):"
send "status 'detailed'\r"

expect "hbase(main):"
send "create 'test', 'data'\r"

expect "hbase(main):"
send "put 'test', 'r0', 'data:2', 'v2'\r"

expect "hbase(main):"
send "put 'test', 'r1', 'data:2', 'v2'\r" 

expect "hbase(main):"
send "put 'test', 'r2', 'data:2', 'v2'\r"

expect "hbase(main):"
send "split 'test', 'r1'\r"

expect eof

EOF
