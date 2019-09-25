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
logEvents(await instance.f0("0x0E683EA0", "\u00e5\u00ef\u0000\u00c8\u000e\u008f\u00fd\u002b\u0087\u0098\u00d3\u0079\u00cd\u00c6\u002a\u00b9\u005b\u008e\u002e\u004f\u00c7\u005e\u0081\u00df\u00b7\u0005\u0058\u0039\u0028\u008b\u0052\u003f\u0087\u00a3\u008a\u005b\u0060\u00f4\u0027\u0032\u00cb\u0052\u002a\u00d6\u00aa\u002e\u0097\u00fe\u00d5\u005e\u003b\u005c\u00af\u00b0\u00fa\u007d\u0032\u00a6\u0091\u0057\u008f\u00fe\u00c9\u00fe\u0095\u00c9\u00a0\u00a4\u009a\u001d\u00ac\u00a9\u005d\u007c\u0005\u001b\u007f\u00a8\u0051\u0097\u0059\u007c\u001e\u0071\u00be\u0018\u00ab\u0063\u00f9\u00d5\u004e\u002c\u009e\u00f8\u0032\u0002\u0021\u0065\u00bf\u00ad\u00fd\u0046\u002c\u0012\u0023\u00ce\u00c2\u0092\u00b4\u0019\u003c\u00ee\u00c0\u004e\u0025\u00ec\u0068\u004c\u00a1\u003f\u001e\u0023\u000e\u0016\u00fb\u00e5\u00ed\u00c7\u0009\u007c\u0049\u001a\u000b\u0052\u0026\u00c3\u000f\u0015\u000a\u00fe\u000e\u00c5\u0084\u00fa\u0031\u00fe\u008f\u0067\u001d\u0051\u0058\u005d\u0039\u003e\u002e\u0051\u0070\u0085\u00c0\u00e8\u0002\u0029\u00ad\u00f3\u000e\u00a5\u0028\u000f\u0034\u00cc\u0006\u00fa\u0047\u00c7\u00db\u00e8\u0042\u00ad\u00da\u00a5\u00f7\u006f\u0010\u00e6\u00ad\u00aa\u0045\u0080\u00a4\u0072\u002d\u00e1\u00e0\u00ea\u00d7\u00c5\u00ec\u0039\u0046\u0094\u0021\u0007\u00df\u009f\u00f5\u00ad\u0000\u0006\u0064", true));
logEvents(await instance.f1(true));
    logEvents(await instance.f2("208", "0xcfd264e9dFa853057Caf5827ef742132909503dc", false, "221303388654245336967046929380895435500742820644761", "0xE5B372E77C7B3E6822BCA4A36A"));
        logEvents(await instance.f3("0xA46ACDB6E74DC82C5687", "0x42933631E683995536E4D22E5E53EBE2EF93F30563BADEAF81", "-121", "0x4EFB35DBFC52F1", "\u002e\u0094\u0014\u00dd\u0089\u007a\u004c\u00a1\u00e8\u0098\u005c\u0050\u00f6\u00a4\u0084\u00a4\u00a3\u0065\u00e3\u001f\u0062\u00a0\u0031\u00a3\u0061\u0071\u0069\u0031"));
            logEvents(await instance.f4("184", false, "21749088584932070656154427740312277337120", "-45744662618620609210384939"));
                logEvents(await instance.f5("-130", "0x2E21CDA5E3100C71FF709B8D32338904A0EC5AF15EA88C"));
                    logEvents(await instance.f6("0x6E8F753807F062FD88FD7D8ACD9AA2A116A1FCB95A66BADA2EFD0BEA57", "0xB651E3D710E1CE68775C6AE8861540CB11CDA6E6477986C059A905EEA0B92D", "196", "0x74DAD40F9CD6EB81AE", "0x2DE07D0336CF592A80B0983501150B688026B4825AD7B1E5A1"));
                        logEvents(await instance.f7("7981223"));
                            logEvents(await instance.f8());
                                logEvents(await instance.f9(true, "197432661039734553870986247", "0x554CFEA64E", "182"));
                                    logEvents(await instance.__outro());
   });
  });
});
