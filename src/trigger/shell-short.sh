

expect << EOF
set timeout -1

spawn hbase shell

expect "hbase(main):"
send "create 'test', 'data'\r"

expect eof

EOF
