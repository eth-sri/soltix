#!/bin/sh

if test "$PATHSDIR" = ""; then
	echo Error: paths.cfg.sh must be supplied with test-env-truffle directory path via PATHSDIR environment variable
	exit 1
fi

BASEDIR="$PATHSDIR"

# Note: For "." input from another script, $0 is the path of THAT script
export PATH_TMPDIR="$BASEDIR"/_tmp

export PATH_PROJECT_DIR="$BASEDIR"/_current-project
export PATH_INPUT_DIR="$PATH_PROJECT_DIR"/input
export PATH_OUTPUT_DIR="$PATH_PROJECT_DIR"/output

export PATH_TRUFFLE_LOG_FILE="$PATH_OUTPUT_DIR"/TruffleTest.log
export PATH_TRUFFLE_RPC_LOG_FILE="$PATH_OUTPUT_DIR"/TruffleTest.rpc.log
export PATH_MAIN_RESULTS_DIR="$PATH_OUTPUT_DIR"/main_results

export PATH_PROFILING_EVENTS_LOG="$PATH_INPUT_DIR"/profiling-events.log

export GETH_BASEDIR="$BASEDIR"/geth
export GETH_BASEDIR=`realpath $GETH_BASEDIR`
export GETH_LOG="$GETH_BASEDIR/geth.log"
export GETH_CONFIG="$GETH_BASEDIR"/genesis.json
export GETH_DATADIR="$GETH_BASEDIR"/datadir
export GETH_KEYSTORE_SOURCE="$GETH_BASEDIR"/keystore
export GETH_KEYSTORE_LIVE="$GETH_DATADIR"/keystore

export TRUFFLE_CONFIG_DIR="$BASEDIR"/truffle-config
export TRUFFLE_CONFIG_DIR=`realpath $TRUFFLE_CONFIG_DIR`
export TRUFFLE_EXTERNAL_COMPILE_SCRIPT="$BASEDIR"/tools/external-solc/truffle-compile.js
export TRUFFLE_EXTERNAL_COMPILE_SCRIPT=`realpath $TRUFFLE_EXTERNAL_COMPILE_SCRIPT`

export EXTERNAL_SOLC_DIR="$BASEDIR"/tools/external-solc
