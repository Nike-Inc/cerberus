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

import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.CreateAliasRequest;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.GetKeyPolicyRequest;
import com.amazonaws.services.kms.model.GetKeyPolicyResult;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.KeyUsageType;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.UuidSupplier;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KmsServiceV2Test {

    private AwsIamRoleDao awsIamRoleDao;
    private UuidSupplier uuidSupplier;
    private KmsClientFactory kmsClientFactory;
    private KmsPolicyServiceV2 kmsPolicyService;

    private KmsServiceV2 kmsService;

    @Before
    public void setup() {
        awsIamRoleDao = mock(AwsIamRoleDao.class);
        uuidSupplier = mock(UuidSupplier.class);
        kmsClientFactory = mock(KmsClientFactory.class);
        kmsPolicyService = mock(KmsPolicyServiceV2.class);
        kmsService = new KmsServiceV2(awsIamRoleDao, uuidSupplier, kmsClientFactory, kmsPolicyService);
    }

    @Test
    public void test_provisionKmsKey() {

        String iamRoleId = "role-id";
        String iamRoleAccountId = "account-id";
        String iamRoleName = "role-name";
        String iamRoleArn = String.format(AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_TEMPLATE, iamRoleAccountId, iamRoleName);
        String awsRegion = "aws-region";
        String user = "user";
        OffsetDateTime dateTime = OffsetDateTime.now();

        String policy = "policy";
        String arn = "arn";

        String awsIamRoleKmsKeyId = "awsIamRoleKmsKeyId";

        when(uuidSupplier.get()).thenReturn(awsIamRoleKmsKeyId);
        when(kmsPolicyService.generateStandardKmsPolicy(iamRoleArn)).thenReturn(policy);

        AWSKMSClient client = mock(AWSKMSClient.class);
        when(kmsClientFactory.getClient(awsRegion)).thenReturn(client);

        CreateKeyRequest request = new CreateKeyRequest();
        request.setKeyUsage(KeyUsageType.ENCRYPT_DECRYPT);
        request.setDescription("Key used by Cerberus for IAM role authentication.");
        request.setPolicy(policy);

        CreateKeyResult createKeyResult = mock(CreateKeyResult.class);
        KeyMetadata metadata = mock(KeyMetadata.class);

        when(createKeyResult.getKeyMetadata()).thenReturn(metadata);
        when(metadata.getArn()).thenReturn(arn);
        when(client.createKey(request)).thenReturn(createKeyResult);

        // invoke method under test
        String actualResult = kmsService.provisionKmsKey(iamRoleId, iamRoleArn, awsRegion, user, dateTime);

        assertEquals(arn, actualResult);

        CreateAliasRequest aliasRequest = new CreateAliasRequest();
        aliasRequest.setAliasName(kmsService.getAliasName(awsIamRoleKmsKeyId));
        aliasRequest.setTargetKeyId(arn);
        verify(client).createAlias(aliasRequest);

        AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = new AwsIamRoleKmsKeyRecord();
        awsIamRoleKmsKeyRecord.setId(awsIamRoleKmsKeyId);
        awsIamRoleKmsKeyRecord.setAwsIamRoleId(iamRoleId);
        awsIamRoleKmsKeyRecord.setAwsKmsKeyId(arn);
        awsIamRoleKmsKeyRecord.setAwsRegion(awsRegion);
        awsIamRoleKmsKeyRecord.setCreatedBy(user);
        awsIamRoleKmsKeyRecord.setLastUpdatedBy(user);
        awsIamRoleKmsKeyRecord.setCreatedTs(dateTime);
        awsIamRoleKmsKeyRecord.setLastUpdatedTs(dateTime);
        verify(awsIamRoleDao).createIamRoleKmsKey(awsIamRoleKmsKeyRecord);
    }

    @Test
    public void test_getAliasName() {
        assertEquals("alias/cerberus/foo", kmsService.getAliasName("foo"));
    }

    @Test
    public void test_validatePolicy() {
        String keyId = "key-id";
        String iamRoleArn = "arn";
        String kmsCMKRegion = "kmsCMKRegion";
        String policy = "policy";

        AWSKMSClient client = mock(AWSKMSClient.class);
        when(kmsClientFactory.getClient(kmsCMKRegion)).thenReturn(client);

        GetKeyPolicyResult result = mock(GetKeyPolicyResult.class);
        when(result.getPolicy()).thenReturn(policy);
        when(client.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(keyId).withPolicyName("default"))).thenReturn(result);
        when(kmsPolicyService.isPolicyValid(policy, iamRoleArn)).thenReturn(true);

        kmsService.validatePolicy(keyId, iamRoleArn, kmsCMKRegion);

        verify(client, times(1)).getKeyPolicy(new GetKeyPolicyRequest().withKeyId(keyId).withPolicyName("default"));
        verify(kmsPolicyService, times(1)).isPolicyValid(policy, iamRoleArn);
    }
}