# Please set the project home directory
project_dir=/home/vagrant/JXCascading-detector             #JX - NO "/" at the end


# Clean the dir of results - remove /tmp/mr-4813_looplog-xml
rm -rf /tmp/mr-4813_looplog-xml


# Call real analysis
cd $project_dir/src/dt/da
./analysis.sh /tmp/mr-4813_looplog

