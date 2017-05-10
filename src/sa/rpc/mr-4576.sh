# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end

bug_id=mr-4576
jars_dir=$project_dir/src/sa/res/mr-4576


# Call real RPC Finder
cd $project_dir/src/sa/rpc
./rpcfinder.sh $jars_dir 