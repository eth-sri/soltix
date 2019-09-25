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
logEvents(await instance.f0("2723259791391533849451"));
logEvents(await instance.f1("-25", "0xDFDDAA2DE9A56E4B5545D108D16E69736822086127484F8D", false, false));
    logEvents(await instance.f2("0xB99DCC198A", false, true));
        logEvents(await instance.f3("164", false, "0x3EEF71E17918A94428451EFB8B"));
            logEvents(await instance.f4("201", "244", "0xCA4830A47B02C1EFB7D5211A985B4AF7A35CD62827", "0xAAD065538068857B20DF6B79E1A2F8C7"));
                logEvents(await instance.f5("0xDC8D6291103811B1DA251AFF3353F62C6E"));
                    logEvents(await instance.f6("160", true, "204", false));
                        logEvents(await instance.f7("0x236697B77B7437189667F9D51B0FE197A552957E569E1BBCAF11"));
                            logEvents(await instance.f8("-139", "0x8F55FEF3AABBCD31F5", "-198", "0xD84666345D63358ABA2352BC63D3EEDF63B9019E471E30947B15B8B572CF94"));
                                logEvents(await instance.f9("625738711589603847682784567678505913099268448", "0x6E2C79A0366107F9", "0xD89804879038FD9F45F156879A", "-226"));
                                    logEvents(await instance.__outro());
   });
  });
});
