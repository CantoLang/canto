#!/bin/bash
#
# script to run canto in an appropriate fashion
#

usage()
{
    echo
    echo "Usage: ${0##*/} server {start|stop|run|restart|status} [server_args]"
    echo "   or: ${0##*/} scriptname [script_args]"
    echo
    exit 1
}

[ $# -gt 0 ] || usage

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

SERVICE_HOME=$DIR
SERVICE_NAME=$(basename $DIR)
CANTO_HOME=$DIR

if [[ "$1" == "server" ]]; then
    shift
    if [[ "$1" == "start" || "$1" == "stop" || "$1" == "status" || "$1" == "run"  || "$1" == "restart" ]]; then
        source ${DIR}/bin/cantoserver.sh "$@"
    else
        echo
        echo "Usage: ${0##*/} server {start|stop|run|restart|status}"
        echo
        exit 1
    fi
else 
    source ${DIR}/bin/cantoscript.sh "$@"
fi
