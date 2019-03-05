#!/bin/sh

# Remove intro/outro metadata that renders solc AST-JSON output invalid as JSON document
if ! test -d _tmp; then
	mkdir _tmp
fi

cat >_tmp/code.ast-json # To file first to enable two passes
LEN=`cat _tmp/code.ast-json | wc -l`
# drop last line and first 4 lines of non-JSON meta garbage
#cat _tmp | head -`expr $LEN - 1` | tail -`expr $LEN - 5`
cat _tmp/code.ast-json | tail -`expr $LEN - 4` | grep -v '====== '

