#!/bin/sh

if test "$#" != 2; then
	echo "Usage: run-background.sh [script.sh] [output-log-file]"
	exit 1
fi

nohup "$1" >"$2" 2>&1 &
