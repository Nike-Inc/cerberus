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

package com.nike.cerberus.domain;

import com.nike.cerberus.validation.group.Updatable;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.groups.Default;
import java.time.OffsetDateTime;

/**
 * Represents a permission granted to a user group with regards to a safe deposit box.
 */
public class UserGroupPermission {

    private String id;

    @NotBlank(message = "USER_GROUP_NAME_BLANK", groups = {Default.class, Updatable.class})
    private String name;

    @NotBlank(message = "USER_GROUP_ROLE_ID_INVALID", groups = {Default.class, Updatable.class})
    private String roleId;

    private OffsetDateTime createdTs;

    private OffsetDateTime lastUpdatedTs;

    private String createdBy;

    private String lastUpdatedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserGroupPermission withName(String name) {
        this.name = name;
        return this;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public UserGroupPermission withRoleId(String roleId) {
        this.roleId = roleId;
        return this;
    }

    public OffsetDateTime getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(OffsetDateTime createdTs) {
        this.createdTs = createdTs;
    }

    public OffsetDateTime getLastUpdatedTs() {
        return lastUpdatedTs;
    }

    public void setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
        this.lastUpdatedTs = lastUpdatedTs;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserGroupPermission that = (UserGroupPermission) o;

        return name != null ? name.equals(that.name) : that.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }


    public static final class Builder {
        private String id;
        private String name;
        private String roleId;
        private OffsetDateTime createdTs;
        private OffsetDateTime lastUpdatedTs;
        private String createdBy;
        private String lastUpdatedBy;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withRoleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder withCreatedTs(OffsetDateTime createdTs) {
            this.createdTs = createdTs;
            return this;
        }

        public Builder withLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
            this.lastUpdatedTs = lastUpdatedTs;
            return this;
        }

        public Builder withCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder withLastUpdatedBy(String lastUpdatedBy) {
            this.lastUpdatedBy = lastUpdatedBy;
            return this;
        }

        public UserGroupPermission build() {
            UserGroupPermission userGroupPermission = new UserGroupPermission();
            userGroupPermission.setId(id);
            userGroupPermission.setName(name);
            userGroupPermission.setRoleId(roleId);
            userGroupPermission.setCreatedTs(createdTs);
            userGroupPermission.setLastUpdatedTs(lastUpdatedTs);
            userGroupPermission.setCreatedBy(createdBy);
            userGroupPermission.setLastUpdatedBy(lastUpdatedBy);
            return userGroupPermission;
        }
    }
}
