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
import java.util.Arrays;

public class SecureFileVersion {

    private String id;
    private String sdboxId;
    private String path;
    private byte[] data;
    private String name;
    private int sizeInBytes;
    private String action;
    private String versionCreatedBy;
    private OffsetDateTime versionCreatedTs;
    private String actionPrincipal;
    private OffsetDateTime actionTs;

    public String getId() {
        return id;
    }

    public SecureFileVersion setId(String id) {
        this.id = id;
        return this;
    }

    public String getSdboxId() {
        return sdboxId;
    }

    public SecureFileVersion setSdboxId(String sdboxId) {
        this.sdboxId = sdboxId;
        return this;
    }

    public String getPath() {
        return path;
    }

    public SecureFileVersion setPath(String path) {
        this.path = path;
        return this;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public SecureFileVersion setData(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
        return this;
    }

    public String getName() {
        return name;
    }

    public SecureFileVersion setName(String name) {
        this.name = name;
        return this;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public SecureFileVersion setSizeInBytes(int sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
        return this;
    }

    public String getAction() {
        return action;
    }

    public SecureFileVersion setAction(String action) {
        this.action = action;
        return this;
    }

    public String getVersionCreatedBy() {
        return versionCreatedBy;
    }

    public SecureFileVersion setVersionCreatedBy(String versionCreatedBy) {
        this.versionCreatedBy = versionCreatedBy;
        return this;
    }

    public OffsetDateTime getVersionCreatedTs() {
        return versionCreatedTs;
    }

    public SecureFileVersion setVersionCreatedTs(OffsetDateTime versionCreatedTs) {
        this.versionCreatedTs = versionCreatedTs;
        return this;
    }

    public String getActionPrincipal() {
        return actionPrincipal;
    }

    public SecureFileVersion setActionPrincipal(String actionPrincipal) {
        this.actionPrincipal = actionPrincipal;
        return this;
    }

    public OffsetDateTime getActionTs() {
        return actionTs;
    }

    public SecureFileVersion setActionTs(OffsetDateTime actionTs) {
        this.actionTs = actionTs;
        return this;
    }

    public enum SecretsAction {
        CREATE,
        UPDATE,
        DELETE
    }
}
