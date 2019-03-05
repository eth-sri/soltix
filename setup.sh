#!/bin/sh

# This script prepares the test environment as follows:
#     - A solc binary path is queried from the user to use for compilation
#     - A settings.cfg.sh file is created that can be source'd by the
# framework scripts to obtain the environment variables $SOLC_BINARY_PATH
# and an updated $PATH including fast-solc-js 
#
# Arguments:
#    --no-prompt           - no intro prompt
#    --solc-path=<path>    - path to solc binary 
#    --nodejs-path=<path>  - path to directory containing npm/node to use
#    --python-path=<path>  - path to dirctory containing python3/pip3 to use

GENERATED_SETTINGS_FILE="settings.cfg.sh"

verify_solc_path() {
	if ! test -x "$SELECTED_COMPILER"; then
		return 1
	else
		return 0
	fi
}

verify_nodejs_path() {
	NODE_BIN_FILE_PATH_NODE=`realpath "$SELECTED_NODE_DIR/node"`	
	NODE_BIN_FILE_PATH_NPM=`realpath "$SELECTED_NODE_DIR/npm"`	
	if ! test -x "$NODE_BIN_FILE_PATH_NODE" || ! test -x "$NODE_BIN_FILE_PATH_NPM"; then
		return 1
	else
		return 0
	fi
}

while test "$#" != 0; do
	arg_part_one=`echo $1 | awk -F'=' '{print $1}'`
	arg_part_two=`echo $1 | awk -F'=' '{print $2}'`
	if test "$arg_part_one" = --no-prompt; then
		NO_PROMPT=yes
	elif test "$arg_part_one" = --solc-path; then
		SELECTED_COMPILER="$arg_part_two"
		if ! verify_solc_path "$SELECTED_COMPILER"; then
			echo "Error: $SELECTED_COMPILER does not appear to be an executable binary file"
			exit 1
		fi
	elif test "$arg_part_one" = --nodejs-path; then
		SELECTED_NODE_DIR="$arg_part_two"
		echo SELECTED_NODE_DIr = "$SELECTED_NODE_DIR"
		if ! verify_nodejs_path; then
			echo Error: "$SELECTED_NODE_DIR" does not contain executable node and npm binaries
			exit 1
		fi
	else
		echo Invalid argument.
		echo "Supported arguments: --no-prompt"
		echo "                     --solc-path=..."
		echo "                     --nodejs-path=..."
		exit 1
	fi
	shift
done

echo Starting test framework setup ...

echo Building soltix...
CURDIR=`pwd`

LOGDIR="$PWD/_setup-logs"
if ! test -d "$LOGDIR"; then
	mkdir "$LOGDIR"
fi
BUILDLOG="$LOGDIR"/build.log

if ! cd soltix || ! mvn install >"$BUILDLOG" 2>&1; then
        echo "Error: Could not build soltix. This could indicate that one of the following is missing:"
        echo "      General build tools    -   sudo apt-get install build-essential"
        echo "      Java SDK 8+            -   sudo apt-add-repository ppa:webupd8team/java"
        echo "                                 sudo apt-get update"
        echo "                                 sudo apt-get install oracle-java8-installer"
        echo "                                 sudo apt-get install oracle-java8-set-default"
        echo "      Maven                  -   sudo apt-get install maven"
        echo "See also log output in $BUILDLOG"
        exit 1
fi
cd "$CURDIR"


echo Building helper tools...
MAKELOG="$LOGDIR"/tools.log
cd soltix/bin
if ! make >"$MAKELOG" 2>&1; then
	echo Build error - see log "$MAKELOG"
	exit 1
fi

cd "$CURDIR" 


INSTALLED_SOLC=`whereis solc | awk '{print $2}'`

# 1. Select compiler binary - using the current solc static binary as initial suggestion
# TODO use truffle external compiler function to reference the selected binary as well
SOLC_VERSION_4="0.4.25"
SOLC_VERSION_5="0.5.1"

USER_INPUT=""

echo The test framework needs a solc compiler binary at least for code parsing.
echo To compile and run a program, this solc binary can be used as well, or solc-js can be used instead.
echo
echo settings.cfg will be generated. It can be used to configure the choice between
echo solc/solcjs, and the compiler to use binary if needed.
echo
echo Generated code currently always complies with 0.5 language rules and does not use
echo 0.4 language-specific constructs anymore.
echo
while test "$USER_INPUT" != y && test "$USER_INPUT" != n; do
	printf "Download static solc binary versions ${SOLC_VERSION_5} and ${SOLC_VERSION_4} now? [y]: " 
	read USER_INPUT
	if test "$USER_INPUT" = ""; then
		USER_INPUT=y
	fi
done

if test "$USER_INPUT" = y; then
	echo Downloading default solc compiler binary $SOLC_VERSION_5 ...
	if ! wget https://github.com/ethereum/solidity/releases/download/v${SOLC_VERSION_5}/solc-static-linux -O solc-${SOLC_VERSION_5} >/dev/null 2>&1; then
		echo Error: could not download solc compiler binary
	else
		INSTALLED_SOLC=$PWD/solc-${SOLC_VERSION_5}
		chmod +x "$INSTALLED_SOLC"
	fi

	echo Downloading solc compiler binary solc-0.4.25 ...
	if ! wget https://github.com/ethereum/solidity/releases/download/v${SOLC_VERSION_4}/solc-static-linux -O solc-${SOLC_VERSION_4} >/dev/null 2>&1; then
		echo Error: could not download solc compiler binary
	fi
fi

if ! test -x "$INSTALLED_SOLC"; then
	INSTALLED_SOLC=""
	echo Could not locate any installed solc binary
else
	echo Located installed solc binary at $INSTALLED_SOLC
fi

while test "$SELECTED_COMPILER" = ""; do
	printf "Please select the solc compiler binary path to use [$INSTALLED_SOLC]: "
	read USER_INPUT
	if test "$USER_INPUT" = ""; then
		if test "$INSTALLED_SOLC" = ""; then
			echo Error: You selected the default compiler, but no installed compiler has been found
		else
			SELECTED_COMPILER="$INSTALLED_SOLC"
		fi
	else 
		if ! verify_solc_path; then
			echo Error: "$SELECTED_COMPILER" does not appear to be an executable binary file
		else
			SELECTED_COMPILER=`realpath "$USER_INPUT"`
		fi
	fi
done


# 2. Select a node version. Try to find a default option first 
SELECTED_NODE_DIR=/usr/local/bin
if ! verify_nodejs_path; then
	SELECTED_NODE_DIR=/usr/bin
	if verify_nodejs_path; then
		DEFAULT_NODE_DIR="$SELECTED_NODE_DIR"
	fi
else
	DEFAULT_NODE_DIR="$SELECTED_NODE_DIR"
fi

SELECTED_NODE_DIR=""
while test "$SELECTED_NODE_DIR" = ""; do
	printf "Please enter the directory path containing the node and npm binaries to use [$DEFAULT_NODE_DIR]: "
	read USER_INPUT

	if test "$USER_INPUT" = ""; then
		# Default choice
		SELECTED_NODE_DIR="$DEFAULT_NODE_DIR"
		break
	fi

	SELECTED_NODE_DIR=`realpath "$USER_INPUT"`
	if ! verify_nodejs_path; then
		echo Error: "$USER_INPUT" does not contain executable node and npm binaries
	fi
	NODE_VERSION=`"$SELECTED_NODE_DIR"/node -v | awk -F. '{print $1}' | awk -Fv '{print $2}'`
	if test "$NODE_VERSION" -lt 10; then
		echo Error: node version too old, must be at least v10
		SELECTED_NODE_DIR=""
	fi
done

echo Installing node packages...
cd test-env-truffle 


NPMLOG="$LOGDIR"/npm-install.log
if ! "$SELECTED_NODE_DIR"/npm install >"$NPMLOG" 2>&1; then
	echo Warning: $SELECTED_NODE_DIR/npm install in ./test-env-truffle returned an error, see "$NPMLOG" for log output.
	echo          If the framework works regardless, this may be ignorable
fi

cd "$CURDIR"


echo "# use solcjs in truffle (otherwise: invoke external solc binary)?"                                        >"$GENERATED_SETTINGS_FILE"
echo "export USE_SOLCJS=no"                                                                                    >>"$GENERATED_SETTINGS_FILE"
echo "# if USE_SOLCJS=yes - absolute path of solc binary to use"                                               >>"$GENERATED_SETTINGS_FILE"
echo "export SOLC_BINARY_PATH=\"$SELECTED_COMPILER\""                                                          >>"$GENERATED_SETTINGS_FILE"
echo "# enable optimization?  will update truffle's  'optimizer { enabled:'  setting   "                       >>"$GENERATED_SETTINGS_FILE"
echo "export USE_SOLC_OPTIMIZATION=yes"                                                                        >>"$GENERATED_SETTINGS_FILE"
echo "# if optimizing - how many runs? will update truffle's  'optimizer { runs:'  setting   "                 >>"$GENERATED_SETTINGS_FILE"
echo "export SOLC_OPTIMIZATION_RUNS=200"                                                                       >>"$GENERATED_SETTINGS_FILE"
echo "# avoid generating exponentiation (**) code - workaround for ganache-cli crashes and old solc versions"  >>"$GENERATED_SETTINGS_FILE"
echo "export CODEGEN_AVOID_EXP_OPERATOR=yes"                                                                   >>"$GENERATED_SETTINGS_FILE"                                           
echo "# avoid generating shift (<<, >>) code - workaround for ganache-cli crashes"                             >>"$GENERATED_SETTINGS_FILE"
echo "export CODEGEN_AVOID_SHIFT_OPERATORS=yes"                                                                >>"$GENERATED_SETTINGS_FILE"                                           
echo "# use experimental version 2 ABI encoder? Needed for constructs like structure arguments, but 20x slower">>"$GENERATED_SETTINGS_FILE"
echo "export CODEGEN_USE_ABI_ENCODER_V2=no"                                                                    >>"$GENERATED_SETTINGS_FILE"
echo "# generate function arguments of struct type? Requires CODEGEN_USE_ABI_ENCODER_V2=yes which may be undesirable due to performance" >>"$GENERATED_SETTINGS_FILE"
echo "export CODEGEN_ALLOW_STRUCTS_IN_FUNCTION_ABI=no"                                                        >>"$GENERATED_SETTINGS_FILE"
echo                                                                                                           >>"$GENERATED_SETTINGS_FILE"
echo "export NODEDIR=\"${SELECTED_NODE_DIR}\""                                                                 >>"$GENERATED_SETTINGS_FILE"
echo "export PATH=\"${PWD}/soltix/bin:${PWD}/test-env-truffle/bin:${PWD}/test-env-truffle/tools:$PATH\""         >>"$GENERATED_SETTINGS_FILE"


. ./settings.cfg.sh  # "$GENERATED_SETTINGS_FILE"

echo Testing whether the framework works...
# Run one test contract
TESTLOG="$LOGDIR"/test.log
if ! cd test-env-truffle || ! echo 'contract c { uint x = 123; function f() public { return ; } }' >x.sol || ! run-one-test.sh x.sol 1 >"$TESTLOG" 2>&1; then
	echo - Error: Framework does not appear to work correctly, see log "$TESTLOG"
	exit 1
else
	echo - Test OK - see log "$TESTLOG"
fi

cd "$CURDIR"


echo "All done - settings (accessible in node via process.env.<name>):"
echo
cat "$GENERATED_SETTINGS_FILE"
echo

echo "You can change these in settings.cfg.sh"

exit 0
