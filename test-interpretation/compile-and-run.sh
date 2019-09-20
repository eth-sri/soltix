#!/bin/sh

if test "$#" != 1; then
	echo "Usage: ./interpret.sh [contract-dir]"
	exit 1
fi

EVENTLOG=../test-env-truffle/_current-project/input/profiling-events.log

rm -f "$EVENTLOG"

if ../test-env-truffle/bin/run-one-test.sh "$1" 0 && test -f "$EVENTLOG"; then
	echo Event log: "$EVENTLOG"	
	exit 0
else
	echo Error: no event log "$EVENTLOG"
	exit 1
fi
