#!/bin/sh


usage() {
	echo "Usage: "
	echo "Same as generate-and-run-contract-set.sh, but using docker."
	echo
	echo "Supply"
	echo "    - docker instance count"
	echo "    - settings.cfg.sh overlay file path (defining environment variables that will override settings.cfg.sh variables in container)"
	echo "    - followed by code generation arguments (in generate-contract-set.sh format)"
	echo "    - followed by execution arguments (in run-all-tests.sh format):"
	echo
        echo "       docker-generate-and-run-contract-set.sh [docker-instance-count] [settings.cfg.sh-overlay-file | none] [contract-count-per-instance] [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [result-directory] [--assignmentSequence|--complete] [mutations-count | c0 | optimize=<runs>[,yul|,standard]]"
        exit 1
}

SETTINGS=`dirname $0`/../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

if test "$#" != 11; then
	echo docker-generate-and-run-contract-set: Invalid argument count $#
	usage
fi


DOCKER_COUNT=$1
SETTINGS_OVERLAY=$2
CONTRACT_COUNT=$3
SEED=$4
FUNCTION_COUNT=$5
STMT_LOWER_BOUND=$6
STMT_UPPER_BOUND=$7
VARIABLE_COUNT=$8
CONTRACT_DIR_HOST="$9"
shift 9

CONTRACT_TYPE=$1
RUN_MODE="$2"


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

if test "$SETTINGS_OVERLAY" = none; then
	SETTINGS_OVERLAY_ARGUMENT=""
else
	if ! test -f "$SETTINGS_OVERLAY"; then
		echo "Error: Settings overlay file $SETTINGS_OVERLAY does not exist (use 'none' if no settings overlay is desired)"
		exit 1
	fi
	SETTINGS_OVERLAY_ARGUMENT="-e `realpath $SETTINGS_OVERLAY`"
fi


start_instance_local() {
	INSTANCE_NAME=soltix$INSTANCE
	ALL_INSTANCE_NAMES="$ALL_INSTANCE_NAMES $INSTANCE_NAME"
	REDIR_FILE="$CONTRACT_DIR_HOST/$INSTANCE".txt

	docker rm -f $INSTANCE_NAME  >/dev/null 2>&1

	# Obtain host IP as coordinator address
	HOST_IP=`ip -4 addr show docker0 | grep inet | awk -F/ '{print $1}' | awk '{print $2}'`

	echo docker run --name soltix$INSTANCE --mount type=bind,source="`realpath $CONTRACT_DIR_HOST`",target=/VOL \
		-e SOLTIX_COORDINATOR_HOST=$HOST_IP -e SOLTIX_NODE_ID=$INSTANCE $SETTINGS_OVERLAY_ARGUMENT \
		soltix \
		"$SCRIPT_PATH" \
		$CONTRACT_COUNT $SEED $FUNCTION_COUNT $STMT_LOWER_BOUND $STMT_UPPER_BOUND $VARIABLE_COUNT $CONTRACT_DIR_CONTAINER/$INSTANCE $CONTRACT_TYPE $RUN_MODE



	(sleep 10 ; docker run --name soltix$INSTANCE --mount type=bind,source="`realpath $CONTRACT_DIR_HOST`",target=/VOL \
		-e SOLTIX_COORDINATOR_HOST=$HOST_IP -e SOLTIX_NODE_ID=$INSTANCE $SETTINGS_OVERLAY_ARGUMENT \
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
