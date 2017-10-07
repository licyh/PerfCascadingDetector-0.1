
echo -e "\nCleanup HBase - ...\n"

echo "kill HBase's threads - begin"
jps | grep Main | grep -v grep | awk '{print $1}' | xargs kill -9
jps | grep HQuorumPeer | grep -v grep | awk '{print $1}' | xargs kill -9
jps | grep HRegionServer | grep -v grep | awk '{print $1}' | xargs kill -9
jps | grep HMaster | grep -v grep | awk '{print $1}' | xargs kill -9
jps | grep ServiceManager | grep -v grep | awk '{print $1}' | xargs kill -9
jps
echo "kill HBase's threads - end"

sleep 1s

echo "initilize HBase - begin"
hadoop fs -rmr /hbase
hadoop fs -lsr /
rm -r /tmp/hbase-vagrant
rm ~/hbase/target/hbase-0.93-SNAPSHOT/hbase-0.93-SNAPSHOT/logs/*
sleep 1s
echo "initilize HBase - end"

echo -e "Cleanup HBase - end\n"

bash runServer.sh

