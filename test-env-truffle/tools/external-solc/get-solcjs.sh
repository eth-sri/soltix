#!/bin/sh

SETTINGS=`dirname $0`/../../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

export PATHSDIR=`dirname $0`/../..
PATHS="$PATHSDIR"/paths.cfg.sh
if ! . "$PATHS"; then
        echo Error: Cannot load paths file $PATHS
        exit 1
fi

if test "$#" != 1; then
	echo "Usage: get-solcjs.sh [version]"
	echo " see https://www.npmjs.com/package/solc"
	exit 1
fi

VERSION="$1"

if ! cd "$EXTERNAL_SOLC_DIR"; then
	echo Error: cannot cd to $EXTERNAL_SOLC_DIR
	exit 1
fi


# TODO skip npm invocation entirely if solc already at correct version?
cat package.json.template | sed 's/###SOLC_VERSION###/'$1'/g' >package.json
$NODEDIR/npm install
