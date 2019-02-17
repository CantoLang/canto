#!/bin/bash
#
# script to run the canto site this script is in
#

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

if [[ "$1" == "start" || "$1" == "stop" || "$1" == "status" || "$1" == "run"  || "$1" == "restart" ]]; then
    source ${DIR}/bin/cantoserver.sh "$@"
else
    echo
    echo "Usage: ${0##*/} {start|stop|run|restart|status}"
    echo
    exit 1
fi
