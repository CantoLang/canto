#!/bin/bash
#
# script to install this site in an appropriate fashion
#

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
EXECUTABLE_NAME=$(basename $DIR)

pushd $DIR >/dev/null

echo "Extracting jars..."
jar xf lib/canto.jar lib/jetty-all.jar lib/servlet-api-3.1.jar

echo "Extracting scripts..."
jar xf lib/canto.jar bin/cantoscript.sh bin/cantoserver.sh bin/generate_service.sh
jar xf lib/canto.jar canto.sh canto_site.sh
mv canto_site.sh $EXECUTABLE_NAME.sh
chmod +x bin/*.sh
chmod +x *.sh

if [[ -f /usr/local/bin/$EXECUTABLE_NAME ]]; then
    echo "/usr/local/bin/$EXECUTABLE_NAME already exists; not overwriting"
    echo
    echo "Installation complete."
elif [[ $EUID -eq 0 ]]; then
    echo "Setting up symlinks..."
    ln -s $DIR/$EXECUTABLE_NAME.sh /usr/local/bin/$EXECUTABLE_NAME
    echo
    echo "Installation complete. Type " $EXECUTABLE_NAME " to run."
else
    echo "Enter password to set up symlinks, or CTRL-C to exit."
    sudo ln -s $DIR/$EXECUTABLE_NAME.sh /usr/local/bin/$EXECUTABLE_NAME
    echo
    echo "Installation complete. Type " $EXECUTABLE_NAME " to run."
fi
 
popd >/dev/null
