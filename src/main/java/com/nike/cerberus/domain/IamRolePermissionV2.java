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

import com.nike.cerberus.validation.group.Updatable;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;
import javax.validation.groups.Default;
import java.time.OffsetDateTime;

import static com.nike.cerberus.util.AwsIamRoleArnParser.AWS_IAM_PRINCIPAL_ARN_REGEX;
import static com.nike.cerberus.util.AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_REGEX;

/**
 * Represents a permission granted to an IAM role with regards to a safe deposit box
 */
public class IamRolePermissionV2 {

    private String id;

    @NotBlank(message = "IAM_ROLE_ROLE_ID_INVALID", groups = {Default.class, Updatable.class})
    private String roleId;

    @Pattern(regexp = AWS_IAM_PRINCIPAL_ARN_REGEX, message = "SDB_IAM_PRINCIPAL_PERMISSION_ARN_INVALID", groups = {Default.class, Updatable.class})
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

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public IamRolePermissionV2 withRoleId(String roleId) {
        this.roleId = roleId;
        return this;
    }

    public String getIamPrincipalArn() {
        return iamPrincipalArn;
    }

    public void setIamPrincipalArn(String iamPrincipalArn) {
        this.iamPrincipalArn = iamPrincipalArn;
    }

    public IamRolePermissionV2 withIamPrincipalArn(String iamRoleArn) {
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

        IamRolePermissionV2 that = (IamRolePermissionV2) o;

        return iamPrincipalArn != null ? iamPrincipalArn.equals(that.iamPrincipalArn) : that.iamPrincipalArn == null;

    }

    @Override
    public int hashCode() {
        return iamPrincipalArn != null ? iamPrincipalArn.hashCode() : 0;
    }
}
