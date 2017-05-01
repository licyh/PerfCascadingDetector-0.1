# Please set the project home directory
#project_dir=/mnt/storage/jiaxinli/workspace/JXCascading-detector            #JX - NO "/" at the end
project_dir=/home/vagrant/JXCascading-detector

# Set the bug variables
bug_id="mr-4813_spoon"
app_src=/home/vagrant/spoonspace/hadoop-0.23.3-src         #JX - NO "/" at the end
app_lib=/home/vagrant/spoonspace/hadoop-jars
proto_src=/home/vagrant/spoonspace/proto                   

tmp_dir=/tmp/$bug_id
in_dir=/tmp/$bug_id/in/hadoop-0.23.3-src
out_dir=/tmp/$bug_id/out/hadoop-0.23.3-src


# FIRST Compile the original app source codes!
#cd $app_src
#./compile.sh > /dev/null 2>&1
#if [ $? -ne 0 ]; then
#  echo "compile src in in-folder error. Exit.."
#  exit
#fi


# Process:
# 1. copy app-src to a tmp folder as in and out
# 3. remove some special *.java that spoon doesn't support
# 4. run spoon
# 5. copy modified *.java to out-folder
# 6. compile the out-folder to verify

# 1. copy app-src to a tmp folder as in and out
echo "JX - INFO - copy app-src to a tmp folder as in and out"
if [ -d $tmp_dir ]; then
  rm -rf $tmp_dir
fi
mkdir -p $tmp_dir/in
mkdir -p $tmp_dir/out
cp -r $app_src $tmp_dir/in
cp -r $app_src $tmp_dir/out


# 3. remove some special *.java that spoon doesn't support
echo "JX - INFO - remove some special *.java that spoon doesn't support"
list="package-info.java PipesNonJavaInputFormat.java InputSampler.java AggregatedLogsBlock.java AggregatedLogsPage.java MRProtos.java MRServiceProtos.java"
list=$list" HSAdminRefreshProtocolProtos.java YarnProtos.java AppBlock.java"

for i in $list
do
  find $in_dir -name $i | xargs -l bash -c 'mv $0 $0_1'
done


# 4. run spoon
echo "JX - INFO - Call Spoon for running"
allJar=`find $app_lib -name "*.jar" | tr '\n' ':'`
allJar=$allJar`find $JAVA_HOME/lib -name "*.jar" | tr '\n' ':'`
#allJar=$allJar${in_dir}/hadoop-tools/hadoop-distcp/target/lib/zookeeper-3.4.2.jar
cd $project_dir/src/com/prepare/res
./pre_dm_run.sh $project_dir/src/com/prepare/res/mr-4813.config $in_dir $allJar $out_dir


# 5. copy modified *.java to out-folder
echo "JX - INFO - copy modified *.java to out-folder"
echo "     this is already done in Spooning"

# 6. copy *.proto for mr
echo "JX - INFO - copy *.proto for mr"
for i in `find $proto_src -name "*.proto"`
do
  name=$(basename "$i")
  tar=`find $out_dir -name $name`
  num=`find $out_dir -name $name | wc -l`
  if [ $num -ne 1 ]; then
    echo "Find multiple proto files: "$name
    continue
  fi
  cp $i $tar
done


# 7. compile out-dir to verify
cd $out_dir
#./compile.sh




