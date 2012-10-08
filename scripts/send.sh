set -o errexit  
set -o errtrace 
set -o nounset  

if [ $# -ne 3 ]
then
  echo "NOT TO BE USED WITHOUT sendAndRun2!"
  exit 65
fi

HOSTNAME=$1
JARFILE=$2
CONFIGFILE=$3

JARFILEB=`basename $JARFILE`
CONFIGFILEB=`basename $CONFIGFILE`

ROLLERCHAINPATH="~/rltest"

ssh $HOSTNAME "mkdir -p $ROLLERCHAINPATH"
scp -C $JARFILE $CONFIGFILE $HOSTNAME:$ROLLERCHAINPATH
