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

// https://www.npmjs.com/package/redwire
var RedWire = require('redwire');
var redwire = new RedWire({
    http: {
        port: 9000,
        websockets: true
    }
});

/**
 * Cerberus is a couple services behind a router so we can simulate that locally
 */
// redirect /secret to Hashicoorp Vault
redwire.http('http://localhost:9000/v1/secret', '127.0.0.1:8200/v1/secret');
redwire.http('http://127.0.0.1:9000/v1/secret', '127.0.0.1:8200/v1/secret');
// redirect dashboard to the Cerberus Management Dashboard
redwire.http('http://localhost:9000/dashboard', '127.0.0.1:8000');
redwire.http('http://127.0.0.1:9000/dashboard', '127.0.0.1:8000');
// redirect rule for Cerberus Management Service
redwire.http('http://localhost:9000/v1', '127.0.0.1:8080/v1');
redwire.http('http://127.0.0.1:9000/v1', '127.0.0.1:8080/v1');
redwire.http('http://localhost:9000/v2', '127.0.0.1:8080/v2');
redwire.http('http://127.0.0.1:9000/v2', '127.0.0.1:8080/v2');

var express = require('express')
var app = express()

app.use(express.static(__dirname + '/../build/dashbord'))

app.listen(8000, function () {
    console.log('express server listing on port 8000')
})

console.log('Cerberus reverse proxy up and running on http://localhost:9000')