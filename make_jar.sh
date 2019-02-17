#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
cd "$( dirname "$SOURCE" )"

jar cvf lib/canto.jar -C classes canto -C classes cantocore bin *.sh LICENSE README.md

