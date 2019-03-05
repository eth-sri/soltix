#!/bin/sh

rm -rf TMP9

#export GANACHE_OPTIMIZATION_WORKAROUNDS_ENV_VAR=--ganacheOptimizationWorkarounds

doit() {
	./generate-contract-set.sh 5 1 10 10 10 30 TMP9 --random ##--assignmentSequence

	echo ======= With optimization =======
	./truffle-optimization-setting.sh on
	./run-all-tests.sh TMP9 0 --generated
	echo ======= Without optimization =======
	./truffle-optimization-setting.sh off 
	./run-all-tests.sh TMP9 0 --generated
}

echo ======= Normal random contracts ========
doit

rm -rf TMP9

echo ======= "Optimization-friendly (with ganache-cli workarounds) random contracts" ========
export GANACHE_OPTIMIZATION_WORKAROUNDS_ENV_VAR=--ganacheOptimizationWorkarounds
doit
