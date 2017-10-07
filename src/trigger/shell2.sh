


expect << EOF

set timeout -1
spawn hbase shell

expect "hbase(main):"
send "alter 'test', METHOD => 'table_att', MAX_FILESIZE => '134217728'\r"

expect eof

EOF
