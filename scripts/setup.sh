#!/bin/bash

#bash fail-fast
set -o errexit  
set -o errtrace 
set -o nounset  

if [ $# -ne 2 ]
then
  echo "Usage: `basename $0` {file with hosts} {jar folder}"
  exit 65
fi

FILENAME=$1
FOLDERNAME=$2

LIBSPATH="~/jarlibs"

HOSTNAME=""
PORT=""
MASTER=""
for TOKEN in `cat $FILENAME  | cut -f 1 -d" " | sort | uniq`
do
  ssh $TOKEN "mkdir -p $LIBSPATH"
  scp $FOLDERNAME/* $TOKEN:$LIBSPATH
done

