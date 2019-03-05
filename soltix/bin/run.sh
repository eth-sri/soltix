#!/bin/sh

#java -cp ../soltix/target/classes:log4j-1.2.14.jar:json-simple-1.1.jar:bcprov-jdk15on-1.60.jar soltix.Main "$@"


SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

BIN_DIR=`dirname $0`
TARGET_DIR=`dirname $0`/../target

LOG4J_CONFIG=`dirname $0`/../config/log4j.properties
export LOG4J_CONFIG

# Quickfix solution to pass various flags from settings.cfg.sh to the application.
# TODO proper config file with more options?
if test "$CODEGEN_AVOID_EXP_OPERATOR" = yes; then
	EXTRAARGS="$EXTRAARGS --avoidExponentialOperator"
fi
if test "$CODEGEN_AVOID_SHIFT_OPERATORS" = yes; then
	EXTRAARGS="$EXTRAARGS --avoidShiftOperators"
fi
if test "$CODEGEN_USE_ABI_ENCODER_V2" = yes; then
	EXTRAARGS="$EXTRAARGS --useABIEncoderV2"
fi
if test "$CODEGEN_ALLOW_STRUCTS_IN_FUNCTION_ABI" = yes; then
	EXTRAARGS="$EXTRAARGS --allowStructsInFunctionABI"
fi

echo Extraargs $EXTRAARGS

java -cp "$TARGET_DIR"/soltix-1.0-SNAPSHOT.jar:"$BIN_DIR"/log4j-1.2.14.jar:"$BIN_DIR"/json-simple-1.1.jar:"$BIN_DIR"/bcprov-jdk15on-1.60.jar soltix.Main "$@" $EXTRAARGS
