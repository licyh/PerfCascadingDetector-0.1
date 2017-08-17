# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end


bug_id=mr-4813
dm_dir=/tmp/mr-4813_dm    #like "/tmp/mr-4576_dm"


# for Start, it may need to log
mkdir -p $dm_dir


# Start hadoop
echo "JX - INFO - start HDFS+MR to see if everything is OK"
start-dfs.sh
sleep 2s
start-yarn.sh
sleep 5s
hdfs dfsadmin -safemode leave
sleep 2s
hdfs dfs -rm -r /tmp
hdfs dfs -rm -r output
sleep 1s
hdfs dfs -ls -R /
jps
sleep 2s

# Stop hadoop so that NOT under logging
echo "JX - INFO - stop hadoop yarn .."
stop-yarn.sh
stop-dfs.sh
if [ $? -ne 0 ]; then
  echo "stop xxx error"
  exit
fi
sleep 2s
jps
sleep 1s
rm ~/hadoop/install/hadoop-0.23.3/logs/*




# Clean
echo "JX - INFO - clean $dm_dir .."
if [ -d $dm_dir ]; then
  rm -rf $dm_dir
fi
mkdir $dm_dir


# Compile
echo "JX - INFO - compile jar-dm-$bug_id .."
cd $project_dir
ant jar-ds-$bug_id
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi


