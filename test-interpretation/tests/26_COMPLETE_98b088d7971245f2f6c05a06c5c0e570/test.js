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
logEvents(await instance.f0("0x95E41CEF24A6C9A41C5347B9E12F43C8E2C1A9CC6CD9517BF40F82AB5487", "0x13DAF24544097724EB6916B8", "-526260412764128414258011095678931337833720263235529330149703", "21"));
logEvents(await instance.f1("0x43B54B1A3092534B4511F8AF392DA25A", "0x760B548D", "207", "67"));
    logEvents(await instance.f2(false, "\u00c2\u0060\u0011\u002d\u0062\u007b\u00fb\u001e\u0030\u0009\u00cd\u0016\u0014\u008a\u0034\u0041\u005c\u00fa\u00e8\u00ea\u0003\u0068\u00ee\u00de\u0086\u005b\u00de\u0044\u00a5\u0010\u002c\u004f\u00d5\u0058\u007d\u004d\u004c\u0096\u006e\u00ec\u0042\u00bf\u006a\u004c\u0002\u0068\u0067\u005f\u003a\u00e6\u00ce\u0058\u000e\u005c\u0024\u0066\u004c\u00e9\u0053\u00dd\u005b\u00bf\u000e\u00d7\u0037\u00b0\u000a\u00e6\u0059\u00f1\u0062\u00f0\u000b\u0044\u00f2\u0044\u002c\u0031\u0002\u0037\u0068\u0057\u001f\u0004\u0030\u009c\u00c0\u0072\u00e9\u000e\u00bf\u00e0\u0035\u009f\u008b\u008a\u0048\u0070\u008f\u0095\u00ee\u004b\u000d\u0053\u00d5\u000c\u0068\u0034\u007b\u003c\u00ae\u0091\u004b\u00ad\u00f9\u00f3\u0007\u002a\u0018\u00b8\u0018\u0045\u00e8\u0017\u00dd\u003b\u008f\u0037\u00a1\u0050\u00d8\u00db\u0084\u00a2\u00d8\u004c\u0024\u005a\u0019\u008e\u00a3\u0044\u0075\u00be\u0029\u007a\u0003\u0043\u00d4\u004d\u0069\u00f4\u00c3\u00df\u0037\u00be\u00a4\u0008\u00e1\u0034\u00fe\u00c2\u0059\u0091\u005f\u0054\u005e\u001e\u0036\u005a\u00e4\u0083\u006c\u008d\u0046\u00a1\u0041\u00a6\u003b\u00b3\u005d\u00c8\u00f7\u00fb\u00ae\u003a\u0021\u0026\u00eb\u00ae"));
        logEvents(await instance.f3(true));
            logEvents(await instance.f4("3268887842200888891915457452788", "-1212478597"));
                logEvents(await instance.f5("19284998449393936447816243323249093788795537"));
                    logEvents(await instance.f6("2566064231538031927728299608505115296557944875"));
                        logEvents(await instance.f7("49", "252", "-37501918582532086689662666181297287453308711176821", "9"));
                            logEvents(await instance.f8("109"));
                                logEvents(await instance.f9("0xE343D3270A458DB2444A0F2DE90FA8A73E75", "0x4689BB84D3D0E467B1ABEC2BBABD2B", "0x7A59D0D8A10192DA84"));
                                    logEvents(await instance.__outro());
   });
  });
});
