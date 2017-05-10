# Input args
# $1 - log_dir, like " /tmp/mr-4576_dm"

# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end

# Compile - prepare
cd $project_dir
ant compile-da
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi


# Cleanup - prepare
if [ -d $1-xml ]; then
  rm -rf $1-xml
fi


# Convert logging dir to xml
build_path=${project_dir}/build/classes/
classpath=$build_path
cd $project_dir
java -cp $classpath da.convert.TexttoXml $1 null null

# da
echo "JX - INFO - da NOW .."
build_path=${project_dir}/build/classes/
classpath=$build_path
cd $project_dir
java -Xmx18G -Xss5M -cp $classpath da.DynamicAnalysis $project_dir $1-xml
