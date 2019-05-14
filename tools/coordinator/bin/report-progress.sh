#!/bin/sh

if test "$SOLTIX_COORDINATOR_HOST" != ""; then
	start-progress-sender.sh $SOLTIX_COORDINATOR_HOST $SOLTIX_NODE_ID "$@"
fi
