#!/bin/sh


usage() {
	echo "Usage: "
	echo "Supply code generation arguments (in generate-contract.sh format)"
	echo "followed by execution arguments (in run-one-test.sh format):"
        echo "       generate-and-run-contract.sh [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [result-directory] [--assignmentSequence|--complete] [mutations-count | c0 | optimize=<runs>[,yul|,standard]]"
        exit 1
}

if test "$#" != 8; then
	echo generate-and-run-contract: Invalid argument count $#
	usage
fi

SETTINGS=`dirname $0`/../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

DIR="$6"
LOG="_generate-contract.log"
if generate-contract.sh $1 $2 $3 $4 $5 $6 $7 >"$LOG" 2>&1; then
	run-one-test.sh "$DIR" $8
	STATUS=$?
else
	STATUS=$?
	grep Error "$LOG"
	echo See "$LOG" for detailed log output
fi
exit $STATUS
