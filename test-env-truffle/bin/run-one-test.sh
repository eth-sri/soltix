#!/bin/sh

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

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi


export PATHSDIR=`dirname $0`/..
. "$PATHSDIR"/paths.cfg.sh
if ! test "$?" = 0; then
        echo Error: Cannot load "$PATHSDIR"/paths.cfg.sh
        exit 1
fi


if test "$#" != 2; then
        echo Invalid argument count "$#"
        usage
fi

c="$1"
MUTATIONS_COUNT="$2"


# TODO proper error evaluation instead of treating each log type the same
case $BLOCKCHAIN_BACKEND in
        ganache)
		BLOCKCHAIN_LOG_FILE="$PATH_TRUFFLE_RPC_LOG_FILE"
                ;;
        geth)
		BLOCKCHAIN_LOG_FILE="$GETH_LOG"
                ;;
        *)
                echo Error: Unknown BLOCKCHAIN_BACKEND value selected in settings.cfg.sh: $BLOCKCHAIN_BACKEND
                exit 1
esac


do-run-one-test.sh "$c" $MUTATIONS_COUNT  >current-contract-out.log 2>current-contract-err.log
RC=$?

if test "$RC" != 0 && test "$CALLED_BY_RUN_ALL_TESTS" != yes; then
	# Show log if an error occurred
	cat current-contract-out.log current-contract-err.log
fi


PROGRESS_SUMMARY=other-error


# Check for some errors that may have occurred even if run-one-test.sh indicated success
if grep 'Error: redPow(normalNum)' "$BLOCKCHAIN_LOG_FILE" >/dev/null;  then
	printf "GANACHE-CLI ERROR redPow()"
elif grep 'Error: Already a number in reduction context' "$BLOCKCHAIN_LOG_FILE" >/dev/null;  then
	printf "GANACHE-CLI ERROR reduction context"
elif grep 'Could not connect to your Ethereum client' "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	# It's not clear when exactly this happens, but it does terminate the test and
	# the test framework doesn't tell us about it
	# TODO propagate error codes properly
	printf "CLIENT ERROR"
elif ! test -f "$PATH_PROFILING_EVENTS_LOG"; then
	# This apparently happens if generated contracts become too complex and hit some node/truffle/... limits.
	# We don't save these cases for now
	printf "FRAMEWORK ERROR: NO EVENTS LOG"
elif grep EXPR_ERROR "$PATH_PROFILING_EVENTS_LOG" >/dev/null; then
	# Evaluation vs. executon mismatch
	printf "POSSIBLE BUG: EXPR_ERROR"
	PROGRESS_SUMMARY=bug-candidate-expr

# Success:
elif test "$RC" = 0; then
	printf OK
	PROGRESS_SUMMARY=ok

	# Include info on whether an exception (assert/require/revert) was encountered but ignored
	if grep EXCEPTION_REQUIRE_FAILURE "$PATH_PROFILING_EVENTS_LOG" >/dev/null; then
		printf " (require ignored)"
	elif grep EXCEPTION_ASSERT_FAILURE "$PATH_PROFILING_EVENTS_LOG" >/dev/null; then
		printf " (assert ignored)"
	elif grep EXCEPTION_ASSERT_FAILURE "$PATH_PROFILING_EVENTS_LOG" >/dev/null; then
		printf " (revert ignored)"
	fi
elif test -f mutator.err; then
	if grep 'ValueGenerator.generateDefaultValue called for unknown type mutator.ast.ASTUserDefinedTypeName' mutator.err >/dev/null; then
		printf "MUTATOR UDT ERROR"
	else
		printf "MUTATOR ERROR"
	fi
elif grep 'Unexpectd finishAddingValues'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null; then
	printf "MUTATOR ERROR (finishAddingValues) "
elif grep 'VM Exception while processing transaction: revert'   "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "(REVERTED)"
elif grep 'Error: VM Exception while processing transaction: invalid opcode'  "$PATH_TRUFFLE_LOG_FILE"  >/dev/null; then
	# This has so far only been encountered for assert() failures (not for require() - what's the difference?)
	# TODO Replace assert/require with custom myassert/myrequire variants to produce more useful result data instead of just aborting?
	# TODO Can usable partial results be obtained from aborted transactions?
	printf "INVALID OPCODE ERROR"
elif grep 'DeclarationError: Identifier not found or not unique.'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "RENAMING ERROR"
elif grep "Error: The contract code couldn't be stored, please check your gas amount."  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "GAS ERROR"
	# gas errors can apparently be caused by different types of errors, such as:
	#    - (encountered here:) Deployment attempt for an abstract contract (missing one or more function bodies)
	#    - (Internet says:) A constructor which isn't but should be "payable"
	#    - (Internet says:) An invocation of a non-public constructor
	if test -f "$PATH_MAIN_RESULTS_DIR"/instrumented-contract/instrumented-memory-dump.log; then
		# The existence of this file implies that the original contract was processed OK by the framework
		# and we failed during the instrumented profiling pass or a subsequent mutation pass
		printf " (INSTRUMENTED)"
	fi
elif grep 'Error: ENOSPC: no space left on device, write'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	# This is apparently caused by a full /tmp, although even then it only occurs sporadically
	# We now delete truffle-related files from /tmp at the beginning, so this shouldn't happen anymore
	printf "NO SPACE LEFT ERROR"
elif grep 'ReferenceError: bytes'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "BYTES ARGUMENT ERROR"
elif grep 'Stack too deep'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "STACK TOO DEEP ERROR"
elif grep 'Cannot locate data in profiling log'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	# This is probably a transaction conflict such as
	#      instance.send.sendTransaction()
	# which may possibly be addressed by renaming the contract's send() to say _send() (and calls to it)
	# Or it could be malformed transaction arguments
	printf "MALFORMED TRANSACTION"
elif grep 'oversized data' current-contract-out.log >/dev/null; then
	printf "GETH DEPLOYMENT GAS ERROR #1"
elif grep 'please check your gas limit' current-contract-out.log >/dev/null; then
	printf "GETH DEPLOYMENT GAS ERROR #2"
elif grep 'ERROR: Memory state difference between original and instrumented' current-contract-out.log >/dev/null; then
	printf "POSSIBLE BUG: INSTRUMENTATION STORAGE ERROR" 
	PROGRESS_SUMMARY=bug-candidate-event
elif grep 'ERROR: Memory state difference between original and mutated' current-contract-out.log >/dev/null; then
	printf "POSSIBLE BUG: MUTATION STORAGE ERROR"
	PROGRESS_SUMMARY=bug-candidate-event
elif grep 'ERROR: Event log difference between' current-contract-out.log >/dev/null; then
	printf "POSSIBLE BUG: EVENT LOG ERROR"
	PROGRESS_SUMMARY=bug-candidate-event
elif grep 'Mutator failed for live EMI application' current-contract-out.log >/dev/null; then
	printf "MUTATION GENERATION ERROR"
elif grep 'JavaScript heap out of memory'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null; then
	# See https://github.com/trufflesuite/truffle/issues/957 for a possible solution
	printf "TRUFFLE JAVASCRIPT HEAP ERROR"
elif grep 'contains unresolved libraries'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "LIBRARY UNRESOLVED ERROR"
elif grep 'Invalid number of arguments to Solidity function'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "INVALID ARGUMENT COUNT"
elif grep 'True expression'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "COMPILE ERROR: CONDOP TYPE MISMATCH"
elif grep 'Attempting to run transaction which calls a contract function, but recipient address'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "CALL TO NON-CONTRACT ADDRESS"
elif grep 'invalid solidity type'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "SOLIDITY TYPE ERROR"
elif grep 'Tracer failed: Command failed'   "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "FRAMEWORK TRACER ERROR"
elif grep 'Cannot find transactable contract'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "NO TRANSACTABLE CONTRACT" 
elif grep 'TypeError: Name has to refer to a struct'   "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "COMPILE ERROR: UDT NAME UNKNOWN"
elif grep 'TypeError: Exactly one argument expected for explicit type conversion'  "$PATH_TRUFFLE_LOG_FILE" >/dev/null;  then
	printf "CAST ARGUMENT COUNT ERROR" 
else
	printf ERROR
#exit 1
fi
echo

echo "$PROGRESS_SUMMARY" >progress-summary.log

exit "$RC"
