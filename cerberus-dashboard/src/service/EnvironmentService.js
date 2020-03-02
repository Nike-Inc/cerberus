/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Service for determining the running environment and domain to use for Cerberus API Calls.
 */
export default class EnvironmentService {

    static getEnvironment() {
        var url = document.URL;

        var re = /(\w+:\/\/)(.*?)\/.*/g;
        var m;
        var host;

        while ((m = re.exec(url)) !== null) {
            if (m.index === re.lastIndex) {
                re.lastIndex++;
            }
            host = m[2];
        }

        var env;
        if (host.startsWith('localhost') || host.startsWith('127.0.0.1')) {
            env = 'local';
        } else {
            var pieces = host.split('.');
            env = pieces[0];
        }

        return env;
    }

    /**
     *  Parses the URL to get the domain of the SMaaS
     */
    static getDomain() {
        var url = document.URL;

        var re = /(\w+:\/\/.*?)\/.*/g;
        var m;
        var domain;

        while ((m = re.exec(url)) !== null) {
            if (m.index === re.lastIndex) {
                re.lastIndex++;
            }
            domain = m[1];
        }

        return domain;
    }
}