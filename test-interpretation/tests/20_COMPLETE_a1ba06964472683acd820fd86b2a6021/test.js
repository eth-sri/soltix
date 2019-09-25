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
logEvents(await instance.f0(false, "335162529008919647357801531744451373017533854439814", true, false));
logEvents(await instance.f1("\u0055\u00b1\u00c5\u005d\u0016\u000e\u001a\u0027\u00f2\u00c9\u00cd\u00dc\u00dc\u008b\u00f9\u00e1\u00ac\u003d\u004e\u0029\u0029\u00c1\u00a1\u005c\u0054\u008c\u0093\u0097\u00c5\u0099\u0071\u002f\u0085\u0006\u0078\u00eb\u003a\u00ee\u00a7\u007e\u007c\u00e0\u0095\u00ce\u00e0\u0066\u0009\u00f8\u009d\u00a5\u00c4\u0060\u0098\u0009\u007e\u0052\u0075\u003d\u00ff\u000f\u007a\u0095\u0047\u00fe\u0007\u0009\u00db\u00ee\u0021\u0097\u009f\u000c\u00b9\u00d0\u0027\u0021\u0055\u00a8", "-117630657922136613762220510785888964175348903951187", "52", "0x71C4B205A40B65A20608724CFAE0AF459009AE6A6F248CCE1C3123", "-26"));
    logEvents(await instance.f2("0x255194A07BF40BE78D9E", "0x2CDBDAB8A373B564898A3528A536F36A585A3C49A414ED3B4517EAD3", false));
        logEvents(await instance.f3("0x42EE911C3871536D", "0xF6A04B66", "126", "0xE9836B870EE5A287952D97E4A6FBB7", "0x3AB20F2D60828F3838A04348BF04089A130F67BD156E5616F056D9E218E8"));
            logEvents(await instance.f4("0x51C0555B331A4C3E58426F1A2083B49383F1974C6C", true, "0xC60FD8E28558D3A7FD00DAF041E835E87D4094ACE9C79E13EB1F1B5737AF1D"));
                logEvents(await instance.f5(true));
                    logEvents(await instance.f6("0x298E4AA183D83C29D2CC8BA74DB2D693D3301F21F6651C8E1D1B", "6473738463231479037786387310702669460740516123319169286116016569", "3082918434148098007890128133374317", "0x69E9F3169B33AD"));
                        logEvents(await instance.f7("-229", true, "440227480112556842019364157582498330996541077045381569071080626082345523236", "-2580506"));
                            logEvents(await instance.f8("0xE2A0A5F94B42", "181", "0x94DA3C595B721EF16DCD1387CE4D52E6", true, "131"));
                                logEvents(await instance.f9());
                                    logEvents(await instance.__outro());
   });
  });
});
