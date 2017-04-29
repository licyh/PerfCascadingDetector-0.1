# Please set the project home directory
project_dir=/mnt/storage/jiaxinli/workspace/JXCascading-detector            #JX - NO "/" at the end


# Compile prepare
cd ${project_dir}
ant compile-prepare
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi


# Clear "spooned" for the results of Spooning
cd ${project_dir}/src/com/prepare/res
if [ -d ./spooned ]; then
  rm -rf ./spooned
fi


# Walaing & Spooning
build_path=${project_dir}/build/classes/
wala_path=${project_dir}/sa/wala-1.3.8-jars/*
spoon_path=${project_dir}/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar
class_path=$build_path:$wala_path:$spoon_path

#$1: config file
#$2: allJar str
#$3: src path
#java -cp ./:${spoon_lib}:${wala_cp} com.prepare.PreDM $1 -i $src_path --source-classpath $allJar --output-type compilationunits
java -cp $class_path com.prepare.PreDM $1 -i $3 --source-classpath $2 --output-type compilationunits