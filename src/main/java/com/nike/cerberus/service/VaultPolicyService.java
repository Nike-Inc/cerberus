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

package com.nike.cerberus.service;

import com.google.common.base.Preconditions;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.RoleRecord;
import com.nike.cerberus.util.Slugger;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.model.VaultPolicy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service that encapsulates various tasks with regards to policies in Vault.
 */
@Singleton
@ParametersAreNonnullByDefault
public class VaultPolicyService {

    public static final String OWNER_CAPABILITIES = "\"create\", \"read\", \"update\", \"delete\", \"list\"";

    public static final String WRITE_CAPABILITIES = "\"create\", \"read\", \"update\", \"delete\", \"list\"";

    public static final String READ_CAPABILITIES = "\"read\", \"list\"";

    private static final String RULES_TEMPLATE = "path \"secret/%s*\" { capabilities = [%s] }";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final VaultAdminClient vaultAdminClient;

    private final Slugger slugger;

    @Inject
    public VaultPolicyService(final VaultAdminClient vaultAdminClient, final Slugger slugger) {
        this.vaultAdminClient = vaultAdminClient;
        this.slugger = slugger;
    }

    /**
     * Based on a safe deposit box, generates the expected set of policies a token may be assigned.
     */
    public void createStandardPolicies(final String safeDepositBoxName, final String safeDepositBoxPath) {
        final VaultPolicy ownerPolicy = buildOwnerPolicy(safeDepositBoxPath);
        final VaultPolicy writePolicy = buildWritePolicy(safeDepositBoxPath);
        final VaultPolicy readPolicy = buildReadPolicy(safeDepositBoxPath);

        try {
            vaultAdminClient.putPolicy(buildPolicyName(safeDepositBoxName, RoleRecord.ROLE_OWNER), ownerPolicy);
            vaultAdminClient.putPolicy(buildPolicyName(safeDepositBoxName, RoleRecord.ROLE_WRITE), writePolicy);
            vaultAdminClient.putPolicy(buildPolicyName(safeDepositBoxName, RoleRecord.ROLE_READ), readPolicy);
        } catch (VaultClientException vce) {
            logger.error("Failed to write policies to Vault.", vce);
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .build();
        }
    }

    public void deleteStandardPolicies(final String safeDepositBoxName) {
        try {
            vaultAdminClient.deletePolicy(buildPolicyName(safeDepositBoxName, RoleRecord.ROLE_OWNER));
            vaultAdminClient.deletePolicy(buildPolicyName(safeDepositBoxName, RoleRecord.ROLE_WRITE));
            vaultAdminClient.deletePolicy(buildPolicyName(safeDepositBoxName, RoleRecord.ROLE_READ));
        } catch (VaultClientException vce) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(vce)
                    .withExceptionMessage("Failed to delete policies from Vault.")
                    .build();
        }
    }

    /**
     * Outputs the expected policy name format used in Vault.
     *
     * @param sdbName Safe deposit box name.
     * @param roleName Role for safe deposit box.
     * @return Formatted policy name.
     */
    public String buildPolicyName(final String sdbName, final String roleName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(sdbName), "sdbName cannot be blank!");
        Preconditions.checkArgument(StringUtils.isNotBlank(roleName), "roleName cannot be blank!");

        final StringBuilder sb = new StringBuilder();
        sb.append(slugger.toSlug(sdbName));
        sb.append('-');
        sb.append(StringUtils.lowerCase(roleName));
        return sb.toString();
    }

    /**
     * Helper method for creating Vault policy objects with properly formatted rules.
     *
     * @param sdbPath Safe deposit box path
     * @param capabilities Capabilities for the policy
     * @return Vault policy
     */
    public VaultPolicy buildPolicy(final String sdbPath, final String capabilities) {
        VaultPolicy vaultPolicy = new VaultPolicy();
        vaultPolicy.setRules(String.format(RULES_TEMPLATE, sdbPath, capabilities));
        return vaultPolicy;
    }

    /**
     * Create owner policy.
     *
     * @param sdbPath Safe deposit box path
     * @return Vault policy
     */
    public VaultPolicy buildOwnerPolicy(final String sdbPath) {
        return buildPolicy(sdbPath, OWNER_CAPABILITIES);
    }

    /**
     * Create write policy.
     *
     * @param sdbPath Safe deposit box path
     * @return Vault policy
     */
    public VaultPolicy buildWritePolicy(final String sdbPath) {
        return buildPolicy(sdbPath, WRITE_CAPABILITIES);
    }

    /**
     * Create read policy.
     *
     * @param sdbPath Safe deposit box path
     * @return Vault policy
     */
    public VaultPolicy buildReadPolicy(final String sdbPath) {
        return buildPolicy(sdbPath, READ_CAPABILITIES);
    }
}
