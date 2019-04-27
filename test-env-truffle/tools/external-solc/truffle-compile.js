const { execSync } = require('child_process');


const tempInputFilePath = '/tmp/_truffle-compile-input.json';
const tempOutputFilePath = '/tmp/_truffle-compile-output.json';
const fs = require('fs'); 

var getResultPath = function() { return tempOutputFilePath; }

var compile = function(solcStandardInput) {
    // For now: Write input to temp file, read result from temp file

//console.log("standard input: " + solcStandardInput);

    fs.writeFileSync(tempInputFilePath, solcStandardInput);

    result = execSync(`cat ${tempInputFilePath} | ${process.env.SOLC_BINARY_PATH} --standard-json > ${tempOutputFilePath}`, { maxBuffer: 1024 * 10000 }, (err, stdout, stderr, status) => {
        if (err) {
            console.log('Compile error: ', err);
            return false;
        }
        if (status) {
            console.log("Compile failure: ", status);
            return false;
        }
    });
    return true;
}

var compileAndLoadResult = function(solcStandardInput) {
    if (!compile(solcStandardInput)) {
       return null;
    }
    // Read compiled result file
    return fs.readFileSync(tempOutputFilePath, 'utf8');
}
    

var compileFile = function(inputFilePath) {
}

/*
 * Invoked from truffle to compile some contract input, taking into account:
 *
 *     - selection of solcjs vs. external binary (if former, compile using passed solcjs handle)
 *     - optimization setting environment variables as defined in settings.cfg.sh (presumed to have been loaded)
 */
var truffleCompile = function(solcjs, solcStandardInput) {
    // Adapt optimization settings. We receive something like:
    //     "optimizer": {
    //         "enabled": false,
    //         "runs": 200
    //      },
    // which needs to be adapted for the current settings.cfg.sh settings, and extended with a 
    //      "details": { "yul": true/false } 
    // sub-item to select the optimizer type.
    const originalOptimizerSettings = JSON.stringify(solcStandardInput.settings, null, 2);

    // truffle as of 5.0.14 no longer even defines the "optimizer" field, so it must be defined before we can alter it.
    solcStandardInput.settings.optimizer = {};
    // ...but this is not sufficient to make it work, since there are apparently other incompatible changes in 5.0.14.
    // We fix the exact version 5.0.12 in package.json for now. TODO fix it or drop truffle

    if (process.env.USE_SOLC_OPTIMIZATION == 'yes') {
        solcStandardInput.settings.optimizer.enabled = true;
    } else if (process.env.USE_SOLC_OPTIMIZATION == 'no') {
        solcStandardInput.settings.optimizer.enabled = false;
    } else {
	console.log(`Error: Invalid USE_SOLC_OPTIMIZATION setting in settings.cfg.sh - must be 'yes' or 'no', but is ${process.env.USE_SOLC_OPTIMIZATION}`);
        process.exit(1);
    }
    solcStandardInput.settings.optimizer.runs = parseInt(process.env.SOLC_OPTIMIZATION_RUNS);

    var useYULOptimizer;
    if (process.env.SOLC_USE_YUL_OPTIMIZER == 'yes') {
        useYULOptimizer = true;
    } else if (process.env.SOLC_USE_YUL_OPTIMIZER == 'no') {
        useYULOptimizer = false;
    } else {
	console.log(`Error: Invalid SOLC_USE_YUL_OPTIMIZER setting in settings.cfg.sh - must be 'yes' or 'no', but is ${process.env.SOLC_USE_YUL_OPTIMIZER}`);
        process.exit(1);
    }
    solcStandardInput.settings.optimizer.details = { "yul": useYULOptimizer };


    const adaptedCompilerSettings = JSON.stringify(solcStandardInput.settings, null, 2);
    console.log(`Compiler settings ${adaptedCompilerSettings}`);

    // Now run solcjs or an external solc, depending on the setting
    const stringifiedInput = JSON.stringify(solcStandardInput, null, 2);

    if (process.env.USE_SOLCJS == 'yes') {
        console.log("Using solcjs compiler");
	return solcjs.compile(stringifiedInput);
    } else if (process.env.USE_SOLCJS == 'no') {
        console.log(`Using external compiler ${process.env.SOLC_BINARY_PATH}`);
        return compileAndLoadResult(stringifiedInput);
    } else {
	console.log(`Error: Invalid USE_SOLCJS setting in settings.cfg.sh - must be 'yes' or 'no', but is ${process.env.USE_SOLCJS}`);
        process.exit(1);
    }
}


module.exports = {
    compile,
    compileAndLoadResult,
    getResultPath,
    truffleCompile
};
