#!/bin/sh

if test "$#" != 1; then
	echo "Usage: ./test.sh [contract-dir]"
	exit 1
fi

# TODO fix path redundancy
EVENTLOG_INTERPRETATION="$1"/events.log
EVENTLOG_COMPILATION=../test-env-truffle/_current-project/input/profiling-events.log

if ./interpret.sh "$1" && ./compile-and-run.sh "$1"; then
	if ../test-env-truffle/tools/event-logs-equal.sh "$EVENTLOG_INTERPRETATION" "$EVENTLOG_COMPILATION"; then
		echo OK
		exit 0
	else
		echo DIFF
		exit 1
	fi
fi

