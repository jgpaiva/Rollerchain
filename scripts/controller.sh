#!/bin/bash

#bash fail-fast
set -o errexit  
set -o errtrace 
set -o nounset  

if [ $# -ne 5 ]
then
  echo "Usage: `basename $0` {filename with hostlist} {jar file} {config file} {NOW string} {host list}"
  exit 65
fi

FILENAME=$1
JARFILE=$2
CONFIGFILE=$3
NOW=$4
HOSTLIST=$5


JAVACLASS="inescid.gsd.centralizedrollerchain.controller.Controller"
LIB1="jarlibs/netty-3.5.7.Final-sources.jar"
LIB2="jarlibs/netty-3.5.7.Final.jar"
HOMEFOLDER="/Users/jgpaiva/tmp/realimpl"
JARFILEB=`basename $JARFILE`
FILENAMEB=`basename $FILENAME`

java -cp "$JARFILEB:$HOMEFOLDER/$LIB1:$HOMEFOLDER/$LIB2" $JAVACLASS $FILENAMEB $HOSTLIST $NOW 1>out.out 2>out.err
