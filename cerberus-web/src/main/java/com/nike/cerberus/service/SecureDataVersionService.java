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

import static java.util.Optional.ofNullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.*;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecureDataVersionService {

  // the default ID for current versions of secure data
  public static final String DEFAULT_ID_FOR_CURRENT_VERSIONS = "CURRENT";

  private final SecureDataVersionDao secureDataVersionDao;
  private final SecureDataService secureDataService;
  private final EncryptionService encryptionService;

  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired
  public SecureDataVersionService(
      SecureDataVersionDao secureDataVersionDao,
      SecureDataService secureDataService,
      EncryptionService encryptionService) {
    this.secureDataVersionDao = secureDataVersionDao;
    this.secureDataService = secureDataService;
    this.encryptionService = encryptionService;
  }

  public Optional<SecureDataVersion> getSecureDataVersionById(
      String sdbId, String versionId, String sdbCategory, String pathToSecureData) {
    log.debug("Reading secure data version: ID: {}", versionId);
    Optional<SecureDataVersionRecord> secureDataVersionRecord =
        StringUtils.equals(versionId, DEFAULT_ID_FOR_CURRENT_VERSIONS)
            ? getCurrentSecureDataVersion(sdbId, pathToSecureData)
            : secureDataVersionDao.readSecureDataVersionById(versionId);

    if (!secureDataVersionRecord.isPresent()
        || !StringUtils.equals(pathToSecureData, secureDataVersionRecord.get().getPath())
        || secureDataVersionRecord.get().getType() != SecureDataType.OBJECT) {
      return Optional.empty();
    }

    SecureDataVersionRecord secureDataVersion = secureDataVersionRecord.get();
    byte[] encryptedBlob = secureDataVersion.getEncryptedBlob();

    // Make sure to convert ciphertext to a String first, then decrypt, because Amazon throws an
    // error if the ciphertext was encrypted as a String, but is not decrypted as a String.
    String ciphertext = new String(encryptedBlob, StandardCharsets.UTF_8);
    String plainText = encryptionService.decrypt(ciphertext, secureDataVersion.getPath());

    return Optional.of(
        SecureDataVersion.builder()
            .action(secureDataVersion.getAction())
            .actionPrincipal(secureDataVersion.getActionPrincipal())
            .actionTs(secureDataVersion.getActionTs())
            .data(plainText)
            .id(secureDataVersion.getId())
            .path(String.format("%s/%s", sdbCategory, secureDataVersion.getPath()))
            .sdboxId(secureDataVersion.getSdboxId())
            .versionCreatedBy(secureDataVersion.getVersionCreatedBy())
            .versionCreatedTs(secureDataVersion.getVersionCreatedTs())
            .build());
  }

  public Optional<SecureFileVersion> getSecureFileVersionById(
      String sdbId, String versionId, String sdbCategory, String pathToSecureData) {
    log.debug("Reading secure data version: ID: {}", versionId);
    Optional<SecureDataVersionRecord> secureDataVersionRecord =
        StringUtils.equals(versionId, DEFAULT_ID_FOR_CURRENT_VERSIONS)
            ? getCurrentSecureDataVersion(sdbId, pathToSecureData)
            : secureDataVersionDao.readSecureDataVersionById(versionId);

    if (!secureDataVersionRecord.isPresent()
        || !StringUtils.equals(pathToSecureData, secureDataVersionRecord.get().getPath())
        || secureDataVersionRecord.get().getType() != SecureDataType.FILE) {
      return Optional.empty();
    }

    SecureDataVersionRecord secureDataVersion = secureDataVersionRecord.get();
    byte[] encryptedBlob = secureDataVersion.getEncryptedBlob();
    byte[] unencryptedBlob = encryptionService.decrypt(encryptedBlob, secureDataVersion.getPath());

    return Optional.of(
        SecureFileVersion.builder()
            .action(secureDataVersion.getAction())
            .actionPrincipal(secureDataVersion.getActionPrincipal())
            .actionTs(secureDataVersion.getActionTs())
            .data(unencryptedBlob)
            .name(StringUtils.substringAfterLast(secureDataVersion.getPath(), "/"))
            .sizeInBytes(secureDataVersion.getSizeInBytes())
            .id(secureDataVersion.getId())
            .path(String.format("%s/%s", sdbCategory, secureDataVersion.getPath()))
            .sdboxId(secureDataVersion.getSdboxId())
            .versionCreatedBy(secureDataVersion.getVersionCreatedBy())
            .versionCreatedTs(secureDataVersion.getVersionCreatedTs())
            .build());
  }

  public SecureDataVersionsResult getSecureDataVersionSummariesByPath(
      String sdbId, String pathToSecureData, String sdbCategory, int limit, int offset) {

    List<SecureDataVersionSummary> secureDataVersionSummaries = Lists.newArrayList();
    List<SecureDataVersionRecord> secureDataVersions = Lists.newArrayList();
    int actualLimit = limit;
    int actualOffset = offset;
    int totalNumVersionsForPath = secureDataVersionDao.getTotalNumVersionsForPath(pathToSecureData);

    // retrieve current secrets versions from secure data table
    Optional<SecureDataVersionRecord> currentSecureDataVersionOpt =
        getCurrentSecureDataVersion(sdbId, pathToSecureData);
    if (currentSecureDataVersionOpt.isPresent()) {
      if (offset == 0) {
        SecureDataVersionRecord currentSecureDataVersion = currentSecureDataVersionOpt.get();
        secureDataVersions.add(currentSecureDataVersion);
        actualLimit--;
      } else {
        actualOffset--;
      }
      totalNumVersionsForPath++;
    } // else, the secret has been deleted and the last version should already be in the list

    // retrieve previous secrets versions from the secure data versions table
    secureDataVersions.addAll(
        secureDataVersionDao.listSecureDataVersionByPath(
            pathToSecureData, actualLimit, actualOffset));

    secureDataVersions.forEach(
        versionRecord ->
            secureDataVersionSummaries.add(
                SecureDataVersionSummary.builder()
                    .action(versionRecord.getAction())
                    .actionPrincipal(versionRecord.getActionPrincipal())
                    .actionTs(versionRecord.getActionTs())
                    .type(versionRecord.getType())
                    .sizeInBytes(versionRecord.getSizeInBytes())
                    .id(versionRecord.getId())
                    .path(String.format("%s/%s", sdbCategory, versionRecord.getPath()))
                    .sdboxId(versionRecord.getSdboxId())
                    .versionCreatedBy(versionRecord.getVersionCreatedBy())
                    .versionCreatedTs(versionRecord.getVersionCreatedTs())
                    .build()));

    return generateSecureDataVersionsResult(
        secureDataVersionSummaries, totalNumVersionsForPath, limit, offset);
  }

  public SecureDataVersionsResult generateSecureDataVersionsResult(
      List<SecureDataVersionSummary> summaries,
      int totalNumVersionsForPath,
      int limit,
      int offset) {

    int nextOffset = limit + offset;
    boolean hasNext = totalNumVersionsForPath > nextOffset;

    SecureDataVersionsResult result =
        SecureDataVersionsResult.builder()
            .limit(limit)
            .offset(offset)
            .totalVersionCount(totalNumVersionsForPath)
            .versionCountInResult(summaries.size())
            .hasNext(hasNext)
            .secureDataVersionSummaries(summaries)
            .build();

    if (hasNext) {
      result.setNextOffset(nextOffset);
    }

    return result;
  }

  private Optional<SecureDataVersionRecord> getCurrentSecureDataVersion(
      String sdbId, String pathToSecureData) {
    Optional<SecureDataRecord> currentSecureDataRecordOpt =
        secureDataService.getSecureDataRecordForPath(sdbId, pathToSecureData);
    SecureDataVersionRecord newSecureDataVersionRecord = null;

    if (currentSecureDataRecordOpt.isPresent()) {
      SecureDataRecord currentSecureDataRecord = currentSecureDataRecordOpt.get();
      SecureDataVersionRecord.SecretsAction action =
          secureDataService.secureDataHasBeenUpdated(currentSecureDataRecord)
              ? SecureDataVersionRecord.SecretsAction.UPDATE
              : SecureDataVersionRecord.SecretsAction.CREATE;

      newSecureDataVersionRecord =
          new SecureDataVersionRecord()
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

    return ofNullable(newSecureDataVersionRecord);
  }

  public Map<String, String> parseVersionMetadata(SecureDataVersion secureDataVersion) {
    Map<String, String> metadata = Maps.newHashMap();

    if (secureDataVersion == null) {
      return metadata;
    }

    metadata.put("version_id", secureDataVersion.getId());
    metadata.put("action", secureDataVersion.getAction());
    metadata.put(
        "action_ts",
        ofNullable(secureDataVersion.getActionTs()).map(OffsetDateTime::toString).orElse(null));
    metadata.put("action_principal", secureDataVersion.getActionPrincipal());
    metadata.put("version_created_by", secureDataVersion.getVersionCreatedBy());
    metadata.put(
        "version_created_ts",
        ofNullable(secureDataVersion.getVersionCreatedTs())
            .map(OffsetDateTime::toString)
            .orElse(null));

    return metadata;
  }
}
