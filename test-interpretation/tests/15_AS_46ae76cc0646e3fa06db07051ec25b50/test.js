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
logEvents(await instance.f0("162"));
logEvents(await instance.f1("0x87E9F21E0C90AC83", "122"));
    logEvents(await instance.f2());
        logEvents(await instance.f3(true, "0xE2C63A"));
            logEvents(await instance.f4("231", "92537085535", "149"));
                logEvents(await instance.f5("211", "184", "14705326161179568510143037793045", "0xCFBA5D22791F343E1AE720F7B5", "167"));
                    logEvents(await instance.f6("0x4227D4533C8F", "0x83A5EFF7EC037DB83E557629784E5FCA86"));
                        logEvents(await instance.f7("5519431816719469020599444418753322220755306992930973295403", "1228203694229712171276723268634775246", "9099663164693811346521272495025"));
                            logEvents(await instance.f8("153258299632725874018890208045355513055546221586122602013147709876314665521", "0x56BB12C1948175BD476DBEE4C67F653FB0CE4B", "41"));
                                logEvents(await instance.f9("\u00cb\u005d\u00a5\u0008\u002a\u0020\u0088\u0020\u0078\u004f\u00a1\u00ae\u009f\u0055\u0044\u005f\u0095\u00ca\u003a\u009b\u0050\u0024\u00d4\u0010\u00d8\u0069\u00ad\u00cd\u0010\u009c\u0005\u00f3\u004e\u00a3\u005e\u006f\u00ef\u00f3\u000d\u00df\u00b6\u0015\u0034\u00ee\u009d\u00f0\u00e8\u006f\u00cb\u0050\u00f7\u0066\u0025\u006a\u0017\u00c0\u00e3\u0043\u0027\u00ce\u00b5\u00d4\u007c\u00cc\u00c7\u003e\u0024\u00b2\u003f\u0023\u0060\u0032\u00ad\u0019\u00c5\u005b\u001f\u0095\u00a3\u0093\u00a1\u0014\u009a\u00b3\u0021\u002c\u00b5\u00db\u00d5\u0061\u0076\u0054\u0091\u00ce\u00ee\u0005\u00a4\u0085\u0014\u007b\u002f\u0078\u003e\u0038\u003b\u00e7\u00df\u00de\u0021\u00e6\u0064\u0062\u003c\u0051\u003d\u0093\u005c\u0096\u0026\u0093\u00c3\u002f\u00d0\u008d\u0009\u001b\u0029\u00c3\u00ac\u00d1\u00af\u0063\u00d2\u001f\u006f\u00d8\u00f8\u0008\u006f\u0055\u004a\u007f\u0098\u00fd\u00b8\u00bd\u0079\u00d2\u00aa\u006f\u006a\u00b0\u00dd", "0x902EF1AC873F17C97CA49848225B7288091543898A48D30DA551B9C7BEC261"));
                                    logEvents(await instance.__outro());
   });
  });
});
