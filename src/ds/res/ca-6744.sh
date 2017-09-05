project_dir=/home/vagrant/JXCascading-detector          #JX - NO "/" at the end

bug_id=ca-6744


scp 11.11.2.62:/tmp/ca-6744_dm/* /tmp/ca-6744_dm/
sleep 1s


cd /tmp/${bug_id}_dm
grep -ri DynamicPoint > $project_dir/src/ds/res/$bug_id/dynamicpoints.txt

cd $project_dir
cd src/ds/res
python dataAnalysis.py $project_dir/src/ds/res/$bug_id/dynamicpoints.txt $project_dir/src/ds/res/$bug_id/dynamicpoints.out

