#!/bin/sh

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

BIN_DIR=`dirname $0`


if test "$#" != 1; then
	echo Usage: solc-ast-json "[solidity-file.sol]"
	exit 1
fi


#solc --ast-json "$1" >_tmp
#LEN=`cat _tmp | wc -l`
# drop last line and first 4 lines of non-JSON meta garbage
    #cat _tmp | head -`expr $LEN - 1` | tail -`expr $LEN - 5`
#cat _tmp | tail -`expr $LEN - 4` | grep -v '====== '


solc --ast-json $SOLC_OPTIMIZATION_FLAG "$1" | "$BIN_DIR"/strip-ast-json-junk.sh
