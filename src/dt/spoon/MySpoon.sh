#Usage of MySpoon
#NOTE: MAY NEED DO THE FOLLOWING CMD for the shell FIRST
#Under Linux: "tr -d "\r" < MySpoon.sh > newMySpoon.sh"

# Config/Env
workspace_dir=/home/vagrant                               	 #JX - NO "/" at the end. JXCascading-detector's parent path. 
spoon_input_dir=/home/vagrant/spoontest/hadoop-0.23.3-src    #JX - NO "/" at the end.
# Generated Env
project_dir=${workspace_dir}/JXCascading-detector            #JX - NO "/" at the end



# Copy out all of Hadoop-related jars
#cd ~/hadoop-0.23.3-src/hadoop-dist/target/hadoop-0.23.3/share/hadoop
#mkdir -p ~/hadoop-jars/
#find -name *.jar | xargs -i cp {} ~/hadoop-jars/

# Enter JXCascading-detector's home directory for the following work
cd ${project_dir}

# Compile dt 
ant compile-dt


# Spooning
# -classpath includes:
# 1. location of "dt.spoon.MySpoon" - /root/JXCascading-detector/build/classes/
# 2. location of "spoon jar" - /home/vagrant/JXCascading-detector/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar
# 3. location of dependencies of "target source codes" - /home/vagrant/hadoop-jars/*
# Spooning Test
#java -cp ${project_dir}/build/classes/:${project_dir}/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar dt.spoon.MySpoon
# Real Spooning
java -cp ${project_dir}/build/classes/:${project_dir}/lib/dt/spoon-core-5.5.0-jar-with-dependencies.jar:${workspace_dir}/hadoop-jars/* dt.spoon.MySpoon ${spoon_input_dir}
