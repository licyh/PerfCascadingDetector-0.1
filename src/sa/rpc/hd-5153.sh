# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end

bug_id=hd-5153
jars_dir=$project_dir/src/sa/res/hd-5153


# Call real RPC Finder
echo "JX - INFO - call rpcfinder.sh .."
cd $project_dir/src/sa/rpc
./rpcfinder.sh $jars_dir 
