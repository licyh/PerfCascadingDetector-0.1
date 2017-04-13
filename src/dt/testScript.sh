
eclipse cmd
cmd: java -cp /root/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar spoon.Launcher -i ~/JXCascading-detector/src/dt/spoon/ --gui (--noclasspath)
runConfig: SpoonTest-gui

java -cp ~/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar:~/JXCascading-detector/build/classes spoon.Launcher -i ~/hadoop-0.23.3-src -p dt.spoon.CatchProcessor



java -cp /root/JXCascading-detector/lib/dt/*:/root/JXCascading-detector/bin spoon.Launcher -i /root/hadoop-0.23.3-src/ -p dt.spoon.CatchProcessor



ant compile-dt
(errors)
java -cp /root/JXCascading-detector/lib/dt/*:/root/JXCascading-detector/build/classes spoon.Launcher -i /root/hadoop-0.23.3-src/hadoop-mapreduce-project/hadoop-mapreduce-client -p dt.spoon.CatchProcessor --noclasspath
(fine)
java -cp /root/JXCascading-detector/lib/dt/*:/root/JXCascading-detector/build/classes spoon.Launcher -i ~/JXCascading-detector/src/dt/spoon/ -p dt.spoon.CatchProcessor
