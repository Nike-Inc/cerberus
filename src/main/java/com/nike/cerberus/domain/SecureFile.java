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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.Arrays;

public class SecureFile {

    @JsonIgnore private String id;
    private String sdboxId;
    private String path;
    @JsonIgnore private byte[] data;
    private int sizeInBytes;
    private String name;
    private String createdBy;
    private OffsetDateTime createdTs;
    private String lastUpdatedBy;
    private OffsetDateTime lastUpdatedTs;

    public String getId() {
        return id;
    }

    public SecureFile setId(String id) {
        this.id = id;
        return this;
    }

    public String getSdboxId() {
        return sdboxId;
    }

    public SecureFile setSdboxId(String sdboxId) {
        this.sdboxId = sdboxId;
        return this;
    }

    public String getPath() {
        return path;
    }

    public SecureFile setPath(String path) {
        this.path = path;
        return this;
    }

    public byte[] getData() {
        return data != null ?
                Arrays.copyOf(data, data.length) :
                null;
    }

    public SecureFile setData(byte[] data) {
        this.data = data != null ?
                Arrays.copyOf(data, data.length) :
                null;
        return this;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public SecureFile setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
        return this;
    }

    public String getName() {
        return name;
    }

    public SecureFile setName(String name) {
        this.name = name;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public SecureFile setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public OffsetDateTime getCreatedTs() {
        return createdTs;
    }

    public SecureFile setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
        return this;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public SecureFile setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
        return this;
    }

    public OffsetDateTime getLastUpdatedTs() {
        return lastUpdatedTs;
    }

    public SecureFile setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
        this.lastUpdatedTs = lastUpdatedTs;
        return this;
    }
}
