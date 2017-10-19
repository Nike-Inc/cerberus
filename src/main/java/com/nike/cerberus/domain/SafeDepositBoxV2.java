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
 *
 */

package com.nike.cerberus.domain;

import com.nike.cerberus.validation.UniqueIamPrincipalPermissions;
import com.nike.cerberus.validation.UniqueOwner;
import com.nike.cerberus.validation.UniqueUserGroupPermissions;
import com.nike.cerberus.validation.group.Updatable;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.groups.Default;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a logical grouping of secrets.
 */
@UniqueOwner(groups = {Default.class, Updatable.class})
public class SafeDepositBoxV2 implements SafeDepositBox {

    private String id;

    @NotBlank(message = "SDB_CATEGORY_ID_INVALID")
    private String categoryId;

    @NotBlank(message = "SDB_NAME_BLANK")
    @Length(max = 100, message = "SDB_NAME_TOO_LONG")
    private String name;

    @Length(max = 1000, message = "SDB_DESCRIPTION_TOO_LONG", groups = {Default.class, Updatable.class})
    private String description;

    private String path;

    private OffsetDateTime createdTs;

    private OffsetDateTime lastUpdatedTs;

    private String createdBy;

    private String lastUpdatedBy;

    @NotBlank(message = "SDB_OWNER_BLANK", groups = {Default.class, Updatable.class})
    @Length(max = 255, message = "SDB_OWNER_TOO_LONG", groups = {Default.class, Updatable.class})
    private String owner;

    @Valid
    @UniqueUserGroupPermissions(groups = {Default.class, Updatable.class})
    private Set<UserGroupPermission> userGroupPermissions = new HashSet<>();

    @Valid
    @UniqueIamPrincipalPermissions(groups = {Default.class, Updatable.class})
    private Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Set<UserGroupPermission> getUserGroupPermissions() {
        return userGroupPermissions;
    }

    public void setUserGroupPermissions(Set<UserGroupPermission> userGroupPermissions) {
        this.userGroupPermissions = userGroupPermissions;
    }

    public Set<IamPrincipalPermission> getIamPrincipalPermissions() {
        return iamPrincipalPermissions;
    }

    public void setIamPrincipalPermissions(Set<IamPrincipalPermission> iamPrincipalPermissions) {
        this.iamPrincipalPermissions = iamPrincipalPermissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SafeDepositBoxV2 that = (SafeDepositBoxV2) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (categoryId != null ? !categoryId.equals(that.categoryId) : that.categoryId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (createdTs != null ? !createdTs.equals(that.createdTs) : that.createdTs != null) return false;
        if (lastUpdatedTs != null ? !lastUpdatedTs.equals(that.lastUpdatedTs) : that.lastUpdatedTs != null)
            return false;
        if (createdBy != null ? !createdBy.equals(that.createdBy) : that.createdBy != null) return false;
        if (lastUpdatedBy != null ? !lastUpdatedBy.equals(that.lastUpdatedBy) : that.lastUpdatedBy != null)
            return false;
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        if (userGroupPermissions != null ? !userGroupPermissions.equals(that.userGroupPermissions) : that.userGroupPermissions != null)
            return false;
        return iamPrincipalPermissions != null ? iamPrincipalPermissions.equals(that.iamPrincipalPermissions) : that.iamPrincipalPermissions == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (categoryId != null ? categoryId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (createdTs != null ? createdTs.hashCode() : 0);
        result = 31 * result + (lastUpdatedTs != null ? lastUpdatedTs.hashCode() : 0);
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        result = 31 * result + (lastUpdatedBy != null ? lastUpdatedBy.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (userGroupPermissions != null ? userGroupPermissions.hashCode() : 0);
        result = 31 * result + (iamPrincipalPermissions != null ? iamPrincipalPermissions.hashCode() : 0);
        return result;
    }


    public static final class Builder {
        private String id;
        private String categoryId;
        private String name;
        private String description;
        private String path;
        private OffsetDateTime createdTs;
        private OffsetDateTime lastUpdatedTs;
        private String createdBy;
        private String lastUpdatedBy;
        private String owner;
        private Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
        private Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withCategoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withPath(String path) {
            this.path = path;
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

        public Builder withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder withUserGroupPermissions(Set<UserGroupPermission> userGroupPermissions) {
            this.userGroupPermissions = userGroupPermissions;
            return this;
        }

        public Builder withIamPrincipalPermissions(Set<IamPrincipalPermission> iamPrincipalPermissions) {
            this.iamPrincipalPermissions = iamPrincipalPermissions;
            return this;
        }

        public SafeDepositBoxV2 build() {
            SafeDepositBoxV2 safeDepositBoxV2 = new SafeDepositBoxV2();
            safeDepositBoxV2.setId(id);
            safeDepositBoxV2.setCategoryId(categoryId);
            safeDepositBoxV2.setName(name);
            safeDepositBoxV2.setDescription(description);
            safeDepositBoxV2.setPath(path);
            safeDepositBoxV2.setCreatedTs(createdTs);
            safeDepositBoxV2.setLastUpdatedTs(lastUpdatedTs);
            safeDepositBoxV2.setCreatedBy(createdBy);
            safeDepositBoxV2.setLastUpdatedBy(lastUpdatedBy);
            safeDepositBoxV2.setOwner(owner);
            safeDepositBoxV2.setUserGroupPermissions(userGroupPermissions);
            safeDepositBoxV2.setIamPrincipalPermissions(iamPrincipalPermissions);
            return safeDepositBoxV2;
        }
    }
}
