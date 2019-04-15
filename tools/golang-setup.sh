#!/bin/sh

if test "$#" != 1; then
	echo "Usage: golang-setup.sh [installation-directory]"
	exit 1
fi

INSTALL_DIR="$1"

if ! wget https://dl.google.com/go/go1.12.4.linux-amd64.tar.gz; then
	echo Error: go download failed
	exit 1
fi

if ! mkdir -p "$INSTALL_DIR" || ! tar -C "$INSTALL_DIR" -xzf go1.12.4.linux-amd64.tar.gz; then
	echo 'Error: Cannot unpack go to '$INSTALL_DIR'. If this is a permissions problem,'
	echo '       you can unpack to a temp directory and "sudo mv" it to the desired'
	echo 'location.'
	exit 1
fi

echo go installation OK, update your PATH variable to use it:
echo '     export PATH="$PATH:'`realpath $INSTALL_DIR`'/go/bin'

exit 0

