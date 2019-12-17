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
 */

package com.nike.cerberus.domain;

import java.time.OffsetDateTime;
import java.util.Map;

public class SDBMetadata {

  private String name;
  private String path;
  private String category;
  private String owner;
  private String description;
  private OffsetDateTime createdTs;
  private String createdBy;
  private OffsetDateTime lastUpdatedTs;
  private String lastUpdatedBy;
  private Map<String, String> userGroupPermissions;
  private Map<String, String> iamRolePermissions;
  private Map<String, Map<String, Object>> data;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public void setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public void setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public void setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
  }

  public Map<String, String> getUserGroupPermissions() {
    return userGroupPermissions;
  }

  public void setUserGroupPermissions(Map<String, String> userGroupPermissions) {
    this.userGroupPermissions = userGroupPermissions;
  }

  public Map<String, String> getIamRolePermissions() {
    return iamRolePermissions;
  }

  public void setIamRolePermissions(Map<String, String> iamRolePermissions) {
    this.iamRolePermissions = iamRolePermissions;
  }

  public Map<String, Map<String, Object>> getData() {
    return data;
  }

  public void setData(Map<String, Map<String, Object>> data) {
    this.data = data;
  }
}
