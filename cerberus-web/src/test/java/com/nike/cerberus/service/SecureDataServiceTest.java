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

import static com.nike.cerberus.service.AuthenticationService.SYSTEM_USER;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.codahale.metrics.Counter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.SecureDataDao;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.*;
import com.nike.cerberus.metric.MetricsService;
import com.nike.cerberus.record.DataKeyInfo;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

public class SecureDataServiceTest {

  private String secret = "{\"k1\":\"val\",\"k2\":\"val\"}";
  private byte[] plaintextBytes = secret.getBytes(StandardCharsets.UTF_8);
  private String principal = SYSTEM_USER;
  private String ciphertext = "fasl;kej fasdf0978023 alskdf as";
  private byte[] ciphertextBytes = ciphertext.getBytes(StandardCharsets.UTF_8);
  private String sdbId = UUID.randomUUID().toString();
  private String path = "super/important/secrets";
  private String partialPathWithoutTrailingSlash = "apps/checkout-service/api-keys";
  private String[] keysRes =
      new String[] {
        "apps/checkout-service/api-keys/signal-fx-api-key",
        "apps/checkout-service/api-keys/splunk-api-key"
      };
  @Mock private SecureDataRecord secureDataRecord;

  @Mock private SecureDataDao secureDataDao;
  @Mock private EncryptionService encryptionService;
  @Mock private DateTimeSupplier dateTimeSupplier;
  @Mock private SecureDataVersionDao secureDataVersionDao;
  @Mock private MetricsService metricsService;
  private ObjectMapper objectMapper;

  private SecureDataService secureDataService;
  @Mock private Counter successCounter;
  @Mock private Counter failureCounter;

  @Before
  public void before() {
    initMocks(this);
    objectMapper = new ObjectMapper();
    Mockito.when(metricsService.getOrCreateCounter("cms.encryption.reencrypt.success", null))
        .thenReturn(successCounter);
    Mockito.when(metricsService.getOrCreateCounter("cms.encryption.reencrypt.fail", null))
        .thenReturn(failureCounter);
    secureDataService =
        new SecureDataService(
            secureDataDao,
            encryptionService,
            objectMapper,
            dateTimeSupplier,
            secureDataVersionDao,
            metricsService);
  }

  @After
  public void after() {
    reset(secureDataDao, encryptionService, dateTimeSupplier);
  }

  @Test
  public void test_that_writeSecret_encrypts_the_payload_and_calls_update() {
    when(encryptionService.encrypt(secret, path)).thenReturn(ciphertext);

    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
    when(dateTimeSupplier.get()).thenReturn(now);

    when(secureDataRecord.getType()).thenReturn(SecureDataType.OBJECT);
    when(secureDataRecord.getCreatedBy()).thenReturn(SYSTEM_USER);
    when(secureDataRecord.getCreatedTs()).thenReturn(now);
    when(secureDataDao.readSecureDataByPath(sdbId, path)).thenReturn(Optional.of(secureDataRecord));

    secureDataService.writeSecret(sdbId, path, secret, principal);
    verify(secureDataDao)
        .updateSecureData(
            sdbId,
            path,
            ciphertextBytes,
            2,
            SecureDataType.OBJECT,
            plaintextBytes.length,
            SYSTEM_USER,
            now,
            SYSTEM_USER,
            now,
            null);
  }

  @Test
  public void test_that_readSecret_returns_empty_optional_if_dao_returns_nothing() {
    when(secureDataDao.readSecureDataByPathAndType(sdbId, path, SecureDataType.OBJECT))
        .thenReturn(Optional.empty());

    Optional<SecureData> result = secureDataService.readSecret(sdbId, path);

    assertFalse(result.isPresent());
  }

  @Test
  public void test_that_readSecret_decrypts_the_payload_when_present() {
    when(secureDataDao.readSecureDataByPathAndType(sdbId, path, SecureDataType.OBJECT))
        .thenReturn(
            Optional.of(
                new SecureDataRecord()
                    .setEncryptedBlob(ciphertext.getBytes(Charset.forName("UTF-8")))));

    when(encryptionService.decrypt(ciphertext, path)).thenReturn(secret);

    Optional<SecureData> result = secureDataService.readSecret(sdbId, path);

    assertTrue(result.isPresent());
    assertTrue(result.get().getData().equals(secret));
  }

  @Test
  public void test_that_listKeys_appends_a_slash_to_the_partial_path_if_not_present() {
    when(secureDataDao.getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT))
        .thenReturn(keysRes);
    secureDataService.listKeys(sdbId, partialPathWithoutTrailingSlash);
    verify(secureDataDao)
        .getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);
  }

  @Test
  public void test_that_listKeys_does_not_append_a_slash_to_the_partial_path_if_already_present() {
    when(secureDataDao.getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT))
        .thenReturn(keysRes);
    secureDataService.listKeys(sdbId, partialPathWithoutTrailingSlash + "/");
    verify(secureDataDao)
        .getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);
  }

  @Test
  public void test_that_listKeys_returns_empty_set_if_dao_returns_null() {
    when(secureDataDao.getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT))
        .thenReturn(null);
    Set<String> res = secureDataService.listKeys(sdbId, partialPathWithoutTrailingSlash);
    verify(secureDataDao)
        .getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

    assertTrue(res != null && res.isEmpty());
  }

  @Test
  public void test_that_listKeys_returns_empty_set_if_dao_returns_empty() {
    when(secureDataDao.getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT))
        .thenReturn(new String[] {});
    Set<String> res = secureDataService.listKeys(sdbId, partialPathWithoutTrailingSlash);
    verify(secureDataDao)
        .getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

    assertTrue(res != null && res.isEmpty());
  }

  @Test
  public void test_that_listKeys_returns_expected_set_of_keys() {
    when(secureDataDao.getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT))
        .thenReturn(keysRes);
    Set<String> res = secureDataService.listKeys(sdbId, partialPathWithoutTrailingSlash);
    verify(secureDataDao)
        .getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

    assertEquals("There should be 2 keys", 2, res.size());
    assertTrue(
        "the list of keys should contain the 2 api keys",
        res.containsAll(ImmutableSet.of("signal-fx-api-key", "splunk-api-key")));
  }

  @Test
  public void
      test_that_listKeys_returns_expected_set_of_keys_with_sub_folder_paths_stripped_to_nearest_folder() {
    when(secureDataDao.getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT))
        .thenReturn(new String[] {"apps/checkout-service/api-keys/sub-folder/some-different-key"});
    Set<String> res = secureDataService.listKeys(sdbId, partialPathWithoutTrailingSlash);
    verify(secureDataDao)
        .getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

    assertEquals("There should be 1 key", 1, res.size());
    assertTrue(res.contains("sub-folder/"));
  }

  @Test
  public void
      test_that_listKeys_returns_expected_set_of_keys_with_sub_folder_paths_stripped_to_nearest_folder_and_contains_key_without_slash_if_present() {
    when(secureDataDao.getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT))
        .thenReturn(
            new String[] {
              "apps/checkout-service/api-keys/sub-folder/some-different-key",
              "apps/checkout-service/api-keys/sub-folder"
            });
    Set<String> res = secureDataService.listKeys(sdbId, partialPathWithoutTrailingSlash);
    verify(secureDataDao)
        .getPathsByPartialPathAndType(
            sdbId, partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

    assertEquals("There should be 2 key", 2, res.size());
    assertTrue(res.contains("sub-folder/"));
    assertTrue(res.contains("sub-folder"));
  }

  @Test
  public void test_that_deleteAllSecretsThatStartWithGivenPartialPath_proxies_to_dao() {
    secureDataService.deleteAllSecretsThatStartWithGivenPartialPath(
        sdbId, partialPathWithoutTrailingSlash);
    verify(secureDataDao)
        .deleteAllSecretsThatStartWithGivenPartialPath(sdbId, partialPathWithoutTrailingSlash);
  }

  @Test
  public void test_that_deleteSecret_proxies_to_dao() {
    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
    when(dateTimeSupplier.get()).thenReturn(now);
    when(secureDataDao.readSecureDataByPathAndType(
            sdbId, partialPathWithoutTrailingSlash, SecureDataType.OBJECT))
        .thenReturn(Optional.of(secureDataRecord));

    secureDataService.deleteSecret(
        sdbId, partialPathWithoutTrailingSlash, SecureDataType.OBJECT, principal);
    verify(secureDataDao).deleteSecret(sdbId, partialPathWithoutTrailingSlash);
  }

  @Test
  public void test_that_restoreSdbSecrets_proxies_to_dao() throws JsonProcessingException {
    String sdbId = "sdb-id";
    String path = "secret/path/one";
    String sdbPath = "category/secret/path/one";
    String secretPath = StringUtils.substringAfter(sdbPath, "/");

    Map<String, Map<String, Object>> data = new HashMap<>();
    Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("foo", "bar");
    data.put(sdbPath, keyValuePairs);

    String plaintext = new ObjectMapper().writeValueAsString(keyValuePairs);
    byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
    String ciphertext = "encrypted payload";
    byte[] ciphertextBytes = ciphertext.getBytes(StandardCharsets.UTF_8);
    when(encryptionService.encrypt(plaintext, secretPath)).thenReturn(ciphertext);

    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
    when(dateTimeSupplier.get()).thenReturn(now);

    when(secureDataDao.readSecureDataByPath(sdbId, path)).thenReturn(Optional.empty());

    secureDataService.restoreSdbSecrets(sdbId, data, principal);
    verify(secureDataDao)
        .writeSecureData(
            sdbId,
            secretPath,
            ciphertextBytes,
            1,
            SecureDataType.OBJECT,
            plaintextBytes.length,
            principal,
            now,
            principal,
            now);
  }

  @Test
  public void test_that_getTopLevelKVPairCount_returns_proper_count() {
    assertEquals(2, secureDataService.getTopLevelKVPairCount("{\"k1\":\"val\",\"k2\":\"val\"}"));
  }

  @Test
  public void test_that_getTopLevelKVPairCount_returns_1_on_failure() {
    assertEquals(1, secureDataService.getTopLevelKVPairCount("{[\"1\",\"2\"]}"));
  }

  @Test
  public void test_that_secureDataHasBeenUpdated_returns_true_when_updated() {
    OffsetDateTime now = OffsetDateTime.now();
    SecureDataRecord differentPrincipals =
        new SecureDataRecord()
            .setCreatedBy("principal")
            .setCreatedTs(now)
            .setLastUpdatedBy("different principal")
            .setLastUpdatedTs(now);

    SecureDataRecord differentTs =
        new SecureDataRecord()
            .setCreatedBy("principal")
            .setCreatedTs(now)
            .setLastUpdatedBy("principal")
            .setLastUpdatedTs(OffsetDateTime.now());

    SecureDataRecord differentPrincipalsAndTs =
        new SecureDataRecord()
            .setCreatedBy("principal")
            .setCreatedTs(now)
            .setLastUpdatedBy("different principal")
            .setLastUpdatedTs(OffsetDateTime.now());

    assertTrue(secureDataService.secureDataHasBeenUpdated(differentPrincipals));
    assertTrue(secureDataService.secureDataHasBeenUpdated(differentTs));
    assertTrue(secureDataService.secureDataHasBeenUpdated(differentPrincipalsAndTs));
  }

  @Test
  public void test_that_secureDataHasBeenUpdated_returns_false_when_not_updated() {
    String principal = "principal";
    OffsetDateTime now = OffsetDateTime.now();
    SecureDataRecord secureDataRecord =
        new SecureDataRecord()
            .setCreatedBy(principal)
            .setCreatedTs(now)
            .setLastUpdatedBy(principal)
            .setLastUpdatedTs(now);

    assertFalse(secureDataService.secureDataHasBeenUpdated(secureDataRecord));
  }

  @Test(expected = ApiException.class)
  public void test_that_writeSecret_does_now_allow_other_types_to_be_overwritten() {
    String pathToFile = "app/sdb/file.pem";
    SecureDataRecord fileRecord = new SecureDataRecord().setType(SecureDataType.FILE);
    when(encryptionService.encrypt(secret, pathToFile)).thenReturn(ciphertext);
    when(secureDataDao.readSecureDataByPath(sdbId, pathToFile)).thenReturn(Optional.of(fileRecord));

    secureDataService.writeSecret(sdbId, pathToFile, secret, "principal");
  }

  @Test(expected = ApiException.class)
  public void test_that_writeFile_does_now_allow_other_types_to_be_overwritten() {
    String pathToObject = "app/sdb/object";
    SecureDataRecord objectRecord = new SecureDataRecord().setType(SecureDataType.OBJECT);
    when(secureDataDao.readSecureDataByPath(sdbId, pathToObject))
        .thenReturn(Optional.of(objectRecord));

    secureDataService.writeSecureFile(sdbId, pathToObject, new byte[] {}, 0, pathToObject);
  }

  @Test
  public void test_that_readSecret_reads_secrets_by_the_correct_type() {
    String pathToObject = "app/sdb/object";
    SecureDataRecord record =
        new SecureDataRecord()
            .setEncryptedBlob(ciphertextBytes)
            .setSizeInBytes(ciphertextBytes.length);
    when(encryptionService.decrypt(ciphertextBytes, pathToObject)).thenReturn(plaintextBytes);
    when(secureDataDao.readSecureDataByPathAndType(sdbId, pathToObject, SecureDataType.OBJECT))
        .thenReturn(Optional.of(record));

    secureDataService.readSecret(sdbId, pathToObject);

    verify(secureDataDao).readSecureDataByPathAndType(sdbId, pathToObject, SecureDataType.OBJECT);
  }

  @Test
  public void test_that_readFile_reads_secrets_by_the_correct_type() {
    String pathToFile = "app/sdb/file.pem";
    SecureDataRecord record =
        new SecureDataRecord()
            .setType(SecureDataType.FILE)
            .setPath(pathToFile)
            .setEncryptedBlob(ciphertextBytes)
            .setSizeInBytes(ciphertextBytes.length);
    when(encryptionService.decrypt(ciphertextBytes, pathToFile)).thenReturn(plaintextBytes);
    when(secureDataDao.readSecureDataByPathAndType(sdbId, pathToFile, SecureDataType.FILE))
        .thenReturn(Optional.of(record));

    secureDataService.readFile(sdbId, pathToFile);

    verify(secureDataDao).readSecureDataByPathAndType(sdbId, pathToFile, SecureDataType.FILE);
  }

  @Test
  public void test_that_reencrypt_file_calls_reencrypt_bytes() {
    String id = "secure data id";
    String newCiphertext = "fasdfkxcvasdff as";
    byte[] newCiphertextBytes = newCiphertext.getBytes(StandardCharsets.UTF_8);
    String pathToFile = "app/sdb/object";
    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
    when(dateTimeSupplier.get()).thenReturn(now);
    SecureDataRecord record =
        new SecureDataRecord()
            .setType(SecureDataType.FILE)
            .setPath(pathToFile)
            .setEncryptedBlob(ciphertextBytes)
            .setSizeInBytes(ciphertextBytes.length);
    when(encryptionService.reencrypt(ciphertextBytes, pathToFile)).thenReturn(newCiphertextBytes);
    when(secureDataDao.readSecureDataByIdLocking(id)).thenReturn(Optional.of(record));

    secureDataService.reencryptData(id);

    verify(secureDataDao).readSecureDataByIdLocking(id);
    ArgumentCaptor<SecureDataRecord> argument = ArgumentCaptor.forClass(SecureDataRecord.class);
    verify(secureDataDao).updateSecureData(argument.capture());
    assertEquals(now, argument.getValue().getLastRotatedTs());
    assertArrayEquals(newCiphertextBytes, argument.getValue().getEncryptedBlob());
  }

  @Test
  public void test_that_reencrypt_object_calls_reencrypt_string() {
    String id = "secure data id";
    String newCiphertext = "fasdfkxcvasdff as";
    byte[] newCiphertextBytes = newCiphertext.getBytes(StandardCharsets.UTF_8);
    String pathToObject = "app/sdb/secret";
    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
    when(dateTimeSupplier.get()).thenReturn(now);
    SecureDataRecord record =
        new SecureDataRecord()
            .setType(SecureDataType.OBJECT)
            .setPath(pathToObject)
            .setEncryptedBlob(ciphertextBytes)
            .setSizeInBytes(ciphertextBytes.length);
    when(encryptionService.reencrypt(ciphertext, pathToObject)).thenReturn(newCiphertext);
    when(secureDataDao.readSecureDataByIdLocking(id)).thenReturn(Optional.of(record));

    secureDataService.reencryptData(id);

    verify(secureDataDao).readSecureDataByIdLocking(id);
    ArgumentCaptor<SecureDataRecord> argument = ArgumentCaptor.forClass(SecureDataRecord.class);
    verify(secureDataDao).updateSecureData(argument.capture());
    assertEquals(now, argument.getValue().getLastRotatedTs());
    assertArrayEquals(newCiphertextBytes, argument.getValue().getEncryptedBlob());
  }

  @Test
  public void test_that_reencrypt_version_file_calls_reencrypt_bytes() {
    String newCiphertext = "fasdfkxcvasdff as";
    byte[] newCiphertextBytes = newCiphertext.getBytes(StandardCharsets.UTF_8);
    String pathToFile = "app/sdb/object";
    String versionId = "version";
    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
    when(dateTimeSupplier.get()).thenReturn(now);
    SecureDataVersionRecord record =
        new SecureDataVersionRecord()
            .setType(SecureDataType.FILE)
            .setPath(pathToFile)
            .setEncryptedBlob(ciphertextBytes)
            .setSizeInBytes(ciphertextBytes.length);
    when(encryptionService.reencrypt(ciphertextBytes, pathToFile)).thenReturn(newCiphertextBytes);
    when(secureDataVersionDao.readSecureDataVersionByIdLocking(versionId))
        .thenReturn(Optional.of(record));

    secureDataService.reencryptDataVersion(versionId);

    verify(secureDataVersionDao).readSecureDataVersionByIdLocking(versionId);
    ArgumentCaptor<SecureDataVersionRecord> argument =
        ArgumentCaptor.forClass(SecureDataVersionRecord.class);
    verify(secureDataVersionDao).updateSecureDataVersion(argument.capture());
    assertEquals(now, argument.getValue().getLastRotatedTs());
    assertArrayEquals(newCiphertextBytes, argument.getValue().getEncryptedBlob());
  }

  @Test
  public void test_that_reencrypt_version_object_calls_reencrypt_string() {
    String newCiphertext = "fasdfkxcvasdff as";
    byte[] newCiphertextBytes = newCiphertext.getBytes(StandardCharsets.UTF_8);
    String pathToObject = "app/sdb/secret";
    String versionId = "version";
    OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
    when(dateTimeSupplier.get()).thenReturn(now);
    SecureDataVersionRecord record =
        new SecureDataVersionRecord()
            .setType(SecureDataType.OBJECT)
            .setPath(pathToObject)
            .setEncryptedBlob(ciphertextBytes)
            .setSizeInBytes(ciphertextBytes.length);
    when(encryptionService.reencrypt(ciphertext, pathToObject)).thenReturn(newCiphertext);
    when(secureDataVersionDao.readSecureDataVersionByIdLocking(versionId))
        .thenReturn(Optional.of(record));

    secureDataService.reencryptDataVersion(versionId);

    verify(secureDataVersionDao).readSecureDataVersionByIdLocking(versionId);
    ArgumentCaptor<SecureDataVersionRecord> argument =
        ArgumentCaptor.forClass(SecureDataVersionRecord.class);
    verify(secureDataVersionDao).updateSecureDataVersion(argument.capture());
    assertEquals(now, argument.getValue().getLastRotatedTs());
    assertArrayEquals(newCiphertextBytes, argument.getValue().getEncryptedBlob());
  }

  @Test
  public void test_that_deleteSecret_checks_type_before_deleting() {
    String pathToFile = "app/sdb/file.pem";
    SecureDataType type = SecureDataType.FILE;
    String principal = "principal";
    SecureDataRecord record =
        new SecureDataRecord()
            .setType(type)
            .setPath(pathToFile)
            .setEncryptedBlob(ciphertextBytes)
            .setSizeInBytes(ciphertextBytes.length);
    when(secureDataDao.readSecureDataByPathAndType(sdbId, pathToFile, type))
        .thenReturn(Optional.of(record));

    secureDataService.deleteSecret(sdbId, pathToFile, SecureDataType.FILE, principal);

    verify(secureDataDao).readSecureDataByPathAndType(sdbId, pathToFile, SecureDataType.FILE);
    verify(secureDataVersionDao)
        .writeSecureDataVersion(
            null,
            pathToFile,
            ciphertextBytes,
            SecureDataVersionRecord.SecretsAction.DELETE,
            type,
            ciphertextBytes.length,
            null,
            null,
            principal,
            null);
  }

  @Test
  public void testRotateDataKeys() {
    DataKeyInfo secureDataKeyInfo = getDataKeyInfo(Source.SECURE_DATA);
    DataKeyInfo secureDataVersionKeyInfo = getDataKeyInfo(Source.SECURE_DATA_VERSION);
    List<DataKeyInfo> dataKeyInfoList = new ArrayList<>();
    dataKeyInfoList.add(secureDataKeyInfo);
    dataKeyInfoList.add(secureDataVersionKeyInfo);
    dataKeyInfoList.add(secureDataKeyInfo);
    dataKeyInfoList.add(secureDataVersionKeyInfo);
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataDao.readSecureDataByIdLocking("id"))
        .thenReturn(Optional.of(secureDataRecord), Optional.empty());
    SecureDataVersionRecord secureDataVersionRecord = getSecureVersionRecord();
    Mockito.when(secureDataVersionDao.readSecureDataVersionByIdLocking("id"))
        .thenReturn(Optional.of(secureDataVersionRecord), Optional.empty());
    Mockito.when(
            secureDataDao.getOldestDataKeyInfo(Mockito.any(OffsetDateTime.class), Mockito.eq(10)))
        .thenReturn(dataKeyInfoList);
    Mockito.when(dateTimeSupplier.get()).thenReturn(OffsetDateTime.now());
    secureDataService.rotateDataKeys(10, 1000, 10);
    Mockito.verify(successCounter, Mockito.times(2)).inc();
    Mockito.verify(failureCounter, Mockito.times(2)).inc();
  }

  @Test
  public void testGetTotalNumberOfFiles() {
    Mockito.when(secureDataDao.countByType(SecureDataType.FILE)).thenReturn(10);
    int totalNumberOfFiles = secureDataService.getTotalNumberOfFiles();
    Assert.assertEquals(10, totalNumberOfFiles);
  }

  @Test
  public void testSecureMetadata() {
    SecureData secureData = getSecureData();
    Map<String, String> metadata = secureDataService.parseSecretMetadata(secureData);
    Assert.assertEquals(4, metadata.size());
    Assert.assertTrue(checkAllKeysPresentInMetadataMap(metadata));
    Assert.assertEquals(secureData.getCreatedBy(), metadata.get("created_by"));
    Assert.assertEquals(secureData.getCreatedTs().toString(), metadata.get("created_ts"));
    Assert.assertEquals(secureData.getLastUpdatedBy(), metadata.get("last_updated_by"));
    Assert.assertEquals(secureData.getLastUpdatedTs().toString(), metadata.get("last_updated_ts"));
  }

  @Test
  public void testSecureMetadataWhenAllValuesAreNull() {
    SecureData secureData = Mockito.mock(SecureData.class);
    Map<String, String> metadata = secureDataService.parseSecretMetadata(secureData);
    Assert.assertEquals(4, metadata.size());
    Assert.assertTrue(checkAllKeysPresentInMetadataMap(metadata));
    Assert.assertNull(metadata.get("created_by"));
    Assert.assertNull(metadata.get("created_ts"));
    Assert.assertNull(metadata.get("last_updated_by"));
    Assert.assertNull(metadata.get("last_updated_ts"));
  }

  @Test
  public void testGetSecureDataRecordForPath() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataDao.readSecureDataByPath("sdbId", "path"))
        .thenReturn(Optional.of(secureDataRecord));
    Optional<SecureDataRecord> secureDataRecordForPath =
        secureDataService.getSecureDataRecordForPath("sdbId", "path");
    assertEquals(secureDataRecord, secureDataRecordForPath.get());
  }

  @Test
  public void testGetTotalNumberOfKeyValuePairs() {
    Mockito.when(secureDataDao.getSumTopLevelKeyValuePairs()).thenReturn(54);
    int totalNumberOfKeyValuePairs = secureDataService.getTotalNumberOfKeyValuePairs();
    Assert.assertEquals(54, totalNumberOfKeyValuePairs);
  }

  @Test
  public void testGetTotalNumberOfDataNodes() {
    Mockito.when(secureDataDao.getTotalNumberOfDataNodes()).thenReturn(43);
    int totalNumberOfDataNodes = secureDataService.getTotalNumberOfDataNodes();
    Assert.assertEquals(43, totalNumberOfDataNodes);
  }

  @Test
  public void testGetPathsById() {
    Set<String> paths = new HashSet<>();
    paths.add("path");
    Mockito.when(secureDataDao.getPathsBySdbId("sdbId")).thenReturn(paths);
    Set<String> pathsBySdbId = secureDataService.getPathsBySdbId("sdbId");
    assertEquals(paths, pathsBySdbId);
    assertSame(paths, pathsBySdbId);
  }

  @Test
  public void testListSecureFilesSummaries() throws JsonProcessingException {
    Mockito.when(secureDataDao.countByPartialPathAndType("partialPath/", SecureDataType.FILE))
        .thenReturn(50);
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    List<SecureDataRecord> secureDataRecords = new ArrayList<>();
    secureDataRecords.add(secureDataRecord);
    Mockito.when(
            secureDataDao.listSecureDataByPartialPathAndType(
                "sdbId", "partialPath/", SecureDataType.FILE, 10, 10))
        .thenReturn(secureDataRecords);
    SecureFileSummaryResult secureFileSummaryResult =
        secureDataService.listSecureFilesSummaries("sdbId", "partialPath", 10, 10);

    List<SecureFileSummary> secureFileSummaries = secureFileSummaryResult.getSecureFileSummaries();
    Assert.assertEquals(1, secureFileSummaries.size());
    Assert.assertEquals(10, secureFileSummaryResult.getLimit());
    Assert.assertEquals(10, secureFileSummaryResult.getOffset());
    Assert.assertEquals(50, secureFileSummaryResult.getTotalFileCount());
    Assert.assertEquals(
        "sdbBoxId", secureFileSummaryResult.getSecureFileSummaries().get(0).getSdboxId());
    Assert.assertEquals("", secureFileSummaryResult.getSecureFileSummaries().get(0).getName());
    Assert.assertEquals(
        10, secureFileSummaryResult.getSecureFileSummaries().get(0).getSizeInBytes());
    Assert.assertEquals("path", secureFileSummaryResult.getSecureFileSummaries().get(0).getPath());
    Assert.assertEquals(
        "user", secureFileSummaryResult.getSecureFileSummaries().get(0).getCreatedBy());
    Assert.assertEquals(Integer.valueOf(20), secureFileSummaryResult.getNextOffset());
  }

  @Test
  public void testReadFileMetadataOnlyWhenFileIsNotPresent() {
    Mockito.when(secureDataDao.readMetadataByPathAndType("sdbId", "path", SecureDataType.FILE))
        .thenReturn(Optional.empty());
    Optional<SecureFileSummary> secureFileSummary =
        secureDataService.readFileMetadataOnly("sdbId", "path");
    Assert.assertFalse(secureFileSummary.isPresent());
  }

  @Test
  public void testReadFileMetadataOnlyWhenFileIsPresent() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    secureDataRecord.setPath("path/record");
    Mockito.when(secureDataDao.readMetadataByPathAndType("sdbId", "path", SecureDataType.FILE))
        .thenReturn(Optional.of(secureDataRecord));
    Optional<SecureFileSummary> secureFileSummaryOptional =
        secureDataService.readFileMetadataOnly("sdbId", "path");
    Assert.assertTrue(secureFileSummaryOptional.isPresent());
    SecureFileSummary secureFileSummary = secureFileSummaryOptional.get();
    Assert.assertEquals(secureDataRecord.getCreatedTs(), secureFileSummary.getCreatedTs());
    Assert.assertEquals(secureDataRecord.getCreatedBy(), secureFileSummary.getCreatedBy());
    Assert.assertEquals(secureDataRecord.getLastUpdatedTs(), secureFileSummary.getLastUpdatedTs());
    Assert.assertEquals(secureDataRecord.getLastUpdatedBy(), secureFileSummary.getLastUpdatedBy());
    Assert.assertEquals(secureDataRecord.getSizeInBytes(), secureFileSummary.getSizeInBytes());
    Assert.assertEquals(secureDataRecord.getPath(), secureFileSummary.getPath());
    Assert.assertEquals(secureDataRecord.getSdboxId(), secureFileSummary.getSdboxId());
    Assert.assertEquals("record", secureFileSummary.getName());
  }

  private boolean checkAllKeysPresentInMetadataMap(Map<String, String> metadata) {
    return metadata.containsKey("created_by")
        && metadata.containsKey("created_ts")
        && metadata.containsKey("last_updated_by")
        && metadata.containsKey("last_updated_ts");
  }

  private SecureData getSecureData() {
    SecureData secureData =
        SecureData.builder()
            .data("data")
            .id("id")
            .createdBy("createdBy")
            .createdTs(OffsetDateTime.MIN)
            .lastUpdatedBy("lastUpdatedBy")
            .lastUpdatedTs(OffsetDateTime.MAX)
            .build();
    return secureData;
  }

  private SecureDataVersionRecord getSecureVersionRecord() {
    SecureDataVersionRecord secureDataVersionRecord =
        new SecureDataVersionRecord()
            .setPath("path")
            .setType(SecureDataType.FILE)
            .setVersionCreatedTs(OffsetDateTime.MAX)
            .setVersionCreatedBy("user")
            .setActionTs(OffsetDateTime.MAX)
            .setAction("action")
            .setSizeInBytes(0)
            .setSdboxId("sdbBoId")
            .setLastRotatedTs(OffsetDateTime.MAX)
            .setEncryptedBlob("blob".getBytes(StandardCharsets.UTF_8))
            .setActionPrincipal("principal")
            .setId("id");
    return secureDataVersionRecord;
  }

  private SecureDataRecord getSecureDataRecord() {
    SecureDataRecord secureDataRecord =
        new SecureDataRecord()
            .setCreatedBy("user")
            .setCreatedTs(OffsetDateTime.MAX)
            .setId(0)
            .setPath("path")
            .setEncryptedBlob("blob".getBytes(StandardCharsets.UTF_8))
            .setLastRotatedTs(OffsetDateTime.MAX)
            .setLastUpdatedBy("user")
            .setLastUpdatedTs(OffsetDateTime.MAX)
            .setSdboxId("sdbBoxId")
            .setSizeInBytes(10)
            .setTopLevelKVCount(9)
            .setType(SecureDataType.FILE);
    return secureDataRecord;
  }

  private DataKeyInfo getDataKeyInfo(Source source) {
    DataKeyInfo dataKeyInfo =
        new DataKeyInfo().setId("id").setLastRotatedTs(OffsetDateTime.MAX).setSource(source);
    return dataKeyInfo;
  }
}
