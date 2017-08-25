# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end


bug_id=ca-6744
dm_dir=/tmp/${bug_id}_dm    #like "/tmp/mr-4576_dm"


# for Start, it may need to log
mkdir -p $dm_dir


# Stop & clean everything of Cassandra
echo "JX - INFO - stop cassandra .."
jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
sleep 3s
jps
#remove all log & data
sudo rm -rf /var/log/cassandra/*
sudo rm -rf /var/lib/cassandra/*
sleep 2s



#start & deploy everything of Cassandra & stop
echo "JX - INFO - start & deploy & stop cassandra .."
cassandra
sleep 70s
echo "JX - INFO - begin to write data to cassandra.. this should be helpful for the the following first cmd to success"

expect << EOF
set timeout -1

spawn cqlsh

expect ">"
send "CREATE SCHEMA schema1 WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };\r"

expect ">"
send "USE schema1;\r"

expect ">"
send "CREATE TABLE users (user_id varchar PRIMARY KEY, first varchar, last varchar, age int);\r"

expect ">"
send "INSERT INTO users (user_id, first, last, age) VALUES ('jsmith', 'John', 'Smith', 42);\r"

expect ">"
send "INSERT INTO users (user_id, first, last, age) VALUES ('adfsdf', 'xsdJohn', 'Smfith', 42);\r"

expect ">"
send "INSERT INTO users (user_id, first, last, age) VALUES ('dfsf', 'Johdsfn', 'Ssmdith', 42);\r"

expect ">"
send "create index xxx_index on schema1.users(first);\r"

expect ">"
send "SELECT * FROM users;\r"

expect ">"
send "exit;\r"

expect eof

EOF

sleep 7s
nodetool flush
sleep 3s
jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
sleep 2s
jps



sudo rm -rf /var/log/cassandra/*

# Clean for dm
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


