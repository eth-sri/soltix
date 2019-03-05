#!/bin/sh

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

if test "$#" != 2; then
	echo "Usage: ./event-logs-equal.sh [first profiling-events.log] [second profiling-events.log]"
	echo "Note that absolute input paths are expected"
	exit 1
fi

#echo running event logs on $1 and $2

run-soltix.sh --no-input-code --testEventLogEquivalence="$1"="$2" 
exit $?
