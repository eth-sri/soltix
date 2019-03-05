#!/bin/sh

SETTINGS=`dirname $0`/../../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

if test "$#" != 2; then
	echo "Usage: external-solc.sh [input.sol] [output.json]"
	exit 1
fi

# Embed input file in solc-compatible JSON document with configuration settings
# on what to generate
#./wrap-for-solc.js "$1"

echo Using external compiler $SOLC_BINARY_PATH to compile $1


SOLC_INPUT_JSON_FILE="_solc-input.json"
cat >"$SOLC_INPUT_JSON_FILE" <<EOF
{
    "language": "Solidity",
EOF

# Wrap input code in "sources" section
`dirname $0`/wrap.sh "$1" >>"$SOLC_INPUT_JSON_FILE"

cat >>"$SOLC_INPUT_JSON_FILE" <<EOF
    "settings": {
        "optimizer": {
EOF

if test "$USE_SOLC_OPTIMIZATION" = yes; then
echo '           "enabled": true,'   >>"$SOLC_INPUT_JSON_FILE"
else
echo '           "enabled": false,'   >>"$SOLC_INPUT_JSON_FILE"
fi


echo '            "runs": '$SOLC_OPTIMIZATION_RUNS >>"$SOLC_INPUT_JSON_FILE"

cat >>"$SOLC_INPUT_JSON_FILE" <<EOF
        },
        "outputSelection": {
            "*": {
                "": [
                    "legacyAST",
                    "ast"
                ],
                "*": [
                    "abi",
                    "evm.bytecode.object",
                    "evm.bytecode.sourceMap",
                    "evm.deployedBytecode.object",
                    "evm.deployedBytecode.sourceMap"
                ]
            }
        }
    }
}
EOF

# TODO how to process errors? These are embedded into the output document, not
# encoded in the return value of solc
cat "$SOLC_INPUT_JSON_FILE" | $SOLC_BINARY_PATH --standard-json >"$2" 

if test "$?" != 0; then
	echo Compiler error
fi

exit "$?"


