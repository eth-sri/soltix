#!/bin/sh

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
	echo Error: Cannot load settings file $SETTINGS - please run setup.sh
	exit 1
fi
. `dirname $0`/../paths.cfg.sh
if ! test "$?" = 0; then
        echo Error: Cannot load `dirname $0`/../paths.cfg.sh
        exit 1
fi


reset() {
        rm -rf "$GETH_DATADIR"
        rm -rf "$GETH_KEYSTORE_LIVE"
}

init() {
        # Copy predefined account data
        mkdir -p "$GETH_KEYSTORE_LIVE"
        cp -R "$GETH_KEYSTORE_SOURCE"/* "$GETH_KEYSTORE_LIVE"/
        # Initialize local geth blockchain
        "$GETH_PATH" --datadir "$GETH_DATADIR" init "$GETH_CONFIG" 
}

start() {
        # Start geth with options largely based on those given in
        #    https://hackernoon.com/set-up-a-private-ethereum-blockchain-and-deploy-your-first-solidity-smart-contract-on-the-caa8334c343d
        # Lines 2 and 3 not in tutorial:
        #    --mine --minerthreads=1    - start mining immediately (miner.start(...) in console)
        #    --etherbase                - required etherbase address for mining
        # Line 3: unlock account with address+password, "allow insecure" flag due to http access
        #    --allow-insecure-unlock --unlock $ADDR --password "$PASSWORD_FILE"
	#
	# "console" flag disabled to avoid terminal read errors terminating geth when started in background
	#
	# TODO find a way to support large contracts, like ganache's "allowUnlimitedContractSize" option.
	# To begin with:
	#     - have a large block gas limit value in the config (ok)
	#     - probably need to patch go-ethereum/core/tx_pool.go, validateTx(), to drop the
	# ErrOversizedData check
	#     - afterwards geth.log appears to indicate successfuly deployment, but there's still an
	# error
        "$GETH_PATH" --port 3000 --networkid 58343 \
                --mine --minerthreads=1 --etherbase '0xa4d8e9cae4d04b093aac82e6cd355b6b963fb7ff' \
                --allow-insecure-unlock --unlock '854c55d65bf425569263d5fae98d01bd7a96fd3c' --password "$GETH_KEYSTORE_LIVE"/password.txt \
                --nodiscover --datadir="$GETH_DATADIR" --maxpeers=0  --rpc --rpcport 8543 --rpcaddr 127.0.0.1 --rpccorsdomain "*" --rpcapi "eth,net,web3,personal,miner"
}

if pgrep -f "$GETH_PATH"; then
	# Reuse existing instance?
	#echo geth instance already running
	#exit 0

	# For now kill instance, reset everything
	pkill -f "$GETH_PATH"
	while pgrep -f "$GETH_PATH"; do
		sleep 1
	done
fi

reset
init
start
