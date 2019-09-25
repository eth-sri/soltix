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
logEvents(await instance.f0(false, "78070381605028", "0x060B46120614732319B5FDD17C1A387DC16ED16768B41A"));
logEvents(await instance.f1("0xEA26ED0A97C032F13D1DCCD2D33CF4E8B078669B2F9B859251A7"));
    logEvents(await instance.f2(false, "26", "-249", "-232"));
        logEvents(await instance.f3("50", "\u0056\u0052\u009e\u0035\u006b\u0057\u0040\u0005\u0039\u00bb\u00d2\u00f9\u00c5\u002d\u00f2\u00e6\u009a\u0073\u00fb\u0016\u00b1\u0086\u000e\u00d0\u00f5\u0041\u00b5\u0000\u004a\u006a\u000c\u005c\u00ea\u0021\u0080\u0077\u00b6\u007a\u0036\u00b4\u0030\u0026\u006d\u00fe\u00e9\u008d\u008a\u008e\u00cf\u00b6\u0039\u00b2\u00e0\u00a1\u006c\u00da\u00e2\u0077\u0061\u008a\u0044\u0025\u004d\u00fa\u005e\u0072\u0001\u0075\u008d\u00ac\u00ca\u005e\u00aa\u0044\u0099\u00d9\u0018\u009c\u00f0\u00e3\u0014\u005f\u000e\u0061\u00bf\u0057\u009a\u005b\u0056\u00ae\u006f\u0031\u00f7\u00f8\u0019\u00fd\u00c7\u0026\u0068\u00d9", "0x79B4AED0E21766BAF35576274340C98C5E80C2D648FBBC5ACFAB21BC", "10467447112037356363864026631395233594112984"));
            logEvents(await instance.f4("22", "0x31E917D9C7C58EF0C194DC792B0CBCCF57C00411F199207227"));
                logEvents(await instance.f5("47"));
                    logEvents(await instance.f6("39907189485382916546478605936861391428740912941799440108470566637", true, "0x9A4C3CCDF9AB54F4"));
                        logEvents(await instance.f7("0xD5D1D0E31AAD2960510A324BBEF80C2126180E25DD", "0x231EE35E712BF5C3B25B98F757AD86BE069A1903DC"));
                            logEvents(await instance.f8("188225480694686645832720554541590535192571299834686411458309282824073024661", "11277789649935257667373837951171592036800", "0x8F9C1DB2DCD09D", "0x16CD91FD3B7F041872D8F20C", "0xE2A493AA110AB4"));
                                logEvents(await instance.f9("157"));
                                    logEvents(await instance.__outro());
   });
  });
});
