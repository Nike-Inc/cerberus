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
import java.util.Objects;

/** POJO for representing a AWS_IAM_ROLE_PERMISSIONS record. */
public class AwsIamRolePermissionRecord {

  private String id;

  private String roleId;

  private String awsIamRoleId;

  private String sdboxId;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  private String createdBy;

  private String lastUpdatedBy;

  public String getId() {
    return id;
  }

  public AwsIamRolePermissionRecord setId(String id) {
    this.id = id;
    return this;
  }

  public String getRoleId() {
    return roleId;
  }

  public AwsIamRolePermissionRecord setRoleId(String roleId) {
    this.roleId = roleId;
    return this;
  }

  public String getAwsIamRoleId() {
    return awsIamRoleId;
  }

  public AwsIamRolePermissionRecord setAwsIamRoleId(String awsIamRoleId) {
    this.awsIamRoleId = awsIamRoleId;
    return this;
  }

  public String getSdboxId() {
    return sdboxId;
  }

  public AwsIamRolePermissionRecord setSdboxId(String sdboxId) {
    this.sdboxId = sdboxId;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public AwsIamRolePermissionRecord setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public AwsIamRolePermissionRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public AwsIamRolePermissionRecord setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public AwsIamRolePermissionRecord setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsIamRolePermissionRecord that = (AwsIamRolePermissionRecord) o;
    return Objects.equals(id, that.id)
        && Objects.equals(roleId, that.roleId)
        && Objects.equals(awsIamRoleId, that.awsIamRoleId)
        && Objects.equals(sdboxId, that.sdboxId)
        && Objects.equals(createdTs, that.createdTs)
        && Objects.equals(lastUpdatedTs, that.lastUpdatedTs)
        && Objects.equals(createdBy, that.createdBy)
        && Objects.equals(lastUpdatedBy, that.lastUpdatedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, roleId, awsIamRoleId, sdboxId, createdTs, lastUpdatedTs, createdBy, lastUpdatedBy);
  }
}
