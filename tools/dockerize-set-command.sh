#!/bin/sh


usage() {
	echo Usage:
	echo
	echo "   split-set-command.sh [command-prefix] [contract-count] [random-seed] [docker-count] [contract-dir] [command-postfix] [logdir]"
	echo
	echo "This can be used to break up a contract set for multiple docker commands to enable parallelization."
	echo "Prefix and postfix can be enclosed in quotes to prevent whitespace issues."
	echo "Specify an empty \"\" logdir argument to avoid logging (output is written to the console)"
	echo
	echo "Example: parallelize the command for 2 docker instances:"
	echo "    ./tools/generate-and-run-contract-set.sh 10 1 10 5 5 30 DIR --assignmentSequence 0" docker-logs
	echo "Assign:"
	echo "    [command-prefix]  = ./tools/generate-and-run-contract-set.sh"
	echo "    [contract-count]  = 10"
	echo "    [random-seed]     = 1"
	echo "    [docker-count]    = 2"
	echo "    [contract-dir]    = DIR"
	echo "    [command-postfix] = 10 5 5 30 --assignmentSequence 0"
	echo "    [logdir]          = docker-logs"
        echo "Run:"
	echo '    ./tools/dockerize-set-command.sh "docker run soltix ./tools/generate-and-run-contract-set.sh" 10 1 2 DIRPATH "10 5 5 30 --assignmentSequence #DIR# 0" "docker-logs"'
	echo "Giving:"
	echo "    docker run ./tools/generate-and-run-contract-set.sh 5 1 2 10 5 5 30 DIR/1 --assignmentSequence 0 docker-logs/1-logname"
	echo "    docker run ./tools/generate-and-run-contract-set.sh 5 6 2 10 5 5 30 DIR/2 --assignmentSequence 0 docker-logs/2-logname"
	exit 1
}

if test "$#" -lt 7; then
	echo Error: Invalid argument count $#
	usage
fi

COMMAND_PREFIX_TEMPLATE="$1"
CONTRACT_COUNT="$2"
RANDOM_SEED="$3"
DOCKER_COUNT="$4"
CONTRACT_DIR="$5"
COMMAND_POSTFIX_TEMPLATE="$6"
DOCKER_LOGDIR="$7"

verify_number() {
	case $1 in
	''|*[!0-9]*)
		echo Error: Invalid argument for $2 - not a number
		usage
	esac
}

verify_number "$CONTRACT_COUNT" "contract count"
verify_number "$RANDOM_SEED" "random seed"
verify_number "$DOCKER_COUNT" "docker instances"

SPLIT_SIZE=`expr $CONTRACT_COUNT / $DOCKER_COUNT`
REST=`expr $SPLIT_SIZE \* $DOCKER_COUNT`
REST=`expr $CONTRACT_COUNT - $REST`

#echo split count $SPLIT_SIZE rest $REST

OUTSTANDING=$CONTRACT_COUNT
CURRENT_SEED=$RANDOM_SEED
ITERATION=1

if ! test -d "$DOCKER_LOGDIR"; then
	mkdir "$DOCKER_LOGDIR"
fi

rm -f _docker_names.tmp
while test "$OUTSTANDING" != 0; do
	if test "$OUTSTANDING" = "$CONTRACT_COUNT"; then
		# First element receives split rest
		ITEMS=`expr $SPLIT_SIZE + $REST`
	else
		ITEMS=$SPLIT_SIZE
	fi

	# Build command
	if test "$DOCKER_LOGDIR" != ""; then
		FILENAME=${ITERATION}-`date | sed 's/:/_/g' | sed 's/ /_/g'`.out-err
		LOGFILE="$DOCKER_LOGDIR/$FILENAME"
		echo "$LOGFILE" >_logfile.tmp
		#OUTPUT_REDIR=">$LOGFILE 2>&1"
		echo "$LOGFILE" >_redir.tmp
	fi
#	rm -rf "${CONTRACT_DIR}/${ITERATION}"
#	if ! test -d "${CONTRACT_DIR}"; then
#		mkdir "${CONTRACT_DIR}"
#	fi
	DOCKER_NAME="soltix${ITERATION}"

#	echo Removing $DOCKER_NAME if existing...
	docker rm -f $DOCKER_NAME  >/dev/null 2>&1

	COMMAND_PREFIX="`echo $COMMAND_PREFIX_TEMPLATE | sed 's/#NAME#/'$DOCKER_NAME'/g'`"
	echo "$DOCKER_NAME" >>_docker_names.tmp

	# Add \ escape in input text because the docker volume dir will have a leading "/"
	COMMAND_POSTFIX="`echo $COMMAND_POSTFIX_TEMPLATE | sed 's/#DIR#/\'${CONTRACT_DIR}"\/"${ITERATION}'/g'`"

	echo $COMMAND_PREFIX $ITEMS $CURRENT_SEED $COMMAND_POSTFIX  # $OUTPUT_REDIR

	OUTSTANDING=`expr $OUTSTANDING - $ITEMS`
	CURRENT_SEED=`expr $CURRENT_SEED + $ITEMS`
	ITERATION=`expr $ITERATION + 1`
done
