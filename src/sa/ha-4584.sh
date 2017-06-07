# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end

bug_id=ha-4584
jars_dir=$project_dir/src/sa/res/ha-4584


# Call real RPC Finder
echo "JX - INFO - call staticanalysis.sh .."
cd $project_dir/src/sa
./staticanalysis.sh $jars_dir 
