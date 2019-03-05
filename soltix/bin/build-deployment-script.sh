#!/bin/sh

if test "$#" != 2; then
	echo "Usage: build-deployment-script.sh [test-contract-name] [base-deployment-file]"
	exit 1
fi
          TEST_CONTRACT_NAME="$1"
BASE_DEPLOYMENT_COMMAND_FILE="$2"

# Part 1 of 3: Intro
echo "var $TEST_CONTRACT_NAME = artifacts.require(\"./${TEST_CONTRACT_NAME}.sol\");"
echo "module.exports = function(deployer) {"

# Part 2 of 3: Append deployment of form
#    deployer.deploy(MetaCoin, arg1, ...);
cat $BASE_DEPLOYMENT_COMMAND_FILE

# Part 3 of 3: Outro
echo "};"

