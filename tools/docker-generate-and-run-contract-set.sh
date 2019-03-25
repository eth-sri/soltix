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
        echo "       docker-generate-and-run-contract-set.sh [docker-instance-count] [contract-count] [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count] [result-directory] [--assignmentSequence|--complete] [mutations-count | c0 | optimize=<runs>[,yul|,standard]]"
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
CONTRACT_DIR="$8"

if test "$DOCKER_LOGDIR" = ""; then
	DOCKER_LOGDIR="docker-logs"
fi

if test -d "$CONTRACT_DIR"; then
       echo Contract directory "$CONTRACT_DIR" already exists - please delete it first
       exit 1
fi       
mkdir "$CONTRACT_DIR"


#SCRIPT_PATH="`dirname $0`/generate-and-run-contract-set.sh"
SCRIPT_PATH="./tools/generate-and-run-contract-set.sh"

COMMAND_PREFIX="docker run --name #NAME# --mount type=bind,source="`realpath $CONTRACT_DIR`",target=/VOL soltix $SCRIPT_PATH"
CONTRACT_DIR="/VOL"

shift 3
COMMAND_POSTFIX="$1 $2 $3 $4 #DIR# $6 $7"


rm -rf _jobs.tmp
echo running   dockerize-set-command.sh "$COMMAND_PREFIX" $CONTRACT_COUNT $SEED $DOCKER_COUNT $CONTRACT_DIR "$COMMAND_POSTFIX" "$DOCKER_LOGDIR"
dockerize-set-command.sh "$COMMAND_PREFIX" $CONTRACT_COUNT $SEED $DOCKER_COUNT $CONTRACT_DIR "$COMMAND_POSTFIX" "$DOCKER_LOGDIR" | while read line; do
	if test "$FIRST" = ""; then
		cat _logfile.tmp >>_first.tmp
		FIRST="$line"
	fi

	echo running  $line
	REDIR_FILE=`cat _redir.tmp`
	$line >"$REDIR_FILE" 2>&1 &

	jobs -p >_jobs.tmp
done
rm -f _logfile.tmp
rm -f _redir.tmp


FIRST_LOGFILE=`cat _first.tmp | tail -2 | head -1`

#echo Following first instance...
#echo $FIRST
#tail -f "$FIRST_LOGFILE"

#wait_on_bg_jobs() {
#        start=`date +%s`
#        sleep 5
#        echo waiting on `cat _jobs.tmp`
#        wait `cat _jobs.tmp`
#	rm -f _jobs.tmp
#        end=`date +%s`
#        echo took `expr $end - $start` s
#}

#wait_on_bg_jobs

sleep 5
echo waiting on docker jobs `cat _docker_names.tmp`
docker wait `cat _docker_names.tmp`
