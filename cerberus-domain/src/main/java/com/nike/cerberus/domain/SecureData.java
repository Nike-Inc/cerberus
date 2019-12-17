/*
 * Copyright (c) 2018 Nike, Inc.
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

import java.time.OffsetDateTime;

public class SecureData {

  private String id;
  private String sdboxId;
  private String path;
  private String data;
  private String createdBy;
  private OffsetDateTime createdTs;
  private String lastUpdatedBy;
  private OffsetDateTime lastUpdatedTs;

  public String getId() {
    return id;
  }

  public SecureData setId(String id) {
    this.id = id;
    return this;
  }

  public String getSdboxId() {
    return sdboxId;
  }

  public SecureData setSdboxId(String sdboxId) {
    this.sdboxId = sdboxId;
    return this;
  }

  public String getPath() {
    return path;
  }

  public SecureData setPath(String path) {
    this.path = path;
    return this;
  }

  public String getData() {
    return data;
  }

  public SecureData setData(String data) {
    this.data = data;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public SecureData setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public SecureData setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public String getLastUpdatedBy() {
    return lastUpdatedBy;
  }

  public SecureData setLastUpdatedBy(String lastUpdatedBy) {
    this.lastUpdatedBy = lastUpdatedBy;
    return this;
  }

  public OffsetDateTime getLastUpdatedTs() {
    return lastUpdatedTs;
  }

  public SecureData setLastUpdatedTs(OffsetDateTime lastUpdatedTs) {
    this.lastUpdatedTs = lastUpdatedTs;
    return this;
  }
}
