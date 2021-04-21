/*
 * Copyright (c) 2021 Nike, Inc.
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

import com.nike.cerberus.domain.AuthTokenInfo;
import java.time.OffsetDateTime;
import lombok.Getter;

public class CerberusJwtClaims implements AuthTokenInfo {

  @Getter private String id;
  @Getter private OffsetDateTime createdTs;
  @Getter private OffsetDateTime expiresTs;
  @Getter private String principal;
  @Getter private String principalType;
  @Getter private Boolean isAdmin;
  @Getter private String groups;
  @Getter private Integer refreshCount;

  public CerberusJwtClaims setId(String id) {
    this.id = id;
    return this;
  }

  public CerberusJwtClaims setCreatedTs(OffsetDateTime createdTs) {
    this.createdTs = createdTs;
    return this;
  }

  public CerberusJwtClaims setExpiresTs(OffsetDateTime expiresTs) {
    this.expiresTs = expiresTs;
    return this;
  }

  public CerberusJwtClaims setPrincipal(String principal) {
    this.principal = principal;
    return this;
  }

  public CerberusJwtClaims setPrincipalType(String principalType) {
    this.principalType = principalType;
    return this;
  }

  @Override
  public String getTokenHash() {
    return null;
  }

  @Override
  public CerberusJwtClaims setTokenHash(String tokenHash) {
    return null;
  }

  public CerberusJwtClaims setIsAdmin(Boolean admin) {
    isAdmin = admin;
    return this;
  }

  public CerberusJwtClaims setGroups(String groups) {
    this.groups = groups;
    return this;
  }

  public CerberusJwtClaims setRefreshCount(Integer refreshCount) {
    this.refreshCount = refreshCount;
    return this;
  }
}
