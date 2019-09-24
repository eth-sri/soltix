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
logEvents(await instance.f0("843948334526817389339836243696413724226438564946", "0xE8F2CED131E8F29FC59E94083259ADA37FD789"));
logEvents(await instance.f1("0x46D6A2525D54771DDDFBF5CD614BC2BCBE2BEA7689C1CC25", "0x9529A52EFB2627D44FDCB75B7BD97CC1034CAE681D371F409A5A60C54938", "245"));
    logEvents(await instance.f2(false));
        logEvents(await instance.f3("0xCEA5976D4FA2", "230"));
            logEvents(await instance.f4("219"));
                logEvents(await instance.f5());
                    logEvents(await instance.f6("0x8544", "328453532013", true));
                        logEvents(await instance.f7("\u00b6\u00e0\u00a3\u0009\u0055\u0072\u0053\u0070\u00aa\u0075\u00d4\u0077\u003f\u0050\u000f\u00b9\u00ef\u009c\u0071\u00a5\u00ef\u00b2\u0040\u002e\u00f6\u00b3\u00f4\u00e0\u00d6\u00bd\u00a3\u0005\u007c\u0065\u0038\u00aa\u00e1\u0094\u007c\u00e6\u0030\u0008\u00bf\u003b\u00e3\u0041\u0033\u001a\u00a5\u004f\u00f9\u00c7\u0019\u000c\u0043\u0056\u00d0\u0099\u00b9\u005d\u0046\u0093\u00ee\u0020\u00ac\u0042\u0094\u0044\u001e\u004c\u0084\u00d2\u00db\u00c0", "0x0F4DDC382CC55080A27B4BF28B3843B514E39F6AB3E4299BB1", "0xC9E4D6236F4DE85430DEDBD70C475955869ED6B2148999C51D2FC3B4", "-10", "0x94791045CDC5CD0B8A"));
                            logEvents(await instance.f8("0x20CDC58659DE94ACDE8EFB743491DD518082751B4EED"));
                                logEvents(await instance.f9(false, "0xDF9C63902886C379D3DA"));
                                    logEvents(await instance.__outro());
   });
  });
});