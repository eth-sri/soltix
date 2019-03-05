#!/bin/sh




OUTPUT_SOL_FILE_NAME="output.sol"
OUTPUT_EVENT_FILE_NAME="events.log"

usage() {
	echo "Usage: interpet.sh [generated-contract-dir]"
	echo
	echo "This will create, in the supplied directory:"
	echo "   - An event log '${OUTPUT_EVENT_FILE_NAME}'"
	echo "   - A Solidity output file '${OUTPUT_SOL_FILE_NAME}' (potentially containing expression fix-ups)" 
	exit 1
}

if test "$#" != 1; then
	usage
fi

CONTRACT_DIR=`realpath $1`
if ! test -d "$CONTRACT_DIR"; then
	echo "Error: $CONTRACT_DIR is not a directory"
	exit 1
fi

OUTPUT_SOL_FILE_PATH="$CONTRACT_DIR/$OUTPUT_SOL_FILE_NAME"
OUTPUT_EVENT_FILE_NAME="$CONTRACT_DIR/$OUTPUT_EVENT_FILE_NAME"

INPUT_SOL_FILE_PATH="$CONTRACT_DIR/c0.sol"
INPUT_TX_JSON_PATH="$CONTRACT_DIR/c0.tx-json"

if ! test -f "$INPUT_SOL_FILE_PATH"; then
	echo Expected input Solidity file "$INPUT_SOL_FILE_PATH" not found
	exit 1
fi

if ! test -f "$INPUT_TX_JSON_PATH"; then
	echo Expected input transactions JSON file "$INPUT_TX_JSON_PATH" not found
	exit 1
fi


. ../settings.cfg.sh
if ! test "$?" = 0; then
        echo Error: Cannot load ../settings.cfg.sh
        exit 1
fi

 ./run-soltix.sh "$INPUT_SOL_FILE_PATH" --solidityOutput="$OUTPUT_SOL_FILE_PATH" --interpret="$INPUT_TX_JSON_PATH"="$OUTPUT_EVENT_FILE_NAME"


