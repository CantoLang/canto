#!/bin/bash

usage()
{
    echo
    echo "Usage: make_release.sh [target_dir]"
    echo
}

if [[ $# == 0 ]]; then
    TARGET_DIR="."
elif [[ -d "$1" ]]; then
    TARGET_DIR="$1"
else
    usage
    exit 1
fi

SOURCE="${BASH_SOURCE[0]}"
cd "$( dirname "$SOURCE" )"


BRANCH=$(sed -n 's/.*\/.*\/\(.*\)/\1/p' .git/HEAD)
VERSION=$(canto scripts/utils/canto_version.canto)
if [[ "$BRANCH" == "master" ]]; then
    CANTO_RELEASE_FILE="$TARGET_DIR/canto-$VERSION.zip"
else
    CANTO_RELEASE_FILE="$TARGET_DIR/canto-$VERSION-$BRANCH.zip"
fi

function get_jetty () {
    mkdir -p lib
    curl https://repo1.maven.org/maven2/org/eclipse/jetty/aggregate/jetty-all/9.4.49.v20220914/jetty-all-9.4.49.v20220914-uber.jar --output lib/jetty-all.jar
}

function compile_all() {
    mkdir -p classes
    find -name "*.java" > sources.txt
    javac -d classes @sources.txt
}

function make_jar() {
    jar cvf lib/canto.jar -C classes canto -C classes cantocore bin *.sh LICENSE README.md
}

get_jetty
compile_all
make_jar
rm $CANTO_RELEASE_FILE
zip $CANTO_RELEASE_FILE lib/canto.jar lib/jetty-all.jar install.sh


