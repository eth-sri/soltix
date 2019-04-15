#!/usr/bin/env node

var fastCompiler = require('./truffle-compile.js');
var fs = require('fs');

const testFile = fs.readFileSync(process.argv[2], 'utf8'); 

jsonFileName = JSON.stringify(process.argv[2]);
jsonFile = JSON.stringify(testFile);

console.log("\"sources\": {");
console.log("    " + jsonFileName + ": {");
console.log("        \"content\": " + jsonFile);
console.log("    }");
console.log("},");

