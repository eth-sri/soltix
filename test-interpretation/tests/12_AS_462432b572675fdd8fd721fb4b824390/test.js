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
logEvents(await instance.f0("165", "140", "0x0366FD1DF0180E6FB232B7", "0x80F9B1"));
logEvents(await instance.f1("\u00f4\u005c\u005e\u0037\u0055\u0014\u005b\u00bf\u0084\u00ff\u00d5\u00eb\u0094\u0011\u00ae\u008f\u0030\u0050\u0093\u00cd\u005e\u00f7\u0030\u00ae\u00ec\u00ec\u0055\u00f7\u0073\u0047\u00a6\u00a8\u0008\u000d\u0039\u00bf\u00b3\u00a2\u00a4\u0093\u003a\u00ab\u0066\u003d\u0007\u004a\u00c4\u00d5\u00f1\u00b8\u0006\u007a\u0002\u0026\u0079\u000a\u00ad\u00bc\u00cd\u007e\u00b4\u0062\u001c\u00c6\u006f\u0082\u005f\u008f\u005c\u00dd\u0053\u0053\u00ab\u00a6\u0045\u006e\u00ed\u00e4\u0013\u0099\u0036\u0028\u0054\u0069\u00ab\u00f3\u00b5\u00e9\u0020\u0061\u00aa\u0078\u0038\u007f\u00ed\u0049\u0075\u00ee\u00a2\u00cf\u00e7\u0001\u00ab\u0073\u0051\u007f\u006c\u0093\u0037\u00c5\u0024\u00c1\u00cd\u0090\u002a\u00a2\u00a3\u0037\u0085\u0098\u009f\u0083\u00b4\u0094\u0054\u009f\u0027\u001b\u007e\u00a3\u006f\u0069\u005b\u00a7\u0031\u003e\u00ae\u00f2\u00d0\u008a\u0063\u0006\u00f9\u0079\u0030\u00ef\u0085\u0016\u001e\u0021\u00b5\u00ad\u0082\u0093\u0051\u0099\u00db\u009c\u0022\u000e\u00f8\u003f\u000c\u00a1\u00f7\u001a\u0048\u0013\u0005\u0067\u0071\u00e3\u0023\u0095\u0086\u00c5\u0007\u001f\u0095\u00d7\u00ae\u0037\u007f\u00f2\u0077\u00da\u002f\u00f1\u005a\u0074\u0098\u0013\u007d\u0018\u0018\u00fa\u004d\u00e4\u0021\u00f0\u0053\u00a6\u00a8\u001c\u0040\u0020\u0062\u0061\u00f6\u00db\u00d4\u009f\u00f6\u0083\u0011\u008e\u0019\u00e2\u0032\u0082\u009b\u00c8\u00f8\u00b1\u003e\u00e5\u0042\u0054\u008b\u00ab\u00d8\u003e\u0089\u00d9\u00b0\u0002\u00b6\u002b\u00bb\u00cf\u00a9\u0014\u006a\u0089\u0097\u0008\u0050", "0x67CC", "\u00a4\u00b2\u0066\u009e\u0001"));
    logEvents(await instance.f2("0x4F73F96AEBD4A757CBFA", "0x1177392B92960338F3C049D9F51A45DD126A72E083"));
        logEvents(await instance.f3("0x7E521601B2"));
            logEvents(await instance.f4());
                logEvents(await instance.f5());
                    logEvents(await instance.f6("0x752032E8A61117DAA2DE4C88F15364B0981C", false, "-179"));
                        logEvents(await instance.f7("0xF5B78F", "0xA43534E5FE9CA274266E18B6FC625BE3BB43DF3C8CA9", true, "0x0FC74B4B363236FDB5A143D796D96ECC5B5CE8E0111D6986A3A4C2"));
                            logEvents(await instance.f8("51", "\u00d8\u0070\u0041\u0017\u0057\u009b\u00f0\u00b2\u0070\u0057\u0092\u00dc\u0076\u00c7\u0089\u00d0\u00c9\u00a0\u0058\u00cc\u00b1\u0089\u00a4\u0045\u00cc\u0073\u000c\u009c\u0028\u000e\u00b4\u0059\u0033\u00ca\u0098\u00d2\u002f\u00c1\u00e1\u00ae\u002f\u00e5\u001e\u003c\u005c\u00b2\u0097\u0002\u001a\u0041\u0018\u004c\u007c\u0074\u00f8\u00a0\u00c1\u0050\u00c8\u004a\u00ad\u0042\u00c8\u0016\u0027\u0048\u000d\u0077\u0007\u005b\u0088\u00ed\u00c4\u009b\u0001\u00a6\u0096\u00d4\u00c9\u0085\u00b3\u0004\u0017\u00e4\u008d\u005e\u0002\u0049\u003e\u0034\u0087\u002b\u00f1\u00c0\u0063\u00e2\u00bf\u009a\u0040\u003c\u0031\u0001\u007a\u0026\u00d9\u00b8\u00b6\u0022\u004d\u0072\u009a\u00cc\u005b\u0010\u0040\u00f9\u00a4\u0032\u0060\u0047\u0075\u009a\u00d7\u00f4\u007e\u00c8\u00c2\u0038\u00f9\u004f\u0078\u00bd\u00d5\u00e7\u008d\u0066\u00dc\u0034\u006a\u00af\u0013\u0091\u002d\u0023\u002e\u00ee\u0030\u0092\u00b2\u0011\u0033\u008e\u0030\u00dc\u00f1\u00ba\u0003\u0097\u00f5\u0027\u005e", "\u008e\u00c7\u0075\u0063\u0029\u008e\u0075\u0062\u00b8\u0066\u0036\u00c6\u006c\u0077\u00d6\u0084\u00c8\u00c5\u00fb\u0074\u0011\u001b\u00d9\u0084\u0016\u00af\u004f\u00e0\u009d\u00bd\u00d9\u00bb\u001b\u006a\u00ae\u00ac\u00dd\u000b\u0024\u00df\u00ab", "80"));
                                logEvents(await instance.f9("102"));
                                    logEvents(await instance.__outro());
   });
  });
});