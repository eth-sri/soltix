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
logEvents(await instance.f0(false));
logEvents(await instance.f1("9936521026642230408561252043142034625845942637617566710601393717066"));
    logEvents(await instance.f2(false, "0x0C4FDD2F8EF6952D", "417006423943554999588507973747774458319500603607381487702761733296867921965", "4233205112694042138460317974261001002969111070160477211445", "72"));
        logEvents(await instance.f3());
            logEvents(await instance.f4("0xA566F2E273AA0A777071DA11EAA64CCFBD396727E9", "0xED2D3B1811107A", "2108324496407950269559371282133729842809540559", false, "-174"));
                logEvents(await instance.f5("0x6900B7D189CB295680373B07E6F3B279A3A8CE32BBACB104E7DA7553", "0x247F250764E3EE7C78E2D94537FCA486FDF742E0A1A7F46AFD85F2B9", "0x6B03F479890B1BC5E80236B5"));
                    logEvents(await instance.f6("-162"));
                        logEvents(await instance.f7("0x21CB657262FEAA415EA2B2F3BF6E449D927824FC5FDC092A74C070", true, "0x6D4F89"));
                            logEvents(await instance.f8("214", "0x077C419057D61A8B202E39E0FBCF5EABE144A465BEC7"));
                                logEvents(await instance.f9("\u002a\u0008\u0078\u0014\u00fc\u0039\u00eb\u0098\u0072\u00da\u0037\u00b0\u00e1\u00b9\u001b\u0037\u0058\u00c1\u006a\u00c8\u00ff\u0079\u00a1\u0083\u0005\u005d\u0033\u0092\u005e\u00d6\u00e2\u0033\u0075\u00e1\u00c1\u0065\u00df\u00aa\u00a4\u0092\u00f3\u005a\u004b\u00a3\u004c\u002c\u00bc\u0070\u0004\u0033\u00ca\u006a\u00b6\u00c5\u000c\u0044\u0028\u0075\u001b\u00e7\u00c7\u005c\u005c\u0086\u0026\u004c\u0014\u008f\u00c7\u00bd\u000c\u0092\u00d6\u009e\u00c6\u0082\u0074\u0036\u009f\u008d\u009b\u00e9\u0006\u00e9\u00ef\u0082\u00a1\u0020\u00ea\u00a4\u00c2\u001b\u0065\u008d\u0071\u0016\u00b7\u00d2\u00fc\u00b8\u0023\u00c9\u00d7\u001a\u0092\u0093\u0050\u0030\u0050\u0004\u0094\u0023\u00b5\u00d0\u0050\u004f\u0074\u00fe\u002c\u0060\u0081\u0063\u003f\u00bd\u0084\u0041\u00ba\u009d\u00f9\u003c\u00de\u00a2\u0021\u00dc\u0082\u0072\u0054\u007f\u005a\u00c9\u0033\u001b\u007d\u00ce\u00b7\u0093\u008a\u00f5\u002c\u004b\u0039\u0093\u009d\u0093\u0017\u0055\u0077\u0034\u00a9\u005d\u008b\u00a7\u0033\u002a\u00fa\u00d8\u00d3\u00b6\u001b\u0033\u00f9\u002b\u00aa\u0086\u006a\u0044\u006e\u00d8\u00a6\u002c\u0084\u0076\u00c1\u00ae\u009a\u00cb\u0033\u000d\u0088\u0060\u00c4\u0054\u00a0\u00e3\u000e\u0005\u007d\u00d7\u00b4\u0014\u00fd\u006e\u00f9\u00c5\u008f\u0051\u00f3\u00aa\u00f2\u00bf\u004d\u000e\u001b\u0010\u001d\u0079\u00f1\u00e2\u0017\u009e\u0037\u0089\u001e\u00b5\u00fd\u000c\u00e4\u00be\u00d1\u00d2\u0034\u0055\u00c4\u0090\u0032\u00ab\u0065\u002e\u00da\u002f\u00c6\u0094\u00c0", "238", false, "-183", "-175"));
                                    logEvents(await instance.__outro());
   });
  });
});