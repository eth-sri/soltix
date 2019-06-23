#!/bin/sh

if test "$#" = 0; then
	REPO_ADDR=eu.gcr.io/soltix/soltix
	echo Warning: No gcloud docker registry address passed as argument - using default address $REPO_ADDR
elif test "$#" = 1; then
	REPO_ADDR="$1"
	echo Pushing image to registry address $REPO_ADDR ...
else
	echo Error: Invalid argument count
	echo "Usage: gcloud-push-image.sh [image-destination-address]"
fi


if ! docker tag soltix "$REPO_ADDR"; then
	echo Error: Cannot tag docker image soltix - not built yet?
	exit 1
fi

if ! docker push "$REPO_ADDR"; then
	echo Error: Cannot push soltix docker image to gcloud. See error messages above.
	exit 1
fi

