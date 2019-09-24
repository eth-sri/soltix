#!/bin/sh

if test "$#" = 0; then
	TESTDIR="./tests"
	echo Defaulting to test dir $TESTDIR 
elif test "$#" = 1; then
	TESTDIR="$1"
else
	echo "Usage: ./test.sh [contracts-dir]"
	exit 1
fi


for testcase in `ls -d "$TESTDIR"/*`; do
	if ! test -d "$testcase"; then
		# Assume metadata file
		continue
	fi

	printf "$testcase ... "
	if ./test.sh $testcase >/dev/null 2>&1; then
		echo OK
	else
		echo ERROR
	fi
done
