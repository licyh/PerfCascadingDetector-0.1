

expect << EOF
set timeout -1

spawn hbase shell

expect "hbase(main):"
send "split 'test', 'r1'\r"

expect eof

EOF
