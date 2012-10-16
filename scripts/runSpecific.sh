#!/bin/bash

#bash fail-fast
set -o errexit  
set -o errtrace 
set -o nounset  

if [ $# -ne 7 ]
then
  echo "Usage: `basename $0` {NOW} {jar file} {config file} {master hostname} {master port} {current hostname} {current port}"
  exit 65
fi

NOW=$1
JARFILE=$2
CONFIGFILE=$3
MASTERHOSTNAME=$4
MASTERPORT=$5
CURRENTHOSTNAME=$6
CURRENTPORT=$7

MASTER="$MASTERHOSTNAME $MASTERPORT"
CURRENT="$CURRENTHOSTNAME $CURRENTPORT"

echo "**** running at $CURRENT **** (master at $MASTER)"
./scripts/runSingle.sh $CURRENT $MASTER $NOW $JARFILE $CONFIGFILE &
