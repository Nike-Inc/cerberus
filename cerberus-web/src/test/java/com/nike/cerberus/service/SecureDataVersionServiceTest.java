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

import static com.nike.cerberus.service.SecureDataVersionService.DEFAULT_ID_FOR_CURRENT_VERSIONS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.*;
import com.nike.cerberus.record.SecureDataRecord;
import com.nike.cerberus.record.SecureDataVersionRecord;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class SecureDataVersionServiceTest {

  @Mock SecureDataVersionDao secureDataVersionDao;
  @Mock SecureDataService secureDataService;
  @Mock EncryptionService encryptionService;

  @InjectMocks private SecureDataVersionService secureDataVersionService;

  @Before
  public void before() {
    initMocks(this);
  }

  @Test
  public void test_that_getSecureDataVersionsByPath_returns_versions() {
    String pathToSecureData = "path to secure data";
    String versionId = "version id";
    String action = "action";
    String sdbId = "sdb id";
    String actionPrincipal = "action principal";
    OffsetDateTime actionTs = OffsetDateTime.now();
    String versionCreatedBy = "version created by";
    OffsetDateTime versionCreatedTs = OffsetDateTime.now();
    String sdbCategory = "sdb category";
    String fullPath = String.format("%s/%s", sdbCategory, pathToSecureData);

    SecureDataVersionRecord record =
        SecureDataVersionRecord.builder()
            .id(versionId)
            .encryptedBlob("encrypted blob".getBytes(Charset.forName("UTF-8")))
            .actionPrincipal(actionPrincipal)
            .sdboxId(sdbId)
            .actionTs(actionTs)
            .path(pathToSecureData)
            .versionCreatedBy(versionCreatedBy)
            .versionCreatedTs(versionCreatedTs)
            .action(action)
            .build();

    List<SecureDataVersionRecord> versions = Lists.newArrayList(record);

    when(secureDataService.getSecureDataRecordForPath(sdbId, pathToSecureData))
        .thenReturn(Optional.empty());
    when(secureDataVersionDao.listSecureDataVersionByPath(pathToSecureData, 1, 0))
        .thenReturn(versions);
    SecureDataVersionsResult summaries =
        secureDataVersionService.getSecureDataVersionSummariesByPath(
            sdbId, pathToSecureData, sdbCategory, 1, 0);
    SecureDataVersionSummary result = summaries.getSecureDataVersionSummaries().get(0);

    assertEquals(record.getAction(), result.getAction());
    assertEquals(record.getActionPrincipal(), result.getActionPrincipal());
    assertEquals(record.getActionTs(), result.getActionTs());
    assertEquals(record.getSdboxId(), result.getSdboxId());
    assertEquals(record.getId(), result.getId());
    assertEquals(fullPath, result.getPath());
    assertEquals(record.getVersionCreatedBy(), result.getVersionCreatedBy());
    assertEquals(record.getVersionCreatedTs(), result.getVersionCreatedTs());
  }

  @Test
  public void
      test_that_getSecureDataVersionsByPath_returns_versions_when_secure_data_record_present_offsetzero() {
    Mockito.when(secureDataVersionDao.getTotalNumVersionsForPath("path")).thenReturn(10);
    List<SecureDataVersionRecord> secureDataVersionRecords = new ArrayList<>();
    secureDataVersionRecords.add(getSecureDataVersionRecord());
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    when(secureDataService.getSecureDataRecordForPath("sdbId", "path"))
        .thenReturn(Optional.of(secureDataRecord));
    when(secureDataVersionDao.listSecureDataVersionByPath("path", 9, 0))
        .thenReturn(secureDataVersionRecords);
    SecureDataVersionsResult summaries =
        secureDataVersionService.getSecureDataVersionSummariesByPath(
            "sdbId", "path", "category", 10, 0);
    List<SecureDataVersionSummary> secureDataVersionSummaries =
        summaries.getSecureDataVersionSummaries();
    Assert.assertEquals(2, secureDataVersionSummaries.size());
    SecureDataVersionSummary result = secureDataVersionSummaries.get(0);

    assertEquals(SecureDataVersionRecord.SecretsAction.CREATE.name(), result.getAction());
    assertEquals(secureDataRecord.getLastUpdatedBy(), result.getActionPrincipal());
    assertEquals(secureDataRecord.getLastUpdatedTs(), result.getActionTs());
    assertEquals(secureDataRecord.getSdboxId(), result.getSdboxId());
    assertEquals(DEFAULT_ID_FOR_CURRENT_VERSIONS, result.getId());
    assertEquals("category/path", result.getPath());
    assertEquals(secureDataRecord.getCreatedBy(), result.getVersionCreatedBy());
    assertEquals(secureDataRecord.getCreatedTs(), result.getVersionCreatedTs());
  }

  @Test
  public void
      test_that_getSecureDataVersionsByPath_returns_versions_when_secure_data_record_present_offset_greater_than_zero() {
    Mockito.when(secureDataVersionDao.getTotalNumVersionsForPath("path")).thenReturn(10);
    List<SecureDataVersionRecord> secureDataVersionRecords = new ArrayList<>();
    SecureDataVersionRecord secureDataVersionRecord = getSecureDataVersionRecord();
    secureDataVersionRecords.add(secureDataVersionRecord);
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    when(secureDataService.getSecureDataRecordForPath("sdbId", "path"))
        .thenReturn(Optional.of(secureDataRecord));
    when(secureDataVersionDao.listSecureDataVersionByPath("path", 10, 9))
        .thenReturn(secureDataVersionRecords);
    SecureDataVersionsResult summaries =
        secureDataVersionService.getSecureDataVersionSummariesByPath(
            "sdbId", "path", "category", 10, 10);
    List<SecureDataVersionSummary> secureDataVersionSummaries =
        summaries.getSecureDataVersionSummaries();
    Assert.assertEquals(1, secureDataVersionSummaries.size());
    SecureDataVersionSummary result = secureDataVersionSummaries.get(0);

    assertEquals(secureDataVersionRecord.getAction(), result.getAction());
    assertEquals(secureDataVersionRecord.getActionPrincipal(), result.getActionPrincipal());
    assertEquals(secureDataVersionRecord.getActionTs(), result.getActionTs());
    assertEquals(secureDataVersionRecord.getSdboxId(), result.getSdboxId());
    assertEquals(secureDataVersionRecord.getId(), result.getId());
    assertEquals("category/" + secureDataVersionRecord.getPath(), result.getPath());
    assertEquals(secureDataVersionRecord.getVersionCreatedBy(), result.getVersionCreatedBy());
    assertEquals(secureDataVersionRecord.getVersionCreatedTs(), result.getVersionCreatedTs());
  }

  private SecureDataVersionRecord getSecureDataVersionRecord() {
    SecureDataVersionRecord secureDataVersionRecord =
        SecureDataVersionRecord.builder()
            .versionCreatedBy("user")
            .versionCreatedTs(OffsetDateTime.MAX)
            .action("action")
            .id("id")
            .actionTs(OffsetDateTime.MAX)
            .actionPrincipal("principal")
            .encryptedBlob("blob".getBytes(StandardCharsets.UTF_8))
            .lastRotatedTs(OffsetDateTime.MAX)
            .sdboxId("sdBoxId")
            .sizeInBytes(10)
            .type(SecureDataType.OBJECT)
            .path("path")
            .build();
    return secureDataVersionRecord;
  }

  @Test
  public void testGetSecureDataByVersionIdWhenVersionIsNotDefault() {
    Mockito.when(secureDataVersionDao.readSecureDataVersionById("versionId"))
        .thenReturn(Optional.empty());
    Optional<SecureDataVersion> secureDataVersionById =
        secureDataVersionService.getSecureDataVersionById("sdbId", "versionId", "category", "path");
    Assert.assertFalse(secureDataVersionById.isPresent());
  }

  @Test
  public void testGetSecureDataByVersionIdWhenVersionIsNotDefaultAndPathIsDifferent() {
    SecureDataVersionRecord secureDataVersionRecord = Mockito.mock(SecureDataVersionRecord.class);
    Mockito.when(secureDataVersionRecord.getPath()).thenReturn("differentPath");
    Mockito.when(secureDataVersionDao.readSecureDataVersionById("versionId"))
        .thenReturn(Optional.of(secureDataVersionRecord));
    Optional<SecureDataVersion> secureDataVersionById =
        secureDataVersionService.getSecureDataVersionById("sdbId", "versionId", "category", "path");
    Assert.assertFalse(secureDataVersionById.isPresent());
  }

  @Test
  public void testGetSecureDataByVersionIdWhenVersionIsNotDefaultAndDataTypeIsNotObject() {
    SecureDataVersionRecord secureDataVersionRecord = Mockito.mock(SecureDataVersionRecord.class);
    Mockito.when(secureDataVersionRecord.getPath()).thenReturn("path");
    Mockito.when(secureDataVersionRecord.getType()).thenReturn(SecureDataType.FILE);
    Mockito.when(secureDataVersionDao.readSecureDataVersionById("versionId"))
        .thenReturn(Optional.of(secureDataVersionRecord));
    Optional<SecureDataVersion> secureDataVersionById =
        secureDataVersionService.getSecureDataVersionById("sdbId", "versionId", "category", "path");
    Assert.assertFalse(secureDataVersionById.isPresent());
  }

  @Test
  public void testGetSecureDataByVersionIdWhenVersionIsCurrentVersion() {
    Mockito.when(secureDataService.getSecureDataRecordForPath("sdbId", "path"))
        .thenReturn(Optional.empty());
    Optional<SecureDataVersion> secureDataVersionById =
        secureDataVersionService.getSecureDataVersionById(
            "sdbId", DEFAULT_ID_FOR_CURRENT_VERSIONS, "category", "path");
    Assert.assertFalse(secureDataVersionById.isPresent());
  }

  @Test
  public void testGetSecureDataByVersionIdWhenVersionIsCurrentVersionAnd() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    Mockito.when(secureDataService.getSecureDataRecordForPath("sdbId", "path"))
        .thenReturn(Optional.of(secureDataRecord));
    Mockito.when(secureDataService.secureDataHasBeenUpdated(secureDataRecord)).thenReturn(true);
    Mockito.when(encryptionService.decrypt("blob", "path")).thenReturn("plainText");
    Optional<SecureDataVersion> secureDataVersionById =
        secureDataVersionService.getSecureDataVersionById(
            "sdbId", DEFAULT_ID_FOR_CURRENT_VERSIONS, "category", "path");
    Assert.assertTrue(secureDataVersionById.isPresent());
    SecureDataVersion secureDataVersion = secureDataVersionById.get();
    Assert.assertEquals(
        SecureDataVersionRecord.SecretsAction.UPDATE.name(), secureDataVersion.getAction());
    Assert.assertEquals("plainText", secureDataVersion.getData());
  }

  @Test
  public void testGetSecureFileVersionByIdWhenFileIsNotPresent() {
    Mockito.when(secureDataVersionDao.readSecureDataVersionById("versionId"))
        .thenReturn(Optional.empty());
    Optional<SecureFileVersion> secureFileVersionById =
        secureDataVersionService.getSecureFileVersionById("sdbId", "versionId", "category", "path");
    Assert.assertFalse(secureFileVersionById.isPresent());
  }

  @Test
  public void testGetSecureFileVersionByIdWhenPathIsDifferent() {
    SecureDataVersionRecord secureDataVersionRecord = Mockito.mock(SecureDataVersionRecord.class);
    Mockito.when(secureDataVersionRecord.getPath()).thenReturn("differentPath");
    Mockito.when(secureDataVersionDao.readSecureDataVersionById("versionId"))
        .thenReturn(Optional.of(secureDataVersionRecord));
    Optional<SecureFileVersion> secureFileVersionById =
        secureDataVersionService.getSecureFileVersionById("sdbId", "versionId", "category", "path");
    Assert.assertFalse(secureFileVersionById.isPresent());
  }

  @Test
  public void testGetSecureFileVersionByIdWhenTypeIsNotFile() {
    SecureDataVersionRecord secureDataVersionRecord = Mockito.mock(SecureDataVersionRecord.class);
    Mockito.when(secureDataVersionRecord.getPath()).thenReturn("differentPath");
    Mockito.when(secureDataVersionRecord.getType()).thenReturn(SecureDataType.OBJECT);
    Mockito.when(secureDataVersionDao.readSecureDataVersionById("versionId"))
        .thenReturn(Optional.of(secureDataVersionRecord));
    Optional<SecureFileVersion> secureFileVersionById =
        secureDataVersionService.getSecureFileVersionById("sdbId", "versionId", "category", "path");
    Assert.assertFalse(secureFileVersionById.isPresent());
  }

  @Test
  public void testGetSecureFileVersionByIdWhenVersionIsCurrentAndRecordNotFound() {
    Mockito.when(secureDataService.getSecureDataRecordForPath("sdbId", "path"))
        .thenReturn(Optional.empty());
    Optional<SecureFileVersion> secureFileVersionById =
        secureDataVersionService.getSecureFileVersionById(
            "sdbId", DEFAULT_ID_FOR_CURRENT_VERSIONS, "category", "path");
    Assert.assertFalse(secureFileVersionById.isPresent());
  }

  @Test
  public void testGetSecureFileVersionById() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    secureDataRecord.setType(SecureDataType.FILE);
    Mockito.when(secureDataService.secureDataHasBeenUpdated(secureDataRecord)).thenReturn(true);
    when(encryptionService.decrypt("blob".getBytes(StandardCharsets.UTF_8), "path"))
        .thenReturn("plainText".getBytes(StandardCharsets.UTF_8));
    Mockito.when(secureDataService.getSecureDataRecordForPath("sdbId", "path"))
        .thenReturn(Optional.of(secureDataRecord));
    Optional<SecureFileVersion> secureFileVersionById =
        secureDataVersionService.getSecureFileVersionById(
            "sdbId", DEFAULT_ID_FOR_CURRENT_VERSIONS, "category", "path");
    Assert.assertTrue(secureFileVersionById.isPresent());
    SecureFileVersion secureFileVersion = secureFileVersionById.get();
    Assert.assertEquals(
        SecureDataVersionRecord.SecretsAction.UPDATE.name(), secureFileVersion.getAction());
  }

  @Test
  public void testGetSecureFileVersionByIdWhenActionIsCreate() {
    SecureDataRecord secureDataRecord = getSecureDataRecord();
    secureDataRecord.setType(SecureDataType.FILE);
    Mockito.when(secureDataService.secureDataHasBeenUpdated(secureDataRecord)).thenReturn(false);
    Mockito.when(secureDataService.getSecureDataRecordForPath("sdbId", "path"))
        .thenReturn(Optional.of(secureDataRecord));
    when(encryptionService.decrypt("blob".getBytes(StandardCharsets.UTF_8), "path"))
        .thenReturn("plainText".getBytes(StandardCharsets.UTF_8));
    Optional<SecureFileVersion> secureFileVersionById =
        secureDataVersionService.getSecureFileVersionById(
            "sdbId", DEFAULT_ID_FOR_CURRENT_VERSIONS, "category", "path");
    Assert.assertTrue(secureFileVersionById.isPresent());
    SecureFileVersion secureFileVersion = secureFileVersionById.get();
    Assert.assertEquals(
        SecureDataVersionRecord.SecretsAction.CREATE.name(), secureFileVersion.getAction());
  }

  @Test
  public void testGenerateSecureDataVersionsResult() {
    List<SecureDataVersionSummary> secureDataVersionSummaries = new ArrayList<>();
    SecureDataVersionSummary secureDataVersionSummary =
        Mockito.mock(SecureDataVersionSummary.class);
    secureDataVersionSummaries.add(secureDataVersionSummary);
    SecureDataVersionsResult secureDataVersionsResult =
        secureDataVersionService.generateSecureDataVersionsResult(
            secureDataVersionSummaries, 19, 10, 10);
    Assert.assertNull(secureDataVersionsResult.getNextOffset());
    Assert.assertSame(
        secureDataVersionSummaries, secureDataVersionsResult.getSecureDataVersionSummaries());
    Assert.assertEquals(
        secureDataVersionSummaries, secureDataVersionsResult.getSecureDataVersionSummaries());
    Assert.assertEquals(
        secureDataVersionSummaries.size(), secureDataVersionsResult.getVersionCountInResult());
    Assert.assertEquals(10, secureDataVersionsResult.getLimit());
    Assert.assertEquals(10, secureDataVersionsResult.getOffset());
    Assert.assertEquals(19, secureDataVersionsResult.getTotalVersionCount());
  }

  @Test
  public void testGenerateSecureDataVersionsResultWhenNextOffsetPresent() {
    List<SecureDataVersionSummary> secureDataVersionSummaries = new ArrayList<>();
    SecureDataVersionSummary secureDataVersionSummary =
        Mockito.mock(SecureDataVersionSummary.class);
    secureDataVersionSummaries.add(secureDataVersionSummary);
    SecureDataVersionsResult secureDataVersionsResult =
        secureDataVersionService.generateSecureDataVersionsResult(
            secureDataVersionSummaries, 21, 10, 10);
    Assert.assertEquals(Integer.valueOf(20), secureDataVersionsResult.getNextOffset());
    Assert.assertSame(
        secureDataVersionSummaries, secureDataVersionsResult.getSecureDataVersionSummaries());
    Assert.assertEquals(
        secureDataVersionSummaries, secureDataVersionsResult.getSecureDataVersionSummaries());
    Assert.assertEquals(
        secureDataVersionSummaries.size(), secureDataVersionsResult.getVersionCountInResult());
    Assert.assertEquals(10, secureDataVersionsResult.getLimit());
    Assert.assertEquals(10, secureDataVersionsResult.getOffset());
    Assert.assertEquals(21, secureDataVersionsResult.getTotalVersionCount());
  }

  @Test
  public void testParseVersionMetadataWhenSecureDataVersionIsNull() {
    Map<String, String> metadata = secureDataVersionService.parseVersionMetadata(null);
    Assert.assertTrue(metadata.isEmpty());
  }

  @Test
  public void testParseVersionMetadataWhenSecureDataVersionWhenAllValuesAreNull() {
    SecureDataVersion secureDataVersion = Mockito.mock(SecureDataVersion.class);
    Map<String, String> metadata = secureDataVersionService.parseVersionMetadata(secureDataVersion);
    Assert.assertFalse(metadata.isEmpty());
    Assert.assertTrue(checkAllKeysPresentInMetadata(metadata));
    Assert.assertNull(metadata.get("version_id"));
    Assert.assertNull(metadata.get("action"));
    Assert.assertNull(metadata.get("action_ts"));
    Assert.assertNull(metadata.get("action_principal"));
    Assert.assertNull(metadata.get("version_created_by"));
    Assert.assertNull(metadata.get("version_created_ts"));
  }

  @Test
  public void testParseVersionMetadataWhenSecureDataVersionWhenAllValuesArePresent() {
    SecureDataVersion secureDataVersion = Mockito.mock(SecureDataVersion.class);
    Mockito.when(secureDataVersion.getId()).thenReturn("id");
    Mockito.when(secureDataVersion.getAction()).thenReturn("action");
    Mockito.when(secureDataVersion.getActionTs()).thenReturn(OffsetDateTime.MAX);
    Mockito.when(secureDataVersion.getActionPrincipal()).thenReturn("principal");
    Mockito.when(secureDataVersion.getVersionCreatedBy()).thenReturn("user");
    Mockito.when(secureDataVersion.getVersionCreatedTs()).thenReturn(OffsetDateTime.MAX);
    Map<String, String> metadata = secureDataVersionService.parseVersionMetadata(secureDataVersion);
    Assert.assertFalse(metadata.isEmpty());
    Assert.assertTrue(checkAllKeysPresentInMetadata(metadata));
    Assert.assertEquals("id", metadata.get("version_id"));
    Assert.assertEquals("action", metadata.get("action"));
    Assert.assertEquals("+999999999-12-31T23:59:59.999999999-18:00", metadata.get("action_ts"));
    Assert.assertEquals("principal", metadata.get("action_principal"));
    Assert.assertEquals("user", metadata.get("version_created_by"));
    Assert.assertEquals(
        "+999999999-12-31T23:59:59.999999999-18:00", metadata.get("version_created_ts"));
  }

  private boolean checkAllKeysPresentInMetadata(Map<String, String> metadata) {
    return metadata.containsKey("version_id")
        && metadata.containsKey("action")
        && metadata.containsKey("action_ts")
        && metadata.containsKey("action_principal")
        && metadata.containsKey("version_created_by")
        && metadata.containsKey("version_created_ts");
  }

  private SecureDataRecord getSecureDataRecord() {
    SecureDataRecord secureDataRecord =
        SecureDataRecord.builder()
            .createdBy("user")
            .createdTs(OffsetDateTime.MAX)
            .id(0)
            .path("path")
            .encryptedBlob("blob".getBytes(StandardCharsets.UTF_8))
            .lastRotatedTs(OffsetDateTime.MAX)
            .lastUpdatedBy("user")
            .lastUpdatedTs(OffsetDateTime.MAX)
            .sdboxId("sdbBoxId")
            .sizeInBytes(10)
            .topLevelKVCount(9)
            .type(SecureDataType.OBJECT)
            .build();
    return secureDataRecord;
  }
}
