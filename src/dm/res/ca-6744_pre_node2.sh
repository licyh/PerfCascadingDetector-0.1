# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end


bug_id=ca-6744
dm_dir=/tmp/${bug_id}_dm    #like "/tmp/mr-4576_dm"


# for Start, it may need to log
mkdir -p $dm_dir


# Stop hadoop so that NOT under logging
echo "JX - INFO - stop cassandra(try nodetool decommission first) .."
nodetool decommission
sleep 3s
jps | grep CassandraDaemon | awk '{print $1}' | xargs kill -9
sleep 3s
jps


#remove log & data
sudo rm -rf /var/log/cassandra/*
#only delete at the second node
sudo rm -rf /var/lib/cassandra/*


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


