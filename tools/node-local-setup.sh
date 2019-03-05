#!/bin/sh

mkdir ~/local
mkdir ~/node-latest-install
if ! cd ~/node-latest-install; then
	exit 1
fi
if ! curl http://nodejs.org/dist/node-latest.tar.gz | tar xz --strip-components=1; then
	exit 1
fi

# TODO Maybe ~/local may already is too general and prone to conflicts - use a dedicated directory instead?
if ! ./configure --prefix=~/local; then
	exit 1
fi
if ! make install; then
	exit 1
fi
exit 0
