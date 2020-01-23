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

package com.nike.cerberus.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

public class AuthKmsKeyMetadata {

  private String awsIamRoleArn;
  private String awsKmsKeyId;
  private String awsRegion;
  private OffsetDateTime createdTs;
  private OffsetDateTime lastUpdatedTs;
  private OffsetDateTime lastValidatedTs;

  public String getAwsIamRoleArn() {
    return awsIamRoleArn;
  }

  public AuthKmsKeyMetadata setAwsIamRoleArn(String awsIamRoleArn) {
    this.awsIamRoleArn = awsIamRoleArn;
    return this;
  }

  public String getAwsKmsKeyId() {
    return awsKmsKeyId;
  }

  public AuthKmsKeyMetadata setAwsKmsKeyId(String awsKmsKeyId) {
    this.awsKmsKeyId = awsKmsKeyId;
    return this;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public AuthKmsKeyMetadata setAwsRegion(String awsRegion) {
    this.awsRegion = awsRegion;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public AuthKmsKeyMetadata setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public AuthKmsKeyMetadata setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  public OffsetDateTime getLastValidatedTs() {
    return lastValidatedTs;
  }

  public AuthKmsKeyMetadata setLastValidatedTs(OffsetDateTime lastValidatedTs) {
    this.lastValidatedTs = lastValidatedTs;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuthKmsKeyMetadata that = (AuthKmsKeyMetadata) o;
    return Objects.equals(awsIamRoleArn, that.awsIamRoleArn)
        && Objects.equals(awsKmsKeyId, that.awsKmsKeyId)
        && Objects.equals(awsRegion, that.awsRegion)
        && Objects.equals(createdTs, that.createdTs)
        && Objects.equals(lastUpdatedTs, that.lastUpdatedTs)
        && Objects.equals(lastValidatedTs, that.lastValidatedTs);
  }

  @Override
  public int hashCode() {

    return Objects.hash(
        awsIamRoleArn, awsKmsKeyId, awsRegion, createdTs, lastUpdatedTs, lastValidatedTs);
  }
}
