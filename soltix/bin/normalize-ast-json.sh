#!/bin/sh

# Takes Solidity solc compiler AST-JSON input and removes "noise" fields that
# would yield differences between otherwise equivalent parse trees.
#
# - "absolutePath" field: designates AST-JSON file name
# - "src" field: references original source code location of tree nodes, introduces
# uninteresting differences if code formatting is changed
# - "documentation" field: Doxygen-like comments that do not need to be preserved
# for equivalence
#
# Finally, remove/reorder apparently "non-deterministic" ordering effects in other
# fields that may be caused by our ignorance of the "documentation" field
#fgrep -v '"src" : ' | fgrep -v '"absolutePath"' | fgrep -v '"documentation"' | sort-json-field-arrays


# Disabled for now - this is only useful for AST correctness validation which
# isn't generally useful, and pulls in the undesirable build-essential dependency
# for sort-json-field-arrays. If this is re-enabled, the setup.sh "make" command
# must also be re-enabled
tee
