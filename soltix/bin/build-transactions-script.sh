#!/bin/sh

if test "$#" != 2; then
	echo "Usage: build-transactions-script.sh [contract-name] [base-transactions-file]"
	exit 1
fi

        CONTRACT_NAME="$1"
TRANSACTION_BASE_FILE="$2"

# Generate transactions, part 1 of 3: Intro

echo "var $CONTRACT_NAME = artifacts.require(\"./${CONTRACT_NAME}.sol\");"

# Event logging function
cat >"/tmp/logEvents.tmp" <<EOF
var fs = require('fs');
var stream = fs.createWriteStream("profiling-events.log");

function logEvents(result) {
    // Log all encountered Solidity events ("emit ..."):
console.log("logEvent received: " + JSON.stringify(result));
    if ('logs' in result) {
        for (var i = 0; i < result.logs.length; ++i) {
            var b = result.logs[i];
            console.log("   > " + b.event + " = " + b.args + "   " +  JSON.stringify(b));
            stream.write(JSON.stringify(b) + "\n");
            for (m in b) {
                console.log("                > " + m); 
            }
        }
    }
}
EOF

cat /tmp/logEvents.tmp

# Test function in truffle-specific format
echo "contract('${CONTRACT_NAME}', function(accounts) {"
echo "  it('description', function() {"
echo "    var fs = require('fs');"
echo "    return ${CONTRACT_NAME}.deployed().then(async function(instance) {"
echo "      console.log(\"Starting transaction...\");"

# Generate transaction, part 2 of 3: hardcoded transactions, TODO: fuzz this
cat "$TRANSACTION_BASE_FILE" 

# Generate transaction, part 3 of 3: Outro 
echo "   });"
echo "  });" 
echo "});"

