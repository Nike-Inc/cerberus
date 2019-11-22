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

package com.nike.cerberus.record;

import java.time.OffsetDateTime;

/**
 * POJO for representing a USER_GROUP_PERMISSION record.
 */
public class UserGroupPermissionRecord {

    private String id;

    private String userGroupId;

    private String roleId;

    private String sdboxId;

    private String createdBy;

    private String lastUpdatedBy;

    private OffsetDateTime createdTs;

    private OffsetDateTime lastUpdatedTs;

    public String getId() {
        return id;
    }

    public UserGroupPermissionRecord setId(String id) {
        this.id = id;
        return this;
    }

    public String getUserGroupId() {
        return userGroupId;
    }

    public UserGroupPermissionRecord setUserGroupId(String userGroupId) {
        this.userGroupId = userGroupId;
        return this;
    }

    public String getRoleId() {
        return roleId;
    }

    public UserGroupPermissionRecord setRoleId(String roleId) {
        this.roleId = roleId;
        return this;
    }

    public String getSdboxId() {
        return sdboxId;
    }

    public UserGroupPermissionRecord setSdboxId(String sdboxId) {
        this.sdboxId = sdboxId;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public UserGroupPermissionRecord setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public UserGroupPermissionRecord setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
        return this;
    }

    public OffsetDateTime getCreatedTs() {
        return createdTs;
    }

    public UserGroupPermissionRecord setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
        return this;
    }

    public OffsetDateTime getLastUpdatedTs() {
        return lastUpdatedTs;
    }

    public UserGroupPermissionRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
        this.lastUpdatedTs = lastUpdatedTs;
        return this;
    }
}
