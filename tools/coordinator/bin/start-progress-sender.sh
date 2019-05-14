#!/bin/sh

SENDER_JAR=`dirname $0`/../sender/target/sender-1.0-SNAPSHOT-jar-with-dependencies.jar


if ! test -f "$SENDER_JAR"; then
	echo Error: jar file $SENDER_JAR does not exist
	exit 1
fi

"$JAVA_HOME"/bin/java -jar "$SENDER_JAR" "$@"


