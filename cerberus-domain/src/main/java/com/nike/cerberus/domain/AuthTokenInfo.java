package com.nike.cerberus.domain;

import java.time.OffsetDateTime;

public interface AuthTokenInfo {
  String getId();

  void setId(String id);

  OffsetDateTime getCreatedTs();

  void setCreatedTs(OffsetDateTime createdTs);

  OffsetDateTime getExpiresTs();

  void setExpiresTs(OffsetDateTime expiresTs);

  String getPrincipal();

  void setPrincipal(String principal);

  String getPrincipalType();

  void setPrincipalType(String principalType);

  Boolean getIsAdmin();

  void setIsAdmin(Boolean admin);

  String getGroups();

  void setGroups(String groups);

  Integer getRefreshCount();

  void setRefreshCount(Integer refreshCount);
}
