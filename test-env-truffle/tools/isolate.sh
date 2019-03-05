#!/bin/sh

# Run a specified contract and save generated instrumented contracts, deployment and
# transaction calls (as well as execution logs) to a specified directoy, such that
# run-one-test.sh can be applied to that directory.
#
# This enables us to debug with fixed, editable files instead of regenerating
# everything
usage() {
	echo Usage: isolate.sh "[input.sol]" "[mutations-count]" "[output-dir]"
	exit 1
}

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
	echo Error: Cannot load settings file $SETTINGS - please run setup.sh
	exit 1
fi

. `dirname $0`/../paths.cfg.sh
if ! test "$?" = 0; then
        echo Error: Cannot load `dirname $0`/../paths.cfg.sh
        exit 1
fi


if test "$#" != 3; then
	usage
fi
INPUT_FILE="$1"
MUTATIIONS_COUNT="$2"
OUTPUT_DIR="$3"
if ! test -f "$INPUT_FILE"; then
	echo Error: File "$INPUT_FILE" does not exist
	usage
fi

if test -f "$OUTPUT_DIR" || test -d "$OUTPUT_DIR"; then
	echo Error: Output directory "$OUTPUT_DIR" already exists
	usage
fi 

rm -f _difflog.tmp
if ! run-one-test.sh "$INPUT_FILE" "$MUTATIONS_COUNT"; then
	echo run-one-test.sh failed
	#exit 1
fi
 
mkdir -p "$OUTPUT_DIR"


# Assume that all relevant logs as well as original and intermediate contracts and
# deployment + transaction files are stored in the "main_results" directory
cp "$PATH_MAIN_RESULTS_DIR"/* "$OUTPUT_DIR"  
#cp _difflog.tmp "$OUTPUT_DIR"
#cp _filtered-first-log.tmp "$OUTPUT_DIR"
#cp _filtered-second-log.tmp "$OUTPUT_DIR"

echo Isolated test case "$INPUT_FILE" to "$OUTPUT_DIR"
echo Run with ./test-env-truffle/bin/run-one-test.sh "$OUTPUT_DIR" ..

exit 0
