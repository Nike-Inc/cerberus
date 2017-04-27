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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.vault.client.VaultAdminClient;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

    @Mock
    private ObjectMapper objectMapper;

//    @Named(ADMIN_GROUP_PROPERTY) final String adminGroup,

    @Mock
    private DateTimeSupplier dateTimeSupplier;

    @Mock
    private AwsIamRoleArnParser awsIamRoleArnParser;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Before
    public void setup() {

        initMocks(this);
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
        String kmsId = "kms id";
        String keyId = "key id";

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
        awsIamRoleKmsKeyRecord.setId(kmsId);
        awsIamRoleKmsKeyRecord.setAwsKmsKeyId(keyId);
        awsIamRoleKmsKeyRecord.setLastValidatedTs(dateTime);

        when(awsIamRoleDao.getKmsKey(iamRoleId, region)).thenReturn(Optional.of(awsIamRoleKmsKeyRecord));

        when(dateTimeSupplier.get()).thenReturn(now);

        authenticationService.getKeyId(iamPrincipalCredentials);

        // verify validate is called once interval has passed
        verify(kmsService, times(1)).validatePolicy(keyId, principalArn, region);

        // reset interval
        awsIamRoleKmsKeyRecord.setLastValidatedTs(now);

        // verify validate is not called when interval has not passed
        authenticationService.getKeyId(iamPrincipalCredentials);
        authenticationService.getKeyId(iamPrincipalCredentials);
        verify(kmsService, times(1)).validatePolicy(keyId, principalArn, region);
    }

}