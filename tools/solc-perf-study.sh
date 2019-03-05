#!/bin/sh


# Generates contracts with varying parameters and measures the compile time
#
# TODO We should also measure truffle runtime
# TODO Plots
# TODO Multiple repetitions


TEMPDIR=`realpath $PWD/_perf_study_temp`
GENERATEDDIR="$TEMPDIR"/TMP
rm -rf "$TEMPDIR"
mkdir  "$TEMPDIR" 

if ! . ../settings.cfg.sh; then
	echo Error: Cannot load settings.cfg.sh
	exit 1
fi

generate_contract() {
	rm -rf "$TEMPDIR"/TMP "$TEMPDIR"/contracts

	if ! /usr/bin/time --format %e -o _timeResult ./generate-contract.sh $1 $2 $3 $4 $5 "$GENERATEDDIR" >/dev/null 2>&1; then
		return 1
	fi
	mkdir "$TEMPDIR"/contracts
	cp "$TEMPDIR"/TMP/c0.sol "$TEMPDIR"/contracts/
	cat >"$TEMPDIR"/truffle.js <<EOF
module.exports={"networks":{"test":{"port":35511,"host":"localhost","network_id":"*","gas":17592186044415,"gasPrice":1}}};
EOF
	return 0
	
}

compile_contract() {
	OLDDIR=`pwd`
	cd "$TEMPDIR"
	rm -rf build # avoid caching
	/usr/bin/time --format %e -o _timeResult $NODEDIR/node --max-old-space-size=8192 ../node_modules/.bin/truffle compile >/dev/null 2>&1
	RC=$?
	cat _timeResult
	cd "$OLDDIR"
	return $RC	
}

error() {
	echo "$@" >/dev/tty
	exit 1
}




doit() {
	if test "$#" != 5; then
		echo Error: Need 5 contract generation args
		exit 1
	fi


	REPS=1
	SUCCESSFUL_REPS=0

	printf "$1 $2 $3 $4 $5... "  >/dev/tty
	i=0
	SEED=$1
	TOTAL_TIME=0
	while test "$i" != "$REPS"; do
		i=`expr $i + 1`
		SEED=`expr $SEED + 1`

		printf "generating... " >/dev/tty
		if ! generate_contract $SEED $2 $3 $4 $5 >/dev/null; then
			echo "Contract generation failed" >dev/tty
			continue
		fi
		printf "`cat _timeResult` s. compiling... " >/dev/tty
		
		DATA_POINT=`compile_contract`
		if test "$?" != 0; then
			echo "Compilation failed" >/dev/tty
			continue
		fi
		printf "$DATA_POINT " >/dev/tty
		TOTAL_TIME=`echo $TOTAL_TIME $DATA_POINT + p | dc`
		SUCCESSFUL_REPS=`expr $SUCCESSFUL_REPS + 1`
	done
	if test "$SUCCESSFUL_REPS" != 0; then
		AVG=`echo 2 k $TOTAL_TIME $REPS / p | dc`
	else
		AVG=no
	fi

	BOGOSIZE=`expr $2 '*' $3` # Function count * statements
	ASTSIZE=`grep totalCodeSize "$GENERATEDDIR"/meta.kv | awk -F= '{print $2}'`

	echo " = $AVG s avg     func*stmt-size $BOGOSIZE     ast-size $ASTSIZE" >/dev/tty

	echo $AVG $BOGOSIZE $ASTSIZE
}

# Result is average in seconds
# Test 1 ... Fix variable count (since it currently probably affects expression complexity)
#            Fix function count, vary statement count
#            Vary function count, fix statement count
#
#        ->  How do both scale?
#            To which extent does a fixed "functions * statements" translate to a fixed compile time?
#            Which configuration compiles the most statements for some amount of time?
for VARCOUNT in 10 30 50; do
	echo ========= Variable count $VARCOUNT ===========
	for FUNCCOUNT in 1 10 50 100; do
		echo ======= Variable count ${VARCOUNT}, function count $FUNCCOUNT ==========
		for STMTCOUNT in 10 30 50 100; do
			RESULT=`doit 1 $FUNCCOUNT $STMTCOUNT $STMTCOUNT $VARCOUNT`
			TIME=`echo $RESULT | awk '{print $1}'`
			BOGOSIZE=`echo $RESULT | awk '{print $2}'`
			ASTSIZE=`echo $RESULT | awk '{print $3}'`
		done
	done
done


