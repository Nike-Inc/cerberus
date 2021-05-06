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

package com.nike.cerberus.security;

import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.domain.CerberusAuthToken;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Represents the authenticated principal. This contains the client token entity and any assigned
 * roles based on that.
 */
public class CerberusPrincipal implements Authentication {

  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  public static final String ROLE_USER = "ROLE_USER";

  public static final String METADATA_KEY_IS_ADMIN = "is_admin";

  public static final String METADATA_KEY_GROUPS = "groups";

  public static final String METADATA_KEY_USERNAME = "username";

  public static final String METADATA_KEY_AWS_ACCOUNT_ID = "aws_account_id";

  public static final String METADATA_KEY_AWS_IAM_ROLE_NAME = "aws_iam_role_name";

  public static final String METADATA_KEY_AWS_IAM_PRINCIPAL_ARN = "aws_iam_principal_arn";

  public static final String METADATA_KEY_IS_IAM_PRINCIPAL = "is_iam_principal";

  public static final String METADATA_KEY_AWS_REGION = "aws_region";

  public static final String METADATA_KEY_TOKEN_REFRESH_COUNT = "refresh_count";

  public static final String METADATA_KEY_MAX_TOKEN_REFRESH_COUNT = "max_refresh_count";

  private boolean authenticated = false;

  private final CerberusAuthToken cerberusAuthToken;
  private final Set<GrantedAuthority> grantedAuthorities;

  public CerberusPrincipal(CerberusAuthToken cerberusAuthToken) {
    this.cerberusAuthToken = cerberusAuthToken;
    grantedAuthorities = buildGrantedAuthorities(cerberusAuthToken);
  }

  private Set<GrantedAuthority> buildGrantedAuthorities(CerberusAuthToken cerberusAuthToken) {
    final ImmutableSet.Builder<GrantedAuthority> roleSetBuilder = ImmutableSet.builder();

    if (cerberusAuthToken.isAdmin()) {
      roleSetBuilder.add(new SimpleGrantedAuthority(ROLE_ADMIN));
    }

    roleSetBuilder.add(new SimpleGrantedAuthority(ROLE_USER));

    return roleSetBuilder.build();
  }

  @Override
  public String getName() {
    return cerberusAuthToken.getPrincipal();
  }

  public String getToken() {
    return cerberusAuthToken.getToken();
  }

  public String getTokenId() {
    return cerberusAuthToken.getId();
  }

  public Set<String> getUserGroups() {
    if (cerberusAuthToken.getGroups() == null) {
      return new HashSet<>();
    }
    return new HashSet<>(Arrays.asList(cerberusAuthToken.getGroups().split(",")));
  }

  public PrincipalType getPrincipalType() {
    return cerberusAuthToken.getPrincipalType();
  }

  public Integer getTokenRefreshCount() {
    return cerberusAuthToken.getRefreshCount();
  }

  public OffsetDateTime getTokenCreated() {
    return cerberusAuthToken.getCreated();
  }

  public OffsetDateTime getTokenExpires() {
    return cerberusAuthToken.getExpires();
  }

  public boolean isAdmin() {
    return cerberusAuthToken.isAdmin();
  }

  @Override
  public String toString() {
    return String.format(
        "[ Name: %s, Type: %s, Token Created: %s, Token Expires: %s, isAdmin: %s ]",
        cerberusAuthToken.getPrincipal(),
        cerberusAuthToken.getPrincipalType().getName(),
        cerberusAuthToken
            .getCreated()
            .format(DateTimeFormatter.ofPattern("MMM d yyyy, hh:mm:ss a Z")),
        cerberusAuthToken
            .getExpires()
            .format(DateTimeFormatter.ofPattern("MMM d yyyy, hh:mm:ss a Z")),
        cerberusAuthToken.isAdmin());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return grantedAuthorities;
  }

  @Override
  public Object getCredentials() {
    return cerberusAuthToken.getToken();
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return cerberusAuthToken;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean authenticated) {
    this.authenticated = authenticated;
  }
}
