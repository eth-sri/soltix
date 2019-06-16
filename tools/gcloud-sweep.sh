#!/bin/sh

IFS="
"
for line in `gcloud compute instances list | grep -v MACHINE_TYPE`; do
	NAME=`echo $line | awk '{print $1}'`
	ZONE=`echo $line | awk '{print $2}'`

	# Parallelize stop + deletion
	(echo gcloud compute instances stop -q --zone=$ZONE $NAME ;
		gcloud compute instances stop -q --zone=$ZONE $NAME ;
		echo gcloud compute instances delete -q --zone=$ZONE $NAME ;
		gcloud compute instances delete -q --zone=$ZONE $NAME ) &
done

