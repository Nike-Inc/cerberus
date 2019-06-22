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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.SecureDataDao;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.SecureData;
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.domain.SecureFile;
import com.nike.cerberus.domain.SecureFileSummary;
import com.nike.cerberus.domain.SecureFileSummaryResult;
import com.nike.cerberus.domain.Source;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.DataKeyInfo;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

        int topLevelKVPairCount = getTopLevelKVPairCount(plainTextPayload);
        byte[] plaintextBytes = plainTextPayload.getBytes(StandardCharsets.UTF_8);
        int sizeInBytes = plaintextBytes.length;

        // Make sure to encrypt payload as a String, then convert to bytes to mimic the previous encryption flow
        String ciphertext = encryptionService.encrypt(plainTextPayload, path);
        byte[] ciphertextBytes = ciphertext.getBytes(StandardCharsets.UTF_8);
        OffsetDateTime now = dateTimeSupplier.get();

        // Fetch the current version if there is one, so that on update it can be moved to the versions table
        Optional<SecureDataRecord> secureDataRecordOpt = secureDataDao.readSecureDataByPath(sdbId, path);
        if (secureDataRecordOpt.isPresent()) {
            SecureDataRecord secureData = secureDataRecordOpt.get();
            if (secureData.getType() != SecureDataType.OBJECT) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.INVALID_SECURE_DATA_TYPE)
                        .build();
            }

            secureDataVersionDao.writeSecureDataVersion(sdbId, path, secureData.getEncryptedBlob(),
                    SecureDataVersionRecord.SecretsAction.UPDATE,
                    SecureDataType.OBJECT,
                    sizeInBytes,
                    secureData.getLastUpdatedBy(),
                    secureData.getLastUpdatedTs(),
                    principal,
                    now
            );

            secureDataDao.updateSecureData(sdbId, path, ciphertextBytes, topLevelKVPairCount,
                    SecureDataType.OBJECT,
                    sizeInBytes,
                    secureData.getCreatedBy(),
                    secureData.getCreatedTs(),
                    principal,
                    now,
                    secureData.getLastRotatedTs());

        } else {
            secureDataDao.writeSecureData(sdbId, path, ciphertextBytes, topLevelKVPairCount, SecureDataType.OBJECT,
                    sizeInBytes,
                    principal,
                    now,
                    principal,
                    now);
        }
    }

    @Transactional
    public void writeSecureFile(String sdbId, String path, byte[] bytes, int sizeInBytes, String principal) {
        log.debug("Writing secure file: SDB ID: {}, Path: {}", sdbId, path);

        byte[] ciphertextBytes = encryptionService.encrypt(bytes, path);
        int topLevelKVPairCount = 0;
        OffsetDateTime now = dateTimeSupplier.get();

        // Fetch the current version if there is one, so that on update it can be moved to the versions table
        Optional<SecureDataRecord> secureDataRecordOpt = secureDataDao.readSecureDataByPath(sdbId, path);
        if (secureDataRecordOpt.isPresent()) {
            SecureDataRecord secureData = secureDataRecordOpt.get();
            if (secureData.getType() != SecureDataType.FILE) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.INVALID_SECURE_DATA_TYPE)
                        .build();
            }

            secureDataVersionDao.writeSecureDataVersion(sdbId, path, secureData.getEncryptedBlob(),
                    SecureDataVersionRecord.SecretsAction.UPDATE,
                    SecureDataType.FILE,
                    sizeInBytes,
                    secureData.getLastUpdatedBy(),
                    secureData.getLastUpdatedTs(),
                    principal,
                    now
            );

            secureDataDao.updateSecureData(sdbId, path, ciphertextBytes, topLevelKVPairCount,
                    SecureDataType.FILE,
                    sizeInBytes,
                    secureData.getCreatedBy(),
                    secureData.getCreatedTs(),
                    principal,
                    now,
                    secureData.getLastRotatedTs());

        } else {
            secureDataDao.writeSecureData(sdbId, path, ciphertextBytes, topLevelKVPairCount, SecureDataType.FILE,
                    sizeInBytes,
                    principal,
                    now,
                    principal,
                    now);
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

    public Optional<SecureData> readSecret(String sdbId, String path) {
        log.debug("Reading secure data: Path: {}", path);
        Optional<SecureDataRecord> secureDataRecordOpt = secureDataDao.readSecureDataByPathAndType(sdbId, path, SecureDataType.OBJECT);
        if (! secureDataRecordOpt.isPresent()) {
            return Optional.empty();
        }

        SecureDataRecord secureDataRecord = secureDataRecordOpt.get();
        byte[] ciphertextBytes = secureDataRecordOpt.get().getEncryptedBlob();

        // Make sure to convert ciphertext to a String first, then decrypt, because Amazon throws an
        // error if the ciphertext was encrypted as a String, but is not decrypted as a String.
        String ciphertext = new String(ciphertextBytes, StandardCharsets.UTF_8);
        String plaintext = encryptionService.decrypt(ciphertext, path);
        SecureData secureData = new SecureData()
                .setCreatedBy(secureDataRecord.getCreatedBy())
                .setCreatedTs(secureDataRecord.getCreatedTs())
                .setData(plaintext)
                .setLastUpdatedBy(secureDataRecord.getLastUpdatedBy())
                .setLastUpdatedTs(secureDataRecord.getLastUpdatedTs())
                .setPath(secureDataRecord.getPath())
                .setSdboxId(secureDataRecord.getSdboxId());

        return Optional.of(secureData);
    }

    public Optional<SecureFile> readFile(String sdbId, String path) {
        log.debug("Reading secure file: Path: {}", path);
        Optional<SecureDataRecord> secureDataRecordOpt = secureDataDao.readSecureDataByPathAndType(sdbId, path, SecureDataType.FILE);
        if (! secureDataRecordOpt.isPresent()) {
            return Optional.empty();
        }

        SecureDataRecord secureDataRecord = secureDataRecordOpt.get();
        byte[] plaintextBytes = encryptionService.decrypt(secureDataRecord.getEncryptedBlob(), secureDataRecord.getPath());

        SecureFile secureFile = new SecureFile()
                .setCreatedBy(secureDataRecord.getCreatedBy())
                .setCreatedTs(secureDataRecord.getCreatedTs())
                .setData(plaintextBytes)
                .setSizeInBytes(plaintextBytes.length)
                .setName(StringUtils.substringAfterLast(secureDataRecord.getPath(), "/"))
                .setLastUpdatedBy(secureDataRecord.getLastUpdatedBy())
                .setLastUpdatedTs(secureDataRecord.getLastUpdatedTs())
                .setPath(secureDataRecord.getPath())
                .setSdboxId(secureDataRecord.getSdboxId());

        return Optional.of(secureFile);
    }

    public Optional<SecureFileSummary> readFileMetadataOnly(String sdbId, String path) {
        log.debug("Reading secure file metadata: Path: {}", path);
        Optional<SecureDataRecord> secureDataRecordOpt = secureDataDao.readMetadataByPathAndType(sdbId, path, SecureDataType.FILE);
        if (! secureDataRecordOpt.isPresent()) {
            return Optional.empty();
        }

        SecureDataRecord secureDataRecord = secureDataRecordOpt.get();
        SecureFileSummary secureFile = new SecureFileSummary()
                .setCreatedBy(secureDataRecord.getCreatedBy())
                .setCreatedTs(secureDataRecord.getCreatedTs())
                .setSizeInBytes(secureDataRecord.getSizeInBytes())
                .setName(StringUtils.substringAfterLast(secureDataRecord.getPath(), "/"))
                .setLastUpdatedBy(secureDataRecord.getLastUpdatedBy())
                .setLastUpdatedTs(secureDataRecord.getLastUpdatedTs())
                .setPath(secureDataRecord.getPath())
                .setSdboxId(secureDataRecord.getSdboxId());

        return Optional.of(secureFile);
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
    public Set<String> listKeys(String sdbId, String partialPath) {
        if (! partialPath.endsWith("/")) {
            partialPath = partialPath + "/";
        }

        Set<String> keys = new HashSet<>();
        String[] pArray = secureDataDao.getPathsByPartialPathAndType(sdbId, partialPath, SecureDataType.OBJECT);
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

    /**
     * Method to list file metadata in the virtual tree structure
     * This method is designed to mimic the Vault ?list=true functionality to maintain the existing API contract.
     *
     * ex: given the following tree structure
     * app/foo/bar/bam
     * app/foo/bam
     * app/bam/foo
     *
     * if you call listSecureFilesSummaries with partialPath = "app/foo" or "app/foo/" you will receive files matching the following set of keys
     * ["bar/", "bam"]
     *
     * @param partialPath path to a node in the data structure that potentially has children
     * @return Array of keys if the key is a data node it will not end with "/"
     */
    public SecureFileSummaryResult listSecureFilesSummaries(String sdbId, String partialPath, int limit, int offset) {
        if (!partialPath.endsWith("/")) {
            partialPath = partialPath + "/";
        }

        int totalNumFiles = secureDataDao.countByPartialPathAndType(partialPath, SecureDataType.FILE);
        List<SecureFileSummary> fileSummaries = Lists.newArrayList();
        List<SecureDataRecord> secureDataRecords = secureDataDao.listSecureDataByPartialPathAndType(sdbId, partialPath, SecureDataType.FILE, limit, offset);
        secureDataRecords.forEach(secureDataRecord -> {
            fileSummaries.add(new SecureFileSummary()
                    .setCreatedBy(secureDataRecord.getCreatedBy())
                    .setCreatedTs(secureDataRecord.getCreatedTs())
                    .setSizeInBytes(secureDataRecord.getSizeInBytes())
                    .setName(StringUtils.substringAfterLast(secureDataRecord.getPath(), "/"))
                    .setLastUpdatedBy(secureDataRecord.getLastUpdatedBy())
                    .setLastUpdatedTs(secureDataRecord.getLastUpdatedTs())
                    .setPath(secureDataRecord.getPath())
                    .setSdboxId(secureDataRecord.getSdboxId()));
        });

        SecureFileSummaryResult result = new SecureFileSummaryResult();
        result.setLimit(limit);
        result.setOffset(offset);
        result.setSecureFileSummaries(fileSummaries);
        result.setFileCountInResult(fileSummaries.size());
        result.setTotalFileCount(totalNumFiles);
        result.setHasNext(result.getTotalFileCount() > (offset + limit));
        if (result.isHasNext()) {
            result.setNextOffset(offset + limit);
        }

        return result;
    }

    public Set<String> getPathsBySdbId(String sdbId) {
        return secureDataDao.getPathsBySdbId(sdbId);
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
    public void deleteAllSecretsThatStartWithGivenPartialPath(String sdbId, String subPath) {
        log.warn("Deleting all secrets under path: {} for sdbId: {}", subPath, sdbId);
        secureDataDao.deleteAllSecretsThatStartWithGivenPartialPath(sdbId, subPath);
    }

    /**
     * Deletes secure data at a given path
     *
     * @param path The sub path to delete all secrets that have paths that start with
     */
    public void deleteSecret(String sdbId, String path, SecureDataType type, String principal) {
        OffsetDateTime now = dateTimeSupplier.get();
        SecureDataRecord secureDataRecord = secureDataDao.readSecureDataByPathAndType(sdbId, path, type)
                .orElseThrow(() ->
                        new ApiException(DefaultApiError.ENTITY_NOT_FOUND)
                );

        secureDataVersionDao.writeSecureDataVersion(
                secureDataRecord.getSdboxId(),
                secureDataRecord.getPath(),
                secureDataRecord.getEncryptedBlob(),
                SecureDataVersionRecord.SecretsAction.DELETE,
                secureDataRecord.getType(),
                secureDataRecord.getSizeInBytes(),
                secureDataRecord.getLastUpdatedBy(),
                secureDataRecord.getLastUpdatedTs(),
                principal,
                now
        );

        secureDataDao.deleteSecret(sdbId, path);
    }

    public int getTotalNumberOfKeyValuePairs() {
        return secureDataDao.getSumTopLevelKeyValuePairs();
    }

    /**
     * @return  True if the secret has ever been updated, false if it has not.
     */
    boolean secureDataHasBeenUpdated(SecureDataRecord secureDataRecord) {
        boolean createdBySameAsUpdatedBy = secureDataRecord.getCreatedBy().equals(secureDataRecord.getLastUpdatedBy());
        boolean createdTsSameAsUpdatedTs = secureDataRecord.getCreatedTs() == secureDataRecord.getLastUpdatedTs();

        return ! (createdBySameAsUpdatedBy && createdTsSameAsUpdatedTs);
    }

    public Optional<SecureDataRecord> getSecureDataRecordForPath(String sdbId, String path) {
        return secureDataDao.readSecureDataByPath(sdbId, path);
    }

    public Map<String, String> parseSecretMetadata(SecureData secureData) {
        Map<String, String> secretMetadata = Maps.newHashMap();

        secretMetadata.put("created_by", secureData.getCreatedBy());
        secretMetadata.put("created_ts", secureData.getCreatedTs().toString());
        secretMetadata.put("last_updated_by", secureData.getLastUpdatedBy());
        secretMetadata.put("last_updated_ts", secureData.getLastUpdatedTs().toString());

        return secretMetadata;
    }

    public int getTotalNumberOfFiles() {
        return secureDataDao.countByType(SecureDataType.FILE);
    }

    public void rotateDataKeys(int numberOfKeys, int pauseTimeInMillis, int rotationIntervalInDays) {
        int[] counter = new int[2];
        OffsetDateTime now = dateTimeSupplier.get();
        OffsetDateTime expiredTs = now.minusDays(rotationIntervalInDays);
        List<DataKeyInfo> oldestDataKeyInfos = secureDataDao.getOldestDataKeyInfo(expiredTs, numberOfKeys);

        // Prioritize latest version
        for (DataKeyInfo dataKeyInfo: oldestDataKeyInfos) {
            Source source = dataKeyInfo.getSource();
            if (source == Source.SECURE_DATA) {
                String id = dataKeyInfo.getId();
                reencryptData(id);
                counter[0]++;
            } else if (source == Source.SECURE_DATA_VERSION) {
                String versionId = dataKeyInfo.getId();
                reencryptDataVersion(versionId);
                counter[1]++;
            }

            try {
                Thread.sleep(pauseTimeInMillis);
            } catch (InterruptedException e) {
                log.error("Failed to sleep between re-encryption", e);
            }
        }
        log.info("Re-encrypted {} secure data and {} secure data version entries.", counter[0], counter[1]);

    }

    @Transactional
    protected void reencryptData(String id) {
        log.debug("Re-encrypting secure data/file: Path: {}", id);
        Optional<SecureDataRecord> secureDataRecordOpt = secureDataDao.readSecureDataByIdLocking(id);
        if (! secureDataRecordOpt.isPresent()) {
            throw new IllegalArgumentException("No secure data found for id: " + id);
        }

        SecureDataRecord secureDataRecord = secureDataRecordOpt.get();
        byte[] ciphertextBytes = secureDataRecord.getEncryptedBlob();
        byte[] reencryptedBytes = reencrypt(secureDataRecord.getType(), ciphertextBytes, secureDataRecord.getPath());
        OffsetDateTime now = dateTimeSupplier.get();

        secureDataRecord.setLastRotatedTs(now);
        secureDataRecord.setEncryptedBlob(reencryptedBytes);
        secureDataDao.updateSecureData(secureDataRecord);
    }

    @Transactional
    protected void reencryptDataVersion(String versionId) {
        log.debug("Re-encrypting secure data/file version: ID: {}", versionId);

        // Lock isn't required when it's the only operation that does update, but just in case.
        Optional<SecureDataVersionRecord> secureDataVersionRecord = secureDataVersionDao.readSecureDataVersionByIdLocking(versionId);
        if (! secureDataVersionRecord.isPresent()) {
            throw new IllegalArgumentException("No secure data version found for version ID: " + versionId);
        }

        SecureDataVersionRecord secureDataVersion = secureDataVersionRecord.get();
        String path = secureDataVersion.getPath();
        byte[] ciphertextBytes = secureDataVersion.getEncryptedBlob();
        byte[] reencryptedBytes = reencrypt(secureDataVersion.getType(), ciphertextBytes, path);
        OffsetDateTime now = dateTimeSupplier.get();

        secureDataVersion.setLastRotatedTs(now);
        secureDataVersion.setEncryptedBlob(reencryptedBytes);
        secureDataVersionDao.updateSecureDataVersion(secureDataVersion);
    }

    private byte[] reencrypt(SecureDataType secureDataType, byte[] ciphertextBytes, String path) {
        byte[] reencryptedBytes;
        if (SecureDataType.OBJECT == secureDataType) {
            // Make sure to convert ciphertext to a String first, then decrypt, because Amazon throws an
            // error if the ciphertext was encrypted as a String, but is not decrypted as a String.
            String ciphertext = new String(ciphertextBytes, StandardCharsets.UTF_8);
            String reencryptedCiphertext = encryptionService.reencrypt(ciphertext, path);
            reencryptedBytes = reencryptedCiphertext.getBytes(StandardCharsets.UTF_8);
        } else if (SecureDataType.FILE == secureDataType) {
            reencryptedBytes = encryptionService.reencrypt(ciphertextBytes, path);
        } else {
            throw new IllegalStateException("Unrecognized data type found at path" + path);
        }
        return reencryptedBytes;
    }
}
