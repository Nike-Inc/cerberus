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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.domain.SecureDataVersionSummary;
import com.nike.cerberus.domain.SecureDataVersionsResult;
import com.nike.cerberus.record.SecureDataVersionRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SecureDataVersionServiceTest {

  @Mock SecureDataVersionDao secureDataVersionDao;
  @Mock SecureDataService secureDataService;
  @Mock EncryptionService encryptionService;

  private SecureDataVersionService secureDataVersionService;

  @Before
  public void before() {
    initMocks(this);

    secureDataVersionService =
        new SecureDataVersionService(secureDataVersionDao, secureDataService, encryptionService);
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
        new SecureDataVersionRecord()
            .setId(versionId)
            .setEncryptedBlob("encrypted blob".getBytes())
            .setActionPrincipal(actionPrincipal)
            .setSdboxId(sdbId)
            .setActionTs(actionTs)
            .setPath(pathToSecureData)
            .setVersionCreatedBy(versionCreatedBy)
            .setVersionCreatedTs(versionCreatedTs)
            .setAction(action);

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
}
