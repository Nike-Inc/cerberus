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

/** POJO for representing a AWS_IAM_ROLE record. */
public class AwsIamRoleRecord {

  private String id;

  private String createdBy;

  private String lastUpdatedBy;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  private String awsIamRoleArn;

  public String getId() {
    return id;
  }

  public AwsIamRoleRecord setId(String id) {
    this.id = id;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public AwsIamRoleRecord setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public AwsIamRoleRecord setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public AwsIamRoleRecord setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public AwsIamRoleRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  public String getAwsIamRoleArn() {
    return awsIamRoleArn;
  }

  public AwsIamRoleRecord setAwsIamRoleArn(String awsIamRoleArn) {
    this.awsIamRoleArn = awsIamRoleArn;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AwsIamRoleRecord that = (AwsIamRoleRecord) o;
    return Objects.equals(id, that.id)
        && Objects.equals(createdBy, that.createdBy)
        && Objects.equals(lastUpdatedBy, that.lastUpdatedBy)
        && Objects.equals(createdTs, that.createdTs)
        && Objects.equals(lastUpdatedTs, that.lastUpdatedTs)
        && Objects.equals(awsIamRoleArn, that.awsIamRoleArn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, createdBy, lastUpdatedBy, createdTs, lastUpdatedTs);
  }
}
