#!/bin/sh

# This script performs the main steps involved in testing a single contract:
#
#    1. Run the original version
#    2. Create and run the instrumented version
#    3. Create and run mutations based on the profile
#
# The "current-contract" directory is populated with the supplied test contract
# and the generated files.

# Since switching to ganache-cli from ethereumjs-testrpc-sc, there are apparently
# lots of zombie instances - kill node for now TODO how to properly fix it?
killall node

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

if ! test -f "$NODEDIR"/node; then
        # We need to run the node interpreter directly to pass the --max-old-space-size option -
        # /usr/local/bin/node may be more likely to be an up-to-date version rather than some
        # system default?!
        echo ERROR: Need $NODEDIR/node to execute truffle
        exit 1
fi


usage() {
	# TODO Fix messy mode selection
	echo run-one-test.sh "[path-to-contract-file.sol | path-to-contract-dir] [mutations-count | c0]" 
	echo
	echo "With Live EMI mutations:"
	echo "1. If a mutations count is given, we apply Live Code Mutation with the given number of repetitions"
	echo "   run-one-test.sh [path-to-contract.sol] [mutations-count]"
	echo "2. If, additionally, the --generated argument is given, the input is expected to be a directory containing"
	echo "   contract c0.tx and the construction and tx files"
	echo "   run-one-test.sh [generated-contract-dir] [mutations-count] --generated"
	echo
	echo "Without mutations:"
	echo "1. If c0 is specified, we assume generated contract execution (no instrumentation, no mutations)"
	echo "   run-one-test.sh [path-to-contract-dir] c0"
	exit 1
}

if test "$#" != 2; then
	echo Invalid argument count "$#"
	usage
fi

if test "$2" = c0; then
	RUN_SINGLE_CONTRACT_FILE=c0.sol
else
	MUTATIONS_COUNT=$2
fi

if test -d "$1" &&  test -f "$1"/c0.sol; then
	GENERATED_DIR=`realpath $1`
	    INPUT_FILE="$GENERATED_DIR/c0.sol"
	HAVE_CONS_FILE="$GENERATED_DIR/2_deploy_contracts.js"
	  HAVE_TX_FILE="$GENERATED_DIR/test.js"
	TEST_CONTRACT_NAME="c0"
	echo Using predefined contract "$TEST_CONTRACT_NAME", cons file "$HAVE_CONS_FILE" and tx file "$HAVE_TX_FILE"
else
	INPUT_FILE=`realpath "$1"`
fi

VERBOSE=yes # temp

debug() {
	if test "$VERBOSE" = "yes"; then
		echo "$@"
	fi
}

# TODO Fix this dependency, always build for now:
RETURNTO=$PWD

#cd ../soltix/bin && make && cd "$RETURNTO" 


debug "Running contract $FILE"


# Our input may be a single contract file (the normal mode of operation) or a directory containing
# the following pre-defined files for debugging:
#     
#      original.sol
#      instrumented.sol
#      mutated-1..n.sol
if test -d "$INPUT_FILE"; then
	DEBUG_INPUT=yes
	if test "$RUN_SINGLE_CONTRACT_FILE" !=  ""; then
		if ! test -f "$INPUT_FILE/c0.sol"; then
			echo Error: Input directory does not contain c0.sol
			usage
		fi
	else
		if ! test -f "$INPUT_FILE/original.sol" || ! test -f "$INPUT_FILE/instrumented.sol"; then
			echo Error: Input directory does not contain original.sol and/or instrumented.sol
			usage
		fi
	fi
fi

if test "$RUN_SINGLE_CONTRACT_FILE" != ""; then
	INPUT_CONTRACT_FILE="$INPUT_FILE/$RUN_SINGLE_CONTRACT_FILE"
elif test "$DEBUG_INPUT" = yes; then
	INPUT_CONTRACT_FILE="$INPUT_FILE/original.sol"
else
	INPUT_CONTRACT_FILE="$INPUT_FILE"
fi

# Determine "top-level" contract name, which is needed to name the file accordingly (as epxected by truffle)
# TODO this is remarkably slow for large contracts - why?
if test "$TEST_CONTRACT_NAME" = ""; then # Only if not already supplied
	TOP_LEVEL_CONTRACT_TMP_FILE="$PWD/top-level-contract.tmp"
	rm -f "$TOP_LEVEL_CONTRACT_TMP_FILE"
	if ! run-soltix.sh "$INPUT_CONTRACT_FILE" --outputTopLevelContract="$TOP_LEVEL_CONTRACT_TMP_FILE"; then
		echo run-soltix.sh failed
		exit 1
	fi
	if ! test -f "$TOP_LEVEL_CONTRACT_TMP_FILE"; then
		echo "File $TOP_LEVEL_CONTRACT_TMP_FILE missing"
		exit 1
	fi
	#TEST_CONTRACT_NAME=`cat $TOP_LEVEL_CONTRACT_TMP_FILE`
	TEST_CONTRACT_NAME=`head -1 $TOP_LEVEL_CONTRACT_TMP_FILE`
fi

    TESTDIR="$PATH_INPUT_DIR"
  OUTPUTDIR="$PATH_OUTPUT_DIR"
PROJECT_CONTRACT_FILE="$TESTDIR/contracts/${TEST_CONTRACT_NAME}.sol"

# Clean up files to avoid stale data interference
rm -rf "$TESTDIR"
rm -rf "$OUTPUTDIR"

# Create directory
mkdir -p "$TESTDIR"/migrations
mkdir -p "$TESTDIR"/contracts
mkdir -p "$TESTDIR"/test


# Simple new framework
IS_NEW_FRAMEWORK=yes
RUN_FRAMEWORK_CMD=simple-run-contract.sh
RUN_FRAMEWORK_ARGS="$TESTDIR"
INPUT_DIR_RESULT_PATH="$TESTDIR"/results
PROFILING_LOG_PATH="$INPUT_DIR_RESULT_PATH"/profiling-events.log


RESULT_DIR="$PATH_MAIN_RESULTS_DIR"
rm -rf "$RESULT_DIR"
echo Creating "$RESULT_DIR"
mkdir -p "$RESULT_DIR"

mkdir -p "$INPUT_DIR_RESULT_PATH"


# Copy original contract file, while renaming one of the contained contracts to
# "MetaCoin" to fulfill truffle expectations of having one contract being named like
# its containing file 
#
# In addtion, we:
#   - Create a suitable constructor invocation
if ! test -d "$PATH_TMPDIR"; then
	mkdir "$PATH_TMPDIR"
fi
CONTRACT_DEPLOYMENT_COMMAND_FILE="$PATH_TMPDIR"/_gen-construction.tmp
TRANSACTION_FILE="$PATH_TMPDIR"/_gen-transaction.tmp
TRANSACTION_FILE_JSON="$PATH_TMPDIR"/_gen-transaction.tmp-json

debug "running soltix for contract ${TEST_CONTRACT_NAME}"
debug "command: " run-soltix.sh "$INPUT_CONTRACT_FILE" --solidityOutput="$PROJECT_CONTRACT_FILE" --generateTruffleConstructorInvocation=${TEST_CONTRACT_NAME}="$CONTRACT_DEPLOYMENT_COMMAND_FILE"

if test "$HAVE_TX_FILE" != ""; then
	MUTATOR_TXGEN_ARG="" 
elif test "$DEBUG_INPUT" = yes && test -f "$INPUT_FILE/test.js"; then
	MUTATOR_TXGEN_ARG=""
else
	MUTATOR_TXGEN_ARG="--generateTruffleTransaction=${TEST_CONTRACT_NAME}=$TRANSACTION_FILE=$TRANSACTION_FILE_JSON"
fi

if test "$HAVE_CONS_FILE" != ""; then
	MUTATOR_CONSTRUCTOR_ARG=""
elif test "$DEBUG_INPUT" = yes && test -f "$INPUT_FILE/2_deploy_contracts.js"; then
	MUTATOR_CONSTRUCTOR_ARG=""
else
	MUTATOR_CONSTRUCTOR_ARG="--generateTruffleConstructorInvocation=${TEST_CONTRACT_NAME}=$CONTRACT_DEPLOYMENT_COMMAND_FILE"
fi


#    --generateTruffleConstructorInvocation=${TEST_CONTRACT_NAME}="$CONTRACT_DEPLOYMENT_COMMAND_FILE" \
#    --generateTruffleTransaction=${TEST_CONTRACT_NAME}="$TRANSACTION_FILE"; then
#
# Now timestamp 1530553715 = July 2nd 2018, ca. 7:50pm
if test "$GENERATED_DIR" != ""; then
	# TODO Fix the $INPUT_FILE stuff. It's set inconsistently for --generated
	cp "$GENERATED_DIR/c0.sol" "$PROJECT_CONTRACT_FILE"
elif test "$RUN_SINGLE_CONTRACT_FILE" != ""; then
	cp "$INPUT_FILE/$RUN_SINGLE_CONTRACT_FILE" "$PROJECT_CONTRACT_FILE"
elif test "$DEBUG_INPUT" = yes; then
	cp "$INPUT_FILE/original.sol" "$PROJECT_CONTRACT_FILE"
else

	if test "$TRACK_STORAGE_WRITES" = no; then
		TRACKING_EVENTS="--eventsReplaceStorageLog"
	else
		TRACKING_EVENTS=""
	fi
            #--injectArraySubscriptChecks \
	if ! run-soltix.sh "$INPUT_CONTRACT_FILE" \
	    --solidityOutput="$PROJECT_CONTRACT_FILE" \
	    --renameExceptionalEvents \
	    --renameNowTimestamps=1530553715 \
	    --renameThrowStatements \
	    $TRACKING_EVENTS \
	    --insertLCMSupportCode\
	    $MUTATOR_CONSTRUCTOR_ARG \
	    $MUTATOR_TXGEN_ARG 2>mutator.err; then
		cat mutator.err
		echo run-soltix.sh failed
		exit 1
	fi
	echo should have generated "$PROJECT_CONTRACT_FILE"
	#exit 1
	rm -f mutator.err
fi

echo runnin: run-soltix.sh "$INPUT_CONTRACT_FILE" \
    --solidityOutput="$PROJECT_CONTRACT_FILE" \
    $MUTATOR_CONSTRUCTOR_ARG \
    $MUTATOR_TXGEN_ARG

#    --generateTruffleConstructorInvocation=${TEST_CONTRACT_NAME}="$CONTRACT_DEPLOYMENT_COMMAND_FILE" \
#    --generateTruffleTransaction=${TEST_CONTRACT_NAME}="$TRANSACTION_FILE"




if test "$HAVE_CONS_FILE" != ""; then
	# There's a supplied construction file
	if ! test -f "$HAVE_CONS_FILE"; then
		echo Error: Construction file "$HAVE_CONS_FILE" does not exist
		exit 1
	fi
	mkdir -p "$TESTDIR/migrations"
	cp "$HAVE_CONS_FILE" "$TESTDIR/migrations/2_deploy_contracts.js" 
elif test "$DEBUG_INPUT" = yes && test -f "$INPUT_FILE/test.js"; then
	# There's a pre-defined deployment file we can use
	mkdir -p "$TESTDIR/migrations"
	cp "$INPUT_FILE/2_deploy_contracts.js" "$TESTDIR/migrations/2_deploy_contracts.js" 
else
	debug "done running soltix"
	cat $CONTRACT_DEPLOYMENT_COMMAND_FILE

	# Generate deployment file, involving our soltix tool to create constructor arguments
	# if needed. TODO Maybe a fuzzer can do this with higher-quality results (but output
	# would likely need to be adapted to the truffle deployment format)
	# Part 1 of 3: Intro

	#cat >"$TESTDIR/migrations/2_deploy_contracts.js" << EOF
	#var MetaCoin = artifacts.require("./MetaCoin.sol");
	#
	#module.exports = function(deployer) {
	#EOF
	echo "var $TEST_CONTRACT_NAME = artifacts.require(\"./${TEST_CONTRACT_NAME}.sol\");"    >"$TESTDIR/migrations/2_deploy_contracts.js"
	echo "module.exports = function(deployer) {"                                           >>"$TESTDIR/migrations/2_deploy_contracts.js"

	# Part 2 of 3: Append deployment of form
	#    deployer.deploy(MetaCoin, arg1, ...);
	#cat >"$TESTDIR/migrations/2_deploy_contracts.js" << EOF
	#  deployer.deploy(MetaCoin);
	cat $CONTRACT_DEPLOYMENT_COMMAND_FILE                                                  >>"$TESTDIR/migrations/2_deploy_contracts.js"

	# Part 3 of 3: Outro
	echo "};"                                                                              >>"$TESTDIR/migrations/2_deploy_contracts.js"
fi
cp "$TESTDIR/migrations/2_deploy_contracts.js" "$RESULT_DIR"



if test "$HAVE_TX_FILE" != ""; then
	if ! test -f "$HAVE_TX_FILE"; then
		echo "Error: Transactions file $HAVE_TX_FILE does not exist"
		exit 1
	fi
	mkdir -p "$TESTDIR/test"
	cp "$HAVE_TX_FILE" "$TESTDIR/test/test.js"
elif test "$DEBUG_INPUT" = yes && test -f "$INPUT_FILE/test.js"; then
	# There's a pre-defined transaction file we can use
	mkdir -p "$TESTDIR/test"
	cp "$INPUT_FILE/test.js" "$TESTDIR/test/test.js"
else
	# Generate transactions, part 1 of 3: Intro

	mkdir -p "$TESTDIR/test"
	echo "var $TEST_CONTRACT_NAME = artifacts.require(\"./${TEST_CONTRACT_NAME}.sol\");"  >"$TESTDIR/test/test.js"

	# Event logging function
	cat >>"$TESTDIR/test/test.js" <<EOF
var fs = require('fs');
var stream = fs.createWriteStream("profiling-events.log");

function logEvents(result) {
    // Log all encountered Solidity events ("emit ..."):
console.log("logEvent received: " + JSON.stringify(result));
    if ('logs' in result) {
        for (var i = 0; i < result.logs.length; ++i) {
            var b = result.logs[i];
            console.log("   > " + b.event + " = " + b.args + "   " +  JSON.stringify(b));
            stream.write(JSON.stringify(b) + "\n");
            for (m in b) {
                console.log("                > " + m); 
            }
        }
    }
}
EOF

	# Test function in truffle-specific format
	echo "contract('${TEST_CONTRACT_NAME}', function(accounts) {"                         >>"$TESTDIR/test/test.js"
	echo "  it('description', function() {"                                               >>"$TESTDIR/test/test.js"
	echo "    var fs = require('fs');"                                                    >>"$TESTDIR/test/test.js"
	echo "    return ${TEST_CONTRACT_NAME}.deployed().then(async function(instance) {"          >>"$TESTDIR/test/test.js"
	echo "      console.log(\"Starting transaction...\");"                                  >>"$TESTDIR/test/test.js"
#	echo "      try {"                                                                    >>"$TESTDIR/test/test.js"
#
#
	# Generate transaction, part 2 of 3: hardcoded transactions, TODO: fuzz this
	cat $TRANSACTION_FILE >> "$TESTDIR/test/test.js" 

	# Generate transaction, part 3 of 3: Outro 
	cat >>"$TESTDIR/test/test.js" << EOF
   }); 
  }); 
});
EOF

fi
cp "$TESTDIR/test/test.js" "$RESULT_DIR"





error() {
	echo "ERROR: $@"
	exit 1
}


# Note: See run-soltix.sh invocation above, which has already created the PROJECT_CONTRACT_FILE

run_contract() {
	if test "$#" != 1; then
		error "run_contract not called with expected contract description argument"
	fi

	rm -f "$PROFILING_LOG_PATH"
	cp "$PROJECT_CONTRACT_FILE" "$RESULT_DIR"/"$1".sol
	"$RUN_FRAMEWORK_CMD" "$RUN_FRAMEWORK_ARGS"
	RC=$?


	if test "$IS_NEW_FRAMEWORK" = yes; then
		# TODO fix this path stuff
		cp "$INPUT_DIR_RESULT_PATH"/profiling-events.log "$PATH_PROFILING_EVENTS_LOG" 
		cp "$INPUT_DIR_RESULT_PATH"/TruffleTest.log "$PATH_TRUFFLE_LOG_FILE" 
		cp "$INPUT_DIR_RESULT_PATH"/TruffleTest.rpc.log "$PATH_TRUFFLE_RPC_LOG_FILE" 

		cp "$INPUT_DIR_RESULT_PATH"/TruffleTest.log "$PATH_TRUFFLE_LOG_FILE" 
		cp "$INPUT_DIR_RESULT_PATH"/TruffleTest.rpc.log "$PATH_TRUFFLE_RPC_LOG_FILE" 
	fi

	if test "$RC" != 0; then
		error "Framework failed for original contract"
	fi


	cp "$PROFILING_LOG_PATH" "$RESULT_DIR"/"$1"-profiling-log.log

	

	if ! test -f "$PROFILING_LOG_PATH"; then
		error "Log file $PROFILING_LOG_PATH does not exist"
	fi
}


debug =============================  Processing original contract =================================
printf "O... " >>/dev/tty # Always show this even if invoked (e.g. from run-all-tests.sh) with redirection

if test "$MUTATIONS_COUNT" = optimize; then # TODO pass flag properly
	# Run original contract with optimization disabled
	truffle-optimization-setting.sh off
fi


# Compare memory dump and event log equivalence for two specified test instantiations
# (designated by the names "original", "instrumented" or "mutated-1...n")
verify_result_equivalence() {
	FIRST_PROFILING_LOG_PATH="$RESULT_DIR"/"$1"-profiling-log.log
	SECOND_PROFILING_LOG_PATH="$RESULT_DIR"/"$2"-profiling-log.log

	if ! test -f "$FIRST_PROFILING_LOG_PATH"; then error "Result file $FIRST_PROFILING_LOG_PATH does not exist"; fi
	if ! test -f "$SECOND_PROFILING_LOG_PATH"; then error "Result file $SECOND_PROFILING_LOG_PATH does not exist"; fi

	# Compare event log. Differences are to be expected at least between the original contract
	# (which only emits user code events) and the instrumented contract (which additionally emits
	# profiling events).
	#
	# Profiling events are currently ignored for the comparison
	# TODO Take them into account where appropriate
	if ! event-logs-equal.sh "$FIRST_PROFILING_LOG_PATH" "$SECOND_PROFILING_LOG_PATH"; then
		error "Event log difference between $1 and $2"
	fi
}



run_contract original



if test "$RUN_SINGLE_CONTRACT_FILE" != ""; then
	# No additional contract executions are needed
	exit 0
fi
if test "$MUTATIONS_COUNT" = 0; then
	echo Original contract OK
	exit 0
elif test "$MUTATIONS_COUNT" = optimize; then # TODO pass flag properly
	# In addition to the initial unoptimized run, repeat it with optimization 
	#  TODO truffle-optimization-setting.sh could be made to set the optimization level.
	# For now we just run it once again with optimization enabled. The level can be set to the
	# maximum level 200 manually. An investigation of anything found at the maximum level can
	# then manually vary to lower levels
	
	debug =============================  Processing optimized - 0 runs - contract =================================
	truffle-optimization-setting.sh on

	run_contract optimized0

	# Restore to off (note: this is not done if run_contract fails! but it's not essential)
	truffle-optimization-setting.sh off

	verify_result_equivalence "original" "optimized0"

	exit 0

	#debug =============================  Processing optimized - 0 runs - contract =================================
	#./truffle-optimization-setting.sh on 0
	#run_contract optimized0
	#debug =============================  Processing optimized - 0 runs - contract =================================
	#./truffle-optimization-setting.sh on 200
	#run_contract optimized200
fi




# no longer seems to make sense to run instrumentation if we already have mutated contracts, which is
# the case for isolate.sh'ed normal contracts and generated contracts (no instrumentation step is even
# needed for the latter since interpretation tells us most we need to know about the values)
SKIP_INSTRUMENTATION=no
if test "$DEBUG_INPUT" = yes; then
	SKIP_INSTRUMENTATION=yes
elif test "$GENERATED_DIR" != ""; then
	if ! test -d "$GENERATED_DIR"/mutations; then
		SKIP_INSTRUMENTATION=no
	else
		SKIP_INSTRUMENTATION=yes
	fi
fi



if test "$SKIP_INSTRUMENTATION" != yes; then 
	#if true; then #test "$DEBUG_INPUT" != yes; then # no longer seems to make sense to run instrumentation if we already have mutated contracts
	debug =============================  Processing instrumented contract ============================
	printf "I... " >>/dev/tty # Always show this even if invoked (e.g. from run-all-tests.sh) with redirection
	if test "$DEBUG_INPUT" = yes; then
		cp "$INPUT_FILE/instrumented.sol" "$PROJECT_CONTRACT_FILE"
	else
	#	./instrument-contract.sh "$INPUT_FILE" "$PROJECT_CONTRACT_FILE"

		# Apply instrumentation to original contract. We have to use the file from the project
		# directory to include transformations that may already have been performed when importing
		# the original contract file (currently contract renaming)
		if ! instrument-contract.sh "$PROJECT_CONTRACT_FILE"                "$PROJECT_CONTRACT_FILE"-instrumented; then
			echo Error: instrument-contract.sh failed
			exit 1
		fi
		mv                       "$PROJECT_CONTRACT_FILE"-instrumented   "$PROJECT_CONTRACT_FILE"
	fi
	run_contract instrumented
	verify_result_equivalence "original" "instrumented"
fi





# Perform live code mutation - for now in a single iteration
# TODO multiple iterations
debug =============================  Processing mutated contracts ============================
if test "$GENERATED_DIR" != "" && test -d "$GENERATED_DIR"/mutations; then
	# Mutated programs must already have been generated. TODO Remove hardcoded count of 4
	cp "$GENERATED_DIR"/mutations/mutated0.sol .
	cp "$GENERATED_DIR"/mutations/mutated1.sol .
	cp "$GENERATED_DIR"/mutations/mutated2.sol .
	cp "$GENERATED_DIR"/mutations/mutated3.sol .
elif test "$DEBUG_INPUT" = yes; then
	# Note: mutated0.sol is already assumed to be the faulty contract and must be made
	# available by the user accordingly
	if ! cp "$INPUT_FILE/mutated0.sol" mutated0.sol; then
		echo Missing mutated0.sol file in source directory
		exit 1
	fi
else
	    #--solidityOutput="$PROJECT_CONTRACT_FILE" \
	if ! run-soltix.sh "$RESULT_DIR"/original.sol \
	    --solidityOutput="$PWD/mutated#.sol" \
	    --replay="$RESULT_DIR"/instrumented-profiling-log.log \
	    --applyLiveEMIMutations=$MUTATIONS_COUNT; then
		error "Mutator failed for live EMI application"
	fi
fi

i=0
while test $i -lt $MUTATIONS_COUNT; do
	debug =============================  Processing mutated `expr $i + 1` of $MUTATIONS_COUNT ============================
	printf "M$i... " >>/dev/tty # Always show this even if invoked (e.g. from run-all-tests.sh) with redirection
	cp mutated${i}.sol "$PROJECT_CONTRACT_FILE"
	# TODO Use solc compiler optimizations where appropriate, e.g.: optimize original, don't optimize profile, optimize mutated version - or all three?
	#cp "$PROJECT_CONTRACT_FILE" "$RESULT_DIR"/mutated0.sol
	#cp "$PROFILING_LOG_PATH"    "$RESULT_DIR"/mutated0-profiling-log.log
	run_contract mutated${i} 
	verify_result_equivalence "original" "mutated${i}"
	i=`expr $i + 1`
done


debug Test OK


exit 0

