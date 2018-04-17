/*
 * Copyright (c) 2018 Nike, Inc.
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
 *
 */

package com.nike.cerberus.service;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.domain.SecureDataVersion;
import com.nike.cerberus.domain.SecureDataVersionSummary;
import com.nike.cerberus.domain.SecureDataVersionsResult;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SecureDataVersionService {

    // the default ID for current versions of secure data
    public static final String DEFAULT_ID_FOR_CURRENT_VERSIONS = "CURRENT";

    private final SecureDataVersionDao secureDataVersionDao;
    private final SecureDataService secureDataService;
    private final EncryptionService encryptionService;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    public SecureDataVersionService(SecureDataVersionDao secureDataVersionDao,
                                    SecureDataService secureDataService,
                                    EncryptionService encryptionService) {
        this.secureDataVersionDao = secureDataVersionDao;
        this.secureDataService = secureDataService;
        this.encryptionService = encryptionService;
    }

    public Optional<SecureDataVersion> getSecureDataVersionById(String versionId, String sdbCategory, String pathToSecureData) {
        log.debug("Reading secure data version: ID: {}", versionId);
        Optional<SecureDataVersionRecord> secureDataVersionRecord = StringUtils.equals(versionId, DEFAULT_ID_FOR_CURRENT_VERSIONS) ?
                        getCurrentSecureDataVersion(pathToSecureData) :
                        secureDataVersionDao.readSecureDataVersionById(versionId);

        if (! secureDataVersionRecord.isPresent() ||
                ! StringUtils.equals(pathToSecureData, secureDataVersionRecord.get().getPath()) ||
                secureDataVersionRecord.get().getType() != SecureDataType.OBJECT) {
            return Optional.empty();
        }

        SecureDataVersionRecord secureDataVersion = secureDataVersionRecord.get();
        byte[] encryptedBlob = secureDataVersion.getEncryptedBlob();

        // Make sure to convert ciphertext to a String first, then decrypt, because Amazon throws an
        // error if the ciphertext was encrypted as a String, but is not decrypted as a String.
        String ciphertext = new String(encryptedBlob, StandardCharsets.UTF_8);
        String plainText = encryptionService.decrypt(ciphertext, secureDataVersion.getPath());

        return Optional.of(new SecureDataVersion()
                .setAction(secureDataVersion.getAction())
                .setActionPrincipal(secureDataVersion.getActionPrincipal())
                .setActionTs(secureDataVersion.getActionTs())
                .setData(plainText)
                .setId(secureDataVersion.getId())
                .setPath(String.format("%s/%s", sdbCategory, secureDataVersion.getPath()))
                .setSdboxId(secureDataVersion.getSdboxId())
                .setVersionCreatedBy(secureDataVersion.getVersionCreatedBy())
                .setVersionCreatedTs(secureDataVersion.getVersionCreatedTs()));

    }

    public SecureDataVersionsResult getSecureDataVersionSummariesByPath(String pathToSecureData,
                                                                        String sdbCategory,
                                                                        int limit,
                                                                        int offset) {

        List<SecureDataVersionSummary> secureDataVersionSummaries = Lists.newArrayList();
        List<SecureDataVersionRecord> secureDataVersions = Lists.newArrayList();
        int actualLimit = limit;
        int actualOffset = offset;
        int totalNumVersionsForPath = secureDataVersionDao.getTotalNumVersionsForPath(pathToSecureData);

        // retrieve current secrets versions from secure data table
        Optional<SecureDataVersionRecord> currentSecureDataVersionOpt = getCurrentSecureDataVersion(pathToSecureData);
        if (currentSecureDataVersionOpt.isPresent()) {
            if (offset == 0) {
                SecureDataVersionRecord currentSecureDataVersion = currentSecureDataVersionOpt.get();
                secureDataVersions.add(currentSecureDataVersion);
                actualLimit--;
            } else {
                actualOffset--;
            }
            totalNumVersionsForPath++;
        }  // else, the secret has been deleted and the last version should already be in the list

        // retrieve previous secrets versions from the secure data versions table
        secureDataVersions.addAll(secureDataVersionDao.listSecureDataVersionByPath(pathToSecureData, actualLimit, actualOffset));

        secureDataVersions.forEach(versionRecord ->
            secureDataVersionSummaries.add(new SecureDataVersionSummary()
                    .setAction(versionRecord.getAction())
                    .setActionPrincipal(versionRecord.getActionPrincipal())
                    .setActionTs(versionRecord.getActionTs())
                    .setType(versionRecord.getType())
                    .setSizeInBytes(versionRecord.getSizeInBytes())
                    .setId(versionRecord.getId())
                    .setPath(String.format("%s/%s", sdbCategory, versionRecord.getPath()))
                    .setSdboxId(versionRecord.getSdboxId())
                    .setVersionCreatedBy(versionRecord.getVersionCreatedBy())
                    .setVersionCreatedTs(versionRecord.getVersionCreatedTs())
            )
        );

        return generateSecureDataVersionsResult(secureDataVersionSummaries, totalNumVersionsForPath, limit, offset);
    }

    public SecureDataVersionsResult generateSecureDataVersionsResult(List<SecureDataVersionSummary> summaries,
                                                                     int totalNumVersionsForPath,
                                                                     int limit,
                                                                     int offset) {

        int nextOffset = limit + offset;
        boolean hasNext = totalNumVersionsForPath > nextOffset;

        SecureDataVersionsResult result = new SecureDataVersionsResult()
                .setLimit(limit)
                .setOffset(offset)
                .setTotalVersionCount(totalNumVersionsForPath)
                .setVersionCountInResult(summaries.size())
                .setHasNext(hasNext)
                .setSecureDataVersionSummaries(summaries);

        if (hasNext) {
            result.setNextOffset(nextOffset);
        }

        return result;
    }

    private Optional<SecureDataVersionRecord> getCurrentSecureDataVersion(String pathToSecureData) {
        Optional<SecureDataRecord> currentSecureDataRecordOpt = secureDataService.getSecureDataRecordForPath(pathToSecureData);
        SecureDataVersionRecord newSecureDataVersionRecord = null;

        if (currentSecureDataRecordOpt.isPresent()) {
            SecureDataRecord currentSecureDataRecord = currentSecureDataRecordOpt.get();
            SecureDataVersionRecord.SecretsAction action =
                    secureDataService.secureDataHasBeenUpdated(currentSecureDataRecord) ?
                        SecureDataVersionRecord.SecretsAction.UPDATE :
                        SecureDataVersionRecord.SecretsAction.CREATE;

            newSecureDataVersionRecord = new SecureDataVersionRecord()
                    .setId(DEFAULT_ID_FOR_CURRENT_VERSIONS)
                    .setAction(action.name())
                    .setEncryptedBlob(currentSecureDataRecord.getEncryptedBlob())
                    .setType(currentSecureDataRecord.getType())
                    .setSizeInBytes(currentSecureDataRecord.getSizeInBytes())
                    .setVersionCreatedBy(currentSecureDataRecord.getLastUpdatedBy())
                    .setVersionCreatedTs(currentSecureDataRecord.getLastUpdatedTs())
                    .setActionPrincipal(currentSecureDataRecord.getLastUpdatedBy())
                    .setActionTs(currentSecureDataRecord.getLastUpdatedTs())
                    .setSdboxId(currentSecureDataRecord.getSdboxId())
                    .setPath(currentSecureDataRecord.getPath());
        }

        return Optional.ofNullable(newSecureDataVersionRecord);
    }

    public Map<String, String> parseVersionMetadata(SecureDataVersion secureDataVersion) {
        Map<String, String> metadata = Maps.newHashMap();

        if (secureDataVersion == null) {
            return metadata;
        }

        metadata.put("version_id", secureDataVersion.getId());
        metadata.put("action", secureDataVersion.getAction());
        metadata.put("action_ts", secureDataVersion.getActionTs().toString());
        metadata.put("action_principal", secureDataVersion.getActionPrincipal());
        metadata.put("version_created_by", secureDataVersion.getVersionCreatedBy());
        metadata.put("version_created_ts", secureDataVersion.getVersionCreatedTs().toString());

        return metadata;
    }
}
