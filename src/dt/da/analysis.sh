#Input args
# $1 - looplog of dir - like "/tmp/mr-4813_looplog"



# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end

cd $project_dir
ant compile-dt
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi


# Loop Analysis
echo "JX - INFO - Loop Analysis NOW .."
#set classpath
build_path=${project_dir}/build/classes/
classpath=$build_path
#cd $project_dir            
java -cp $classpath dt.da.Analysis $1

