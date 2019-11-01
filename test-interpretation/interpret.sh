#!/bin/sh

if test "$#" != 1; then
	echo "Usage: ./interpret.sh [contract-dir]"
	exit 1
fi

EVENTLOG="$1"/events.log

rm -f "$EVENTLOG"

# TODO fix all path dependencies here and in related scripts (../tools/stmt_..)
if ../soltix/bin/interpret.sh "$1" && test -f "$EVENTLOG"; then
	echo Event log: "$EVENTLOG" 
	exit 0
else
	echo Error: no event log "$EVENTLOG"
	exit 1
fi
