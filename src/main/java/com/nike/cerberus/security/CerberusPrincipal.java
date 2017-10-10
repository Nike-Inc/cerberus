/*
 * Copyright (c) 2016 Nike, Inc.
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

package com.nike.cerberus.security;

import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.domain.CerberusAuthToken;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the authenticated principal.  This contains the vault client token entity and any assigned roles based
 * on that.
 */
public class CerberusPrincipal implements Principal {

    public static final String ROLE_ADMIN = "admin";

    public static final String ROLE_USER = "user";

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

    private final CerberusAuthToken cerberusAuthToken;

    private final Set<String> roles;

    public CerberusPrincipal(CerberusAuthToken cerberusAuthToken) {
        this.cerberusAuthToken = cerberusAuthToken;
        roles = buildRoles(cerberusAuthToken);
    }

    private Set<String> buildRoles(CerberusAuthToken cerberusAuthToken) {
        final ImmutableSet.Builder<String> roleSetBuilder = ImmutableSet.builder();

        if (cerberusAuthToken.isAdmin()) {
            roleSetBuilder.add(ROLE_ADMIN);
        }

        roleSetBuilder.add(ROLE_USER);

        return roleSetBuilder.build();
    }

    @Override
    public String getName() {
        return cerberusAuthToken.getPrincipal();
    }

    public String getToken() {
        return cerberusAuthToken.getToken();
    }

    public boolean hasRole(final String role) {
        return roles.contains(role);
    }

    public Set<String> getUserGroups() {
        return new HashSet<>(Arrays.asList(cerberusAuthToken.getGroups().split(",")));
    }

    public PrincipalType getPrincipalType() {
        return cerberusAuthToken.getPrincipalType();
    }

    public Integer getTokenRefreshCount() {
        return cerberusAuthToken.getRefreshCount();
    }
}
