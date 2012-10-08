set -o errexit  
set -o errtrace 
set -o nounset  

if [ $# -ne 7 ]
then
  echo "NOT TO BE USED WITHOUT sendAndRunSingle2!"
  exit 65
fi

HOSTNAME=$1
PORT=$2
MASTERHOSTNAME=$3
MASTERPORT=$4
NOW=$5
JARFILE=$6
CONFIGFILE=$7

CURRENT="$HOSTNAME $PORT"
MASTER="$MASTERHOSTNAME $MASTERPORT"

JARFILEB=`basename $JARFILE`
CONFIGFILEB=`basename $CONFIGFILE`

JAVACLASS="inescid.gsd.centralizedrollerchain.application.keyvalue.test.DistributedTest"
LIB1="jarlibs/netty-3.5.7.Final-sources.jar"
LIB2="jarlibs/netty-3.5.7.Final.jar"

ROLLERCHAINPATH="~/rltest"

HOMEFOLDER=`ssh $HOSTNAME dirs -l`

MYFOLDER=${NOW}_$PORT

ssh $HOSTNAME "mkdir -p $ROLLERCHAINPATH/$MYFOLDER/ ; cd $ROLLERCHAINPATH/$MYFOLDER ; cp $ROLLERCHAINPATH/$JARFILE . ; cp $ROLLERCHAINPATH/$CONFIGFILE . ;  java -cp \"$JARFILEB:$HOMEFOLDER/$LIB1:$HOMEFOLDER/$LIB2\" $JAVACLASS $CURRENT $MASTER 1>out.out 2>out.err &"
