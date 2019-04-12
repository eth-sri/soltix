#!/bin/sh

# Nasty go-ethereum code patch routine to increase contract size limits. Changes inspired by Julio Faura's response in
#  https://ethereum.stackexchange.com/questions/56000/quorum-oversized-data-during-contract-creation-contract-code-size-limit
replace() {
	if test "$#" != 3; then
		echo replace: Invalid number of arguments
		exit 1
	fi

	FILE="$1"
	FRAGMENT="$2"
	REPLACMENT="$3"

	GREP_RESULT=`grep "$FRAGMENT" "$FILE"`
	if test "$?" != 0 || test `echo $GREP_RESULT | wc -l` != 1; then
		echo Error: Cannot find fragment $FRAGMENT in $FILE or more than 1 occurrence
		exit 1
	fi

	if cat "$FILE" | sed s/"${FRAGMENT}"/"${REPLACMENT}"/g >_tmp; then
		mv _tmp "$FILE"
	else
		echo Error: cat "$FILE" '|' sed s/"${FRAGMENT}"/"${REPLACMENT}"/g failed
	fi
}


# Note: changes in spacing prevent repeated application of changes

# -       MaxCodeSize = 24576 // Maximum bytecode to permit for a contract
# +       MaxCodeSize= 100 * 24576 // Maximum bytecode to permit for a contract
replace params/protocol_params.go 'MaxCodeSize = ' 'MaxCodeSize= 100 * '

# -                return ErrOversizedData
# +                //return  ErrOversizedData
replace core/tx_pool.go 'return ErrOversizedData' '\/\/return  ErrOversizedData'

# -       maxRequestContentLength = 1024 * 512
# +       maxRequestContentLength= 10 * 1024 * 512
replace rpc/http.go 'maxRequestContentLength = ' 'maxRequestContentLength= 10 * '

exit 0
