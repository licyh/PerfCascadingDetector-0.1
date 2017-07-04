# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end

# Set the bug variables
log_dir=/home/vagrant/JXCascading-detector/input/MR-4813                          #/tmp/mr-4576_dm  


# Call real da
cd $project_dir/src/da
./da.sh $log_dir




