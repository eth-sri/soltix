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
logEvents(await instance.f0("0x54E82367C1E41E66FF4ADB380A5416A8BB5B92AE10", "0xAC180D912D52F8BD03A43121"));
logEvents(await instance.f1("19155912360202926"));
    logEvents(await instance.f2("-1107608909", "-189"));
        logEvents(await instance.f3("0xE557744E90D1CC", "0x35EBA4D761"));
            logEvents(await instance.f4());
                logEvents(await instance.f5("231", "81895241933", false, "0x8B7CA93451FDE4C5B094725E864A3B22675FB716FCCABFBAA288C17C564B9C"));
                    logEvents(await instance.f6("1090045414746", "\u0073\u00da\u00a0\u00e3\u00e6\u0095\u0089\u00cb\u0097\u008d\u0054\u00b9\u003f\u00b3\u00f8\u007f\u00b4\u00d3\u00ff\u0068\u00c2\u00a7\u00b0\u0031\u005d\u00e9\u0067\u002e\u0057\u0060\u00c9\u0063\u0047\u00a8\u00d1\u0080\u0037\u00fe\u008c\u00c4\u002b\u0086\u00bd\u007a\u00ab\u00b0\u0098\u0085\u00f8\u0025\u001b\u0096\u00a9\u0051\u00a5\u00ff\u0017\u00fb\u00d6\u00d7\u0026\u0099\u00d6\u00fb\u0090\u00fd\u00b3\u00d1\u0034\u00c4\u00aa\u0020\u0073\u005b\u00b1\u002d\u003a\u00f3\u00f7\u004e\u00c4\u009d\u0082\u009f\u00d5\u00c1\u008d\u00f8\u00c2\u00db\u00d1\u0075\u0032\u00b9\u0063\u00bd\u005c\u00c0\u0062\u001b\u00f5\u00bc\u006e\u00c8\u0020\u008c\u0038\u004d\u0002\u0076\u0084\u00c9\u00f9\u0067\u0056\u00da\u00aa\u006c\u00ea\u008d\u008e\u009b\u00d8\u001c\u0024\u0061\u0071\u0075\u000d\u0093\u005e\u0094\u0049\u00ef\u0040\u0061\u00b2\u00af\u000a\u005b\u00fc\u00b4\u00ac\u00d1\u0071\u0059\u009d\u0033\u00c1\u0068\u00c7\u004b\u006c\u00d2\u006a\u0089\u004e\u00ac\u0078\u0042\u0030\u00c2\u0022\u00b8\u0041\u0086\u004a\u001e\u00e6\u0069\u00bd\u0033\u002d\u00b8\u004e\u000d\u00ee\u00ff\u00fb\u0094\u00a1\u007f", "0xB0CF87BE48A3FE5FE69F876EF7CE19008CD84BA222B10A555F1D4AE9"));
                        logEvents(await instance.f7("1498976342554454858", "0xF27FE03BDF09A63F3DBAF4378130", "0x4215E0D71585", "0xB87F46FAB768CC4DA801F71F41F0BCE7BE1A1DCAD4", "-202"));
                            logEvents(await instance.f8("112", "359306151787977027436265", "119668853029550438243989681397032833832"));
                                logEvents(await instance.f9("1120730927017510562325138"));
                                    logEvents(await instance.__outro());
   });
  });
});