#!/usr/bin/env node

var fastCompiler = require('./truffle-compile.js');
var fs = require('fs');
const testFile = fs.readFileSync('test-input.json', 'utf8'); 
const compiledResult = fastCompiler.compileAndLoadResult(testFile);

console.log(compiledResult);
