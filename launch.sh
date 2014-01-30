#!/bin/bash
echo "Starting Elsinore as $(whoami)"  
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA="$(which java)"
sudo $JAVA -jar $DIR/Elsinore.jar --config $DIR/elsinore.cfg $@
sleep 10

