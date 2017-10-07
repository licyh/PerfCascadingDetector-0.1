

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
