#!/bin/sh

# This script generates, executes and evaluates randomized contracts in an infinite loop,
# currently based on the simple expression generation and evaluation scheme.
#
# A random number seed is supplied, at which the generation routine will start the first
# contract test and increase indefinitely for subsequent tests.
# A contract is uniquely identified by the code generation parameters:
#
#    - Random number seed  (+ technically PRNG variant used/Java library version)
#    - Function count
#    - Function statement min/max count
#    - Variable count
#    - Predicate min/max depth
#
# TODO make the latter 3 configurable
#
# Results for contracts that expose bugs either in our contract generation routine or
# (preferably) the solc compiler are stored in a specified directory.

if test "$#" = 3; then
	# Run an existing generated contract set
	EXISTING_INPUT_DIR="$1"
	RESULT_DIR="$2"
	CONTRACT_NAME="$3"
elif test "$#" != 6; then
	echo "Usage:"
	echo "    1. Generate contracts on the fly:"
	echo "       loop-generated-contracts.sh [result-dir] [random-start-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count]"
	echo "    2. Run existing generated contract set (by generate-contract-set.sh)"
	echo "       loop-generated-contracts.sh [input-dir] [result-dir] [contract-name]"
	exit 1
else
	             RESULT_DIR="$1"
	              PRNG_SEED="$2"
	         FUNCTION_COUNT="$3"
	ASSIGNMENTS_LOWER_BOUND="$4"
	ASSIGNMENTS_UPPER_BOUND="$5"
	         VARIABLE_COUNT="$6"
fi

TEMP_CONTRACT_DIR="_generated_contract"

if test -d "$RESULT_DIR"; then
	echo "Result directory $RESULT_DIR already exists - please delete it first"
	exit 1
fi

if ! mkdir "$RESULT_DIR"; then
	echo "Cannot create result directory $RESULT_DIR"
	exit 1
fi

# TODO move clean_tmp-like stuff to a util.sh and read it in?
echo Cleaning up /tmp...
clean_tmp() {
        rm -rf /tmp/tmp-*
        rm -rf /tmp/test-*
}
clean_tmp


cleanup_after_iteration() {
	if test "$#" != 1 || ! test -d "$1"; then
		echo cleanup_after_iteration called without temp contract directory argument
		exit 1
	fi
	# Misc. cleanup
	rm -rf "$1"
        clean_tmp
        killall node 2>/dev/null

	# Since we have no fixed termination criterion, wait for some time that is miniscule
	# in comparison to the time spent on a single generated contract, but still avoids
	# out-of-control infinite error loops if something is off:
	sleep 1
}

# Evaluates the profiling log and sets SAVED_RESULT_DIR if needed 
evaluate_result() {
	# Add md5 sum of generated contract to enable the detection of unexpected effects such as:
	#    - Collisions
	#    - PRNG differences between systems
	printf "id `cat $TEMP_CONTRACT_DIR/md5`    - " 

	# The contract executed successfully - check results
	if ! test -f "$PATH_PROFILING_EVENTS_LOG"; then
		# This apparently happens if generated contracts become too complex and hit some node/truffle/... limits.
		# We don't save these cases for now
		echo FRAMEWORK ERROR
	elif grep EXPR_ERROR "$PATH_PROFILING_EVENTS_LOG" >/dev/null; then
		# Evaluation vs. executon mismatch
		echo POSSIBLE COMPILER BUG 
		SAVED_RESULT_DIR="$RESULT_DIR/${PRNG_SEED}_BUG"
	elif grep TAUTOLOGY_ERROR "$PATH_PROFILING_EVENTS_LOG" >/dev/null; then
		echo TAUTOLOGY ERROR
		SAVED_RESULT_DIR="$RESULT_DIR/${PRNG_SEED}_TAUTOLOGY"
	else
		echo OK
	fi
}

save_results() {
	if test "$SAVED_RESULT_DIR" != ""; then
		# Save a copy of the contract and log files to investigate the issue that occurred
		cp -R "$TEMP_CONTRACT_DIR" "$SAVED_RESULT_DIR"
		cp run-generated-contract.out "$SAVED_RESULT_DIR"/
		cp -R "$PATH_MAIN_RESULTS_DIR" "$SAVED_RESULT_DIR"/ 
	fi
}

if test "$EXISTING_INPUT_DIR" != ""; then
	for current_contract_dir in `ls -d $EXISTING_INPUT_DIR/*`; do
		if ! test -d "$current_contract_dir"; then
			continue
		fi
		printf "Trying recorded contract ... "

		SAVED_RESULT_DIR=""
		if ! ./run-generated-contract.sh "$current_contract_dir" "$CONTRACT_NAME" >run-generated-contract.out 2>&1; then
			# Either contract generation or execution failed
			echo ERROR
			SAVED_RESULT_DIR="$RESULT_DIR/${PRNG_SEED}_ERROR"
		else
			evaluate_result
		fi
		save_results
		PRNG_SEED=`expr $PRNG_SEED + 1`
		cleanup_after_iteration "$TEMP_CONTRACT_DIR"
	done
else
	while true; do
		printf "Trying contract $PRNG_SEED ... "

		SAVED_RESULT_DIR=""
		if ! ./run-generated-contract.sh "$PRNG_SEED" "$FUNCTION_COUNT" "$ASSIGNMENTS_LOWER_BOUND" "$ASSIGNMENTS_UPPER_BOUND" "$VARIABLE_COUNT" >run-generated-contract.out 2>&1; then
			# Either contract generation or execution failed
			echo ERROR
			SAVED_RESULT_DIR="$RESULT_DIR/${PRNG_SEED}_ERROR"
		else
			evaluate_result
		fi
		save_results
		PRNG_SEED=`expr $PRNG_SEED + 1`
		cleanup_after_iteration "$TEMP_CONTRACT_DIR"
	done
fi
