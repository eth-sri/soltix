#!/bin/sh

echo run with "$@" >/tmp/fooooooooooooo

if test "$#" != 2; then
	echo "Usage:  stmt-injection-prepare input-file.sol output-file.sol"
	exit 1
fi

# Empty all functions (returning dummy values where needed) to
#    - still have correctly typed declarations
#    - speed up processing of that reduced file
../soltix/bin/run-file.sh "$1"  --reduceFunctions=all --solidityOutput="$2"
