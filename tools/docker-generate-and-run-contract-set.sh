#!/bin/sh


usage() {
	echo "Usage: "
	echo "Same as generate-and-run-contract-set.sh, but using docker."
	echo
	echo "Supply"
	echo "    - docker instance count"
	echo "    - followed by code generation arguments (in generate-contract-set.sh format)"
	echo "    - followed by execution arguments (in run-all-tests.sh format):"
	echo
        echo "       docker-generate-and-run-contract-set.sh [docker-instance-count] [contract-count-per-instance] [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [result-directory] [--assignmentSequence|--complete] [mutations-count | c0 | optimize=<runs>[,yul|,standard]]"
        exit 1
}

SETTINGS=`dirname $0`/../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

if test "$#" != 10; then
	echo docker-generate-and-run-contract-set: Invalid argument count $#
	usage
fi


DOCKER_COUNT=$1
CONTRACT_COUNT=$2
SEED=$3
FUNCTION_COUNT=$4
STMT_LOWER_BOUND=$5
STMT_UPPER_BOUND=$6
VARIABLE_COUNT=$7
CONTRACT_DIR_HOST="$8"
CONTRACT_TYPE=$9

shift 9
RUN_MODE="$1"


if test "$DOCKER_LOGDIR" = ""; then
	DOCKER_LOGDIR="docker-logs"
fi

if test -d "$CONTRACT_DIR_HOST"; then
       echo Contract directory "$CONTRACT_DIR_HOST" already exists - please delete it first
       exit 1
fi       

mkdir "$CONTRACT_DIR_HOST"

SCRIPT_PATH="./tools/generate-and-run-contract-set.sh"

CONTRACT_DIR_CONTAINER="/VOL"


start_instance_local() {
	INSTANCE_NAME=soltix$INSTANCE
	ALL_INSTANCE_NAMES="$ALL_INSTANCE_NAMES $INSTANCE_NAME"
	REDIR_FILE="$CONTRACT_DIR_HOST/$INSTANCE".txt

	docker rm -f $INSTANCE_NAME  >/dev/null 2>&1

	# Obtain host IP as coordinator address
	HOST_IP=`ip -4 addr show docker0 | grep inet | awk -F/ '{print $1}' | awk '{print $2}'`

	echo docker run --name $INSTANCE_NAME --mount type=bind,source="`realpath $CONTRACT_DIR_HOST`",target=/VOL soltix "$SCRIPT_PATH" $CONTRACT_COUNT $SEED $FUNCTION_COUNT $STMT_LOWER_BOUND $STMT_UPPER_BOUND $VARIABLE_COUNT $CONTRACT_DIR_CONTAINER/$INSTANCE $CONTRACT_TYPE $RUN_MODE
	(sleep 10 ; docker run --name soltix$INSTANCE --mount type=bind,source="`realpath $CONTRACT_DIR_HOST`",target=/VOL \
		-e SOLTIX_COORDINATOR_HOST=$HOST_IP -e SOLTIX_NODE_ID=$INSTANCE \
		soltix \
		"$SCRIPT_PATH" \
		$CONTRACT_COUNT $SEED $FUNCTION_COUNT $STMT_LOWER_BOUND $STMT_UPPER_BOUND $VARIABLE_COUNT $CONTRACT_DIR_CONTAINER/$INSTANCE $CONTRACT_TYPE $RUN_MODE \
		) >"$REDIR_FILE"   2>&1   &
}

start_instance() {
	start_instance_local
}


i=0
while test "$i" -lt "$DOCKER_COUNT"; do
	INSTANCE=`expr $i + 1`

	start_instance

	SEED=`expr $SEED + $CONTRACT_COUNT`
	i=`expr $i + 1`
done


start-progress-receiver.sh $DOCKER_COUNT

echo waiting on docker jobs $ALL_INSTANCE_NAMES 
docker wait $ALL_INSTANCE_NAMES 
