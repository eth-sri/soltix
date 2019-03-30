#!/bin/sh

SETTINGS=`dirname $0`/../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

# Test effect of multiple docker instances to process tests. Most code generation
# and execution operations are largely or entirely single-threaded and benefit
# from multiple instances on multi-core systems
#
# TODO doing this may be counter-productive for large test cases, e.g. in Live-EMI.
# Investigate this.

docker_list() {
        name=$1

        # TODO there must be a better way to do this
        docker ps | while read line; do
                if echo $line | grep $name >/dev/null; then
                        id=`echo $line | awk '{print $1}'`
			printf $id
                fi
        done
}

docker_op() {
	name=$1
	op=$2

	list=`docker_list $name`

	if test "$list" != ""; then
		echo Doing docker $op on $list ...
		docker $op $list
	fi
}

wait_on_bg_jobs() {
	start=`date +%s`
	sleep 5
	jobs -p >_jobs.txt
	cat _jobs.txt
	wait `cat _jobs.txt`
	end=`date +%s`
	echo took `expr $end - $start` s
}

CONTRACT_DIR="_X2"
rm -rf "$CONTRACT_DIR"

docker rm -f soltix1 soltix2 soltix3 soltix4

echo ===== Native =====
rm -rf $CONTRACT_DIR
start=`date +%s`
./tools/generate-and-run-contract-set.sh 10 1 10 5 5 30 $CONTRACT_DIR --assignmentSequence 0 &
wait_on_bg_jobs
end=`date +%s`
echo took `expr $end - $start` s
echo ===== Docker: 1 instance =====
rm -rf $CONTRACT_DIR
start=`date +%s`
docker-generate-and-run-contract-set.sh 1 10 1 10 5 5 30  $CONTRACT_DIR --assignmentSequence 0
end=`date +%s`
echo took `expr $end - $start` s

echo ===== Docker: 2 instances =====
rm -rf "$CONTRACT_DIR"
docker_op soltix stop
start=`date +%s`
docker-generate-and-run-contract-set.sh 2  10 1 10 5 5 30  $CONTRACT_DIR --assignmentSequence 0
end=`date +%s`
echo took `expr $end - $start` s

echo ===== Docker: 3 instances =====
rm -rf "$CONTRACT_DIR"
docker_op soltix stop
start=`date +%s`
docker-generate-and-run-contract-set.sh 3  10 1 10 5 5 30  $CONTRACT_DIR --assignmentSequence 0
end=`date +%s`
echo took `expr $end - $start` s

echo ===== Docker: 4 instances =====
rm -rf "$CONTRACT_DIR"
docker_op soltix stop
start=`date +%s`
docker-generate-and-run-contract-set.sh 4  10 1 10 5 5 30  $CONTRACT_DIR --assignmentSequence 0
end=`date +%s`
echo took `expr $end - $start` s
