# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end

# Set the bug variables
bug_id=mr-4576
bug_config_dir=${project_dir}/src/dt/spoon/res/mr-4576
app_src=/home/vagrant/spoonspace/hadoop-1.0.0         #JX - NO "/" at the end
app_lib=/home/vagrant/spoonspace/hadoop-jars
#proto_src=/home/vagrant/spoonspace/proto                   
tmp_dir=/tmp/mr-4576_spoon
in_dir=$tmp_dir/in/hadoop-1.0.0
spooned_dir=$tmp_dir/spooned
out_dir=$tmp_dir/out/hadoop-1.0.0



# Process:
# 1. copy app-src to a tmp folder as in and out
# 3. remove some special *.java that spoon doesn't support
# 4. run spoon
# 5. copy modified *.java to out-folder
# 6. compile the out-folder to verify

# 1. copy app-src to a tmp folder as in and out
#:<<tmp
echo "JX - INFO - copy app-src to a tmp folder as in and out"
if [ -d $tmp_dir ]; then
  rm -rf $tmp_dir
fi
mkdir -p $tmp_dir/in
mkdir -p $tmp_dir/out
cp -r $app_src $tmp_dir/in
cp -r $app_src $tmp_dir/out
#tmp

# 3. remove some special *.java that spoon doesn't support
echo "JX - INFO - remove some special *.java that spoon doesn't support"
list="xxx1 xxx2"
list=$list" xxx3"

for i in $list
do
  find $in_dir -name $i | xargs -l bash -c 'mv $0 $0_1'
done


# 4. run spoon
echo "JX - INFO - Call Spoon for running"
alljars=`find $app_lib -name "*.jar" | tr '\n' ':'`
allJars=$alljars`find $JAVA_HOME/lib -name "*.jar" | tr '\n' ':'`
#allJar=$allJar${in_dir}/hadoop-tools/hadoop-distcp/target/lib/zookeeper-3.4.2.jar
cd $project_dir/src/dt/spoon
./myspoon.sh $bug_config_dir $in_dir $alljars $spooned_dir $out_dir


# 5. copy modified *.java to out-folder
echo "JX - INFO - copy modified *.java to out-folder. This is already done in Spooning"
echo "JX - INFO - spoon finished, the result is in" $out_dir





