#!/bin/sh

if ! test -f settings.cfg.sh; then
        echo Error: settings.cfg.sh not found - please run setup.sh
        exit 1
fi
 
. ./settings.cfg.sh
if ! test "$?" = 0; then
        echo Error: Cannot load settings.cfg.sh
        exit 1
fi


rm -rf ~/truffle-local

# Using our own truffle fork with external compiler support is still preferable, since
# we haven't been able to get the official truffle-external-compile to work so far
if ! test -d ~/truffle-local; then
	if ! git clone https://github.com/nweller/truffle.git ~/truffle-local; then
#	if ! git clone https://github.com/trufflesuite/truffle.git ~/truffle-local; then
		echo Error: Cannot clone truffle
		exit 1
	fi 
fi

LASTDIR=`pwd`
cd ~/truffle-local
git checkout origin/develop

# TODO: npm install is wrong, what seems to work instead is:
#
#  (...preferably using $NODEDIR/npm as well...)
#    	~/local/bin/npm install lerna
#    	~/local/bin/npm install yarn
#    	~/local/bin/npm run bootstrap
#   	cd packages/truffle
#    	~/local/bin/npm run build-cli

"$NODEDIR"/npm install lerna
"$NODEDIR"/npm install yarn 
"$NODEDIR"/npm run bootstrap 

cd packages/truffle
if ! "$NODEDIR"/npm run build-cli; then
	echo Error: Cannot build truffle
	exit 1
fi
cd "$LASTDIR"



# TODO Just copying node_modules is too simple. It requires an additional "npm install",
# which will then however overwrite our files. So instead the installation should
# probably go:
#      npm install
#      ...copy relevant truffle-specific files...?
# Or better yet find a way to pull in a local package? But "build-cli" probably does not
# build a complete package

#rm -rf test-env-truffle/node_modules
#cp -R ~/truffle-local/node_modules test-env-truffle/
rm -rf test-env-truffle/node_modules/truffle-old
mv test-env-truffle/node_modules/truffle test-env-truffle/node_modules/truffle-old

echo in dir `pwd`

cp -R ~/truffle-local/packages/truffle test-env-truffle/node_modules/

exit 0
