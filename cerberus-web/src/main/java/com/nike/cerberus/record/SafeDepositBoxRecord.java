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

/** POJO for representing a SAFE_DEPOSIT_BOX record. */
public class SafeDepositBoxRecord {

  private String id;

  private String categoryId;

  private String name;

  private String description;

  private String path;

  private String sdbNameSlug;

  private String createdBy;

  private String lastUpdatedBy;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  public String getId() {
    return id;
  }

  public SafeDepositBoxRecord setId(String id) {
    this.id = id;
    return this;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public SafeDepositBoxRecord setCategoryId(String categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  public String getName() {
    return name;
  }

  public SafeDepositBoxRecord setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public SafeDepositBoxRecord setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getPath() {
    return path;
  }

  public SafeDepositBoxRecord setPath(String path) {
    this.path = path;
    return this;
  }

  public String getSdbNameSlug() {
    return sdbNameSlug;
  }

  public SafeDepositBoxRecord setSdbNameSlug(String sdbNameSlug) {
    this.sdbNameSlug = sdbNameSlug;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public SafeDepositBoxRecord setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public SafeDepositBoxRecord setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public SafeDepositBoxRecord setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public SafeDepositBoxRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SafeDepositBoxRecord record = (SafeDepositBoxRecord) o;

    if (id != null ? !id.equals(record.id) : record.id != null) return false;
    if (categoryId != null ? !categoryId.equals(record.categoryId) : record.categoryId != null)
      return false;
    if (name != null ? !name.equals(record.name) : record.name != null) return false;
    if (sdbNameSlug != null ? !sdbNameSlug.equals(record.sdbNameSlug) : record.sdbNameSlug != null)
      return false;
    if (description != null ? !description.equals(record.description) : record.description != null)
      return false;
    if (path != null ? !path.equals(record.path) : record.path != null) return false;
    if (createdBy != null ? !createdBy.equals(record.createdBy) : record.createdBy != null)
      return false;
    if (lastUpdatedBy != null
        ? !lastUpdatedBy.equals(record.lastUpdatedBy)
        : record.lastUpdatedBy != null) return false;
    if (createdTs != null ? !createdTs.equals(record.createdTs) : record.createdTs != null)
      return false;
    return lastUpdatedTs != null
        ? lastUpdatedTs.equals(record.lastUpdatedTs)
        : record.lastUpdatedTs == null;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (categoryId != null ? categoryId.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (path != null ? path.hashCode() : 0);
    result = 31 * result + (sdbNameSlug != null ? sdbNameSlug.hashCode() : 0);
    result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
    result = 31 * result + (lastUpdatedBy != null ? lastUpdatedBy.hashCode() : 0);
    result = 31 * result + (createdTs != null ? createdTs.hashCode() : 0);
    result = 31 * result + (lastUpdatedTs != null ? lastUpdatedTs.hashCode() : 0);
    return result;
  }
}
