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
 * POJO representing the payload of a create login session token response.
 */
class SessionLoginTokenData {

    private String status;

    private SessionUser user;

    private String returnToUrl;

    private String sessionToken;

    private String stateToken;

    private String callBackUrl;

    private List<MfaDevice> devices;

    public String getStatus() {
        return status;
    }

    public SessionLoginTokenData setStatus(String status) {
        this.status = status;
        return this;
    }

    public SessionUser getUser() {
        return user;
    }

    public SessionLoginTokenData setUser(SessionUser user) {
        this.user = user;
        return this;
    }

    public String getReturnToUrl() {
        return returnToUrl;
    }

    public SessionLoginTokenData setReturnToUrl(String returnToUrl) {
        this.returnToUrl = returnToUrl;
        return this;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public SessionLoginTokenData setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
        return this;
    }

    public String getStateToken() {
        return stateToken;
    }

    public SessionLoginTokenData setStateToken(String stateToken) {
        this.stateToken = stateToken;
        return this;
    }

    public String getCallBackUrl() {
        return callBackUrl;
    }

    public SessionLoginTokenData setCallBackUrl(String callBackUrl) {
        this.callBackUrl = callBackUrl;
        return this;
    }

    public List<MfaDevice> getDevices() {
        return devices;
    }

    public SessionLoginTokenData setDevices(List<MfaDevice> devices) {
        this.devices = devices;
        return this;
    }
}
