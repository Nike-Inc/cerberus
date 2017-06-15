/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var path = require('path');

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
redwire.http('http://localhost:' + reverseProxyPort + '/dashboard', '127.0.0.1:' + nodeServerPort)
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/dashboard', '127.0.0.1:' + nodeServerPort)
// redirect rule for Cerberus Management Service
redwire.http('http://localhost:' + reverseProxyPort + '/v1', '127.0.0.1:' + cmsPort + '/v1')
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/v1', '127.0.0.1:' + cmsPort + '/v1')
redwire.http('http://localhost:' + reverseProxyPort + '/v2', '127.0.0.1:' + cmsPort + '/v2')
redwire.http('http://127.0.0.1:' + reverseProxyPort + '/v2', '127.0.0.1:' + cmsPort + '/v2')

var express = require('express')
var app = express()

app.use(express.static(__dirname + '/../build/dashboard'))

app.listen(nodeServerPort, function () {
    console.log('express server listing on port ' + nodeServerPort)
})

console.log('Cerberus reverse proxy up and running on http://localhost:' + reverseProxyPort)