# Input args
# $1 - dir of jars

# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end

# Compile sa
cd $project_dir
ant compile-sa
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi

# Wala + RPC
echo "JX - INFO - Wala+RPC NOW .."
build_path=${project_dir}/build/classes/
wala_path=${project_dir}/lib/sa/wala-1.3.8-jars/*
classpath=$build_path:$wala_path
cd $project_dir        
java -cp $classpath sa.rpc.RPCFinder src/sa/res/mr-4813