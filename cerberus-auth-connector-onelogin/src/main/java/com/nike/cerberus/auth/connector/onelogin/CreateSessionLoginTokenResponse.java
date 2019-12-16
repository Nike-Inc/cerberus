/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.auth.connector.onelogin;

import java.util.List;

/**
 * POJO representing a create session login token response.
 */
class CreateSessionLoginTokenResponse {

    private ResponseStatus status;

    private List<SessionLoginTokenData> data;

    public ResponseStatus getStatus() {
        return status;
    }

    public CreateSessionLoginTokenResponse setStatus(ResponseStatus status) {
        this.status = status;
        return this;
    }

    public List<SessionLoginTokenData> getData() {
        return data;
    }

    public CreateSessionLoginTokenResponse setData(List<SessionLoginTokenData> data) {
        this.data = data;
        return this;
    }
}
