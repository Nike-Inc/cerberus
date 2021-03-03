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

package com.nike.cerberus.jwt;

import java.time.OffsetDateTime;

public class CerberusJwtClaims {

  private String id;

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

  public CerberusJwtClaims setId(String id) {
    this.id = id;
    return this;
  }

  public OffsetDateTime getCreatedTs() {
    return createdTs;
  }

  public CerberusJwtClaims setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public OffsetDateTime getExpiresTs() {
    return expiresTs;
  }

  public CerberusJwtClaims setExpiresTs(OffsetDateTime expiresTs) {
    this.expiresTs = expiresTs;
    return this;
  }

  public String getPrincipal() {
    return principal;
  }

  public CerberusJwtClaims setPrincipal(String principal) {
    this.principal = principal;
    return this;
  }

  public String getPrincipalType() {
    return principalType;
  }

  public CerberusJwtClaims setPrincipalType(String principalType) {
    this.principalType = principalType;
    return this;
  }

  public Boolean getIsAdmin() {
    return isAdmin;
  }

  public CerberusJwtClaims setIsAdmin(Boolean admin) {
    isAdmin = admin;
    return this;
  }

  public String getGroups() {
    return groups;
  }

  public CerberusJwtClaims setGroups(String groups) {
    this.groups = groups;
    return this;
  }

  public Integer getRefreshCount() {
    return refreshCount;
  }

  public CerberusJwtClaims setRefreshCount(Integer refreshCount) {
    this.refreshCount = refreshCount;
    return this;
  }
}
