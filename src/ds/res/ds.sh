project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end


cd /tmp/mr-4576_dm
grep -ri DynamicPoint > /tmp/dynamicpoints.txt

cd $project_dir
cd src/ds
python dataAnalysis.py

