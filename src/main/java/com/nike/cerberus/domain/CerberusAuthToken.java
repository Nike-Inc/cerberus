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

package com.nike.cerberus.domain;


import com.nike.cerberus.PrincipalType;

import java.time.OffsetDateTime;

public class CerberusAuthToken {

    private String token;
    private OffsetDateTime created;
    private OffsetDateTime expires;
    private String principal;
    private PrincipalType principalType;
    private boolean isAdmin;
    private String groups;
    private int refreshCount;
    private String id;

    public String getToken() {
        return token;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public OffsetDateTime getExpires() {
        return expires;
    }

    public String getPrincipal() {
        return principal;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public String getGroups() {
        return groups;
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public String getId() {
        return id;
    }

    public static final class Builder {
        private String token;
        private OffsetDateTime created;
        private OffsetDateTime expires;
        private String principal;
        private PrincipalType principalType;
        private boolean isAdmin;
        private String groups;
        private int refreshCount;
        private String id;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withToken(String token) {
            this.token = token;
            return this;
        }

        public Builder withCreated(OffsetDateTime created) {
            this.created = created;
            return this;
        }

        public Builder withExpires(OffsetDateTime expires) {
            this.expires = expires;
            return this;
        }

        public Builder withPrincipal(String principal) {
            this.principal = principal;
            return this;
        }

        public Builder withPrincipalType(PrincipalType principalType) {
            this.principalType = principalType;
            return this;
        }

        public Builder withIsAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        public Builder withGroups(String groups) {
            this.groups = groups;
            return this;
        }

        public Builder withRefreshCount(int refreshCount) {
            this.refreshCount = refreshCount;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public CerberusAuthToken build() {
            CerberusAuthToken generateTokenResult = new CerberusAuthToken();
            generateTokenResult.refreshCount = this.refreshCount;
            generateTokenResult.principal = this.principal;
            generateTokenResult.token = this.token;
            generateTokenResult.isAdmin = this.isAdmin;
            generateTokenResult.expires = this.expires;
            generateTokenResult.groups = this.groups;
            generateTokenResult.principalType = this.principalType;
            generateTokenResult.created = this.created;
            generateTokenResult.id = this.id;
            return generateTokenResult;
        }
    }
}
