const { execSync } = require('child_process');


const tempInputFilePath = '/tmp/_fast-solc-js-input.json';
const tempOutputFilePath = '/tmp/_fast-solc-js-output.json';
const fs = require('fs'); 

var getResultPath = function() { return tempOutputFilePath; }

var compile = function(solcStandardInput) {
    // For now: Write input to temp file, read result from temp file

console.log("standard input: " + solcStandardInput);

    fs.writeFileSync(tempInputFilePath, solcStandardInput);

//    execSync(`fast-solc-js-run-binary.sh ${tempInputFilePath} ${tempOutputFilePath}`, { maxBuffer: 1024 * 10000 }, (err, stdout, stderr) => {
//    execSync(`cat ${tempInputFilePath} | /usr/local/bin/solc --standard-json > ${tempOutputFilePath}`, { maxBuffer: 1024 * 10000 }, (err, stdout, stderr) => {
    result = execSync(`cat ${tempInputFilePath} | ${process.env.SOLC_BINARY_PATH} --standard-json > ${tempOutputFilePath}`, { maxBuffer: 1024 * 10000 }, (err, stdout, stderr, status) => {
        if (err) {
            console.log('Compile error: ', err);
            return false;
        }
        if (status) {
            console.log("Compile failure: ", status);
            return false;
        } else {
console.log("gud");
}
    });
console.log("result = " + result.status);
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

module.exports = {
    compile,
    compileAndLoadResult,
    getResultPath
};
