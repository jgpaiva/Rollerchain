#!/bin/bash

#bash fail-fast
set -o errexit  
set -o errtrace 
set -o nounset  

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` {filename with hostlist} {jar file} {config
  file}"
  exit 65
fi

FILENAME=$1
JARFILE=$2
CONFIGFILE=$3


HOSTNAME=""
PORT=""
MASTER=""
NOW=`date +%m%d%H%M`
for TOKEN in `cat $FILENAME`
do
  if [ "$HOSTNAME" = "" ]
  then
    HOSTNAME=$TOKEN
  else
    PORT=$TOKEN
    CURRENT="$HOSTNAME $PORT"
    if [ "$MASTER" = "" ]
    then
      MASTER=$CURRENT
      echo "MASTER at $CURRENT"
    fi
    HOMEFOLDER=`ssh $HOSTNAME dirs -l`
    
    ./scripts/sendAndRunSingle.sh $CURRENT $MASTER $NOW $JARFILE $CONFIGFILE &

    HOSTNAME=""
    PORT=""
  fi
done
