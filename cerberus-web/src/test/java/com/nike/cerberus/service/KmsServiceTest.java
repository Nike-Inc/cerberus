/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.service;

import static com.nike.cerberus.service.KmsPolicyService.CERBERUS_MANAGEMENT_SERVICE_SID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.KmsClientFactory;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.domain.AuthKmsKeyMetadata;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.info.BuildProperties;

public class KmsServiceTest {

  private static final String ARTIFACT = "cerberus";
  private static final String VERSION = "fakeVersion";
  private static final String ENV = "fakeEnv";

  private AwsIamRoleDao awsIamRoleDao;
  private UuidSupplier uuidSupplier;
  private KmsClientFactory kmsClientFactory;
  private KmsPolicyService kmsPolicyService;
  private DateTimeSupplier dateTimeSupplier;
  private Slugger slugger;

  private KmsService kmsService;

  @Before
  public void setup() {
    awsIamRoleDao = mock(AwsIamRoleDao.class);
    uuidSupplier = mock(UuidSupplier.class);
    kmsClientFactory = mock(KmsClientFactory.class);
    kmsPolicyService = mock(KmsPolicyService.class);
    dateTimeSupplier = mock(DateTimeSupplier.class);
    slugger = new Slugger();

    var props = new Properties();
    props.setProperty("artifact", ARTIFACT);
    props.setProperty("version", VERSION);
    var buildProps = new BuildProperties(props);

    kmsService =
        new KmsService(
            awsIamRoleDao,
            uuidSupplier,
            kmsClientFactory,
            kmsPolicyService,
            dateTimeSupplier,
            new AwsIamRoleArnParser(true, false),
            3000,
            ENV,
            slugger,
            buildProps);
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
            new Tag().withTagKey("created_by").withTagValue(ARTIFACT + VERSION),
            new Tag().withTagKey("created_for").withTagValue("cerberus_auth"),
            new Tag().withTagKey("auth_principal").withTagValue(arn),
            new Tag().withTagKey("cerberus_env").withTagValue(ENV)));

    CreateKeyResult createKeyResult = mock(CreateKeyResult.class);
    KeyMetadata metadata = mock(KeyMetadata.class);
    when(metadata.getArn()).thenReturn(arn);
    when(createKeyResult.getKeyMetadata()).thenReturn(metadata);
    when(client.createKey(any())).thenReturn(createKeyResult);

    // invoke method under test
    String actualResult =
        kmsService.provisionKmsKey(iamRoleId, arn, awsRegion, user, dateTime).getAwsKmsKeyId();

    assertEquals(arn, actualResult);

    CreateAliasRequest aliasRequest = new CreateAliasRequest();
    aliasRequest.setAliasName(kmsService.getAliasName(awsIamRoleKmsKeyId, arn));
    aliasRequest.setTargetKeyId(arn);
    verify(client).createAlias(aliasRequest);

    AwsIamRoleKmsKeyRecord awsIamRoleKmsKeyRecord = AwsIamRoleKmsKeyRecord.builder().build();
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
    assertEquals(
        "alias/cerberus/fakeEnv/12345678901234/some-role/uuid",
        kmsService.getAliasName("uuid", "arn:aws:iam::12345678901234:role/some-role"));
  }

  @Test
  public void test_getAliasName_with_overly_long_descriptive_text() {
    assertEquals(
        "alias/cerberus/fakeEnv/12345678901234/this/is/a/very/long/path/that/just/keeps/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/uuid",
        kmsService.getAliasName(
            "uuid",
            "arn:aws:iam::12345678901234:role/this/is/a/very/long/path/that/just/keeps/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/on/going/on/and/some-role"));
  }

  @Test
  public void test_getAliasName_starts_with_alias() {
    // aliases must start with "alias/"
    assertTrue(
        kmsService
            .getAliasName("uuid", "arn:aws:iam::12345678901234:role/some-role")
            .startsWith("alias/"));
  }

  @Test
  public void test_validatePolicy_validates_policy_when_validate_interval_has_passed() {
    String kmsKeyArn = "kms key arn";
    String awsIamRoleRecordId = "aws iam role record id";
    String kmsCMKRegion = "kmsCMKRegion";
    String policy = "policy";
    OffsetDateTime lastValidated = OffsetDateTime.of(2016, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
    OffsetDateTime now = OffsetDateTime.now();

    AWSKMSClient client = mock(AWSKMSClient.class);
    when(client.describeKey(anyObject()))
        .thenReturn(
            new DescribeKeyResult()
                .withKeyMetadata(new KeyMetadata().withKeyState(KeyState.Enabled)));

    when(kmsClientFactory.getClient(kmsCMKRegion)).thenReturn(client);

    GetKeyPolicyResult result = mock(GetKeyPolicyResult.class);
    when(result.getPolicy()).thenReturn(policy);
    when(client.getKeyPolicy(
            new GetKeyPolicyRequest().withKeyId(kmsKeyArn).withPolicyName("default")))
        .thenReturn(result);
    when(kmsPolicyService.isPolicyValid(policy)).thenReturn(true);

    AwsIamRoleKmsKeyRecord kmsKey = mock(AwsIamRoleKmsKeyRecord.class);
    when(kmsKey.getAwsIamRoleId()).thenReturn(awsIamRoleRecordId);
    when(kmsKey.getAwsKmsKeyId()).thenReturn(kmsKeyArn);
    when(kmsKey.getAwsRegion()).thenReturn(kmsCMKRegion);
    when(kmsKey.getLastValidatedTs()).thenReturn(lastValidated);
    when(awsIamRoleDao.getKmsKey(awsIamRoleRecordId, kmsCMKRegion)).thenReturn(Optional.of(kmsKey));

    when(dateTimeSupplier.get()).thenReturn(now);
    kmsService.validateKeyAndPolicy(kmsKey, kmsKeyArn);

    verify(client, times(1))
        .getKeyPolicy(new GetKeyPolicyRequest().withKeyId(kmsKeyArn).withPolicyName("default"));
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
    OffsetDateTime lastValidated = OffsetDateTime.of(2016, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
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
    when(client.getKeyPolicy(new GetKeyPolicyRequest().withKeyId(keyId).withPolicyName("default")))
        .thenThrow(AmazonServiceException.class);

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

    AwsIamRoleKmsKeyRecord dbRecord = AwsIamRoleKmsKeyRecord.builder().build();
    dbRecord.setAwsRegion(awsRegion);
    dbRecord.setAwsIamRoleId(iamRoleId);
    dbRecord.setLastValidatedTs(OffsetDateTime.now());
    when(awsIamRoleDao.getKmsKey(iamRoleId, awsRegion)).thenReturn(Optional.of(dbRecord));

    kmsService.updateKmsKey(iamRoleId, awsRegion, user, dateTime, dateTime);

    AwsIamRoleKmsKeyRecord expected = AwsIamRoleKmsKeyRecord.builder().build();
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
    when(kmsClient.describeKey(anyObject()))
        .thenReturn(new DescribeKeyResult().withKeyMetadata(new KeyMetadata().withKeyState(state)));

    String result = kmsService.getKmsKeyState(kmsKeyId, awsRegion);

    assertEquals(state, result);
  }

  @Test
  public void test_deleteKmsKeyById_proxies_the_dao() {
    String key = "the-key-id";

    kmsService.deleteKmsKeyById(key);

    verify(awsIamRoleDao, times(1)).deleteKmsKeyById(key);
  }

  @Test
  public void test_that_getAuthenticationKmsMetadata_returns_empty_list_when_there_are_no_keys() {
    when(awsIamRoleDao.getAllKmsKeys()).thenReturn(Optional.empty());

    assertEquals(new LinkedList<AuthKmsKeyMetadata>(), kmsService.getAuthenticationKmsMetadata());
  }

  @Test
  public void test_that_getAuthenticationKmsMetadata_returns_AuthKmsKeyMetadata_from_dao_data() {
    OffsetDateTime create = OffsetDateTime.now().plus(5, ChronoUnit.MINUTES);
    OffsetDateTime update = OffsetDateTime.now().plus(3, ChronoUnit.MINUTES);
    OffsetDateTime validate = OffsetDateTime.now().plus(7, ChronoUnit.MINUTES);
    List<AwsIamRoleKmsKeyRecord> keyRecords =
        ImmutableList.of(
            AwsIamRoleKmsKeyRecord.builder()
                .awsIamRoleId("iam-role-id")
                .awsKmsKeyId("key-id")
                .awsRegion("us-west-2")
                .createdTs(create)
                .lastUpdatedTs(update)
                .lastValidatedTs(validate)
                .build());

    List<AuthKmsKeyMetadata> expected =
        ImmutableList.of(
            AuthKmsKeyMetadata.builder()
                .awsIamRoleArn("iam-role-arn")
                .awsKmsKeyId("key-id")
                .awsRegion("us-west-2")
                .createdTs(create)
                .lastUpdatedTs(update)
                .lastValidatedTs(validate)
                .build());

    when(awsIamRoleDao.getAllKmsKeys()).thenReturn(Optional.ofNullable(keyRecords));
    when(awsIamRoleDao.getIamRoleById("iam-role-id"))
        .thenReturn(Optional.of(AwsIamRoleRecord.builder().awsIamRoleArn("iam-role-arn").build()));

    assertArrayEquals(expected.toArray(), kmsService.getAuthenticationKmsMetadata().toArray());
  }

  @Test
  public void
      test_that_filterKeysCreatedByKmsService_filters_out_keys_that_do_not_contain_expected_arn_prefix() {

    Policy policyThatShouldBeInSet =
        new Policy()
            .withStatements(
                new Statement(Statement.Effect.Allow)
                    .withId(CERBERUS_MANAGEMENT_SERVICE_SID)
                    .withPrincipals(
                        new Principal("arn:aws:iam:123456:role/" + ENV + "-cms-role-alk234khsdf")),
                new Statement(Statement.Effect.Allow),
                new Statement(Statement.Effect.Allow),
                new Statement(Statement.Effect.Allow));

    Policy policyThatShouldNotBeInSet =
        new Policy()
            .withStatements(
                new Statement(Statement.Effect.Allow)
                    .withId(CERBERUS_MANAGEMENT_SERVICE_SID)
                    .withPrincipals(
                        new Principal("arn:aws:iam:123456:role/prod-cms-role-alk234khsdf")),
                new Statement(Statement.Effect.Allow),
                new Statement(Statement.Effect.Allow),
                new Statement(Statement.Effect.Allow));

    Policy policyThatWasntCreatedByCms =
        new Policy()
            .withStatements(
                new Statement(Statement.Effect.Allow)
                    .withId("foo-bar")
                    .withPrincipals(
                        new Principal("arn:aws:iam:123456:role/" + ENV + "-cms-role-alk234khsdf")));

    KmsService kmsServiceSpy = spy(kmsService);

    Set<String> allKmsCmkIdsForRegion = ImmutableSet.of("key1", "key2", "key3", "key4", "key5");

    String region = "us-west-2";

    Set<String> expectedKeys = ImmutableSet.of("key3");

    doReturn(Optional.of(policyThatShouldNotBeInSet))
        .when(kmsServiceSpy)
        .downloadPolicy("key1", region, 0);
    doReturn(Optional.of(policyThatShouldNotBeInSet))
        .when(kmsServiceSpy)
        .downloadPolicy("key2", region, 0);
    doReturn(Optional.of(policyThatShouldBeInSet))
        .when(kmsServiceSpy)
        .downloadPolicy("key3", region, 0);
    doReturn(Optional.of(policyThatShouldNotBeInSet))
        .when(kmsServiceSpy)
        .downloadPolicy("key4", region, 0);
    doReturn(Optional.of(policyThatWasntCreatedByCms))
        .when(kmsServiceSpy)
        .downloadPolicy("key5", region, 0);

    Set<String> actual = kmsServiceSpy.filterKeysCreatedByKmsService(allKmsCmkIdsForRegion, region);

    assertEquals(expectedKeys, actual);
  }
}
