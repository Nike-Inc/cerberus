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

public class RoleRecord {

  public static final String ROLE_OWNER = "owner";

  public static final String ROLE_WRITE = "write";

  public static final String ROLE_READ = "read";

  private String id;

  private String name;

  private OffsetDateTime createdTs;

  private OffsetDateTime lastUpdatedTs;

  private String createdBy;

  private String lastUpdatedBy;

  public String getId() {
    return id;
  }

  public RoleRecord setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public RoleRecord setName(String name) {
    this.name = name;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public RoleRecord setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public RoleRecord setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public RoleRecord setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public RoleRecord setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }
}
