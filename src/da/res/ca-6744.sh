# Please set the project home directory        
project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end

# Set the bug variables
log_dir=/tmp/ca-6744_dm


#scp 11.11.2.62:/tmp/ca-6744_dm/* /tmp/ca-6744_dm/
#sleep 1s



# Call real da
cd $project_dir/src/da
./da.sh $log_dir




