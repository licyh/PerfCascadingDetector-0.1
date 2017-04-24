#Usage of MySpoon

# Compile dt 
cd ~/JXCascading-detector/
ant compile-dt

# Copy out all of Hadoop-related jars
#cd ~/hadoop-0.23.3-src/hadoop-dist/target/hadoop-0.23.3/share/hadoop
#mkdir -p ~/hadoop-jars/
#find -name *.jar | xargs -i cp {} ~/hadoop-jars/

# Spooning
# -classpath includes:
# 1. location of "dt.spoon.MySpoon" - /root/JXCascading-detector/build/classes/
# 2. location of "spoon jar" - /home/vagrant/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar
# 3. location of dependencies of "target source codes" - /home/vagrant/hadoop-jars/*
#java -cp /root/JXCascading-detector/build/classes/:/root/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar dt.spoon.MySpoon xxx
cd ~
java -cp /home/vagrant/JXCascading-detector/build/classes/:/home/vagrant/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar:/home/vagrant/hadoop-jars/* dt.spoon.MySpoon /home/vagrant/spoontest/hadoop-0.23.3-src