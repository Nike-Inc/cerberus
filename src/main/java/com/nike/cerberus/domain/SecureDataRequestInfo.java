/*
 * Copyright (c) 2018 Nike, Inc.
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
 *
 */

package com.nike.cerberus.domain;

import com.nike.cerberus.security.CerberusPrincipal;

import java.util.Optional;

public class SecureDataRequestInfo {
    private String category;
    private String sdbSlug;
    private String sdbId;
    private String subPath;
    private CerberusPrincipal principal;

    public String getCategory() {
        return category;
    }

    public SecureDataRequestInfo setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getSdbSlug() {
        return sdbSlug;
    }

    public SecureDataRequestInfo setSdbSlug(String sdbSlug) {
        this.sdbSlug = sdbSlug;
        return this;
    }

    public String getSdbId() {
        return sdbId;
    }

    public SecureDataRequestInfo setSdbId(String sdbId) {
        this.sdbId = sdbId;
        return this;
    }

    public SecureDataRequestInfo setSubPath(String path) {
        this.subPath = path;
        return this;
    }

    public CerberusPrincipal getPrincipal() {
        return principal;
    }

    public SecureDataRequestInfo setPrincipal(CerberusPrincipal principal) {
        this.principal = principal;
        return this;
    }

    public String getPath() {
        return String.format("%s/%s", sdbSlug, Optional.ofNullable(subPath).orElse(""));
    }
}
