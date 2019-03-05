#!/bin/sh

# Note: For "." input from another script, $0 is the path of THAT script
export PATH_TMPDIR=`dirname $0`/../_tmp

export PATH_PROJECT_DIR=`dirname $0`/../_current-project
export PATH_INPUT_DIR="$PATH_PROJECT_DIR"/input
export PATH_OUTPUT_DIR="$PATH_PROJECT_DIR"/output

export PATH_TRUFFLE_LOG_FILE="$PATH_OUTPUT_DIR"/TruffleTest.log
export PATH_TRUFFLE_RPC_LOG_FILE="$PATH_OUTPUT_DIR"/TruffleTest.rpc.log
export PATH_MAIN_RESULTS_DIR="$PATH_OUTPUT_DIR"/main_results

export PATH_PROFILING_EVENTS_LOG="$PATH_INPUT_DIR"/profiling-events.log

