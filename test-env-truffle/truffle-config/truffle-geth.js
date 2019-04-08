module.exports = {
    rpc: {
        host:"localhost",
        port:8543
    },

    networks: {
        test: {
            host: "localhost",
            port: 8543,
            network_id: "*",
            from: "854c55d65bf425569263d5fae98d01bd7a96fd3c", // predefined account address (geth-keystore) 
            gas: 20000000
        }
    }
};
