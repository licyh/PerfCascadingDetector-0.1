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


# Convert logging dir to xml
:<<tmp
if [ -d $1-xml ]; then
  rm -rf $1-xml
fi
build_path=${project_dir}/build/classes/
classpath=$build_path
cd $project_dir
java -cp $classpath da.convert.TexttoXml $1 null null
tmp


# da
echo "JX - INFO - da NOW .."
if [ -d $1-xmlresult ]; then
  rm -rf $1-xmlresult
fi
build_path=${project_dir}/build/classes/
classpath=$build_path
cd $project_dir
java -Xmx29G -Xss15M -cp $classpath da.DynamicAnalysis $project_dir $1-xml
