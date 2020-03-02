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

export var simulateLogin = () => {
    sessionStorage.setItem('token', JSON.stringify({
        "client_token": "7f6808f1-ede3-2177-aa9d-45f507391310",
        "policies": [
            "web",
            "stage"
        ],
        "metadata": {
            "username": "john.doe@nike.com",
            "is_admin": "false",
            "groups": "Lst-CDT.CloudPlatformEngine.FTE,Lst-digital.platform-tools.internal"
        },
        "lease_duration": 3600,
        "renewable": true
    }))
}