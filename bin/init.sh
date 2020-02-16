#!/bin/bash -xe
#
#  init.sh -- initialize newly provisioned server
#

echo "Initializing Canto Server"
echo "Running as $USER"

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

if [[ $( basename "$DIR" ) == "bin" ]]; then
    DIR="$( dirname $DIR )"
    echo "Now DIR is $DIR"
fi

PROJECT=$( basename "$DIR" )
CANTO_USER=canto

apt-get --assume-yes install openjdk-8-jdk

id -u $CANTO_USER > /dev/null
if [ $? -ne 0 ]
then
    echo "Adding user $CANTO_USER"
    useradd --user-group --shell /bin/nologin --home-dir /opt/$PROJECT $CANTO_USER
fi


cd $DIR
echo "Extracting jars..."
jar xf lib/canto.jar lib/jetty-all.jar lib/servlet-api-3.1.jar

echo "Extracting scripts..."
jar xf lib/canto.jar bin/cantoscript.sh bin/cantoserver.sh bin/generate_service.sh
jar xf lib/canto.jar canto.sh
chmod +x bin/*.sh
chmod +x *.sh

chown -R $CANTO_USER:$CANTO_USER $DIR
SERVICE_NAME=$PROJECT
. generate_service.sh
systemctl enable $SERVICE_NAME
systemctl start $SERVICE_NAME
echo $?
