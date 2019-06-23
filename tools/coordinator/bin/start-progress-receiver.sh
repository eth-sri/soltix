#!/bin/sh

RECEIVER_JAR=`dirname $0`/../receiver/target/receiver-1.0-SNAPSHOT-jar-with-dependencies.jar

if ! test -f "$RECEIVER_JAR"; then
	echo Error: jar file $RECEIVER_JAR does not exist
	exit 1
fi

java -jar "$RECEIVER_JAR" "$@"

