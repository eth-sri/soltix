var c0 = artifacts.require("./c0.sol");
var c1 = artifacts.require("./c0.sol");
module.exports = function(deployer) {
deployer.deploy(c0, "5");
deployer.deploy(c1, "5");
};
