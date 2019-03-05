#!/bin/sh

# TODO The name of this for-each.sh script sounds generic but it's pretty much tied to ast-test.sh
# at this point. Change this.

# Apply script to each file in a directory of Solidity contracts, stop as soon as an error is
# encountered (indicated by the script's return value).
# The script's output is suppressed for all successful test cases, but not for erroneous cases
usage() {
	echo Usage: for_each.sh "[solidity-contracts-dir] [action-script.sh] [--skipto=<testno>]"
	exit 1
}

if test "$#" != 2 && "$#" != 3; then
	usage
fi
if test "$#" = 3; then
	ONAME=`echo $3 | awk -F= '{print $1}'`
	OARG=`echo $3 | awk -F= '{print $2}'`
	if test "$ONAME" != "--skipto"; then
		usage
	fi
	SKIPTO=$OARG
fi

COMPILE=yes
VERIFY=no
EXIT_ON_ERROR=no

DIR="$1"
SCRIPT="$2"
CURTEST=1
TESTCOUNT=`ls $DIR/*.sol $DIR/*/*.sol | wc -l`
for i in `ls $DIR/*.sol $DIR/*/*.sol`; do
	FILE=$i
	printf "$CURTEST of $TESTCOUNT: $FILE - parsing ... "
	if test "$SKIPTO" != "" && test "$CURTEST" -lt "$SKIPTO"; then
		echo "(skipped)"
	elif ! ./"$SCRIPT" "$FILE" >/dev/null 2>&1; then
		echo ERROR
		if test "$EXIT_ON_ERROR" = yes; then
			# Re-run without output redirection to get debug output
			./"$SCRIPT" "$FILE"
			echo ERROR on case number $CURTEST - $i, see above for debug output 
			exit 1
		fi
	else
		if test "$COMPILE" != yes; then
			echo OK
		else
			printf "OK. compiling ... "
			if solc --ast-json $SOLC_OPTIMIZATION_FLAG transformed.sol >transformed.ast-json 2>transformed.warnings; then
				if test "$VERIFY" != yes; then
					echo OK
				else
					printf "OK. verifying ... "
					cat default.ast-json     | ./normalize-ast-json.sh                            >normalized-input.ast-json
					cat transformed.ast-json | ./normalize-ast-json.sh | ./strip-ast-json-junk.sh >normalized-output.ast-json
					DIFF=`diff normalized-input.ast-json normalized-output.ast-json`
					if test "$DIFF" != ""; then
						echo ERROR

						# The diff output isn't generally instructive
						#echo $DIFF

						if test "$EXIT_ON_ERROR" = yes; then
							echo ERROR on case number $CURTEST - $i, see differences between normalized-input.ast-json and normalized-output.ast-json 
							exit 1
						fi
					else
						echo OK
					fi
				fi
			else
				echo ERROR
				if test "$EXIT_ON_ERROR" = yes; then
					# Re-run compilation step
					solc transformed.sol
					echo ERROR on case number $CURTEST - $i, see above for solc compiler output 
					exit 1
				fi
			fi
		fi
	fi	
	CURTEST=`expr $CURTEST + 1`
done
