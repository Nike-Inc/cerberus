var path = require('path');
var WebpackDevServer = require("webpack-dev-server");
var webpack = require("webpack");
var config = require("./webpack.config.js");

var reverseProxyPort = 9001
var nodeServerPort = 8000
var cmsPort = 8080
var vaultPort = 8200

// https://www.npmjs.com/package/redwire
var RedWire = require('redwire');
var redwire = new RedWire({
    http: {
        port: reverseProxyPort,
        websockets: true
    }
});

/**
 * Cerberus is a couple services behind a router so we can simulate that locally
 */
// redirect /secret to Hashicoorp Vault
redwire.http('http://localhost:' + reverseProxyPort + '/v1/secret', '127.0.0.1:' + vaultPort + '/v1/secret')
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/v1/secret', '127.0.0.1:' + vaultPort + '/v1/secret')
// redirect dashboard to the Cerberus Management Dashboard
redwire.http('http://localhost:' + reverseProxyPort + '/dashboard', '127.0.0.1:' + cmsPort + '/dashboard/')
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/dashboard', '127.0.0.1:' + cmsPort + '/dashboard/')
redwire.http('http://localhost:' + reverseProxyPort + '/', '127.0.0.1:' + cmsPort + '/dashboard/')
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/', '127.0.0.1:' + cmsPort + '/dashboard/')
// redirect rule for Cerberus Management Service
redwire.http('http://localhost:' + reverseProxyPort + '/v1', '127.0.0.1:' + cmsPort + '/v1')
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/v1', '127.0.0.1:' + cmsPort + '/v1')
redwire.http('http://localhost:' + reverseProxyPort + '/v2', '127.0.0.1:' + cmsPort + '/v2')
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/v2', '127.0.0.1:' + cmsPort + '/v2')


// configure proxy for hot module web socket
redwire.http('http://localhost:' + reverseProxyPort + '/sockjs-node', 'http://127.0.0.1:' + nodeServerPort + '/sockjs-node');
config.entry.app.unshift('webpack-dev-server/client?http://localhost:' + nodeServerPort + '/', 'webpack/hot/dev-server');

// run the local server
var compiler = webpack(config);
var server = new WebpackDevServer(compiler, {
    contentBase: path.resolve(__dirname, "build"),
    hot: true,
    inline: true,
    historyApiFallback: true,

    // webpack-dev-middleware options
    quiet: false,
    noInfo: false,
    lazy: false,
    filename: 'browser-bundle.js',
    watchOptions: {
        aggregateTimeout: 300,
        poll: 1000
    },
    stats: { colors: false }
});
server.listen(nodeServerPort, "0.0.0.0", function() {})
