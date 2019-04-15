#!/bin/sh


# Patch truffle to redirect the compilation command solc.compile() to our own script, where we
# can freely choose the compiler and optimization settings - temporary solution until the
# "external compilers" support in truffle can be made to work instead.
# Our external-solc.sh compilation output was apparently broken by artifact schema changes,
# so truffle silently rejects its artifacts as invalid and just recompiles the contracts using 
# solcjs. Creating artifacts in compatible format is nontrivial. 

if test "$#" != 1; then
	echo "Usage: patch-truffle.sh [truffle-file]"
	exit 1
fi
TRUFFLE_FILE="$1"
if ! test -f "$TRUFFLE_FILE"; then
	echo Error: truffle file $TRUFFLE_FILE does not exist
	exit 1
fi


COUNT=`fgrep "$FRAGMENT" node_modules/.bin/truffle | wc -l`
if test "$COUNT" -lt 1; then
	echo Error: Cannot find $FRAGMENT in $TRUFFLE_FILE 
	exit 1
fi


printf "Patching truffle ... "
if cat "$TRUFFLE_FILE" | sed "s#solc.compile(JSON.stringify(solcStandardInput))#require(process.env.TRUFFLE_EXTERNAL_COMPILE_SCRIPT).truffleCompile(solc, solcStandardInput)#g" >_tmp_truffle; then
	mv _tmp_truffle "$TRUFFLE_FILE"
	echo OK
	exit 0
else
	echo Error: Replacement failed
	rm -f _tmp_truffle
	exit 1
fi

