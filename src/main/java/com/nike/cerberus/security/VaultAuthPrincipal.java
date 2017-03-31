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
import com.nike.vault.client.model.VaultClientTokenResponse;
import org.apache.commons.lang3.StringUtils;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents the authenticated principal.  This contains the vault client token entity and any assigned roles based
 * on that.
 */
public class VaultAuthPrincipal implements Principal {

    public static final String ROLE_ADMIN = "admin";

    public static final String ROLE_USER = "user";

    public static final String METADATA_KEY_IS_ADMIN = "is_admin";

    public static final String METADATA_KEY_GROUPS = "groups";

    public static final String METADATA_KEY_USERNAME = "username";

    public static final String METADATA_KEY_AWS_REGION = "aws_region";

    private final VaultClientTokenResponse clientToken;

    private final Set<String> userGroupSet;

    private final String username;

    private final Set<String> roles;

    public VaultAuthPrincipal(VaultClientTokenResponse clientToken) {
        this.clientToken = clientToken;
        this.roles = buildRoles(clientToken);
        this.userGroupSet = extractUserGroups(clientToken);
        this.username = extractUsername(clientToken);
    }

    private Set<String> buildRoles(VaultClientTokenResponse clientToken) {
        final ImmutableSet.Builder<String> roleSetBuilder = ImmutableSet.builder();
        final Map<String, String> meta = clientToken.getMeta();

        if (meta != null && Boolean.valueOf(meta.get(METADATA_KEY_IS_ADMIN))) {
            roleSetBuilder.add(ROLE_ADMIN);
        }

        roleSetBuilder.add(ROLE_USER);

        return roleSetBuilder.build();
    }

    private Set<String> extractUserGroups(final VaultClientTokenResponse clientToken) {
        final Map<String, String> meta = clientToken.getMeta();
        final String groupString = meta == null ? "" : meta.get(METADATA_KEY_GROUPS);
        if (StringUtils.isBlank(groupString)) {
            return Collections.emptySet();
        } else {
            return ImmutableSet.copyOf(StringUtils.split(groupString, ','));
        }
    }

    private String extractUsername(final VaultClientTokenResponse clientToken) {
        final Map<String, String> meta = clientToken.getMeta();
        // if a Token that is the root token or created outside of CMS,
        // then meta might be null and there will be no username set
        return meta == null ? "unknown-user-manually-created-token" : meta.get(METADATA_KEY_USERNAME);
    }

    @Override
    public String getName() {
        return username;
    }

    public VaultClientTokenResponse getClientToken() {
        return clientToken;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(final String role) {
        return roles.contains(role);
    }

    public Set<String> getUserGroups() {
        return userGroupSet;
    }
}
