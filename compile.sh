#!/bin/bash
scriptdir=`dirname $0`
cd "$scriptdir"

#./run-thrift
mvn -U clean install
mvn -f apps.xml install
