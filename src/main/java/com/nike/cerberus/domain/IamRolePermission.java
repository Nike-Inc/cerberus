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

import javax.validation.constraints.Pattern;
import javax.validation.groups.Default;
import java.time.OffsetDateTime;

import static com.nike.cerberus.domain.IamRoleRegex.IAM_ROLE_ACCT_ID_REGEX;
import static com.nike.cerberus.domain.IamRoleRegex.IAM_ROLE_NAME_REGEX;

/**
 * Represents a permission granted to an IAM role with regards to a safe deposit box
 */
public class IamRolePermission {

    private String id;

    @Pattern(regexp = IAM_ROLE_ACCT_ID_REGEX, message = "IAM_ROLE_ACCT_ID_INVALID", groups = {Default.class, Updatable.class})
    private String accountId;

    // TODO: remove
    @Pattern(regexp = IAM_ROLE_NAME_REGEX, message = "IAM_ROLE_NAME_INVALID", groups = {Default.class, Updatable.class})
    private String iamRoleName;

    // TODO: remove
    @NotBlank(message = "IAM_ROLE_ROLE_ID_INVALID", groups = {Default.class, Updatable.class})
    private String roleId;

    private String iamPrincipalArn;

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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public IamRolePermission withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getIamRoleName() {
        return iamRoleName;
    }

    public void setIamRoleName(String iamRoleName) {
        this.iamRoleName = iamRoleName;
    }

    public IamRolePermission withIamRoleName(String iamRoleName) {
        this.iamRoleName = iamRoleName;
        return this;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public IamRolePermission withRoleId(String roleId) {
        this.roleId = roleId;
        return this;
    }

    public String getIamPrincipalArn() {
        return iamPrincipalArn;
    }

    public void setIamPrincipalArn(String iamPrincipalArn) {
        this.iamPrincipalArn = iamPrincipalArn;
    }

    public IamRolePermission withIamRoleArn(String iamRoleArn) {
        this.iamPrincipalArn = iamRoleArn;
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

        IamRolePermission that = (IamRolePermission) o;

        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
        if (iamRoleName != null ? !iamRoleName.equals(that.iamRoleName) : that.iamRoleName == null) return false;
        return iamPrincipalArn != null ? iamPrincipalArn.equals(that.iamPrincipalArn) : that.iamPrincipalArn == null;

    }

    @Override
    public int hashCode() {
        int result = accountId != null ? accountId.hashCode() : 0;
        result = 31 * result + (iamRoleName != null ? iamRoleName.hashCode() : 0);
        result = 31 * result + (iamPrincipalArn != null ? iamPrincipalArn.hashCode() : 0);
        return result;
    }
}
