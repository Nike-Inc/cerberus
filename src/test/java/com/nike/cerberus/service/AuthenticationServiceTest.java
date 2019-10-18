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
import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.domain.MfaCheckRequest;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.server.config.CmsConfig;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
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

import static com.nike.cerberus.util.AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE;
import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
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

    private ObjectMapper objectMapper;

    @Mock
    private DateTimeSupplier dateTimeSupplier;

    @Mock
    private AwsIamRoleArnParser awsIamRoleArnParser;

    private Slugger slugger = new Slugger();

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AwsIamRoleService awsIamRoleService;

    private AuthenticationService authenticationService;

    private static int MAX_LIMIT = 2;

    @Before
    public void setup() {
        initMocks(this);
        objectMapper = CmsConfig.configureObjectMapper();
        authenticationService = new AuthenticationService(
                awsIamRoleDao,
                authConnector,
                kmsService,
                kmsClientFactory,
                objectMapper,
                "foo",
                MAX_LIMIT,
                dateTimeSupplier,
                awsIamRoleArnParser,
                authTokenService,
                "1h",
                "1h",
                awsIamRoleService,
                false,
                null
        );
    }

    @Test
    public void triggerChallengeSuccess() {

        String stateToken = "state token";

        MfaCheckRequest challengeRequest = mock(MfaCheckRequest.class);

        AuthResponse expectedResponse = mock(AuthResponse.class);
        AuthData expectedData = mock(AuthData.class);
        when(expectedData.getStateToken()).thenReturn(stateToken);
        when(expectedResponse.getData()).thenReturn(expectedData);

        doAnswer(invocation -> expectedResponse).when(authConnector).triggerChallenge(any(), any());

        AuthResponse actualResponse = authenticationService.triggerChallenge(challengeRequest);

        assertEquals(expectedResponse, actualResponse);
        assertEquals(expectedResponse.getData().getStateToken(), actualResponse.getData().getStateToken());
    };

    @Test
    public void tests_that_generateCommonVaultPrincipalAuthMetadata_contains_expected_fields() {

        String principalArn = "principal arn";
        String region = "region";

        Map<String, String> result = authenticationService.generateCommonIamPrincipalAuthMetadata(principalArn, region);

        assertTrue(result.containsKey(CerberusPrincipal.METADATA_KEY_USERNAME));
        assertEquals(principalArn, result.get(CerberusPrincipal.METADATA_KEY_USERNAME));

        assertTrue(result.containsKey(CerberusPrincipal.METADATA_KEY_AWS_REGION));
        assertEquals(region, result.get(CerberusPrincipal.METADATA_KEY_AWS_REGION));

        assertTrue(result.containsKey(CerberusPrincipal.METADATA_KEY_GROUPS));

        assertTrue(result.containsKey(CerberusPrincipal.METADATA_KEY_IS_ADMIN));
    }

    @Test
    public void tests_that_generateCommonIamPrincipalAuthMetadata_checks_base_iam_role_arn_of_assumed_role_arn() {
        authenticationService.adminRoleArns = "arn:aws:iam::0000000000:role/admin";

        String principalArn = "arn:aws:sts::0000000000:assumed-role/admin/role-session";
        String iamRoleArn = "arn:aws:iam::0000000000:role/admin";
        when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(principalArn)).thenReturn(iamRoleArn);
        when(awsIamRoleArnParser.isAssumedRoleArn(principalArn)).thenReturn(true);

        Map<String, String> result = authenticationService.generateCommonIamPrincipalAuthMetadata(principalArn);

        assertTrue(result.containsKey(CerberusPrincipal.METADATA_KEY_IS_ADMIN));
        assertEquals("true", result.get(CerberusPrincipal.METADATA_KEY_IS_ADMIN));
    }

    @Test
    public void tests_that_generateCommonIamPrincipalAuthMetadata_checks_assumed_role_arn() {
        authenticationService.adminRoleArns = "arn:aws:sts::0000000000:assumed-role/admin/role-session";

        String principalArn = "arn:aws:sts::0000000000:assumed-role/admin/role-session";
        String iamRoleArn = "arn:aws:iam::0000000000:role/admin";
        when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(principalArn)).thenReturn(iamRoleArn);
        when(awsIamRoleArnParser.isAssumedRoleArn(principalArn)).thenReturn(true);

        Map<String, String> result = authenticationService.generateCommonIamPrincipalAuthMetadata(principalArn);

        assertTrue(result.containsKey(CerberusPrincipal.METADATA_KEY_IS_ADMIN));
        assertEquals("true", result.get(CerberusPrincipal.METADATA_KEY_IS_ADMIN));
    }

    @Test
    public void tests_that_generateCommonIamPrincipalAuthMetadata_checks_role_arn() {
        authenticationService.adminRoleArns = "arn:aws:iam::0000000000:role/admin";

        String iamRoleArn = "arn:aws:iam::0000000000:role/admin";

        Map<String, String> result = authenticationService.generateCommonIamPrincipalAuthMetadata(iamRoleArn);

        assertTrue(result.containsKey(CerberusPrincipal.METADATA_KEY_IS_ADMIN));
        assertEquals("true", result.get(CerberusPrincipal.METADATA_KEY_IS_ADMIN));
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

        String result = authenticationService.getKmsKeyRecordForIamPrincipal(awsIamRoleRecord, region).getAwsKmsKeyId();

        // verify validate is called once interval has passed
        assertEquals(cmkId, result);
        verify(kmsService, times(1)).validateKeyAndPolicy(awsIamRoleKmsKeyRecord, principalArn);
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
        String rootArn = String.format("arn:aws:iam::%s:root", accountId);

        when(awsIamRoleDao.getIamRole(principalArn)).thenReturn(Optional.empty());
        when(awsIamRoleDao.getIamRole(roleArn)).thenReturn(Optional.empty());
        when(awsIamRoleDao.getIamRole(rootArn)).thenReturn(Optional.empty());

        when(awsIamRoleArnParser.isRoleArn(principalArn)).thenReturn(false);
        when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(principalArn)).thenReturn(roleArn);
        when(awsIamRoleArnParser.convertPrincipalArnToRootArn(roleArn)).thenReturn(rootArn);

        Optional<AwsIamRoleRecord> result = authenticationService.findIamRoleAssociatedWithSdb(principalArn);

        assertFalse(result.isPresent());
    }

    @Test
    public void test_that_findIamRoleAssociatedWithSdb_returns_generic_role_when_iam_principal_not_found_and_root_found() {

        String accountId = "0000000000";
        String roleName = "role/path";
        String principalArn = String.format("arn:aws:iam::%s:instance-profile/%s", accountId, roleName);
        String roleArn = String.format(AWS_IAM_ROLE_ARN_TEMPLATE, accountId, roleName);
        String rootArn = String.format("arn:aws:iam::%s:root", accountId);

        AwsIamRoleRecord rootRecord = mock(AwsIamRoleRecord.class);
        AwsIamRoleRecord roleRecord = mock(AwsIamRoleRecord.class);
        when(awsIamRoleDao.getIamRole(principalArn)).thenReturn(Optional.empty());
        when(awsIamRoleDao.getIamRole(roleArn)).thenReturn(Optional.empty());
        when(awsIamRoleDao.getIamRole(rootArn)).thenReturn(Optional.of(rootRecord));

        when(awsIamRoleArnParser.isRoleArn(principalArn)).thenReturn(false);
        when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(principalArn)).thenReturn(roleArn);
        when(awsIamRoleArnParser.convertPrincipalArnToRootArn(roleArn)).thenReturn(rootArn);

        when(awsIamRoleService.createIamRole(roleArn)).thenReturn(roleRecord);

        Optional<AwsIamRoleRecord> result = authenticationService.findIamRoleAssociatedWithSdb(principalArn);

        assertEquals(roleRecord, result.get());
    }

    @Test
    public void tests_that_validateAuthPayloadSizeAndTruncateIfLargerThanMaxKmsSupportedSize_returns_the_original_payload_if_the_size_can_be_encrypted_by_kms() throws JsonProcessingException {
        AuthTokenResponse response = new AuthTokenResponse()
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

        AuthTokenResponse response = new AuthTokenResponse()
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
        CerberusPrincipal principal = mock(CerberusPrincipal.class);

        when(principal.getPrincipalType()).thenReturn(PrincipalType.IAM);

        Exception e = null;
        try {
            authenticationService.refreshUserToken(principal);
        } catch (Exception e2) {
            e = e2;
        }

        assertTrue(e instanceof ApiException);
        assertTrue(((ApiException) e).getApiErrors().contains(DefaultApiError.USER_ONLY_RESOURCE));
    }

    @Test
    public void tests_that_refreshUserToken_refreshes_token_when_count_is_less_than_limit() {
        Integer curCount = MAX_LIMIT - 1;

        CerberusAuthToken authToken = CerberusAuthToken.Builder.create()
                .withPrincipalType(PrincipalType.USER)
                .withPrincipal("principal")
                .withGroups("group1,group2")
                .withRefreshCount(curCount)
                .withToken(UUID.randomUUID().toString())
                .build();

        CerberusPrincipal principal = new CerberusPrincipal(authToken);

        OffsetDateTime now = OffsetDateTime.now();
        when(authTokenService.generateToken(
                anyString(),
                any(PrincipalType.class),
                anyBoolean(),
                anyString(),
                anyInt(),
                anyInt())
        ).thenReturn(
                CerberusAuthToken.Builder.create()
                        .withPrincipalType(PrincipalType.USER)
                        .withPrincipal("principal")
                        .withGroups("group1,group2")
                        .withRefreshCount(curCount + 1)
                        .withToken(UUID.randomUUID().toString())
                        .withCreated(now)
                        .withExpires(now.plusHours(1))
                        .build()
        );

        AuthResponse response = authenticationService.refreshUserToken(principal);
        assertEquals(curCount + 1, Integer.parseInt(response.getData().getClientToken().getMetadata().get(CerberusPrincipal.METADATA_KEY_TOKEN_REFRESH_COUNT)));
    }

    @Test
    public void tests_that_refreshUserToken_throws_access_denied_token_when_count_is_eq_or_greater_than_limit() {
        CerberusPrincipal principal = mock(CerberusPrincipal.class);

        when(principal.getPrincipalType()).thenReturn(PrincipalType.USER);
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
