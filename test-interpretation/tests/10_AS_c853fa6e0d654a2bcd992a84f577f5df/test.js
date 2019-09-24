var c0 = artifacts.require("./c0.sol");
var fs = require('fs');
var stream = fs.createWriteStream("profiling-events.log");

function logEvents(result) {
    // Log all encountered Solidity events ("emit ..."):
console.log("logEvent received: " + JSON.stringify(result));
    if ('logs' in result) {
        for (var i = 0; i < result.logs.length; ++i) {
            var b = result.logs[i];
            console.log("   > " + b.event + " = " + b.args + "   " +  JSON.stringify(b));
            stream.write(JSON.stringify(b) + "\n");
            for (m in b) {
                console.log("                > " + m); 
            }
        }
    }
}
contract('c0', function(accounts) {
  it('description', function() {
    var fs = require('fs');
    return c0.deployed().then(async function(instance) {
      console.log("Starting transaction...");
logEvents(await instance.f0("0x7BE1B38F3E6241EBC71DD07CCF13E13BC7E1DFD24BA1CE7734", "0x391859261303F7F4A15A", "2745640"));
logEvents(await instance.f1("138", "38566619780", "180", "100"));
    logEvents(await instance.f2("48", false, true, "0x780DE7F406641E19180FBBFDE92293DA8159671195B9FD4A0274E3B991", "0x36750B1EC28091B43F71052D29744FE383EEC19386D82A20B1055C"));
        logEvents(await instance.f3("0x6E796A7A9D8A5B99C8B47228E6429034AE45FB31C2D8B1F5C8EC91AE4C4F4E", "-104", "0x4174FA40865A099C2a4758666cA1BFF9c456c298", "91"));
            logEvents(await instance.f4("-200"));
                logEvents(await instance.f5("85"));
                    logEvents(await instance.f6("0x606AD1626F14EF", "0x0AA01D59DBE54C7C6AF7DDAE46CB8CBF080226E3155C", "0x997BAFAFE2CAE282ED968D1AB0427F67FEF4DCF333031E"));
                        logEvents(await instance.f7("356117615093"));
                            logEvents(await instance.f8("0xA3E076202405531801C9D046C908E9FE2FEF", "0xB35885EAC5FA7F46D236D3C1B0EB", "0x4BAA86DF35A96B101AA1575D"));
                                logEvents(await instance.f9("0xBEF66E695D39F8F6907781AE36A79731A948B0FD5E7FA4FC1EF1", "0x8318BB8C161E93A3336AF13529FCBA", "-218", "209", "137"));
                                    logEvents(await instance.__outro());
   });
  });
});
