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
 *
 */

package com.nike.cerberus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.record.SafeDepositBoxRoleRecord;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.cerberus.server.config.CmsConfig;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultAuthResponse;
import com.nike.vault.client.model.VaultClientTokenResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.nike.cerberus.service.AuthenticationService.LOOKUP_SELF_POLICY;
import static com.nike.cerberus.util.AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE;
import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests the AuthenticationService class
 */
public class AuthenticationServiceTest {

    @Mock
    private SafeDepositBoxDao safeDepositBoxDao;

    @Mock
    private AwsIamRoleDao awsIamRoleDao;

    @Mock
    private AuthConnector authConnector;

    @Mock
    private KmsService kmsService;

    @Mock
    private KmsClientFactory kmsClientFactory;

    @Mock
    private VaultAdminClient vaultAdminClient;

    @Mock
    private VaultPolicyService vaultPolicyService;

    private ObjectMapper objectMapper;

    @Mock
    private DateTimeSupplier dateTimeSupplier;

    @Mock
    private AwsIamRoleArnParser awsIamRoleArnParser;

    private AuthenticationService authenticationService;

    private static int MAX_LIMIT = 2;

    @Before
    public void setup() {
        initMocks(this);
        objectMapper = CmsConfig.configureObjectMapper();
        authenticationService = new AuthenticationService(safeDepositBoxDao,
                awsIamRoleDao, authConnector, kmsService, kmsClientFactory,
                vaultAdminClient, vaultPolicyService, objectMapper, "foo", MAX_LIMIT,
                dateTimeSupplier, awsIamRoleArnParser);
    }

    @Test
    public void tests_that_generateCommonVaultPrincipalAuthMetadata_contains_expected_fields() {

        String principalArn = "principal arn";
        String region = "region";

        Map<String, String> result = authenticationService.generateCommonIamPrincipalAuthMetadata(principalArn, region);

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_USERNAME));
        assertEquals(principalArn, result.get(VaultAuthPrincipal.METADATA_KEY_USERNAME));

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_AWS_REGION));
        assertEquals(region, result.get(VaultAuthPrincipal.METADATA_KEY_AWS_REGION));

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_GROUPS));

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_IS_ADMIN));
    }

    @Test
    public void test_that_getKeyId_only_validates_kms_policy_one_time_within_interval() {

        String principalArn = "principal arn";
        String region = "region";
        String iamRoleId = "iam role id";
        String kmsKeyId = "kms id";
        String cmkId = "key id";

        // ensure that validate interval is passed
        OffsetDateTime dateTime = OffsetDateTime.of(2016, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.now();

        IamPrincipalCredentials iamPrincipalCredentials = new IamPrincipalCredentials();
        iamPrincipalCredentials.setIamPrincipalArn(principalArn);
        iamPrincipalCredentials.setRegion(region);

        AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord().setAwsIamRoleArn(principalArn);
        awsIamRoleRecord.setAwsIamRoleArn(principalArn);
        awsIamRoleRecord.setId(iamRoleId);
        when(awsIamRoleDao.getIamRole(principalArn)).thenReturn(Optional.of(awsIamRoleRecord));

        AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = new AwsIamRoleKmsKeyRecord();
        awsIamRoleKmsKeyRecord.setId(kmsKeyId);
        awsIamRoleKmsKeyRecord.setAwsKmsKeyId(cmkId);
        awsIamRoleKmsKeyRecord.setLastValidatedTs(dateTime);

        when(awsIamRoleDao.getKmsKey(iamRoleId, region)).thenReturn(Optional.of(awsIamRoleKmsKeyRecord));

        when(dateTimeSupplier.get()).thenReturn(now);

        String result = authenticationService.getKeyId(iamPrincipalCredentials);

        // verify validate is called once interval has passed
        assertEquals(cmkId, result);
        verify(kmsService, times(1)).validatePolicy(awsIamRoleKmsKeyRecord, principalArn);
    }

    @Test
    public void test_that_buildCompleteSetOfPolicies_returns_all_policies() {

        String accountId = "0000000000";
        String roleName = "role/path";
        String principalArn = String.format("arn:aws:iam::%s:instance-profile/%s", accountId, roleName);

        String roleArn = String.format(AWS_IAM_ROLE_ARN_TEMPLATE, accountId, roleName);
        when(awsIamRoleArnParser.isRoleArn(principalArn)).thenReturn(false);
        when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(principalArn)).thenReturn(roleArn);

        String principalPolicy1 = "principal policy 1";
        String principalPolicy2 = "principal policy 2";
        String principalArnSdb1 = "principal arn sdb 1";
        String principalArnSdb2 = "principal arn sdb 2";
        SafeDepositBoxRoleRecord principalArnRecord1 = new SafeDepositBoxRoleRecord().setRoleName(roleName).setSafeDepositBoxName(principalArnSdb1);
        SafeDepositBoxRoleRecord principalArnRecord2 = new SafeDepositBoxRoleRecord().setRoleName(roleName).setSafeDepositBoxName(principalArnSdb2);
        List<SafeDepositBoxRoleRecord> principalArnRecords = Lists.newArrayList(principalArnRecord1, principalArnRecord2);
        when(safeDepositBoxDao.getIamRoleAssociatedSafeDepositBoxRoles(principalArn)).thenReturn(principalArnRecords);
        when(vaultPolicyService.buildPolicyName(principalArnSdb1, roleName)).thenReturn(principalPolicy1);
        when(vaultPolicyService.buildPolicyName(principalArnSdb2, roleName)).thenReturn(principalPolicy2);

        String rolePolicy = "role policy";
        String roleArnSdb = "role arn sdb";
        SafeDepositBoxRoleRecord roleArnRecord = new SafeDepositBoxRoleRecord().setRoleName(roleName).setSafeDepositBoxName(roleArnSdb);
        List<SafeDepositBoxRoleRecord> roleArnRecords = Lists.newArrayList(roleArnRecord);
        when(safeDepositBoxDao.getIamRoleAssociatedSafeDepositBoxRoles(roleArn)).thenReturn(roleArnRecords);
        when(vaultPolicyService.buildPolicyName(roleArnSdb, roleName)).thenReturn(rolePolicy);

        List<String> expectedPolicies = Lists.newArrayList(principalPolicy1, principalPolicy2, rolePolicy, LOOKUP_SELF_POLICY);
        Set<String> expected = Sets.newHashSet(expectedPolicies);
        Set<String> result = authenticationService.buildCompleteSetOfPolicies(principalArn);

        assertEquals(expected, result);
    }

    @Test
    public void test_that_findIamRoleAssociatedWithSdb_returns_first_matching_iam_role_record_if_found() {

        String principalArn = "principal arn";
        AwsIamRoleRecord awsIamRoleRecord = mock(AwsIamRoleRecord.class);
        when(awsIamRoleDao.getIamRole(principalArn)).thenReturn(Optional.of(awsIamRoleRecord));

        Optional<AwsIamRoleRecord> result = authenticationService.findIamRoleAssociatedWithSdb(principalArn);

        assertEquals(awsIamRoleRecord, result.get());
    }

    @Test
    public void test_that_findIamRoleAssociatedWithSdb_returns_generic_role_when_iam_principal_not_found() {

        String accountId = "0000000000";
        String roleName = "role/path";
        String principalArn = String.format("arn:aws:iam::%s:instance-profile/%s", accountId, roleName);
        String roleArn = String.format(AWS_IAM_ROLE_ARN_TEMPLATE, accountId, roleName);

        AwsIamRoleRecord awsIamRoleRecord = mock(AwsIamRoleRecord.class);
        when(awsIamRoleDao.getIamRole(principalArn)).thenReturn(Optional.empty());
        when(awsIamRoleDao.getIamRole(roleArn)).thenReturn(Optional.of(awsIamRoleRecord));

        when(awsIamRoleArnParser.isRoleArn(principalArn)).thenReturn(false);
        when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(principalArn)).thenReturn(roleArn);

        Optional<AwsIamRoleRecord> result = authenticationService.findIamRoleAssociatedWithSdb(principalArn);

        assertEquals(awsIamRoleRecord, result.get());
    }

    @Test
    public void test_that_findIamRoleAssociatedWithSdb_returns_empty_optional_when_roles_not_found() {

        String accountId = "0000000000";
        String roleName = "role/path";
        String principalArn = String.format("arn:aws:iam::%s:instance-profile/%s", accountId, roleName);
        String roleArn = String.format("arn:aws:iam::%s:role/%s", accountId, roleName);

        when(awsIamRoleDao.getIamRole(principalArn)).thenReturn(Optional.empty());
        when(awsIamRoleDao.getIamRole(roleArn)).thenReturn(Optional.empty());

        when(awsIamRoleArnParser.isRoleArn(principalArn)).thenReturn(false);
        when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(principalArn)).thenReturn(roleArn);

        Optional<AwsIamRoleRecord> result = authenticationService.findIamRoleAssociatedWithSdb(principalArn);

        assertFalse(result.isPresent());
    }

    @Test
    public void tests_that_validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize_returns_the_original_payload_if_the_size_can_be_encrypted_by_kms() throws JsonProcessingException {
        VaultAuthResponse response = new VaultAuthResponse()
                .setClientToken(UUID.randomUUID().toString())
                .setLeaseDuration(3600)
                .setMetadata(new HashMap<>())
                .setPolicies(new HashSet<>())
                .setRenewable(false);

        byte[] serializedAuth = new ObjectMapper().writeValueAsBytes(response);

        byte[] actual = authenticationService.validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize(serializedAuth, response, "foo");

        assertEquals(serializedAuth, actual);
    }

    @Test
    public void tests_that_validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize_returns_a_truncated_payload_if_the_size_cannot_be_encrypted_by_kms() throws JsonProcessingException {
        Map<String, String> meta = new HashMap<>();
        Set<String> policies = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            policies.add(RandomStringUtils.random(25));
        }

        VaultAuthResponse response = new VaultAuthResponse()
                .setClientToken(UUID.randomUUID().toString())
                .setLeaseDuration(3600)
                .setMetadata(meta)
                .setPolicies(policies)
                .setRenewable(false);

        byte[] serializedAuth = new ObjectMapper().writeValueAsBytes(response);
        assertTrue(serializedAuth.length > AuthenticationService.KMS_SIZE_LIMIT);

        byte[] actual = authenticationService.validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize(serializedAuth, response, "foo");

        assertNotEquals(serializedAuth, actual);
        assertTrue(actual.length < AuthenticationService.KMS_SIZE_LIMIT);
    }

    @Test
    public void tests_that_refreshUserToken_throws_access_denied_when_an_iam_principal_tries_to_call_it() {
        VaultAuthPrincipal principal = mock(VaultAuthPrincipal.class);

        when(principal.isIamPrincipal()).thenReturn(true);

        Exception e = null;
        try {
            authenticationService.refreshUserToken(principal);
        } catch (Exception e2) {
            e = e2;
        }

        assertTrue(e instanceof ApiException);
        assertTrue(((ApiException) e).getApiErrors().contains(DefaultApiError.IAM_PRINCIPALS_CANNOT_USE_USER_ONLY_RESOURCE));
    }

    @Test
    public void tests_that_refreshUserToken_refreshes_token_when_count_is_less_than_limit() {
        VaultAuthPrincipal principal = mock(VaultAuthPrincipal.class);

        when(principal.isIamPrincipal()).thenReturn(false);
        when(principal.getTokenRefreshCount()).thenReturn(MAX_LIMIT - 1);

        VaultClientTokenResponse response = mock(VaultClientTokenResponse.class);

        when(principal.getClientToken()).thenReturn(response);

        when(response.getId()).thenReturn("");

        authenticationService.refreshUserToken(principal);
    }

    @Test
    public void tests_that_refreshUserToken_throws_access_denied_token_when_count_is_eq_or_greater_than_limit() {
        VaultAuthPrincipal principal = mock(VaultAuthPrincipal.class);

        when(principal.isIamPrincipal()).thenReturn(false);
        when(principal.getTokenRefreshCount()).thenReturn(MAX_LIMIT);

        Exception e = null;
        try {
            authenticationService.refreshUserToken(principal);
        } catch (Exception e2) {
            e = e2;
        }

        assertTrue(e instanceof ApiException);
        assertTrue(((ApiException) e).getApiErrors().contains(DefaultApiError.MAXIMUM_TOKEN_REFRESH_COUNT_REACHED));
    }
}