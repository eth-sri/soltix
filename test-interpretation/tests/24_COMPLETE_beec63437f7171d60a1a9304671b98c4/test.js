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
logEvents(await instance.f0("82825205839002918988114820", "0x1037CB423B89BBA038F923A3D8212D350CA3563EB3", "0x1D98C2128A2E06C52D", "-222"));
logEvents(await instance.f1());
    logEvents(await instance.f2("25", false));
        logEvents(await instance.f3("200355424127636512991380378892124608654339666275491042395515340240691361152", "0x7A64980BC1E8EA0FF0A931E71E557D0F16623690B8DB0BC4FED94644D4E7", "0x762C2E142AB7F864B179631582F02E61E6A85D98F9D286E9", "46", "0x9789eBFA98fDc9FE43c094daE6D837801EF3282D"));
            logEvents(await instance.f4("1253302976098070209991850183610426559121192585624941864576575203050692866", "165", "287130344511925738937933488514183391187441885097812126906637609817811454550"));
                logEvents(await instance.f5("-183"));
                    logEvents(await instance.f6());
                        logEvents(await instance.f7(false));
                            logEvents(await instance.f8("58", "0xB900ACC9E26E82D2E56D73F2DDD2175FC3F4A152E5DC", "-8050929"));
                                logEvents(await instance.f9());
                                    logEvents(await instance.__outro());
   });
  });
});
