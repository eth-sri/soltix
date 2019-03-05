#!/bin/sh

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi


CWD="$PWD"

CMD="run-file.sh"
if test "$1" = "--no-input-code"; then
	# Some non-Solidity-input soltix application function
	CMD="run.sh"
	INPUT_FILE=""
	shift	
elif test "$#" -lt 1 || ! test -f "$1"; then
	echo "Error: First argument $1 isn't contract file"
	exit 1
else
	if ! cd /tmp || ! test -f "$1"; then
		# Not an absolute path
		INPUT_FILE="$CWD/$1"
	else
		INPUT_FILE="$1"
	fi
	shift
	cd "$CWD"
fi

echo running "$CMD" "$INPUT_FILE" "$@"
# Note: $INPUT_FILE may be 1 or 0 args

"$CMD" $INPUT_FILE "$@"

STATUS=$?
cd "$CWD"
exit $STATUS
