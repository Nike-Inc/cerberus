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

import java.time.OffsetDateTime;

public class SecureFileSummary {

    private String sdboxId;
    private String path;
    private int sizeInBytes;
    private String name;
    private String createdBy;
    private OffsetDateTime createdTs;
    private String lastUpdatedBy;
    private OffsetDateTime lastUpdatedTs;

    public String getSdboxId() {
        return sdboxId;
    }

    public SecureFileSummary setSdboxId(String sdboxId) {
        this.sdboxId = sdboxId;
        return this;
    }

    public String getPath() {
        return path;
    }

    public SecureFileSummary setPath(String path) {
        this.path = path;
        return this;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public SecureFileSummary setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
        return this;
    }

    public String getName() {
        return name;
    }

    public SecureFileSummary setName(String name) {
        this.name = name;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public SecureFileSummary setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public OffsetDateTime getCreatedTs() {
        return createdTs;
    }

    public SecureFileSummary setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
        return this;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public SecureFileSummary setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
        return this;
    }

    public OffsetDateTime getLastUpdatedTs() {
        return lastUpdatedTs;
    }

    public SecureFileSummary setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
        this.lastUpdatedTs = lastUpdatedTs;
        return this;
    }
}
