project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end

bug_id=mr-4576


cd /tmp/mr-4576_dm
grep -ri DynamicPoint > $project_dir/src/ds/res/$bug_id/dynamicpoints.txt

cd $project_dir
cd src/ds/res
python dataAnalysis.py $project_dir/src/ds/res/$bug_id/dynamicpoints.txt $project_dir/src/ds/res/$bug_id/dynamicpoints.out

