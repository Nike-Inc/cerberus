/*
 * Copyright (c) 2019 Nike, Inc.
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

package com.nike.cerberus.domain;

import java.util.Map;
import java.util.Set;

public class AuthTokenResponse {

    private String clientToken;

    private Set<String> policies;

    private Map<String, String> metadata;

    private long leaseDuration;

    private boolean renewable;

    public String getClientToken() {
        return clientToken;
    }

    public AuthTokenResponse setClientToken(String clientToken) {
        this.clientToken = clientToken;
        return this;
    }

    public Set<String> getPolicies() {
        return policies;
    }

    public AuthTokenResponse setPolicies(Set<String> policies) {
        this.policies = policies;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public AuthTokenResponse setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public long getLeaseDuration() {
        return leaseDuration;
    }

    public AuthTokenResponse setLeaseDuration(long leaseDuration) {
        this.leaseDuration = leaseDuration;
        return this;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public AuthTokenResponse setRenewable(boolean renewable) {
        this.renewable = renewable;
        return this;
    }

}
