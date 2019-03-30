#!/bin/sh

# Baseline script with the major use cases applied to comparatively
# small test sets (which are however unlikely to pass entirely without
# errors if there are bugs).



usage() {
	echo "Usage:"
	echo "    mini-baseline-generated.sh [docker-instances] [working-directory]"
	echo
#	echo "[seed]              - start seed"   let's randomize this instead to add some more diversity
	echo "[docker-instances]  - 0 for no docker, >0 for the docker instance count to use."
	echo "                      See also ./tools/docker-benchmark.sh to find a good value."
	echo "[working-directory] - where contracts and docker logs are stored"
	exit 1
}

if test "$#" != 2; then
	usage
fi

DOCKER_INSTANCES="$1"
WORKING_DIRECTORY="$2"
case "$DOCKER_INSTANCES" in
''|*[!0-9]*)
	echo Error: Invalid docker instance count - not a number
	usage
esac


if test -d "$WORKING_DIRECTORY"; then
	echo Error: Working directory "$WORKING_DIRECTORY" already exists - please delete it first.
	exit 1
fi
mkdir -p "$WORKING_DIRECTORY"


generate_and_run_contract_set() {
	if test "$#" != 9; then
		echo Bug: Invalid argument count for generate_and_run_contract_set: $#
		exit 1
	fi

	if test "$DOCKER_INSTANCES" = 0; then
		generate-and-run-contract-set.sh $1 $2 $3 $4 $5 $6 $7 $8 $9
	else
		DOCKER_LOGDIR="$7" docker-generate-and-run-contract-set.sh $DOCKER_INSTANCES $1 $2 $3 $4 $5 $6 $7 $8 $9
	fi	
}



echo Please check settings.cfg.sh - are the settings OK for the test run?
echo CODEGEN_AVOID_EXP_OPERATOR / CODEGEN_AVOID_SHIFT_OPERATORS should probably be disabled to work around ganache-cli bugs. 
echo
echo 
printf "Proceed? [y] "
read input
if test "$input" != "" && test "$input" != y; then
	exit 1
fi

SETTINGS=`dirname $0`/../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi




CONTRACT_COUNT=100
SEED=`date +%s`

# Plain Assignment-Sequence
echo ===== Test 1: Plain AS contracts ======
generate_and_run_contract_set $CONTRACT_COUNT $SEED 10 1 10 40 "$WORKING_DIRECTORY/test1" --assignmentSequence 0

# Live-EMI / semantically-equivalent transformation
# 1. On "Assignment-Sequence" contracts
echo ===== Test 2: Live-EMI on AS contracts ======
SEED=`date +%s`
generate_and_run_contract_set $CONTRACT_COUNT $SEED 10 1 10 40 "$WORKING_DIRECTORY/test2" --assignmentSequence 4

# 2. On "Complete" contracts
echo ===== Test 3: Live-EMI on Complete contracts ======
SEED=`date +%s`
generate_and_run_contract_set $CONTRACT_COUNT $SEED 10 1 2 40 "$WORKING_DIRECTORY/test3" --complete 4

# DOL
echo ===== Test 4: DOL on AS contracts ======
SEED=`date +%s`
generate_and_run_contract_set $CONTRACT_COUNT $SEED 10 1 10 40 "$WORKING_DIRECTORY/test4" --assignmentSequence optimize=2000,yul

echo ===== Test 5: DOL on Complete contracts ======
SEED=`date +%s`
generate_and_run_contract_set $CONTRACT_COUNT $SEED 10 1 2 40 "$WORKING_DIRECTORY/test5" --complete optimize=2000,yul
