#!/bin/sh

if test "$#" != 1; then
	echo Usage: "ast-dump.sh [Solidity file]"
	exit 1
fi

./solc-ast-json.sh "$1" >default.ast-json
./run.sh --debugASTOutput --skipASTProcessing <default.ast-json 
if test "$?" = 0; then
	echo === SUCCESS ===
	exit 0
else
	echo === ERROR ==
	exit 1
fi
