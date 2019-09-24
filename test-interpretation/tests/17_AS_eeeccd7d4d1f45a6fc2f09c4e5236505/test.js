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
logEvents(await instance.f0(true, "0xcAD8FEE1D6C16516cee2944E184c3aC9d318B314", "0x3F3EBA8DD1A30589786A95CEB29D9205262197E610B2CC"));
logEvents(await instance.f1("243", true, "0xDC60A0ABDFE1FEFDFE4C"));
    logEvents(await instance.f2("0x76", "545476462834125573075601", "0x14F318DCD877AB", "0x5BD56D09AC7C10215B", "0x21DBAFD17756158D4548E230B5C1"));
        logEvents(await instance.f3("0xADA5EF9BF1699FFF434EBCE7BA476E64997DCE5F0DDCFB07D4A6", "0x195BBBCE1680EB3B84425A51EBB8DF3EE78412CCBB9AA6343BB3", "0xFD88AEEE4D0AB41E486E525E1DA50E885C746BD5C64321DFD94212CCD8E08E", "42", "0x054B29034933EFE7A4A1"));
            logEvents(await instance.f4("0x3A9EAC"));
                logEvents(await instance.f5(true));
                    logEvents(await instance.f6("0x1DEB14D374566770E38835AA3306B68051CCFE953D52AB650B0FF6C3E0"));
                        logEvents(await instance.f7(true, "17", "0x4FCEB6044A6EF79D41845DD9692E1D045AE0A32E6F50C98DB76529", true));
                            logEvents(await instance.f8("0xB5C577589A7D70C106", "101", "-111", "0x6E54CF"));
                                logEvents(await instance.f9());
                                    logEvents(await instance.__outro());
   });
  });
});
