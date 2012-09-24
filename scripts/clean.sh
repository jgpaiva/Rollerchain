#!/bin/bash

#bash fail-fast
#set -o errexit  
#set -o errtrace 
#set -o nounset  

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` {file with hosts}"
  exit 65
fi

FILENAME=$1

HOSTNAME=""
PORT=""
MASTER=""
for TOKEN in `cat $FILENAME  | cut -f 1 -d" " | sort | uniq`
do
  ssh $TOKEN "killall java"
  ssh $TOKEN "rm -rf ~/rltest"
done

