#!/bin/sh


usage() {
	echo "Usage: "
	echo "Same as generate-and-run-contract-set.sh, but using docker."
	echo
	echo "Supply"
	echo "    - local/gcloud execution selection"
	echo "    - maximum duration (cap cloud processing costs in advance in case things go wrong)"
	echo "    - docker instance count"
	echo "    - settings.cfg.sh overlay file path (defining environment variables that will override settings.cfg.sh variables in container)"
	echo "    - followed by code generation arguments (in generate-contract-set.sh format)"
	echo "    - followed by execution arguments (in run-all-tests.sh format):"
	echo
        echo "       docker-generate-and-run-contract-set.sh [local | gcloud] [max-duration-in-seconds] [docker-instance-count] [settings.cfg.sh-overlay-file | none] [contract-count-per-instance] [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [result-directory] [--assignmentSequence|--complete] [mutations-count | c0 | optimize=<runs>[,yul|,standard]]"
        exit 1
}

SETTINGS=`dirname $0`/../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

if test "$#" != 13; then
	echo docker-generate-and-run-contract-set: Invalid argument count $#
	usage
fi


MODE=$1

if test "$MODE" != local && test "$MODE" != gcloud; then
	echo Error: mode should be local or gcloud, but is $MODE
	usage
fi
MAX_DURATION="$2"
# Mutation - must be number
case "$MAX_DURATION" in
	''|*[!0-9]*)
		echo Error: Invalid duration $MAX_DURATION not a number
		usage
		;;
	*) ;;
esac

shift 2
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

echo Instance count $DOCKER_COUNT


if test "$DOCKER_LOGDIR" = ""; then
	DOCKER_LOGDIR="docker-logs"
fi

if test -d "$CONTRACT_DIR_HOST"; then
       echo Contract directory "$CONTRACT_DIR_HOST" already exists - please delete it first
       exit 1
fi       

mkdir "$CONTRACT_DIR_HOST"

EXECUTOR_SCRIPT_PATH="./tools/generate-and-run-contract-set.sh"
COORDINATOR_SCRIPT_PATH="./tools/coordinator/bin/start-progress-receiver.sh"




if test "$SETTINGS_OVERLAY" = none; then
	SETTINGS_OVERLAY_ARGUMENT=""
else
	if ! test -f "$SETTINGS_OVERLAY"; then
		echo "Error: Settings overlay file $SETTINGS_OVERLAY does not exist (use 'none' if no settings overlay is desired)"
		exit 1
	fi
	if test "$MODE" = local; then
		          SETTINGS_OVERLAY_ARGUMENT="--env-file `realpath $SETTINGS_OVERLAY`"
	else
		SETTINGS_OVERLAY_ARGUMENT="--container-env-file `realpath $SETTINGS_OVERLAY`"
	fi
fi

COORDINATOR_INSTANCE_NAME=soltix-coordinator

###################################################################################################
########################################### Local variant #########################################
###################################################################################################

LOCAL_CONTRACT_DIR_CONTAINER="/VOL"

# Obtain host IP as coordinator address
LOCAL_COORDINATOR_HOST_IP=`ip -4 addr show docker0 | grep inet | awk -F/ '{print $1}' | awk '{print $2}'`

start_instance_local() {
	INSTANCE_ID=$1
	INSTANCE_NAME=$2
	ALL_INSTANCE_NAMES="$ALL_INSTANCE_NAMES $INSTANCE_NAME"
	REDIR_FILE="$CONTRACT_DIR_HOST/$INSTANCE_ID".txt

	docker rm -f $INSTANCE_NAME  >/dev/null 2>&1


	echo docker run --name soltix$INSTANCE_ID --mount type=bind,source="`realpath $CONTRACT_DIR_HOST`",target=/VOL \
		-e SOLTIX_COORDINATOR_HOST=$LOCAL_COORDINATOR_HOST_IP -e SOLTIX_NODE_ID=$INSTANCE_ID $SETTINGS_OVERLAY_ARGUMENT \
		soltix \
		"$EXECUTOR_SCRIPT_PATH" \
		$CONTRACT_COUNT $SEED $FUNCTION_COUNT $STMT_LOWER_BOUND $STMT_UPPER_BOUND $VARIABLE_COUNT $LOCAL_CONTRACT_DIR_CONTAINER/$INSTANCE_ID $CONTRACT_TYPE $RUN_MODE



	(sleep 10 ; docker run --name soltix$INSTANCE_ID --mount type=bind,source="`realpath $CONTRACT_DIR_HOST`",target=/VOL \
		-e SOLTIX_COORDINATOR_HOST=$LOCAL_COORDINATOR_HOST_IP -e SOLTIX_NODE_ID=$INSTANCE_ID $SETTINGS_OVERLAY_ARGUMENT \
		soltix \
		"$EXECUTOR_SCRIPT_PATH" \
		$CONTRACT_COUNT $SEED $FUNCTION_COUNT $STMT_LOWER_BOUND $STMT_UPPER_BOUND $VARIABLE_COUNT $LOCAL_CONTRACT_DIR_CONTAINER/$INSTANCE_ID $CONTRACT_TYPE $RUN_MODE \
		) >"$REDIR_FILE"   2>&1   &
	# TODO schedule container shutdown? or don't care locally?
}



start_coordinator_local() {
	docker rm -f $COORDINATOR_INSTANCE_NAME  >/dev/null 2>&1
echo    docker run -p "$LOCAL_COORDINATOR_HOST_IP":22732:22732/tcp --name $COORDINATOR_INSTANCE_NAME $SETTINGS_OVERLAY_ARGUMENT soltix "$COORDINATOR_SCRIPT_PATH" "$DOCKER_COUNT"
	docker run -p "$LOCAL_COORDINATOR_HOST_IP":22732:22732/tcp --name $COORDINATOR_INSTANCE_NAME $SETTINGS_OVERLAY_ARGUMENT soltix "$COORDINATOR_SCRIPT_PATH" "$DOCKER_COUNT" >/dev/null 2>&1 &
}

stop_instance_local() {
	echo docker wait $1
	docker wait $1
}

stop_coordinator_local() {
	stop_instance_local "$COORDINATOR_INSTANCE_NAME"
}

follow_coordinator_local() {
	docker logs -f $COORDINATOR_INSTANCE_NAME
}


###################################################################################################
########################################### Cloud variant #########################################
#############
######################################################################################

GCLOUD_ZONE="europe-west1-b europe-west2-b europe-west3-b"
GCLOUD_SELECTED_ZONE=""

# TODO Sanity check on zones - are they up?
# ... gcloud compute zones list ...


# Map node ID to zone (stored in GCLOUD_SELECTED_ZONE) to address 8-nodes-per-zone limitation
get_gcloud_zone_by_id() {
	# The default limit is apparently 8 instances per zone
	# The instance id is 0-based, so 0-7 go to zone 1, 8 to 15 to zone 2, etc.
	ZONE_INDEX=`expr $1 / 8`
	ZONE_INDEX=`expr $ZONE_INDEX + 1`
	ZONE=`echo $GCLOUD_ZONE | awk '{print $'$ZONE_INDEX'}'`
	if test "$ZONE" = ""; then
		echo Error: Too many gcloud nodes at $1, invalid zone index $ZONE 
		# TODO shutdown and exit?
	fi
	GCLOUD_SELECTED_ZONE=$ZONE
}

# default:
#	n1-standard-1
# see:
#	gcloud compute machine-types list | grep europe-west1-b
# and
#	https://cloud.google.com/compute/pricing#machinetype
#
# ./tools/docker-generate-and-run-contract-set.sh local 600 1 settings-overlay.cfg.sh 5 1 10 1 10 40 X --assignmentSequence  0 
#    n1-standard-1: avg ca. 30s/case
#    n1-standard-2: avg ca. 28s/case
#    n1-highcpu-2:  avg ca. 30s/case 
#    n1-highmem-2:  avg ca. 30s/case
MACHINE_TYPE=n1-standard-1

IMAGE_ADDRESS=eu.gcr.io/soltix/soltix
if test "$IMAGE_ADDRESS" = ""; then
	echo Error: IMAGE_ADDRESS not set - should hold soltix docker image address
	exit 1
fi

do_start_instance_gcloud() {
	INSTANCE_NUMBER="$1"  # only meaningful for worker nodes
	INSTANCE_NAME="$2"

	if test "$INSTANCE_NUMBER" != 0; then
		INSTANCE_ID=$INSTANCE_NUMBER
		GCLOUD_COORDINATOR_HOST_IP=`gcloud compute instances list | grep soltix-coordinator | awk '{print $4}'`
		COORDINATOR_COMM_ARGS="--container-env SOLTIX_COORDINATOR_HOST=$GCLOUD_COORDINATOR_HOST_IP --container-env SOLTIX_NODE_ID=$INSTANCE_NUMBER"
	else
		INSTANCE_ID=0
		COORDINATOR_COMM_ARGS=""
	fi

	shift 2

	# Build argument list
	ARGLIST=""
	while test "$#" != 0; do
		ARGLIST="$ARGLIST --container-arg=$1"
		shift 1
	done


	get_gcloud_zone_by_id $INSTANCE_ID
	

	echo        gcloud compute instances create-with-container "$INSTANCE_NAME" --zone="$GCLOUD_SELECTED_ZONE" --machine-type="$MACHINE_TYPE" --container-image "$IMAGE_ADDRESS" \
                --container-restart-policy never \
                $SETTINGS_OVERLAY_ARGUMENT \
		$COORDINATOR_COMM_ARGS \
                $ARGLIST

	# Note:
	#   - overlay argument may be empty
	#   - arglist may expand to multiple arguments
	#   - the docker instance will use the host's network stack, so the coordinator port need not be exported
	gcloud compute instances create-with-container "$INSTANCE_NAME" --zone="$GCLOUD_SELECTED_ZONE" --machine-type="$MACHINE_TYPE" --container-image "$IMAGE_ADDRESS" \
        	--container-restart-policy never \
		$SETTINGS_OVERLAY_ARGUMENT \
		$COORDINATOR_COMM_ARGS \
		$ARGLIST

		# arglist e.g.:
	        #--container-arg=./tools/generate-and-run-contract-set.sh \
	        #--container-arg=1 \
	        #--container-arg=1 \
	        #--container-arg=1 \
	        #--container-arg=1 \
	        #--container-arg=1 \
	        #--container-arg=1 \
	        #--container-arg=/TMP \
	        #--container-arg=--complete \
	        #--container-arg=0
	
	# Schedule shutdown at a specified point in the future. This catches hangups that
	# prevent the coordinator from terminating, as well as connectivity issues to the
	# coordinator
	# TODO
#	gcloud compute ssh "$INSTANCE_NAME" -- nohup sudo /bin/sh -c '(sleep '$MAX_DURATION'; shutdown -h now) &'
}



start_instance_gcloud() {
	# TODO instead of saving to a container-local RESULTS directory, consider exporting them.
	# (Uncertain benefit due to reproducibility of results)
	do_start_instance_gcloud $1 $2 "$EXECUTOR_SCRIPT_PATH" \
		$CONTRACT_COUNT $SEED $FUNCTION_COUNT $STMT_LOWER_BOUND $STMT_UPPER_BOUND $VARIABLE_COUNT RESULTS $CONTRACT_TYPE $RUN_MODE

}

stop_instance_gcloud() {
	INSTANCE_ID=$1
	INSTANCE_NAME=$2
	get_gcloud_zone_by_id $INSTANCE_ID
	gcloud compute instances stop --zone "$GCLOUD_SELECTED_ZONE" -q "$INSTANCE_NAME"
	gcloud compute instances delete --zone "$GCLOUD_SELECTED_ZONE" -q "$INSTANCE_NAME"
}

start_coordinator_gcloud() {
	do_start_instance_gcloud 0 "$COORDINATOR_INSTANCE_NAME" "$COORDINATOR_SCRIPT_PATH" "$DOCKER_COUNT"

	# allow coordinator connection
	gcloud compute firewall-rules create coordinator-rule --allow tcp:22732
}

stop_coordinator_gcloud() {
	stop_instance_gcloud 0 "$COORDINATOR_INSTANCE_NAME" &   # in background
}

follow_coordinator_gcloud() {
	# Retrieve docker container ID from within the compute instance
	# TODO do this prior to starting worker nodes to fix race?
	while true; do
		SOLTIX_LINE=`gcloud compute ssh "$COORDINATOR_INSTANCE_NAME" -- docker ps -a | grep soltix`
		if test "$SOLTIX_LINE" != ""; then
			break
		fi
		echo soltix coordinator container not running yet, retrying...
		sleep 1
	done


	CONTAINER_ID=`echo $SOLTIX_LINE | awk '{print $1}'`

	# Follow coordinator container
	echo gloud compute ssh "$COORDINATOR_INSTANCE_NAME" -- docker logs -f $CONTAINER_ID
	gcloud compute ssh "$COORDINATOR_INSTANCE_NAME" -- docker logs -f $CONTAINER_ID
}


###################################################################################################
########################################### Do it #################################################
###################################################################################################

start_instance() {
	if test "$#" != 2; then
		echo Bug: start_instance requires arguments: instance-id instance-name
		exit 1
	fi
	INSTANCE_ID=$1
	INSTANCE_NAME=$2
	echo ===== Starting instance $INSTANCE_NAME ...
	if test "$MODE" = local; then
		start_instance_local $INSTANCE_ID $INSTANCE_NAME
	else
		start_instance_gcloud $INSTANCE_ID $INSTANCE_NAME
	fi
	echo ===== Done starting instance $INSTANCE_NAME ...
}

stop_instance() {
	if test "$#" != 2; then
		echo Bug: stop_instance requires instance id and instance name argument
		exit 1
	fi

	INSTANCE_ID=$1
	INSTANCE_NAME=$2
	echo ===== Stopping instance $INSTANCE_NAME ...
	if test "$MODE" = local; then
		stop_instance_local $INSTANCE_NAME
	else
		stop_instance_gcloud $INSTANCE_ID $INSTANCE_NAME &    # in background
	fi
	echo ===== Done stopping instance $INSTANCE_NAME ...
}

start_coordinator() {
	echo ===== Starting coordinator ...
	if test "$MODE" = local; then
		start_coordinator_local
	else
		start_coordinator_gcloud
	fi
	echo ===== Done starting coordinator
}

stop_coordinator() {
	echo ===== Stopping coordinator ...
	if test "$MODE" = local; then
		stop_coordinator_local
	else
		stop_coordinator_gcloud
	fi
	echo ===== Done stopping coordinator
}

follow_coordinator() {
	if test "$MODE" = local; then
		follow_coordinator_local
	else
		follow_coordinator_gcloud
	fi
}





INSTANCE_ID=0
start_coordinator
sleep 10 

i=0
while test "$i" -lt "$DOCKER_COUNT"; do
	INSTANCE_ID=`expr $i + 1` # id starts at 1 (coordinator is at 0)
	INSTANCE_NAME=soltix$INSTANCE_ID

	start_instance $INSTANCE_ID $INSTANCE_NAME

	SEED=`expr $SEED + $CONTRACT_COUNT`
	i=`expr $i + 1`
done

echo Started $DOCKER_COUNT instances: $ALL_INSTANCE_NAMES

# Await end
follow_coordinator


i=0
# TODO instance id generation redundant to loop above
while test "$i" -lt "$DOCKER_COUNT"; do
	INSTANCE_ID=`expr $i + 1`
	INSTANCE_NAME=soltix$INSTANCE

	stop_instance $INSTANCE_ID $INSTANCE_NAME

	i=`expr $i + 1`
done


stop_coordinator

