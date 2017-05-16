# Input args
# $1 - bug_config_dir - new added
# $2 - spoon input dir
# $3 - classpath
# $4 - spooned dir
# $5 - output dir
#echo $1 $2 $3 $4 $5

# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector                                #JX - NO "/" at the end

# Compile prepare
cd $project_dir
ant compile-dt
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi


# Clear "spooned" for the results of Spooning
cd $project_dir
mkdir -p output
cd output
touch ForSpoonRunningNormally.java  #Spoon Bug #1208 - jx: before outputing "spooned", it needs to traverse the current working dir for *.java files, so if the working dir is bigger, Spoon is slow for each execution
if [ -d ./spooned ]; then
  rm -rf ./spooned
fi


# Walaing & Spooning
echo "JX - INFO - Spooning NOW .."
# set classpath for Walaing & Spooning
build_path=${project_dir}/build/classes/
spoon_path=${project_dir}/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar
#wala_path=${project_dir}/lib/sa/wala-1.3.8-jars/*
classpath=$build_path:$spoon_path
cd $project_dir              #JX: should be here!!!
java -cp $classpath dt.spoon.MySpoon $1 $2 $3 $4 $5
