# Please set the project home directory
project_dir=/mnt/storage/jiaxinli/workspace/JXCascading-detector            #JX - NO "/" at the end


# Compile prepare
cd $project_dir
ant compile-prepare
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
wala_path=${project_dir}/lib/sa/wala-1.3.8-jars/*
spoon_path=${project_dir}/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar
classpath=$build_path:$wala_path:$spoon_path

#$1: config file
#$2: allJar str
#$3: src path
java -cp $classpath com.prepare.PreDM $1 -i $3 --source-classpath $2 --output-type compilationunits

