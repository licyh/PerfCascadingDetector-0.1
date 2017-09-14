project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end

bug_id=hd-5153


cd /tmp/${bug_id}_dm
grep -ri DynamicPoint > $project_dir/src/dm/ds/res/$bug_id/dynamicpoints.txt

cd $project_dir
cd src/dm/ds/res
python dataAnalysis.py $project_dir/src/dm/ds/res/$bug_id/dynamicpoints.txt $project_dir/src/dm/ds/res/$bug_id/dynamicpoints.out

