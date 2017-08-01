# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end


bug_id=mr-4576
dm_dir=/tmp/${bug_id}_dm    #like "/tmp/mr-4576_dm"



# Stop hadoop so that NOT under logging
echo "JX - INFO - stop hadoop .."
stop-all.sh
if [ $? -ne 0 ]; then
  echo "stop-all.sh error"
  exit
fi
sleep 3s

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


