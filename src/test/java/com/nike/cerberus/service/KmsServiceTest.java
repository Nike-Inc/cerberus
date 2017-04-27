package com.nike.cerberus.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.*;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.UuidSupplier;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class KmsServiceTest {

    private AwsIamRoleDao awsIamRoleDao;
    private UuidSupplier uuidSupplier;
    private KmsClientFactory kmsClientFactory;
    private KmsPolicyService kmsPolicyService;
    private DateTimeSupplier dateTimeSupplier;

    private KmsService kmsService;

    @Before
    public void setup() {
        awsIamRoleDao = mock(AwsIamRoleDao.class);
        uuidSupplier = mock(UuidSupplier.class);
        kmsClientFactory = mock(KmsClientFactory.class);
        kmsPolicyService = mock(KmsPolicyService.class);
        dateTimeSupplier = mock(DateTimeSupplier.class);

        kmsService = new KmsService(awsIamRoleDao, uuidSupplier, kmsClientFactory, kmsPolicyService, dateTimeSupplier);
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
        awsIamRoleKmsKeyRecord.setLastValidatedTs(dateTime);
        verify(awsIamRoleDao).createIamRoleKmsKey(awsIamRoleKmsKeyRecord);
    }

    @Test
    public void test_getAliasName() {
        assertEquals("alias/cerberus/foo", kmsService.getAliasName("foo"));
    }

    @Test
    public void test_validatePolicy_validates_policy_when_validate_interval_has_passed() {
        String kmsKeyArn = "kms key arn";
        String awsIamRoleRecordId = "aws iam role record id";
        String kmsCMKRegion = "kmsCMKRegion";
        String policy = "policy";
        OffsetDateTime lastValidated = OffsetDateTime.of(2016, 1, 1, 1, 1,
                1, 1, ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.now();

        AWSKMSClient client = mock(AWSKMSClient.class);
        when(kmsClientFactory.getClient(kmsCMKRegion)).thenReturn(client);

        GetKeyPolicyResult result = mock(GetKeyPolicyResult.class);
        when(result.getPolicy()).thenReturn(policy);
        when(client.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(kmsKeyArn)
                .withPolicyName("default"))).thenReturn(result);
        when(kmsPolicyService.isPolicyValid(policy, kmsKeyArn)).thenReturn(true);

        AwsIamRoleKmsKeyRecord kmsKey = mock(AwsIamRoleKmsKeyRecord.class);
        when(kmsKey.getAwsIamRoleId()).thenReturn(awsIamRoleRecordId);
        when(kmsKey.getAwsKmsKeyId()).thenReturn(kmsKeyArn);
        when(kmsKey.getAwsRegion()).thenReturn(kmsCMKRegion);
        when(kmsKey.getLastValidatedTs()).thenReturn(lastValidated);
        when(awsIamRoleDao.getKmsKey(awsIamRoleRecordId, kmsCMKRegion)).thenReturn(Optional.of(kmsKey));

        when(dateTimeSupplier.get()).thenReturn(now);
        kmsService.validatePolicy(kmsKey, kmsKeyArn);

        verify(client, times(1)).getKeyPolicy(new GetKeyPolicyRequest().withKeyId(kmsKeyArn)
                .withPolicyName("default"));
        verify(kmsPolicyService, times(1)).isPolicyValid(policy, kmsKeyArn);
    }

    @Test
    public void test_validatePolicy_validates_policy_when_validate_interval_has_not_passed() {
        String awsKmsKeyArn = "aws kms key arn";
        String iamPrincipalArn = "arn";
        String awsIamRoleRecordId = "aws iam role record id";
        String kmsCMKRegion = "kmsCMKRegion";
        OffsetDateTime now = OffsetDateTime.now();

        AwsIamRoleKmsKeyRecord kmsKey = mock(AwsIamRoleKmsKeyRecord.class);
        when(kmsKey.getAwsKmsKeyId()).thenReturn(awsKmsKeyArn);
        when(kmsKey.getAwsIamRoleId()).thenReturn(awsIamRoleRecordId);
        when(kmsKey.getAwsRegion()).thenReturn(kmsCMKRegion);
        when(kmsKey.getLastValidatedTs()).thenReturn(now);

        when(dateTimeSupplier.get()).thenReturn(now);
        kmsService.validatePolicy(kmsKey, iamPrincipalArn);

        verify(kmsClientFactory, never()).getClient(anyString());
        verify(kmsPolicyService, never()).isPolicyValid(anyString(), anyString());
    }

    @Test
    public void test_validatePolicy_does_not_throw_error_when_cannot_validate() {
        String keyId = "key-id";
        String iamPrincipalArn = "arn";
        String kmsCMKRegion = "kmsCMKRegion";
        String policy = "policy";
        OffsetDateTime lastValidated = OffsetDateTime.of(2016, 1, 1, 1, 1,
                1, 1, ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.now();
        when(dateTimeSupplier.get()).thenReturn(now);

        AwsIamRoleKmsKeyRecord kmsKey = mock(AwsIamRoleKmsKeyRecord.class);
        when(kmsKey.getAwsKmsKeyId()).thenReturn(keyId);
        when(kmsKey.getAwsIamRoleId()).thenReturn(iamPrincipalArn);
        when(kmsKey.getAwsRegion()).thenReturn(kmsCMKRegion);
        when(kmsKey.getLastValidatedTs()).thenReturn(lastValidated);

        AWSKMSClient client = mock(AWSKMSClient.class);
        when(kmsClientFactory.getClient(kmsCMKRegion)).thenReturn(client);

        GetKeyPolicyResult result = mock(GetKeyPolicyResult.class);
        when(result.getPolicy()).thenReturn(policy);
        when(client.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(keyId).withPolicyName("default"))).thenThrow(AmazonServiceException.class);

        kmsService.validatePolicy(kmsKey, iamPrincipalArn);

        verify(kmsPolicyService, never()).isPolicyValid(policy, iamPrincipalArn);
        verify(client, never()).putKeyPolicy(anyObject());
    }

    @Test
    public void test_updateKmsKey() {

        String iamRoleId = "role-id";
        String awsRegion = "aws-region";
        String user = "user";
        OffsetDateTime dateTime = OffsetDateTime.now();

        AwsIamRoleKmsKeyRecord dbRecord = new AwsIamRoleKmsKeyRecord();
        dbRecord.setAwsRegion(awsRegion);
        dbRecord.setAwsIamRoleId(iamRoleId);
        dbRecord.setLastValidatedTs(OffsetDateTime.now());
        when(awsIamRoleDao.getKmsKey(iamRoleId, awsRegion)).thenReturn(Optional.of(dbRecord));

        kmsService.updateKmsKey(iamRoleId, awsRegion, user, dateTime, dateTime);

        AwsIamRoleKmsKeyRecord expected = new AwsIamRoleKmsKeyRecord();
        expected.setAwsIamRoleId(iamRoleId);
        expected.setLastUpdatedBy(user);
        expected.setLastUpdatedTs(dateTime);
        expected.setLastValidatedTs(dateTime);
        expected.setAwsRegion(awsRegion);

        verify(awsIamRoleDao).updateIamRoleKmsKey(expected);
    }

    @Test(expected = ApiException.class)
    public void test_updateKmsKey_fails_when_record_not_found() {

        String iamRoleId = "role-id";
        String awsRegion = "aws-region";
        String user = "user";
        OffsetDateTime dateTime = OffsetDateTime.now();

        when(awsIamRoleDao.getKmsKey(iamRoleId, awsRegion)).thenReturn(Optional.empty());

        kmsService.updateKmsKey(iamRoleId, awsRegion, user, dateTime, dateTime);
    }
}