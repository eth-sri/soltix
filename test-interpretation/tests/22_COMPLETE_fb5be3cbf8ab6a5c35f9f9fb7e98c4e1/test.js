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
logEvents(await instance.f0("221", "207", "791004808918", "0xCEA3DA47ECA413"));
logEvents(await instance.f1("78858551485846264437140360441699797955728530252858", false, "-175", "0x38CD764B58B8E5E7A7213058446C1F3700072453271129"));
    logEvents(await instance.f2());
        logEvents(await instance.f3("0xD68B9CAD79CD2AA6A2768E11B4", false, "0xD0397E38643457", true, "1766995375732927608621393155208909452512810871706081424441"));
            logEvents(await instance.f4("0x0E5D5A679D39E7"));
                logEvents(await instance.f5("178"));
                    logEvents(await instance.f6("0x2FEDFB11272E73241E0827AAA8ED8415F62C", "0xDCECE24B9CDC85A0"));
                        logEvents(await instance.f7("0x7F05587C6A64E4D6975B6EC6B951E4C6F125CA8B367E49", "2308744219590552405257", "\u0011\u0047\u001b\u00a2\u0054\u00b8\u005f\u00b8\u00e9\u001b\u00f9\u0097\u00ed\u00b9\u0090\u0098\u00e7\u005a\u00e4\u004b\u0001\u00a7\u00d5\u00c2\u00c8\u0030\u00d0\u003d\u0086\u0053\u0092\u00fd\u008d\u0061\u00aa\u0059\u00ea\u0085\u005a\u00ae\u007a\u00ab\u0048\u00f2\u009f\u00da\u00fa\u0011\u00fb\u00d9\u002e\u00f7\u00ec\u00fa\u00f7\u00c8\u00fd", "0x3717E04F7FF0E4A64F7E1EDC19E6AC9A2496F7"));
                            logEvents(await instance.f8("130"));
                                logEvents(await instance.f9(true, "155971917259842762756465875"));
                                    logEvents(await instance.__outro());
   });
  });
});
