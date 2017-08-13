# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end


bug_id=hb-3483
dm_dir=/tmp/${bug_id}_dm    #like "/tmp/mr-4576_dm"


# for Start, it may need to log
mkdir -p $dm_dir


# Start HBase
echo "JX - INFO - start HBase to see if everything is OK"
start-hbase.sh
sleep 5s

expect << EOF
set timeout -1

spawn hbase shell

expect "hbase(main):"
send "status 'detailed'\r"

expect "hbase(main):"
send "disable 'test'\r"

expect "hbase(main):"
send "drop 'test'\r"


expect "hbase(main):"
send "create 'test', 'data'\r"

expect "hbase(main):"
send "put 'test', 'r0', 'data:2', 'v2'\r"

expect "hbase(main):"
send "put 'test', 'r1', 'data:2', 'v2'\r" 

expect "hbase(main):"
send "scan 'test'\r" 

expect "hbase(main):"
send "exit\r" 

expect eof

EOF

sleep 1s

# Stop hadoop so that NOT under logging
echo "JX - INFO - stop hbase .."
stop-hbase.sh
#jps | grep HMaster | awk '{print $1}' | xargs kill -9
#jps | grep HRegionServer | awk '{print $1}' | xargs kill -9
#jps | grep HQuorumPeer | awk '{print $1}' | xargs kill -9
if [ $? -ne 0 ]; then
  echo "stop xxx error"
  exit
fi
sleep 1s
jps | grep HQuorumPeer | awk '{print $1}' | xargs kill -9
sleep 1s
jps
sleep 1s

rm ~/hbase-0.90.0/logs/*

# Clean
echo "JX - INFO - clean $dm_dir .."
if [ -d $dm_dir ]; then
  rm -rf $dm_dir
fi
mkdir $dm_dir


# Compile
echo "JX - INFO - compile jar-dm-$bug_id .."
cd $project_dir

ant jar-dm-$bug_id

if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi


