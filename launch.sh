#!/bin/bash
echo "Starting Elsinore as $(whoami)"  
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA="$(which java)"

usage()
{
cat << EOF
usage $0 options

This script is used to launch elsinore server. It will prompt for root if required

OPTIONS:

	-h 	Show this message
	-j	Java Debug
	-p <port>	Port to run on
	-g <file>	GPIO Definitions File
	-c <config>	Config file (instead of elsinore.cfg)
	-d 	Debug logging
EOF
$JAVA -jar $DIR/Elsinore.jar --help
}

JAVA_OPTS=
PORT=
CONFIG=$DIR/elsinore.cfg
GPIO=
DEBUG=

while getopts ":hjp:c:g:d" OPTION
do
     case $OPTION in
         h)
             usage
             exit 1
             ;;
         j)
             JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,address=5050,server=y,suspend=y
             ;;
         p)
             PORT="--port $OPTARG"
             ;;
         c)
             CONFIG=$OPTARG
             ;;
         g)
             GPIO="--gpio_definitions $OPTARGS"
             ;;
	 d)
	     DEBUG="--d"
	     ;;
         ?)
             usage
             exit
             ;;
     esac
done

RUNTIME_OPTS="$PORT $GPIO $DEBUG"
RC=128
while [ $RC -eq 128 ] 
do
	sudo $JAVA $JAVA_OPTS -jar $DIR/Elsinore.jar --config $CONFIG $RUNTIME_OPTS 
	RC=$?
done

