#!/bin/sh

#if test "$#" = 1; then
#	:
#elif test "$#" = 2; then
#	MODE="$2"
#	if test `echo "$MODE" | grep '^--replay' >/dev/null`; then
#		if grep Profiling_ $1; then
#			echo ERROR: replay option applied to instrumented Solidity file version - needs original version instead
#			exit 1
#		fi
#	fi
#else
#	echo Usage: "ast-test.sh [Solidity file] [--instrument|--replay=<eventlog>]"
#	exit 1
#fi
if test "$#" -lt 1; then
	echo Usage: "ast-test.sh [Solidity file] [--instrument|--replay=<eventlog>|...]"
	exit 1
fi
INPUT=$1
shift


if test `echo "$@" | grep '--replay' >/dev/null`; then
	if grep Profiling_ $1; then
		echo ERROR: replay option applied to instrumented Solidity file version - needs original version instead
		exit 1
	fi
fi


MODE=--instrument

./solc-ast-json.sh "$INPUT" >default.ast-json
#./run.sh $MODE --debugGraphPlot=plot.dot --solidityOutput=transformed.sol <default.ast-json 
./run.sh $MODE "$@" --debugGraphPlot=plot.dot --solidityOutput=transformed.sol <default.ast-json 
if test "$?" = 0; then
	echo === SUCCESS ===
	if ! dot -Tpng plot.dot > plot.png; then
		echo Graph plot failed
		exit 1
	fi
	exit 0
else
	echo === ERROR ==
	exit 1
fi
