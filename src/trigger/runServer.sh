#ant jar-server

logback_conf=./config

java -classpath ./build/Server.jar:${logback_conf} edu.uchicago.sg.dcbt.ServiceManager ./config/server/config
