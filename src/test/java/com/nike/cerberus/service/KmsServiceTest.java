package com.nike.cerberus.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.CreateAliasRequest;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.DescribeKeyResult;
import com.amazonaws.services.kms.model.GetKeyPolicyRequest;
import com.amazonaws.services.kms.model.GetKeyPolicyResult;
import com.amazonaws.services.kms.model.KeyMetadata;
import com.amazonaws.services.kms.model.KeyState;
import com.amazonaws.services.kms.model.KeyUsageType;
import com.amazonaws.services.kms.model.Tag;
import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.UuidSupplier;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KmsServiceTest {

    private static final String VERSION = "fakeVersion";
    private static final String ENV = "fakeEnv";

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

        kmsService = new KmsService(awsIamRoleDao, uuidSupplier, kmsClientFactory, kmsPolicyService, dateTimeSupplier, new AwsIamRoleArnParser(), VERSION, ENV);
    }

    @Test
    public void test_provisionKmsKey() {

        String iamRoleId = "role-id";
        String awsRegion = "aws-region";
        String user = "user";
        OffsetDateTime dateTime = OffsetDateTime.now();

        String policy = "policy";
        String arn = "arn:aws:iam::12345678901234:role/some-role";

        String awsIamRoleKmsKeyId = "awsIamRoleKmsKeyId";

        when(uuidSupplier.get()).thenReturn(awsIamRoleKmsKeyId);
        when(kmsPolicyService.generateStandardKmsPolicy(arn)).thenReturn(policy);

        AWSKMSClient client = mock(AWSKMSClient.class);
        when(kmsClientFactory.getClient(awsRegion)).thenReturn(client);

        CreateKeyRequest request = new CreateKeyRequest();
        request.setKeyUsage(KeyUsageType.ENCRYPT_DECRYPT);
        request.setDescription("Key used by Cerberus fakeEnv for IAM role authentication. " + arn);
        request.setPolicy(policy);
        request.setTags(
                Lists.newArrayList(
                        new Tag().withTagKey("created_by").withTagValue("cms" + VERSION),
                        new Tag().withTagKey("created_for").withTagValue("cerberus_auth"),
                        new Tag().withTagKey("auth_principal").withTagValue(arn),
                        new Tag().withTagKey("cerberus_env").withTagValue(ENV)
                )

        );

        CreateKeyResult createKeyResult = mock(CreateKeyResult.class);
        KeyMetadata metadata = mock(KeyMetadata.class);
        when(metadata.getArn()).thenReturn(arn);
        when(createKeyResult.getKeyMetadata()).thenReturn(metadata);
        when(client.createKey(request)).thenReturn(createKeyResult);

        // invoke method under test
        String actualResult = kmsService.provisionKmsKey(iamRoleId, arn, awsRegion, user, dateTime).getAwsKmsKeyId();

        assertEquals(arn, actualResult);

        CreateAliasRequest aliasRequest = new CreateAliasRequest();
        aliasRequest.setAliasName(kmsService.getAliasName(awsIamRoleKmsKeyId, arn));
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
        assertEquals("alias/cerberus/fakeEnv/12345678901234/some-role/uuid", kmsService.getAliasName("uuid", "arn:aws:iam::12345678901234:role/some-role"));
    }

    @Test
    public void test_getAliasName_with_overly_long_descriptive_text() {
        assertEquals("alias/cerberus/fakeEnv/12345678901234/this/is/a/very/long/path/that/just/keeps/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/uuid",
                kmsService.getAliasName("uuid", "arn:aws:iam::12345678901234:role/this/is/a/very/long/path/that/just/keeps/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/some-role")
        );
    }

    @Test
    public void test_getAliasName_starts_with_alias() {
        // aliases must start with "alias/"
        assertTrue(kmsService.getAliasName("uuid", "arn:aws:iam::12345678901234:role/some-role").startsWith("alias/"));
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
        when(client.describeKey(anyObject())).thenReturn(
                new DescribeKeyResult()
                        .withKeyMetadata(
                                new KeyMetadata()
                                        .withKeyState(KeyState.Enabled)));

        when(kmsClientFactory.getClient(kmsCMKRegion)).thenReturn(client);

        GetKeyPolicyResult result = mock(GetKeyPolicyResult.class);
        when(result.getPolicy()).thenReturn(policy);
        when(client.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(kmsKeyArn)
                .withPolicyName("default"))).thenReturn(result);
        when(kmsPolicyService.isPolicyValid(policy)).thenReturn(true);

        AwsIamRoleKmsKeyRecord kmsKey = mock(AwsIamRoleKmsKeyRecord.class);
        when(kmsKey.getAwsIamRoleId()).thenReturn(awsIamRoleRecordId);
        when(kmsKey.getAwsKmsKeyId()).thenReturn(kmsKeyArn);
        when(kmsKey.getAwsRegion()).thenReturn(kmsCMKRegion);
        when(kmsKey.getLastValidatedTs()).thenReturn(lastValidated);
        when(awsIamRoleDao.getKmsKey(awsIamRoleRecordId, kmsCMKRegion)).thenReturn(Optional.of(kmsKey));

        when(dateTimeSupplier.get()).thenReturn(now);
        kmsService.validateKeyAndPolicy(kmsKey, kmsKeyArn);

        verify(client, times(1)).getKeyPolicy(new GetKeyPolicyRequest().withKeyId(kmsKeyArn)
                .withPolicyName("default"));
        verify(kmsPolicyService, times(1)).isPolicyValid(policy);
    }

    @Test
    public void test_validateKeyAndPolicy_validates_policy_when_validate_interval_has_not_passed() {
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
        kmsService.validateKeyAndPolicy(kmsKey, iamPrincipalArn);

        verify(kmsClientFactory, never()).getClient(anyString());
        verify(kmsPolicyService, never()).isPolicyValid(anyString());
    }

    @Test
    public void test_validateKeyAndPolicy_does_not_throw_error_when_cannot_validate() {
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

        kmsService.validateKeyAndPolicy(kmsKey, iamPrincipalArn);

        verify(kmsPolicyService, never()).isPolicyValid(policy);
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

    @Test
    public void test_getKmsKeyState_happy() {
        String awsRegion = "aws region";

        String kmsKeyId = "kms key id";
        String state = "state";
        AWSKMSClient kmsClient = mock(AWSKMSClient.class);
        when(kmsClientFactory.getClient(awsRegion)).thenReturn(kmsClient);
        when(kmsClient.describeKey(anyObject())).thenReturn(
                new DescribeKeyResult()
                        .withKeyMetadata(
                                new KeyMetadata()
                                        .withKeyState(state)));

        String result = kmsService.getKmsKeyState(kmsKeyId, awsRegion);

        assertEquals(state, result);
    }

}