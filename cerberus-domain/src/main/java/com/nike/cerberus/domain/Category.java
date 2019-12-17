/*
 * Copyright (c) 2019 Nike, Inc.
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
import javax.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/** Represents a category. */
public class Category {

  /** Unique identifier for a category. */
  private String id;

  /** User friendly display name for a category. */
  @NotBlank(message = "CATEGORY_DISPLAY_NAME_BLANK")
  @Length(max = 100, message = "CATEGORY_DISPLAY_NAME_TOO_LONG")
  private String displayName;

  /** The path segment used in provisioning. */
  private String path;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  private String createdBy;

  private String lastUpdatedBy;

  public String getId() {
    return id;
  }

  public Category setId(String id) {
    this.id = id;
    return this;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Category setDisplayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  public String getPath() {
    return path;
  }

  public Category setPath(String path) {
    this.path = path;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public Category setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public Category setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public Category setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public Category setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }
}
