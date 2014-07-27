#!/bin/bash
echo "Starting Elsinore as $(whoami)"  
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA="$(which java)"
RC=128
while [ $RC -eq 128 ] 
do
	sudo $JAVA -jar $DIR/Elsinore.jar --config $DIR/elsinore.cfg $@
	RC=$?
done

