#!/bin/sh

for testcase in `ls -d tests/*`; do
	printf "$testcase ... "
	if ./test.sh $testcase >/dev/null 2>&1; then
		echo OK
	else
		echo ERROR
	fi
done
