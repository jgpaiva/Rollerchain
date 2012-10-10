set -o errexit  
set -o errtrace 
set -o nounset  

if [ $# -ne 1 ]
then
  echo "usage: `basename $0` {filename with hostlist} "
  exit 65
fi

FILENAME=$1

for TOKEN in `cat $FILENAME  | cut -f 1 -d" " | sort | uniq`
do
  echo "**** sending to $TOKEN ****"
  ./scripts/send.sh $TOKEN $JARFILE $CONFIGFILE
done
