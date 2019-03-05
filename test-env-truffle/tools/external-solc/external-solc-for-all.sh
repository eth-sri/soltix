#!/bin/sh

if test "$#" != 2 || ! test -d "$1" || test -f "$2"; then
	echo "Usage: external-solc-for-all.sh [input-dir] [output-dir]"
	exit 1
fi

if ! test -d "$2"; then
	mkdir "$2"
fi 

RC=0
for input in `ls $1/*.sol`; do
	output="$2"/`basename $input | sed 's/\.sol/\.json/g'`
	if test `basename $input` = Migrations.sol; then
		continue
	fi
	echo $input = $output
	if ! `dirname $0`/external-solc.sh "$input" "$output"; then
		RC=1
	fi
done

exit "$RC"
