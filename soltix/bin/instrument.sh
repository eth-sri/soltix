#!/bin/sh

if test "$#" != 1; then
	echo "Usage: ./instrument.sh [input-file.sol]"
	exit 1
fi


#./run.sh $1 --profiling --solidityOutput=transformed.sol
#cat transformed.sol


./solc-ast-json.sh "$1" >default.ast-json
./run.sh --debugASTOutput --instrument --debugGraphPlot=plot.dot --solidityOutput=transformed.sol <default.ast-json 
cat transformed.sol
