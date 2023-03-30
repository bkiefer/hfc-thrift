#!/bin/bash

here="`pwd`"
scriptloc=`dirname $(realpath "$0")`
cd "$scriptloc/.."
APP_HOME="`pwd`"
cd "$here"

if test -z "$CLIENT_LOG"; then
    CLIENT_LOG="/tmp/hfc_client.log"
fi

java -Dlogfile.name="$CLIENT_LOG" \
     -Dlog4j.configuration="file:$APP_HOME/log4j.properties" \
     -jar "$APP_HOME/target/hfc-client.jar" "$@"
