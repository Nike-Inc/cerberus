package com.nike.cerberus.service;

import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.*;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.UuidSupplier;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class KmsServiceV1Test {

    private AwsIamRoleDao awsIamRoleDao;
    private UuidSupplier uuidSupplier;
    private KmsClientFactory kmsClientFactory;
    private KmsPolicyServiceV1 kmsPolicyService;

    private KmsServiceV1 kmsService;

    @Before
    public void setup() {
        awsIamRoleDao = mock(AwsIamRoleDao.class);
        uuidSupplier = mock(UuidSupplier.class);
        kmsClientFactory = mock(KmsClientFactory.class);
        kmsPolicyService = mock(KmsPolicyServiceV1.class);
        kmsService = new KmsServiceV1(awsIamRoleDao, uuidSupplier, kmsClientFactory, kmsPolicyService);
    }

    @Test
    public void test_provisionKmsKey() {

        String iamRoleId = "role-id";
        String awsRegion = "aws-region";
        String user = "user";
        OffsetDateTime dateTime = OffsetDateTime.now();

        String policy = "policy";
        String arn = "arn";

        String awsIamRoleKmsKeyId = "awsIamRoleKmsKeyId";

        when(uuidSupplier.get()).thenReturn(awsIamRoleKmsKeyId);
        when(kmsPolicyService.generateStandardKmsPolicy(arn)).thenReturn(policy);

        AWSKMSClient client = mock(AWSKMSClient.class);
        when(kmsClientFactory.getClient(awsRegion)).thenReturn(client);

        CreateKeyRequest request = new CreateKeyRequest();
        request.setKeyUsage(KeyUsageType.ENCRYPT_DECRYPT);
        request.setDescription("Key used by Cerberus for IAM role authentication.");
        request.setPolicy(policy);

        CreateKeyResult createKeyResult = mock(CreateKeyResult.class);
        KeyMetadata metadata = mock(KeyMetadata.class);
        when(metadata.getArn()).thenReturn(arn);
        when(createKeyResult.getKeyMetadata()).thenReturn(metadata);
        when(client.createKey(request)).thenReturn(createKeyResult);

        // invoke method under test
        String actualResult = kmsService.provisionKmsKey(iamRoleId, arn, awsRegion, user, dateTime);

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