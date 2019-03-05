#!/bin/sh

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi


if test "$#" != 2; then
	echo instrument-contract.sh: Invalid number of arguments
	exit 1
fi

ORIGINAL_CONTRACT="$1"
INSTRUMENTED_CONTRACT="$2"

CWD=$PWD

echo args: "$@" > instrument-contract.out
echo running ./run-file.sh "$ORIGINAL_CONTRACT" --instrument --solidityOutput="$INSTRUMENTED_CONTRACT" >> instrument-contract.out
run-file.sh "$ORIGINAL_CONTRACT" --instrument --solidityOutput="$INSTRUMENTED_CONTRACT" >> instrument-contract.out
cd $CWD
exit 0
