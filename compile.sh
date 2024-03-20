#!/bin/bash
run-thrift
mvn -U clean install
mvn -f apps.xml install
