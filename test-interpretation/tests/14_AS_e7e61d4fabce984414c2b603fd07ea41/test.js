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
logEvents(await instance.f0("33", "0x04788A", "0x1AF7014E3E42B6B2F06904FD2BB85B3ABC8DF23A7BE091", "0x365CE2C28EAF205B0277198B223EB380700C2369CACA3CA2"));
logEvents(await instance.f1("0xF71E671888"));
    logEvents(await instance.f2());
        logEvents(await instance.f3("81"));
            logEvents(await instance.f4("0x2481475FADCB2296E34EBF69A51ADB2190CE4F"));
                logEvents(await instance.f5("86", "198", "-99"));
                    logEvents(await instance.f6("20814162227327793423550842450423596912774", "0x36AAB9F9723D0F47A853F13EAB63E75B146F853D83464F5B", "82"));
                        logEvents(await instance.f7("\u0090\u00f3\u00c1\u002d\u00a5\u00eb\u00c9\u00d2\u0099\u0031\u000c\u001e\u0016\u000d\u004e\u00f9\u00b6\u00c7\u00ec\u0061\u001a\u002b\u00e2\u0013\u0050\u0004\u0043\u001e\u00e7\u00b0\u00a1\u0040\u0042\u0098\u00de\u00a0\u0099\u0002\u005f\u0008\u00d2\u008a\u0006\u0024\u0038\u00d2\u0007\u00b5\u0047\u0098\u0071\u00de\u001a\u0085\u0050\u00d5\u003a", "70", "7885360850476551169468648340259", true));
                            logEvents(await instance.f8(false, "0x174E6809FE8946F5CCB3C4C9D9700E9A4B59ADEB297083D5FE14", "0x3D950D4E2DE59CF789B838", "0x394E62F6F010CAFD10F90A8D09299CD2724C"));
                                logEvents(await instance.f9("-10", "0xAAB50D4B590778BAAD885B102F36E586ED9BB82BE93A47B0", "193", "0x18DC4C6ADF5680A2A57A323A", "3"));
                                    logEvents(await instance.__outro());
   });
  });
});