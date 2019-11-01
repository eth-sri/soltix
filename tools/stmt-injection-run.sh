#!/bin/sh

if test "$#" != 3; then
	echo "Usage: expr-injection-run.sh [input.sol] [output.sol] [expr-file]"
	exit 1
fi

if ! cat "$1" >"$2"; then
	echo Error: Cannot copy $1 to $2
	exit 1
fi

echo 'contract __i {'                         >>"$2"
echo '   function __MARKER() public { }'      >>"$2"
echo '   function __input() {'                >>"$2"
cat  $3                                       >>"$2"
echo '   }'                                   >>"$2"
echo '}'                                      >>"$2"


rm -f _tmp.ast
solc --ast-json "$2" | ../soltix/bin/strip-ast-json-junk.sh >_tmp.ast
mv _tmp.ast $2
