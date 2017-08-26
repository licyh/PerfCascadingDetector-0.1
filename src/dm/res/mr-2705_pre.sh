# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end


bug_id=mr-2705
dm_dir=/tmp/${bug_id}_dm    #like "/tmp/mr-4576_dm"


# for Start, it may need to log
mkdir -p $dm_dir


# Start hadoop
echo "JX - INFO - start HDFS+MR to see if everything is OK"
start-dfs.sh
sleep 5s
hadoop dfsadmin -safemode leave
sleep 2s
hadoop fs -rmr output
hadoop fs -lsr /
jps
sleep 2s

# Stop hadoop so that NOT under logging
echo "JX - INFO - stop hadoop .."
hadoop-daemon.sh stop tasktracker
hadoop-daemon.sh stop jobtracker
#stop-dfs.sh
if [ $? -ne 0 ]; then
  echo "stop xxx error"
  exit
fi
sleep 3s
jps
sleep 1s

#rm ~/hadoop-0.20.204.0/logs/*
rm ~/hadoop-0.21.0/logs/*

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


