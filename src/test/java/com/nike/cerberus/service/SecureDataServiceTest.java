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
 */

package com.nike.cerberus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.dao.SecureDataDao;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.SecureData;
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Maps;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.nike.cerberus.service.AuthenticationService.SYSTEM_USER;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecureDataServiceTest {

    private String secret = "{\"k1\":\"val\",\"k2\":\"val\"}";
    private byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    private String principal = SYSTEM_USER;
    private String encryptedPayload = "fasl;kej fasdf0978023 alskdf as";
    private String sdbId = UUID.randomUUID().toString();
    private String path = "super/important/secrets";
    private String partialPathWithoutTrailingSlash = "apps/checkout-service/api-keys";
    private String[] keysRes = new String[]{
        "apps/checkout-service/api-keys/signal-fx-api-key",
        "apps/checkout-service/api-keys/splunk-api-key"
    };
    @Mock private SecureDataRecord secureDataRecord;

    @Mock private SecureDataDao secureDataDao;
    @Mock private EncryptionService encryptionService;
    @Mock private DateTimeSupplier dateTimeSupplier;
    @Mock private SecureDataVersionDao secureDataVersionDao;
    private ObjectMapper objectMapper;

    private SecureDataService secureDataService;

    @Before
    public void before() {
        initMocks(this);
        objectMapper = new ObjectMapper();
        secureDataService = new SecureDataService(secureDataDao, encryptionService, objectMapper, dateTimeSupplier, secureDataVersionDao);
    }

    @After
    public void after() {
        reset(secureDataDao, encryptionService, dateTimeSupplier);
    }

    @Test
    public void test_that_writeSecret_encrypts_the_payload_and_calls_update() {
        byte[] encryptedBytes = encryptedPayload.getBytes(StandardCharsets.UTF_8);
        when(encryptionService.encrypt(secret.getBytes(), path)).thenReturn(encryptedBytes);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        when(dateTimeSupplier.get()).thenReturn(now);

        when(secureDataRecord.getType()).thenReturn(SecureDataType.OBJECT);
        when(secureDataRecord.getCreatedBy()).thenReturn(SYSTEM_USER);
        when(secureDataRecord.getCreatedTs()).thenReturn(now);
        when(secureDataDao.readSecureDataByPath(path)).thenReturn(Optional.of(secureDataRecord));

        secureDataService.writeSecret(sdbId, path, secret, principal);
        verify(secureDataDao).updateSecureData(
                sdbId,
                path,
                encryptedBytes,
                2,
                SecureDataType.OBJECT,
                secretBytes.length,
                SYSTEM_USER,
                now,
                SYSTEM_USER,
                now);
    }

    @Test
    public void test_that_readSecret_returns_empty_optional_if_dao_returns_nothing() {
        when(secureDataDao.readSecureDataByPathAndType(path, SecureDataType.OBJECT)).thenReturn(Optional.empty());

        Optional<SecureData> result = secureDataService.readSecret(path);

        assertFalse(result.isPresent());
    }

    @Test
    public void test_that_readSecret_decrypts_the_payload_when_present() {
        when(secureDataDao.readSecureDataByPathAndType(path, SecureDataType.OBJECT))
                .thenReturn(Optional.of(new SecureDataRecord().setEncryptedBlob(encryptedPayload.getBytes())));

        when(encryptionService.decrypt(encryptedPayload.getBytes(), path)).thenReturn(secretBytes);

        Optional<SecureData> result = secureDataService.readSecret(path);

        assertTrue(result.isPresent());
        assertTrue(result.get().getData().equals(secret));
    }

    @Test
    public void test_that_listKeys_appends_a_slash_to_the_partial_path_if_not_present() {
        when(secureDataDao.getPathsByPartialPathAndType(
                partialPathWithoutTrailingSlash + "/",
                SecureDataType.OBJECT)).thenReturn(keysRes);
        secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPathAndType(partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);
    }

    @Test
    public void test_that_listKeys_does_not_append_a_slash_to_the_partial_path_if_already_present() {
        when(secureDataDao.getPathsByPartialPathAndType(
                partialPathWithoutTrailingSlash + "/",
                SecureDataType.OBJECT)).thenReturn(keysRes);
        secureDataService.listKeys(partialPathWithoutTrailingSlash  + "/");
        verify(secureDataDao).getPathsByPartialPathAndType(partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);
    }

    @Test
    public void test_that_listKeys_returns_empty_set_if_dao_returns_null() {
        when(secureDataDao.getPathsByPartialPathAndType(
                partialPathWithoutTrailingSlash + "/",
                SecureDataType.OBJECT)).thenReturn(null);
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash );
        verify(secureDataDao).getPathsByPartialPathAndType(partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

        assertTrue(res != null && res.isEmpty());
    }

    @Test
    public void test_that_listKeys_returns_empty_set_if_dao_returns_empty() {
        when(secureDataDao.getPathsByPartialPathAndType(
                partialPathWithoutTrailingSlash + "/",
                SecureDataType.OBJECT)).thenReturn(new String[] {});
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash );
        verify(secureDataDao).getPathsByPartialPathAndType(partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

        assertTrue(res != null && res.isEmpty());
    }

    @Test
    public void test_that_listKeys_returns_expected_set_of_keys() {
        when(secureDataDao.getPathsByPartialPathAndType(
                partialPathWithoutTrailingSlash + "/",
                SecureDataType.OBJECT)).thenReturn(keysRes);
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPathAndType(partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

        assertEquals("There should be 2 keys", 2, res.size());
        assertTrue("the list of keys should contain the 2 api keys", res.containsAll(ImmutableSet.of("signal-fx-api-key", "splunk-api-key")));
    }

    @Test
    public void test_that_listKeys_returns_expected_set_of_keys_with_sub_folder_paths_stripped_to_nearest_folder() {
        when(secureDataDao.getPathsByPartialPathAndType(
                partialPathWithoutTrailingSlash + "/",
                SecureDataType.OBJECT)).thenReturn(new String[]{
                "apps/checkout-service/api-keys/sub-folder/some-different-key"
        });
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPathAndType(partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

        assertEquals("There should be 1 key", 1, res.size());
        assertTrue(res.contains("sub-folder/"));
    }

    @Test
    public void test_that_listKeys_returns_expected_set_of_keys_with_sub_folder_paths_stripped_to_nearest_folder_and_contains_key_without_slash_if_present() {
        when(secureDataDao.getPathsByPartialPathAndType(
                partialPathWithoutTrailingSlash + "/",
                SecureDataType.OBJECT)).thenReturn(new String[]{
                "apps/checkout-service/api-keys/sub-folder/some-different-key",
                "apps/checkout-service/api-keys/sub-folder"
        });
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPathAndType(partialPathWithoutTrailingSlash + "/", SecureDataType.OBJECT);

        assertEquals("There should be 2 key", 2, res.size());
        assertTrue(res.contains("sub-folder/"));
        assertTrue(res.contains("sub-folder"));
    }

    @Test
    public void test_that_deleteAllSecretsThatStartWithGivenPartialPath_proxies_to_dao() {
        secureDataService.deleteAllSecretsThatStartWithGivenPartialPath(partialPathWithoutTrailingSlash);
        verify(secureDataDao).deleteAllSecretsThatStartWithGivenPartialPath(partialPathWithoutTrailingSlash);
    }

    @Test
    public void test_that_deleteSecret_proxies_to_dao() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        when(dateTimeSupplier.get()).thenReturn(now);
        when(secureDataDao.readSecureDataByPath(partialPathWithoutTrailingSlash)).thenReturn(Optional.of(secureDataRecord));

        secureDataService.deleteSecret(partialPathWithoutTrailingSlash, principal);
        verify(secureDataDao).deleteSecret(partialPathWithoutTrailingSlash);
    }

    @Test
    public void test_that_restoreSdbSecrets_proxies_to_dao() throws JsonProcessingException {
        String sdbId = "sdb-id";
        String path = "secret/path/one";
        String sdbPath = "category/secret/path/one";
        String secretPath = StringUtils.substringAfter(sdbPath, "/");

        Map<String, Map<String, Object>> data = Maps.newHashMap();
        Map<String, Object> keyValuePairs = Maps.newHashMap();
        keyValuePairs.put("foo", "bar");
        data.put(sdbPath, keyValuePairs);

        String unencryptedPayload = new ObjectMapper().writeValueAsString(keyValuePairs);
        byte[] unencryptedBytes = unencryptedPayload.getBytes(StandardCharsets.UTF_8);
        String encryptedPayload = "encrypted payload";
        byte[] encryptedBytes = encryptedPayload.getBytes(StandardCharsets.UTF_8);
        when(encryptionService.encrypt(unencryptedBytes, secretPath)).thenReturn(encryptedBytes);

        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        when(dateTimeSupplier.get()).thenReturn(now);

        when(secureDataDao.readSecureDataByPath(path)).thenReturn(Optional.empty());

        secureDataService.restoreSdbSecrets(sdbId, data, principal);
        verify(secureDataDao).writeSecureData(
                sdbId,
                secretPath,
                encryptedBytes,
                1,
                SecureDataType.OBJECT,
                unencryptedBytes.length,
                principal,
                now,
                principal,
                now);
    }

    @Test
    public void test_that_getTopLevelKVPairCount_returns_proper_count() {
        assertEquals(2, secureDataService
                .getTopLevelKVPairCount("{\"k1\":\"val\",\"k2\":\"val\"}"));
    }

    @Test
    public void test_that_getTopLevelKVPairCount_returns_1_on_failure() {
        assertEquals(1, secureDataService
                .getTopLevelKVPairCount("{[\"1\",\"2\"]}"));
    }

    @Test
    public void test_that_secureDataHasBeenUpdated_returns_true_when_updated() {
        OffsetDateTime now = OffsetDateTime.now();
        SecureDataRecord differentPrincipals = new SecureDataRecord()
                .setCreatedBy("principal")
                .setCreatedTs(now)
                .setLastUpdatedBy("different principal")
                .setLastUpdatedTs(now);

        SecureDataRecord differentTs = new SecureDataRecord()
                .setCreatedBy("principal")
                .setCreatedTs(now)
                .setLastUpdatedBy("principal")
                .setLastUpdatedTs(OffsetDateTime.now());

        SecureDataRecord differentPrincipalsAndTs = new SecureDataRecord()
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
        SecureDataRecord secureDataRecord = new SecureDataRecord()
                .setCreatedBy(principal)
                .setCreatedTs(now)
                .setLastUpdatedBy(principal)
                .setLastUpdatedTs(now);

        assertFalse(secureDataService.secureDataHasBeenUpdated(secureDataRecord));
    }
}
