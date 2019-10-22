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
logEvents(await instance.f0("0xE134881E9F"));
logEvents(await instance.f1(true));
    logEvents(await instance.f2("-156", "0x214CB0", "0x0848E9D12576289CA2DEBC098BA6C3A4ED", "0xC665"));
        logEvents(await instance.f3("0xFABDDDBDA5", "0x3E8C4C22FCD7DD5BDA77BE3A9DE38EB794F5D81257C49CE5F449533711", "0x33734E1D4FE0818EC8B5D84194D20365403AB14F86C02E1E0FC6A7CE", "0xC8C8768D", true));
            logEvents(await instance.f4());
                logEvents(await instance.f5("0xA16FDEC2197505907B581BE108F79C", "0x705435EFA2DA1B266443D5", "0xA0F96D4CCDDA74AE5A5B1DA76E4FE86547A4B539706249FDFA645B27B09C", "209", true));
                    logEvents(await instance.f6("-17", "173", "116953954292594444474704162674040795484554755941540511698875401031538595953"));
                        logEvents(await instance.f7("0x6D", "7594030", "1493068195597374534351120452234843096278556053167112768426229395843860102"));
                            logEvents(await instance.f8());
                                logEvents(await instance.f9("6155306943768201180474506775772215778313763989610002537955489507367566", "35", "343820921673335628198988604829206868660301209280218", "2382987735945094461139442674757585505427337974556565209750571145568017"));
                                    logEvents(await instance.__outro());
   });
  });
});