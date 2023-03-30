#!/bin/bash
#set -x

here="`pwd`"
scriptloc=`dirname $(realpath "$0")`
cd "$scriptloc/.."
APP_HOME="`pwd`"
cd "$here"
# profiling:
# -agentpath:/home/kiefer/src/external/honestprofiler/liblagent.so=interval=23,logPath=/home/kiefer/tmp/hfc-database-log.hpl

java -Dlogfile.name="/tmp/hfcdb.log" \
     -Dlog4j.configuration="file:$APP_HOME/log4j.properties" \
     -jar "$APP_HOME/target/hfc-server.jar" "$@"
