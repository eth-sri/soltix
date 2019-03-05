Experimental wrapper to run binary solc from JavaScript without the JS wrapper.

Initial tests with a hacked "truffle test" version using this instead of solcjs
compileStandard() indicate a performance improvement of 15-20% for bigger 
contracts. There may be further compiler invocations which have been missed so
far.

TODO:
	- Proper path lookup of scripts ($PATH needs this directory to find run-compiler.sh)
	- Integration in truffle (fork, patch or description) 
	- Documentation

