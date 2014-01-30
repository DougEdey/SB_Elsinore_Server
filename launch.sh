#!/bin/bash
echo "Starting Elsinore as $(whoami)"  
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
sudo /opt/jdk8/bin/java -jar $DIR/Elsinore.jar --config $DIR/elsinore.cfg $@
sleep 10

