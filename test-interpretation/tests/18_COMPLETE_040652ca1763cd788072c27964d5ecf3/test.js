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
logEvents(await instance.f0());
logEvents(await instance.f1("0x879F988EDE012AE03BA4E58DF96B4BECA48FD077C11B354D430D", "96", "0xA0600DEC55F0439BCE80A012", "0x8D66C5F177A115A29C606BF32F244750B064E2B2687D1631D02ECB6E"));
    logEvents(await instance.f2("0x6179CC658605BBA67D81FC6E898B7C76", "0x9952E9EBF5", "0x46A844B84F5E8DD4855EF2AA1D364635ACD593694EC257451C29C0A96F9888", "0x45D19C3F4D2191B70468550DB607F515077B89811EA419D2AC9EA23C60"));
        logEvents(await instance.f3(true, "0x664B3191DE6C75709DEBCAD3027300FF924778E11D4BA1EA4DDF66F93E06", true, "5"));
            logEvents(await instance.f4("251", "8", false, "0x78EB79D55A23A28AAA3979A549DD11574BBEDBAB86EADBC17BFB", "236"));
                logEvents(await instance.f5("-55", "115261570658139637969604207", true));
                    logEvents(await instance.f6("207"));
                        logEvents(await instance.f7(false, true, "-120", "0x2BF9690712ADEA2B71ABA8BF2229DAD7E35C47138767", "170503651575750"));
                            logEvents(await instance.f8(false, "-7728000323513796336333576214836549961735249237203923433", "1112663606920060422366576033135356990158280131073901371579165", "203", "233"));
                                logEvents(await instance.f9("0x274EE8", "250", "20677", "-212"));
                                    logEvents(await instance.__outro());
   });
  });
});
