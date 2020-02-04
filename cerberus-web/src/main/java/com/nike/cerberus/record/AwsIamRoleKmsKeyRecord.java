/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

/** POJO for representing a AWS_IAM_ROLE_KMS_KEY record. */
public class AwsIamRoleKmsKeyRecord {

  private String id;

  private String awsIamRoleId;

  private String awsRegion;

  private String awsKmsKeyId;

  private String createdBy;

  private String lastUpdatedBy;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  private OffsetDateTime lastValidatedTs;

  public String getId() {
    return id;
  }

  public AwsIamRoleKmsKeyRecord setId(String id) {
    this.id = id;
    return this;
  }

  public String getAwsIamRoleId() {
    return awsIamRoleId;
  }

  public AwsIamRoleKmsKeyRecord setAwsIamRoleId(String awsIamRoleId) {
    this.awsIamRoleId = awsIamRoleId;
    return this;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public AwsIamRoleKmsKeyRecord setAwsRegion(String awsRegion) {
    this.awsRegion = awsRegion;
    return this;
  }

  public String getAwsKmsKeyId() {
    return awsKmsKeyId;
  }

  public AwsIamRoleKmsKeyRecord setAwsKmsKeyId(String awsKmsKeyId) {
    this.awsKmsKeyId = awsKmsKeyId;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public AwsIamRoleKmsKeyRecord setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public AwsIamRoleKmsKeyRecord setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public AwsIamRoleKmsKeyRecord setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public AwsIamRoleKmsKeyRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  public OffsetDateTime getLastValidatedTs() {
    return lastValidatedTs;
  }

  public AwsIamRoleKmsKeyRecord setLastValidatedTs(OffsetDateTime lastValidatedTs) {
    this.lastValidatedTs = lastValidatedTs;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsIamRoleKmsKeyRecord that = (AwsIamRoleKmsKeyRecord) o;
    return Objects.equals(id, that.id)
        && Objects.equals(awsIamRoleId, that.awsIamRoleId)
        && Objects.equals(awsRegion, that.awsRegion)
        && Objects.equals(awsKmsKeyId, that.awsKmsKeyId)
        && Objects.equals(createdBy, that.createdBy)
        && Objects.equals(lastUpdatedBy, that.lastUpdatedBy)
        && Objects.equals(createdTs, that.createdTs)
        && Objects.equals(lastUpdatedTs, that.lastUpdatedTs)
        && Objects.equals(lastValidatedTs, that.lastValidatedTs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        awsIamRoleId,
        awsRegion,
        awsKmsKeyId,
        createdBy,
        lastUpdatedBy,
        createdTs,
        lastUpdatedTs,
        lastValidatedTs);
  }
}
