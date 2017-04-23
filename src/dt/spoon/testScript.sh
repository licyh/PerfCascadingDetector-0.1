
java cmd   
#就不进去cd ~/JXCascading-detector/bin/    #or ~/JXCascading-detector/build/classes/了，因为eclipse中也是当project根目录为当前目录
#(-cp couldn't include "~")
cd ~/JXCascading-detector/  
java -cp /root/JXCascading-detector/build/classes/:/root/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar dt.spoon.MySpoon
or
java -cp /root/JXCascading-detector/bin/:/root/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar dt.spoon.MySpoon



eclipse cmd
cmd: java -cp /root/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar spoon.Launcher -i ~/JXCascading-detector/src/dt/spoon/ --gui (--noclasspath)
runConfig: SpoonTest-gui

java -cp xx/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar:xx/JXCascading-detector/build/classes spoon.Launcher -i ~/hadoop-0.23.3-src -p dt.spoon.CatchProcessor



java -cp /root/JXCascading-detector/lib/dt/*:/root/JXCascading-detector/bin spoon.Launcher -i /root/hadoop-0.23.3-src/ -p dt.spoon.CatchProcessor



ant compile-dt
(errors)
java -cp /root/JXCascading-detector/lib/dt/*:/root/JXCascading-detector/build/classes spoon.Launcher -i /root/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client -p dt.spoon.CatchProcessor --noclasspath
(fine)
java -cp /root/JXCascading-detector/lib/dt/*:/root/JXCascading-detector/build/classes spoon.Launcher -i ~/JXCascading-detector/src/dt/spoon/ -p dt.spoon.CatchProcessor


java -cp /home/vagrant/JXCascading-detector/lib/dt/*:/home/vagrant/JXCascading-detector/build/classes:/home/vagrant/hadoopjars/* spoon.Launcher -i ~/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client 
-o withouted -p dt.spoon.CatchProcessor --no-copy-resources --noclasspath --level WARN