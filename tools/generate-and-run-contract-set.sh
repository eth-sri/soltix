#!/bin/sh


usage() {
	echo "Usage: "
	echo "Supply code generation arguments (in generate-contract-set.sh format)"
	echo "followed by execution arguments (in run-all-tests.sh format):"
        echo "       generate-and-run-contract-set.sh [contract-count] [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [result-directory] [--assignmentSequence|--complete] [mutations-count | c0 | optimize=<runs>[,yul|,standard]]"
        exit 1
}

if test "$#" != 9; then
	echo generate-and-run-contract-set: Invalid argument count $#
	echo "$@"
	usage
fi

SETTINGS=`dirname $0`/../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

DIR="$7"
LOG="_generate-contract.log"

report-progress.sh status on

if generate-contract-set.sh $1 $2 $3 $4 $5 $6 $7 $8; then
	run-all-tests.sh "$DIR" $9
	STATUS=$?
else
	STATUS=$?
	grep Error "$LOG"
	echo See "$LOG" for detailed log output
fi

report-progress.sh status off



exit $STATUS
