#!/bin/sh

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

BIN_DIR=`dirname $0`


if test "$#" != 1 && test "$#" != 2; then
	echo "Usage: plot.sh [contract-file.sol] [opt: profiling-log]"
	exit 1
fi

FILE="$1"

if test "$#" = 2; then
	PROFILING_LOG_OPTION=--replay=`realpath $2`
fi

"$BIN_DIR"/solc-ast-json.sh "$FILE" >default.ast-json

"$BIN_DIR"/run.sh --debugGraphPlot=plot.dot $PROFILING_LOG_OPTION --solidityOutput=transformed.sol <default.ast-json 

#./run.sh --debugGraphPlot=plot.dot --replay=/home/nweller/msc-thesis/test-env/test-env-truffle/assets/sol-contract-tester/outputs/logs/current-contract/main_results/instrumented-profiling-log.log --solidityOutput=transformed.sol <default.ast-json 

if test "$?" = 0; then
        echo === SUCCESS ===
        if ! dot -Tpng plot.dot > plot.png; then
                echo Graph plot failed
                exit 1
        fi
	okular plot.png
        exit 0
else
        echo === ERROR ==
        exit 1
fi

