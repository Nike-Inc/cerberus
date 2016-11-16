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

/**
 * POJO representing refresh token request.
 */
class RefreshTokenRequest {

    private String grantType = "refresh_token";

    private String accessToken;

    private String refreshToken;

    public String getGrantType() {
        return grantType;
    }

    public RefreshTokenRequest setGrantType(String grantType) {
        this.grantType = grantType;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public RefreshTokenRequest setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public RefreshTokenRequest setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }
}
