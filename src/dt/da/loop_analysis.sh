# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end

cd $project_dir
ant compile-dt
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi


:<<tmp
cd $project_dir
mkdir -p output
cd output
touch ForSpoonRunningNormally.java  #Spoon Bug #1208 - jx: before outputing "spooned", it needs to traverse the current working dir for *.java files, so if the working dir is bigger, Spoon is slow for each execution
if [ -d ./spooned ]; then
  rm -rf ./spooned
fi
tmp





# remove /tmp/mr-4813_looplog-xml
rm -rf /tmp/mr-4813_looplog-xml


# Loop Analysis
echo "JX - INFO - Loop Analysis NOW .."
#set classpath
build_path=${project_dir}/build/classes/
classpath=$build_path
#cd $project_dir            
java -cp $classpath dt.da.Analysis /tmp/mr-4813_looplog

