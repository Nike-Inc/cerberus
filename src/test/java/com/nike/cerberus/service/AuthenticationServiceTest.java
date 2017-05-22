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
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.cerberus.server.config.CmsConfig;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.vault.client.VaultAdminClient;
import com.nike.vault.client.model.VaultAuthResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
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

    @Before
    public void setup() {
        initMocks(this);
        objectMapper = CmsConfig.configureObjectMapper();
        authenticationService = new AuthenticationService(safeDepositBoxDao,
                awsIamRoleDao, authConnector, kmsService, kmsClientFactory,
                vaultAdminClient, vaultPolicyService, objectMapper, "foo",
                dateTimeSupplier, awsIamRoleArnParser);
    }

    @Test
    public void tests_that_generateCommonVaultPrincipalAuthMetadata_contains_expected_fields() {

        String principalArn = "principal arn";
        String region = "region";

        Map<String, String> result = authenticationService.generateCommonVaultPrincipalAuthMetadata(principalArn, region);

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_USERNAME));
        assertEquals(principalArn, result.get(VaultAuthPrincipal.METADATA_KEY_USERNAME));

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_AWS_REGION));
        assertEquals(region, result.get(VaultAuthPrincipal.METADATA_KEY_AWS_REGION));

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_GROUPS));

        assertTrue(result.containsKey(VaultAuthPrincipal.METADATA_KEY_IS_ADMIN));
    }

    @Test
    public void tests_that_getKeyId_only_validates_kms_policy_one_time_within_interval() {

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
}