#!/bin/sh

if ! docker tag soltix eu.gcr.io/soltix/soltix; then
	echo Error: Cannot tag docker image soltix - not built yet?
	exit 1
fi

if ! docker push eu.gcr.io/soltix/soltix; then
	echo Error: Cannot push soltix docker image to gcloud. See error messages above.
	exit 1
fi

