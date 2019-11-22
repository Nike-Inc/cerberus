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

package com.nike.cerberus.auth.connector;

/**
 * Authentication response from the auth connector.
 */
public class AuthResponse {

    private AuthStatus status;

    private AuthData data;

    public AuthStatus getStatus() {
        return status;
    }

    public AuthResponse setStatus(AuthStatus status) {
        this.status = status;
        return this;
    }

    public AuthData getData() {
        return data;
    }

    public AuthResponse setData(AuthData data) {
        this.data = data;
        return this;
    }
}
