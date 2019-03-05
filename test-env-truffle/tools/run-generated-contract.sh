#!/bin/sh

if test "$#" = 2; then
	# In this mode, we use an existing generated contract template
	HAVE_EXISTING=yes
	EXISTING_DIR="$1"
	CONTRACT_NAME="$2"
        EXISTING_CONTRACT_FILE="$EXISTING_DIR/${CONTRACT_NAME}.sol"
	EXISTING_CONSTRUCTOR_BASE_FILE="$EXISTING_DIR/${CONTRACT_NAME}.cons"
	EXISTING_TRANSACTIONS_BASE_FILE="$EXISTING_DIR/${CONTRACT_NAME}.tx"
elif test "$#" != 5; then
	echo "Usage: ./run-generated-contract.sh [random-numbers-seed] [function-count] [assignments-lower-bound] [assignments-upper-bound] [variable-count]"
	exit 1
fi

if test "$HAVE_EXISTING" != yes; then
	PRNG_SEED="$1"
	FUNCTION_COUNT="$2"
	ASSIGNMENTS_LOWER_BOUND="$3"
	ASSIGNMENTS_UPPER_BOUND="$4"
	VARIABLE_COUNT="$5"
fi

# Create a new "isolated" truffle project direcory that can be executed using run-one-test.sh
PROJECT_DIR="$PWD/_generated_contract"

# Prepare project directory
  rm -rf "$PROJECT_DIR"
mkdir -p "$PROJECT_DIR"

	 CONTRACT_NAME="c0"
         CONTRACT_FILE="$PROJECT_DIR/${CONTRACT_NAME}.sol"
 CONSTRUCTOR_BASE_FILE="$PROJECT_DIR/${CONTRACT_NAME}.cons"
TRANSACTIONS_BASE_FILE="$PROJECT_DIR/${CONTRACT_NAME}.tx"
 CONSTRUCTOR_FULL_FILE="$PROJECT_DIR/2_deploy_contracts.js"
TRANSACTIONS_FULL_FILE="$PROJECT_DIR/test.js"

if test "$HAVE_EXISTING" != yes; then
	# Generate contract based on seed, giving c.sol (contract), c.cons (constructor invocation), c.tx (transactions list)
	if ! ./generate-contract.sh "$PRNG_SEED" "$FUNCTION_COUNT" "$ASSIGNMENTS_LOWER_BOUND" "$ASSIGNMENTS_UPPER_BOUND" "$VARIABLE_COUNT" \
				"$CONTRACT_FILE" "$CONSTRUCTOR_BASE_FILE" "$TRANSACTIONS_BASE_FILE" "$CONSTRUCTOR_FULL_FILE" "$TRANSACTIONS_FULL_FILE"; then
		echo Error: Contract generation failed
		exit 1
	fi
	md5sum "$CONTRACT_FILE" | awk '{print $1}' >"$PROJECT_DIR"/md5

	echo Generated "$CONTRACT_FILE", "$CONSTRUCTOR_BASE_FILE", "$TRANSACTIONS_BASE_FILE"
else
	if ! cp "$EXISTING_CONTRACT_FILE" "$CONTRACT_FILE"; then
		echo Error: No contract file in source directory
		exit 1
	fi
	if ! cp "$EXISTING_CONSTRUCTOR_BASE_FILE" "$CONSTRUCTOR_BASE_FILE"; then
		echo Error: No constructor base file in source directory
		exit 1
	fi
	if ! cp "$EXISTING_TRANSACTIONS_BASE_FILE" "$TRANSACTIONS_BASE_FILE"; then
		echo Error: No transactions base file in source directory
		exit 1
	fi
fi

# Execute contract using run-one-test.sh
./run-one-test.sh "$PROJECT_DIR" "$CONTRACT_NAME"
