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

package com.nike.cerberus.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.mapper.SecureDataVersionMapper;
import com.nike.cerberus.record.SecureDataVersionRecord;
import com.nike.cerberus.util.UuidSupplier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class SecureDataVersionDaoTest {

  private static final String versionId = "version id";
  private static final String sdbId = "sdb id";
  private static final String path = "path";
  private static final String encryptedBlob = "encrypted blob";
  private final SecureDataVersionRecord.SecretsAction action =
      SecureDataVersionRecord.SecretsAction.UPDATE;
  private static final String actionPrincipal = "system";
  private static final String versionCreatedBy = "system";
  private final OffsetDateTime actionTs = OffsetDateTime.now(ZoneId.of("UTC"));
  private final OffsetDateTime versionCreatedTs = actionTs;

  private final SecureDataVersionRecord secureDataVersionRecord =
      SecureDataVersionRecord.builder()
          .id(versionId)
          .sdboxId(sdbId)
          .action(action.name())
          .path(path)
          .encryptedBlob(encryptedBlob.getBytes(Charset.forName("UTF-8")))
          .versionCreatedBy(versionCreatedBy)
          .actionPrincipal(actionPrincipal)
          .versionCreatedTs(versionCreatedTs)
          .actionTs(actionTs)
          .build();

  private final List<SecureDataVersionRecord> secureDataVersionRecords =
      Lists.newArrayList(secureDataVersionRecord);

  private final String[] paths = {path};

  @Mock private SecureDataVersionMapper secureDataVersionMapper;

  @Mock private UuidSupplier uuidSupplier;

  private SecureDataVersionDao subject;

  @Before
  public void setUp() throws Exception {
    initMocks(this);

    subject = new SecureDataVersionDao(secureDataVersionMapper, uuidSupplier);
  }

  @Test
  public void listSecureDataVersionsByPath_returns_list_of_version_records() {
    when(secureDataVersionMapper.listSecureDataVersionsByPath(path, 1, 0))
        .thenReturn(secureDataVersionRecords);

    List<SecureDataVersionRecord> actual = subject.listSecureDataVersionByPath(path, 1, 0);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(secureDataVersionRecords);
  }

  @Test
  public void getVersionPathsByPartialPath_returns_list_of_paths() {
    when(secureDataVersionMapper.getVersionPathsByPartialPath(path)).thenReturn(paths);

    String[] actual = subject.getVersionPathsByPartialPath(path);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(Arrays.asList(paths));
  }

  @Test
  public void readSecureDataVersionById_returns_record_when_found() {
    when(secureDataVersionMapper.readSecureDataVersionById(versionId))
        .thenReturn(secureDataVersionRecord);

    final Optional<SecureDataVersionRecord> actual = subject.readSecureDataVersionById(versionId);

    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get()).isEqualTo(secureDataVersionRecord);
  }

  @Test
  public void readSecureDataVersionById_returns_empty_when_record_not_found() {
    when(secureDataVersionMapper.readSecureDataVersionById(versionId)).thenReturn(null);

    final Optional<SecureDataVersionRecord> actual = subject.readSecureDataVersionById(versionId);

    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void deleteAllVersionsThatStartWithPartialPath_returns_record_when_found() {
    when(secureDataVersionMapper.deleteAllVersionsThatStartWithPartialPath(path)).thenReturn(1);

    subject.deleteAllVersionsThatStartWithPartialPath(path);

    verify(secureDataVersionMapper).deleteAllVersionsThatStartWithPartialPath(path);
  }

  @Test
  public void writeSecureDataVersion_calls_data_version_dao() {
    when(uuidSupplier.get()).thenReturn(versionId);
    when(secureDataVersionMapper.writeSecureDataVersion(secureDataVersionRecord)).thenReturn(1);

    subject.writeSecureDataVersion(
        sdbId,
        path,
        encryptedBlob.getBytes(StandardCharsets.UTF_8),
        action,
        SecureDataType.FILE,
        1024,
        versionCreatedBy,
        versionCreatedTs,
        actionPrincipal,
        actionTs);

    verify(secureDataVersionMapper).writeSecureDataVersion(anyObject());
  }
}
