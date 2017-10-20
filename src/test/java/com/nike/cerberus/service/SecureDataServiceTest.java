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

import com.google.common.collect.ImmutableSet;
import com.nike.cerberus.dao.SecureDataDao;
import com.nike.cerberus.record.SecureDataRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecureDataServiceTest {

    private String secret = "S3cr3t";
    private String encryptedPayload = "fasl;kej fasdf0978023 alskdf as";
    private String sdbId = UUID.randomUUID().toString();
    private String path = "super/important/secrets";
    private String partialPathWithoutTrailingSlash = "apps/checkout-service/api-keys";
    private String[] keysRes = new String[]{
        "apps/checkout-service/api-keys/signal-fx-api-key",
        "apps/checkout-service/api-keys/splunk-api-key"
    };

    @Mock private SecureDataDao secureDataDao;
    @Mock private EncryptionService encryptionService;

    private SecureDataService secureDataService;

    @Before
    public void before() {
        initMocks(this);

        secureDataService = new SecureDataService(secureDataDao, encryptionService);
    }

    @Test
    public void test_that_writeSecret_encrypts_the_payload_before_writing_to_data_store() {
        when(encryptionService.encrypt(secret)).thenReturn(encryptedPayload);

        secureDataService.writeSecret(sdbId, path, secret);
        verify(secureDataDao).writeSecureData(sdbId, path, encryptedPayload);
    }

    @Test
    public void test_that_readSecret_returns_empty_optional_if_dao_returns_nothing() {
        when(secureDataDao.readSecureDataByPath(path)).thenReturn(Optional.empty());

        Optional<String> result = secureDataService.readSecret(path);

        assertFalse(result.isPresent());
    }

    @Test
    public void test_that_readSecret_decrypts_the_paload_when_present() {
        when(secureDataDao.readSecureDataByPath(path))
                .thenReturn(Optional.of(new SecureDataRecord().setEncryptedBlob(encryptedPayload)));

        when(encryptionService.decrypt(encryptedPayload)).thenReturn(secret);

        Optional<String> result = secureDataService.readSecret(path);

        assertTrue(result.isPresent());
        assertTrue(result.get().equals(secret));
    }

    @Test
    public void test_that_listKeys_appends_a_slash_to_the_partial_path_if_not_present() {
        when(secureDataDao.getPathsByPartialPath(partialPathWithoutTrailingSlash + "/")).thenReturn(keysRes);
        secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPath(partialPathWithoutTrailingSlash + "/");
    }

    @Test
    public void test_that_listKeys_does_not_append_a_slash_to_the_partial_path_if_already_present() {
        when(secureDataDao.getPathsByPartialPath(partialPathWithoutTrailingSlash + "/")).thenReturn(keysRes);
        secureDataService.listKeys(partialPathWithoutTrailingSlash  + "/");
        verify(secureDataDao).getPathsByPartialPath(partialPathWithoutTrailingSlash + "/");
    }

    @Test
    public void test_that_listKeys_returns_empty_set_if_dao_returns_null() {
        when(secureDataDao.getPathsByPartialPath(partialPathWithoutTrailingSlash + "/")).thenReturn(null);
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash );
        verify(secureDataDao).getPathsByPartialPath(partialPathWithoutTrailingSlash + "/");

        assertTrue(res != null && res.isEmpty());
    }

    @Test
    public void test_that_listKeys_returns_empty_set_if_dao_returns_empty() {
        when(secureDataDao.getPathsByPartialPath(partialPathWithoutTrailingSlash + "/")).thenReturn(new String[] {});
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash );
        verify(secureDataDao).getPathsByPartialPath(partialPathWithoutTrailingSlash + "/");

        assertTrue(res != null && res.isEmpty());
    }

    @Test
    public void test_that_listKeys_returns_expected_set_of_keys() {
        when(secureDataDao.getPathsByPartialPath(partialPathWithoutTrailingSlash + "/")).thenReturn(keysRes);
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPath(partialPathWithoutTrailingSlash + "/");

        assertEquals("There should be 2 keys", 2, res.size());
        assertTrue("the list of keys should contain the 2 api keys", res.containsAll(ImmutableSet.of("signal-fx-api-key", "splunk-api-key")));
    }

    @Test
    public void test_that_listKeys_returns_expected_set_of_keys_with_sub_folder_paths_stripped_to_nearest_folder() {
        when(secureDataDao.getPathsByPartialPath(partialPathWithoutTrailingSlash + "/")).thenReturn(new String[]{
                "apps/checkout-service/api-keys/sub-folder/some-different-key"
        });
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPath(partialPathWithoutTrailingSlash + "/");

        assertEquals("There should be 1 key", 1, res.size());
        assertTrue(res.contains("sub-folder/"));
    }

    @Test
    public void test_that_listKeys_returns_expected_set_of_keys_with_sub_folder_paths_stripped_to_nearest_folder_and_contains_key_without_slash_if_present() {
        when(secureDataDao.getPathsByPartialPath(partialPathWithoutTrailingSlash + "/")).thenReturn(new String[]{
                "apps/checkout-service/api-keys/sub-folder/some-different-key",
                "apps/checkout-service/api-keys/sub-folder"
        });
        Set<String> res = secureDataService.listKeys(partialPathWithoutTrailingSlash);
        verify(secureDataDao).getPathsByPartialPath(partialPathWithoutTrailingSlash + "/");

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
        secureDataService.deleteSecret(partialPathWithoutTrailingSlash);
        verify(secureDataDao).deleteSecret(partialPathWithoutTrailingSlash);
    }

}
