#!/bin/sh

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi


usage() {
        echo "Usage: generate-contract-set.sh [contract-count] [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [directory] [--assignmentSequence|--complete]"
        exit 1
}

if test "$#" != 8; then
	usage
fi

         CONTRACT_COUNT="$1"
              PRNG_SEED="$2"
         FUNCTION_COUNT="$3"
ASSIGNMENTS_LOWER_BOUND="$4"
ASSIGNMENTS_UPPER_BOUND="$5"
         VARIABLE_COUNT="$6"
              DIRECTORY=`realpath $7`

                   MODE="$8"

if test -d "$DIRECTORY"; then
	echo Error: Directory "$DIRECTORY" already exists - please delete it first
	exit 1
fi

mkdir -p "$DIRECTORY"
touch "$DIRECTORY"/is_generated_set

TEMPDIR="$DIRECTORY/_generated"
TEMP_CONSTR_BASE_FILE="$TEMPDIR/c0.cons"
TEMP_TX_BASE_FILE="$TEMPDIR/c0.tx"
TEMP_TX_JSON_FILE="$TEMPDIR/c0.tx-json"
TEMP_CONTRACT_FILE="$TEMPDIR/c0.sol"
TEMP_CONSTR_FULL_FILE="$TEMPDIR/2_deploy_contracts.js"
TEMP_TX_FULL_FILE="$TEMPDIR/test.js"

if test "$MODE" != --assignmentSequence && test "$MODE" != --complete; then
	echo Error: Unsupported mode "$MODE"
	usage
fi

i=1
while test "$i" -le "$CONTRACT_COUNT"; do
	printf "Generating $i of $CONTRACT_COUNT (seed $PRNG_SEED) ... "

#	mkdir -p "$TEMPDIR"

	if ! generate-contract.sh "$PRNG_SEED" "$FUNCTION_COUNT" "$ASSIGNMENTS_LOWER_BOUND" "$ASSIGNMENTS_UPPER_BOUND" "$VARIABLE_COUNT" "$TEMPDIR" "$MODE" >/dev/null 2>&1; then
#				"$TEMP_CONTRACT_FILE" "$TEMP_CONSTR_BASE_FILE" "$TEMP_TX_BASE_FILE" "$TEMP_CONSTR_FULL_FILE" "$TEMP_TX_FULL_FILE" >/dev/null 2>&1; then
		echo Command: generate-contract.sh "$PRNG_SEED" "$FUNCTION_COUNT" "$ASSIGNMENTS_LOWER_BOUND" "$ASSIGNMENTS_UPPER_BOUND" "$VARIABLE_COUNT" "$TEMPDIR" "$MODE" 
		echo ERROR in generate-contract.sh
		
exit 1
		# Don't abort for now, we still have evaluation bugs:
		# exit 1
	fi
	i=`expr $i + 1`
	PRNG_SEED=`expr $PRNG_SEED + 1`

	MD5=`md5sum "$TEMP_CONTRACT_FILE" | awk '{print $1}'`
	printf "$MD5 - "
	SAVEDIR="$DIRECTORY/$MD5"
	if test -d "$SAVEDIR"; then
		echo COLLISION
		continue
	else
		echo "OK"
	fi
	mv "$TEMPDIR" "$SAVEDIR"
	echo $MD5 >"$SAVEDIR"/md5
done

