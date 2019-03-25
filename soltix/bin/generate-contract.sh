#!/bin/sh

usage() {
	echo "Usage: generate-contract.sh [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [contract-file] [construction-file-base] [transaction-file-base] [transaction-file-json] [construction-file-truffle] [transaction-file-truffle] [metadata-output-file] [--assignmentSequence|--complete]"
	echo " Or, to only supply a directory to fill with default-named files:"
	echo "       generate-contract.sh [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [result-directory] [--assignmentSequence|--complete]"
	echo "Note: File paths should be absolute (e.g. '$PWD'/file)"
	exit 1
}

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi


PROG_ARGS="$@"

if test "$#" = 7; then
        MODE="$7"
	RESULT_DIR=`realpath $6`
	if test -d "$RESULT_DIR"; then
		echo Error: Result directory $RESULT_DIR already exists - please delete it first
		exit 1
	fi
	if ! mkdir "$RESULT_DIR"; then
		echo Error: Cannot create directory $RESUL_DIR
		exit 1
	fi
           CONTRACT_FILE="$RESULT_DIR/c0.sol"
 FORMATTED_CONTRACT_FILE="$RESULT_DIR/c0-formatted.sol.x"
   CONSTRUCTOR_BASE_FILE="$RESULT_DIR/c0-base.cons"
  TRANSACTIONS_BASE_FILE="$RESULT_DIR/c0-base.tx"
  TRANSACTIONS_JSON_FILE="$RESULT_DIR/c0.tx-json"
   CONSTRUCTOR_FULL_FILE="$RESULT_DIR/2_deploy_contracts.js"
  TRANSACTIONS_FULL_FILE="$RESULT_DIR/test.js"
    METADATA_OUTPUT_FILE="$RESULT_DIR/meta.kv"
elif test "$#" = 13; then
# TODO Drop this mode entirely - deprecated
           CONTRACT_FILE="$6"
   CONSTRUCTOR_BASE_FILE="$7"
  TRANSACTIONS_BASE_FILE="$8"
  TRANSACTIONS_JSON_FILE="$9"
   CONSTRUCTOR_FULL_FILE="$10"
  TRANSACTIONS_FULL_FILE="$11"
    METADATA_OUTPUT_FILE="$12"
                    MODE="$13"
else
	echo generate-contract: Invalid argument count "$#"
	usage
fi

if test "$MODE" != --assignmentSequence && test "$MODE" != --complete; then
	echo Unknown mode specified: $MODE
	usage
fi

               PRNG_SEED="$1"
          FUNCTION_COUNT="$2"
 ASSIGNMENTS_LOWER_BOUND="$3"
 ASSIGNMENTS_UPPER_BOUND="$4"
          VARIABLE_COUNT="$5"

echo INPUT "$@"

if test "$MODE" = --assignmentSequence; then
	FUNCTION_TYPE="assignmentSequence"
	FUNCTION_SPECIFIC_ARGS=""
elif test "$MODE" = --complete; then
	FUNCTION_TYPE="random"
	FUNCTION_SPECIFIC_ARGS="--inject-loop-limits --guaranteeSafeExecution"
else
	echo Unimplemented mode $MODE
	exit 1
fi

# Generate contract and associated construction and transaction files.
#
# Support code is already inserted at this point. This is the most natural thing to do, since
# __outro() calls can then already be included in the original transaction file
if ! run-soltix.sh --no-input-code \
	--prng-seed="$PRNG_SEED" \
	--contractGenerationFunctionCount="$FUNCTION_COUNT" \
	--contractGenerationFunctionStatementsLowerBound="$ASSIGNMENTS_LOWER_BOUND" \
	--contractGenerationFunctionStatementsUpperBound="$ASSIGNMENTS_UPPER_BOUND" \
	--contractGenerationVariableCount="$VARIABLE_COUNT" \
	--generateFullContract="$CONTRACT_FILE"="$FUNCTION_TYPE" \
	$FUNCTION_SPECIFIC_ARGS \
	$GANACHE_OPTIMIZATION_WORKAROUNDS_ENV_VAR \
	--generateTruffleConstructorInvocation=c0="$CONSTRUCTOR_BASE_FILE" \
	--generateTruffleTransaction=c0="$TRANSACTIONS_BASE_FILE"="$TRANSACTIONS_JSON_FILE" \
	--metadataOutput="$METADATA_OUTPUT_FILE"; then
	echo generate-contract.sh: Contract generation error
	exit 1
fi


# Reprocess generated contract to generate metadata output file (this is not
# easily possible during generation, since the AST is constructed differently
# (this bypasses various accounting things, e.g. due to lack of AST.addInnerNode()
# calls as well as opaque ASTVerbatimText nodes).
# This also adds formatting.
# TODO Inject support code as well? -> move this to an external script,
# then use it in run-one-contract.sh as wel
#
# This step needs solc
if ! test -f ../settings.cfg.sh || ! . ../settings.cfg.sh ; then
	if ! test -f ./settings.cfg.sh || ! . ./settings.cfg.sh ; then
		echo Error: Cannot load settings.cfg.sh - will skip metadata generation and formatting step
	fi
else
	if test "$MODE" = --assignmentSequence; then
		# An assignment sequence function type - all evaluations, fix-ups and mutatins have
		# already be performed
		if ! run-soltix.sh "$CONTRACT_FILE" --solidityOutput="$FORMATTED_CONTRACT_FILE" --metadataOutput="$METADATA_OUTPUT_FILE"; then
			echo generate-contract.sh: Error processing generated contract
			exit 1
		fi
	elif test "$MODE" = --complete; then
		# This is a "random" rather than fixed (assignment sequence) contract function type -
		# we perform a separate interpretation step
		:
		if ! run-soltix.sh "$CONTRACT_FILE" --solidityOutput="$FORMATTED_CONTRACT_FILE" --metadataOutput="$METADATA_OUTPUT_FILE"; then
		#	--inject-loop-limits; then
#			--interpret="$TRANSACTIONS_JSON_FILE"="	
			echo generate-contract.sh: Error processing generated contract
			exit 1
		fi
	else
		echo Unimplemented mode $MODE for contract reprocessing
		exit 1
	fi

	# Switch formatted and original contract
	mv "$CONTRACT_FILE" /tmp/tmp.sol
	mv "$FORMATTED_CONTRACT_FILE" "$CONTRACT_FILE"
	mv /tmp/tmp.sol "$FORMATTED_CONTRACT_FILE"
fi

echo Copying mutated contracts...

if test "$MODE" != --complete; then   # TODO make it work for random generation (currently requires separate instrmentaton step due to lack of interpretation)
	# Assume 4 mutated programs were generated
	# TODO configure this
	mkdir "$RESULT_DIR"/mutations
	if ! cp /tmp/mutated0.sol "$RESULT_DIR"/mutations/; then
		echo Cannot copy /tmp/mutated0.sol
		exit 1
	fi
	if ! cp /tmp/mutated1.sol "$RESULT_DIR"/mutations/; then
		echo Cannot copy /tmp/mutated0.sol
		exit 1
	fi
	if ! cp /tmp/mutated2.sol "$RESULT_DIR"/mutations/; then
		echo Cannot copy /tmp/mutated2.sol
		exit 1
	fi
	if ! cp /tmp/mutated3.sol "$RESULT_DIR"/mutations/; then
		echo Cannot copy /tmp/mutated3.sol
		exit 1
	fi
fi

CONTRACT_NAME=c0 # always true for now  (TODO)

echo Building deployment script...
# Build deployment script, embedding constructor invocation
build-deployment-script.sh   "$CONTRACT_NAME" "$CONSTRUCTOR_BASE_FILE"  >"$CONSTRUCTOR_FULL_FILE" 

echo Building transactions script...
# Build transactions script, embedding transactions list
build-transactions-script.sh "$CONTRACT_NAME" "$TRANSACTIONS_BASE_FILE" >"$TRANSACTIONS_FULL_FILE" 

if test "$RESULT_DIR" != ""; then
	# Store auxiliary information
	cat "$CONTRACT_FILE" | md5sum >"$RESULT_DIR"/md5

	echo "$PROG_ARGS"                                  >"$RESULT_DIR"/params.txt
	echo "prng seed       = $PRNG_SEED"               >>"$RESULT_DIR"/params.txt
	echo "function count  = $FUNCTION_COUNT"          >>"$RESULT_DIR"/params.txt
	echo "min assignments = $ASSIGNMENTS_LOWER_BOUND" >>"$RESULT_DIR"/params.txt
	echo "max assignments = $ASSIGNMENTS_UPPER_BOUND" >>"$RESULT_DIR"/params.txt
	echo "variable count  = $VARIABLE_COUNT"          >>"$RESULT_DIR"/params.txt
fi

exit 0

