#!/bin/sh

rm -rf builddeps
mkdir builddeps
cd builddeps

if ! wget https://github.com/ethereum/solidity/releases/download/v0.5.8/solc-static-linux -O solc; then
	echo wget failed for solc
	exit 1
fi
if ! git clone https://github.com/ethereum/go-ethereum.git; then
	echo git clone failed for go-ethereum
	exit 1
fi
if ! wget https://dl.google.com/go/go1.12.4.linux-amd64.tar.gz -O go.tgz; then
	echo wget failed for go
	exit 1
fi
