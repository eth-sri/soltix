#!/bin/sh

usage() {
	echo "Usage: ./run-all-tests.sh [directory] [mutations-count] [--skipto=num]"
	exit 1
}

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

. `dirname $0`/../paths.cfg.sh
if ! test "$?" = 0; then
        echo Error: Cannot load ./paths.cfg.sh
        exit 1
fi



if test "$#" -gt 3 || test "$#" -lt 2; then
	echo Invalid argument count $#
	usage
fi

# strip trailing directory separator, if supplied
DIR=`realpath "$1" | sed 's/\/$//g'`
MUTATIONS_COUNT=$2
shift 2

if ! test -d "$DIR"; then
	echo Error: Contract directory $DIR does not exist
	usage
fi

while test "$#" -gt 0; do 
	ONAME=`echo $1 | awk -F= '{print $1}'`
	OARG=`echo $1 | awk -F= '{print $2}'`
	if test "$ONAME" = "--skipto"; then
		SKIPTO=$OARG
	elif test "$ONAME" = "--generated"; then
		IS_GENERATED_SET="--generated"
	else
		usage
	fi
	shift
done


# Cleanup of /tmp to avoid "no space left on device" errors after running too many tests
echo Cleaning up /tmp...
clean_tmp() {
	rm -rf /tmp/tmp-*
	rm -rf /tmp/test-*
}
clean_tmp

total_start_timestamp=`date '+%s'`

if test "$SKIPTO"; then
	echo Skipping to ... $SKIPTO
fi

CONTRACT_SET=`ls $DIR/*/*.sol`

i=0

if ! test -f "$DIR"/is_generated_set; then
	# A traditional (e.g. Securify) test set - construction and transactions will be
	# generated, so we look up and pass the individual arbitrarily named .sol files 
	# instead of passing the directory 
	total=`ls $DIR/*/*.sol | wc -l`
	CONTRACT_SET=`ls $DIR/*/*.sol`
else
	# A generated test set that notably already contains transactions and construction files
	# Thus we pass the directory to run-one-test.sh instead of the individual contract file
	total=`ls -d $DIR/*/ | wc -l`
	CONTRACT_SET=`ls -d $DIR/*/`
fi

for c in $CONTRACT_SET; do
        i=`expr $i + 1`
        if test "$SKIPTO" != "" && test "$i" -lt "$SKIPTO"; then
                : # too loud: echo "(skipped)"
	else
        	SUBPROJNAME=`echo $c | awk -F'/' '{ print $(NF-1) }'`
        	printf "$i of $total: $SUBPROJNAME "
		start_timestamp=`date '+%s'`

		CALLED_BY_RUN_ALL_TESTS=yes run-one-test.sh "$c" $MUTATIONS_COUNT $IS_GENERATED_SET >summary.log
		RC=$?

		if grep 'POSSIBLE BUG: EVENT LOG ERROR' summary.log >/dev/null 2>/dev/null; then
			# There's a rare and unfortunate false positive in which the framework produces a
			# spurious, non-reproducible event log error. Repeat the test and see whether the
			# issue is still there
			CALLED_BY_RUN_ALL_TESTS=yes run-one-test.sh "$c" $MUTATIONS_COUNT $IS_GENERATED_SET >summary.log
			RC=$?
			
			if grep 'OK' summary.log >/dev/null 2>/dev/null; then
				echo 'OK (false positive)' >summary.log
			fi
		fi

		printf "`cat summary.log`"



		rm -f "${DIR}/${SUBPROJNAME}"/TruffleTest.log
		rm -rf "${DIR}/${SUBPROJNAME}"/main_results
		rm -f "${DIR}/${SUBPROJNAME}"/current-contract*.*
		if test -f  "$PATH_TRUFFLE_LOG_FILE"; then
			cp  "$PATH_TRUFFLE_LOG_FILE" "${DIR}/${SUBPROJNAME}"/
		fi
		if test -f "$PATH_TRUFFLE_RPC_LOG_FILE"; then
			cp "$PATH_TRUFFLE_RPC_LOG_FILE" "${DIR}/${SUBPROJNAME}"/
		fi
		if test -d "$PATH_MAIN_RESULTS_DIR"; then
			cp -R "$PATH_MAIN_RESULTS_DIR" "${DIR}/${SUBPROJNAME}"/
		fi
		if test -f current-contract-out.log; then
			cp current-contract-out.log current-contract-err.log "${DIR}/${SUBPROJNAME}"/
		fi
#echo $PWD
#exit 1

		end_timestamp=`date '+%s'`

		duration=`expr $end_timestamp - $start_timestamp`
		total_duration_seconds=`expr $end_timestamp - $total_start_timestamp`
		total_duration_minutes=`expr $total_duration_seconds / 60`
		total_duration_seconds=`expr $total_duration_seconds % 60`

		printf "           $duration sec,   total: %02d:%02d min\n" $total_duration_minutes $total_duration_seconds
		
	fi
	# Remove garbage
	clean_tmp
	# Apparently some node instances are left behind, which may be responsible for memory issues
	killall node 2>/dev/null
done
