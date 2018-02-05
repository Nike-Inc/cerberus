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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.SecureDataDao;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SecureDataService {

    private final SecureDataDao secureDataDao;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final DateTimeSupplier dateTimeSupplier;
    private final SecureDataVersionDao secureDataVersionDao;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public SecureDataService(SecureDataDao secureDataDao,
                             EncryptionService encryptionService,
                             ObjectMapper objectMapper,
                             DateTimeSupplier dateTimeSupplier,
                             SecureDataVersionDao secureDataVersionDao) {
        this.secureDataDao = secureDataDao;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
        this.dateTimeSupplier = dateTimeSupplier;
        this.secureDataVersionDao = secureDataVersionDao;
    }

    @Transactional
    public void writeSecret(String sdbId, String path, String plainTextPayload, String principal) {
        log.debug("Writing secure data: SDB ID: {}, Path: {}", sdbId, path);

        String encryptedPayload = encryptionService.encrypt(plainTextPayload, path);
        int topLevelKVPairCount = getTopLevelKVPairCount(plainTextPayload);
        OffsetDateTime now = dateTimeSupplier.get();

        // this extra DB call is necessary to distinguish between create and update (for auditing) because
        // both create and update are lumped together under HTTP method "POST"
        Optional<SecureDataRecord> secureDataRecordOpt = secureDataDao.readSecureDataByPath(path);
        if (secureDataRecordOpt.isPresent()) {
            SecureDataRecord secureData = secureDataRecordOpt.get();

            secureDataVersionDao.writeSecureDataVersion(sdbId, path, encryptedPayload,
                    SecureDataVersionRecord.SecretsAction.UPDATE,
                    secureData.getLastUpdatedBy(),
                    secureData.getLastUpdatedTs(),
                    principal,
                    now
            );

            secureDataDao.updateSecureData(sdbId, path, encryptedPayload, topLevelKVPairCount,
                    secureData.getCreatedBy(),
                    secureData.getCreatedTs(),
                    principal,
                    now);

        } else {
            secureDataDao.writeSecureData(sdbId, path, encryptedPayload, topLevelKVPairCount, principal, now, principal, now);
        }
    }

    /**
     * Attempts to deserialize the plain text payload and determine how many key value pairs it contains, in order to
     * capture this metadata metric for KPI reporting.
     *
     * @param plainTextPayload the json payload
     * @return The number of top level key value pairs the json payload contains
     */
    protected int getTopLevelKVPairCount(String plainTextPayload) {
        int kvCount = 1;
        try {
            Map<String, Object> data = objectMapper.readValue(plainTextPayload, new TypeReference<HashMap<String, Object>>() {});
            kvCount = data.size();
        } catch (Exception e) {
            log.error("Failed to get top level kv pair count metric from plainTextPayload", e);
        }
        return kvCount;
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

    public void restoreSdbSecrets(String sdbId, Map<String, Map<String, Object>> data, String principal) {
        data.forEach((String path, Map<String, Object> secretsData) -> {
            String pathWithoutCategory = StringUtils.substringAfter(path, "/");
            try {
                String plainTextSecrets = objectMapper.writeValueAsString(secretsData);
                writeSecret(sdbId, pathWithoutCategory, plainTextSecrets, principal);
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
    public void deleteSecret(String path, String principal) {
        OffsetDateTime now = dateTimeSupplier.get();
        SecureDataRecord secureDataRecord = secureDataDao.readSecureDataByPath(path)
                .orElseThrow(() ->
                        new ApiException(DefaultApiError.ENTITY_NOT_FOUND)
                );

        secureDataVersionDao.writeSecureDataVersion(
                secureDataRecord.getSdboxId(),
                secureDataRecord.getPath(),
                secureDataRecord.getEncryptedBlob(),
                SecureDataVersionRecord.SecretsAction.DELETE,
                secureDataRecord.getLastUpdatedBy(),
                secureDataRecord.getLastUpdatedTs(),
                principal,
                now
        );

        secureDataDao.deleteSecret(path);
    }

    public int getTotalNumberOfKeyValuePairs() {
        return secureDataDao.getSumTopLevelKeyValuePairs();
    }
}
