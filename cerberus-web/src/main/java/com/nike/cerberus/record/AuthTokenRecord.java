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

package com.nike.cerberus.record;

import java.time.OffsetDateTime;

public class AuthTokenRecord {

  private String id;

  private String tokenHash;

  private OffsetDateTime createdTs;

  private OffsetDateTime expiresTs;

  private String principal;

  private String principalType;

  private Boolean isAdmin;

  private String groups;

  private Integer refreshCount;

  public String getId() {
    return id;
  }

  public AuthTokenRecord setId(String id) {
    this.id = id;
    return this;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public AuthTokenRecord setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public AuthTokenRecord setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getExpiresTs() {
    return expiresTs;
  }

  public AuthTokenRecord setExpiresTs(OffsetDateTime expiresTs) {
    this.expiresTs = expiresTs;
    return this;
  }

  public String getPrincipal() {
    return principal;
  }

  public AuthTokenRecord setPrincipal(String principal) {
    this.principal = principal;
    return this;
  }

  public String getPrincipalType() {
    return principalType;
  }

  public AuthTokenRecord setPrincipalType(String principalType) {
    this.principalType = principalType;
    return this;
  }

  public Boolean getIsAdmin() {
    return isAdmin;
  }

  public AuthTokenRecord setIsAdmin(Boolean admin) {
    isAdmin = admin;
    return this;
  }

  public String getGroups() {
    return groups;
  }

  public AuthTokenRecord setGroups(String groups) {
    this.groups = groups;
    return this;
  }

  public Integer getRefreshCount() {
    return refreshCount;
  }

  public AuthTokenRecord setRefreshCount(Integer refreshCount) {
    this.refreshCount = refreshCount;
    return this;
  }
}
