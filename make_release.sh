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

function make_jar() {
    jar cvf lib/canto.jar -C classes canto -C classes cantocore bin *.sh LICENSE README.md
}

#make_jar
echo $CANTO_RELEASE_FILE


