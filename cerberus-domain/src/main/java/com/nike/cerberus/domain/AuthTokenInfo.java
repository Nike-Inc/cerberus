package com.nike.cerberus.domain;

import java.time.OffsetDateTime;

public interface AuthTokenInfo {
  String getId();

  AuthTokenInfo setId(String id);

  OffsetDateTime getCreatedTs();

  AuthTokenInfo setCreatedTs(OffsetDateTime createdTs);

  OffsetDateTime getExpiresTs();

  AuthTokenInfo setExpiresTs(OffsetDateTime expiresTs);

  String getPrincipal();

  AuthTokenInfo setPrincipal(String principal);

  String getPrincipalType();

  AuthTokenInfo setPrincipalType(String principalType);

  Boolean getIsAdmin();

  AuthTokenInfo setIsAdmin(Boolean admin);

  String getGroups();

  AuthTokenInfo setGroups(String groups);

  Integer getRefreshCount();

  AuthTokenInfo setRefreshCount(Integer refreshCount);
}
