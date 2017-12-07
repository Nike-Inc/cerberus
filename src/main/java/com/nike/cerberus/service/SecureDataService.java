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
import com.nike.cerberus.dao.SecureDataDao;
import com.nike.cerberus.record.SecureDataRecord;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SecureDataService {

    private final SecureDataDao secureDataDao;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public SecureDataService(SecureDataDao secureDataDao,
                             EncryptionService encryptionService,
                             ObjectMapper objectMapper) {
        this.secureDataDao = secureDataDao;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void writeSecret(String sdbId, String path, String plainTextPayload) {
        log.debug("Writing secure data: SDB ID: {}, Path: {}", sdbId, path);

        String encryptedPayload = encryptionService.encrypt(plainTextPayload, path);

        secureDataDao.writeSecureData(sdbId, path, encryptedPayload);
    }

    public Optional<String> readSecret(String path) {
        log.debug("Reading secure data: Path: {}", path);
        Optional<SecureDataRecord> secureDataRecord = secureDataDao.readSecureDataByPath(path);
        if (! secureDataRecord.isPresent()) {
            return Optional.empty();
        }

        String encryptedBlob = secureDataRecord.get().getEncryptedBlob();
        String plainText = encryptionService.decrypt(encryptedBlob, path);

        return Optional.of(plainText);
    }

    public void restoreSdbSecrets(String sdbId, Map<String, Map<String, Object>> data) {
        data.forEach((String path, Map<String, Object> secretsData) -> {
            String pathWithoutCategory = removeCategoryFromPath(path);
            try {
                String plainTextSecrets = objectMapper.writeValueAsString(secretsData);
                writeSecret(sdbId, pathWithoutCategory, plainTextSecrets);
            } catch (JsonProcessingException jpe) {
                throw new RuntimeException("Failed to parse secrets data for SDB ID: " + sdbId, jpe);
            }
        });
    }

    /**
     * Method to list keys in the virtual tree structure
     * This method is designed to mimic the Vault ?list=true functionality to maintain the existing API contract.
     *
     * ex: given the following tree structure
     * app/foo/bar/bam
     * app/foo/bam
     * app/bam/foo
     *
     * if you call listKeys with partialPath = "app/foo" or "app/foo/" you will receive the following set of keys
     * ["bar/", "bam"]
     *
     * @param partialPath path to a node in the data structure that potentially has children
     * @return Array of keys if the key is a data node it will not end with "/"
     */
    public Set<String> listKeys(String partialPath) {
        if (! partialPath.endsWith("/")) {
            partialPath = partialPath + "/";
        }

        Set<String> keys = new HashSet<>();
        String[] pArray = secureDataDao.getPathsByPartialPath(partialPath);
        if (pArray == null || pArray.length < 1) {
            return keys;
        }

        for (int i = 0; i < pArray.length; i++) {
            String fullPath = pArray[i];
            keys.add(StringUtils
                    .removeStart(fullPath, partialPath)
                    .replaceAll("\\/.*$", "/")
            );
        }
        return keys;
    }

    public int getTotalNumberOfDataNodes() {
        return secureDataDao.getTotalNumberOfDataNodes();
    }

    public void deleteAllSecretsInSdb(String sdbPath) {
        String pathWithoutCategory = removeCategoryFromPath(sdbPath);
        deleteAllSecretsThatStartWithGivenPartialPath(pathWithoutCategory);
    }

    /**
     * Deletes all of the secure data from stored at the safe deposit box's partial path.
     *
     * ex: assume the following paths have secure data
     * app/test/foo/1
     * app/test/foo/2
     * app/test/foo/3
     * app/test/foo/4/bar
     * app/test/foo/4/bam
     * app/test/bam
     * and you call this method with path = app/test/foo/
     * all the above secrets except app/test/bam will be deleted
     *
     * @param subPath The sub path to delete all secrets that have paths that start with
     */
    @Transactional
    public void deleteAllSecretsThatStartWithGivenPartialPath(String subPath) {
        log.warn("Deleting all secrets under path: {}", subPath);
        secureDataDao.deleteAllSecretsThatStartWithGivenPartialPath(subPath);
    }

    /**
     * Deletes secure data at a given path
     *
     * @param path The sub path to delete all secrets that have paths that start with
     */
    public void deleteSecret(String path) {
        secureDataDao.deleteSecret(path);
    }

    private String removeCategoryFromPath(String sdbPath) {
        String pathSeparator = "/";
        String pathWithoutTrailingSlash = StringUtils.removeEnd(sdbPath, pathSeparator);
        return sdbPath.contains(pathWithoutTrailingSlash) ?
                StringUtils.substringAfter(pathWithoutTrailingSlash, "/") :
                sdbPath;
    }
}
