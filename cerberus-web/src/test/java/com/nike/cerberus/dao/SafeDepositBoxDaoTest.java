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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nike.cerberus.mapper.SafeDepositBoxMapper;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.record.SafeDepositBoxRoleRecord;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class SafeDepositBoxDaoTest {

  private static final String safeDepositBoxId = "SDB_ID";

  private static final String categoryId = "CATEGORY_ID";

  private static final String name = "SDB_NAME";

  private static final String description = "DESCRIPTION";

  private static final String path = "PATH";

  private static final String createdBy = "system";

  private static final String lastUpdatedBy = "system";

  private final OffsetDateTime createdTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private final OffsetDateTime lastUpdatedTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private static final String roleName = "ROLE_NAME";

  private static final String userGroup = "USER_GROUP";

  private final Set<String> userGroupSet = Sets.newHashSet(userGroup);

  private static final String awsIamRoleArn = "AWS_IAM_ROLE_ARN";

  private static final String iamRootArn = "IAM_ROOT_ARN";

  private final SafeDepositBoxRecord safeDepositBoxRecord =
      SafeDepositBoxRecord.builder()
          .id(safeDepositBoxId)
          .categoryId(categoryId)
          .name(name)
          .description(description)
          .path(path)
          .createdBy(createdBy)
          .lastUpdatedBy(lastUpdatedBy)
          .createdTs(createdTs)
          .lastUpdatedTs(lastUpdatedTs)
          .build();

  private final List<SafeDepositBoxRecord> safeDepositBoxRecordList =
      Lists.newArrayList(safeDepositBoxRecord);

  private final SafeDepositBoxRoleRecord safeDepositBoxRoleRecord =
      SafeDepositBoxRoleRecord.builder().safeDepositBoxName(name).roleName(roleName).build();

  private final List<SafeDepositBoxRoleRecord> safeDepositBoxRoleRecordList =
      Lists.newArrayList(safeDepositBoxRoleRecord);

  private SafeDepositBoxMapper safeDepositBoxMapper;

  private SafeDepositBoxDao subject;

  @Before
  public void setUp() throws Exception {
    safeDepositBoxMapper = mock(SafeDepositBoxMapper.class);
    subject = new SafeDepositBoxDao(safeDepositBoxMapper);
  }

  @Test
  public void getUserAssociatedSafeDepositBoxRoles_returns_list_of_role_records() {
    when(safeDepositBoxMapper.getUserAssociatedSafeDepositBoxRoles(userGroupSet))
        .thenReturn(safeDepositBoxRoleRecordList);

    List<SafeDepositBoxRoleRecord> actual =
        subject.getUserAssociatedSafeDepositBoxRoles(userGroupSet);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(safeDepositBoxRoleRecordList);
  }

  @Test
  public void getIamRoleAssociatedSafeDepositBoxRoles_returns_list_of_role_records() {
    when(safeDepositBoxMapper.getIamRoleAssociatedSafeDepositBoxRoles(awsIamRoleArn, iamRootArn))
        .thenReturn(safeDepositBoxRoleRecordList);

    List<SafeDepositBoxRoleRecord> actual =
        subject.getIamRoleAssociatedSafeDepositBoxRoles(awsIamRoleArn, iamRootArn);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(safeDepositBoxRoleRecordList);
  }

  @Test
  public void getIamAssumedRoleAssociatedSafeDepositBoxRoles_returns_list_of_role_records() {
    when(safeDepositBoxMapper.getIamAssumedRoleAssociatedSafeDepositBoxRoles(
            "ASSUMED_ROLE_ARN", awsIamRoleArn, iamRootArn))
        .thenReturn(safeDepositBoxRoleRecordList);

    List<SafeDepositBoxRoleRecord> actual =
        subject.getIamAssumedRoleAssociatedSafeDepositBoxRoles(
            "ASSUMED_ROLE_ARN", awsIamRoleArn, iamRootArn);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(safeDepositBoxRoleRecordList);
  }

  @Test
  public void getUserAssociatedSafeDepositBoxes_returns_list_of_role_records() {
    when(safeDepositBoxMapper.getUserAssociatedSafeDepositBoxes(userGroupSet))
        .thenReturn(safeDepositBoxRecordList);

    List<SafeDepositBoxRecord> actual = subject.getUserAssociatedSafeDepositBoxes(userGroupSet);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(safeDepositBoxRecordList);
  }

  @Test
  public void getSafeDepositBoxes_returns_list_of_role_records() {
    when(safeDepositBoxMapper.getSafeDepositBoxes(1000, 0)).thenReturn(safeDepositBoxRecordList);

    List<SafeDepositBoxRecord> actual = subject.getSafeDepositBoxes(1000, 0);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(safeDepositBoxRecordList);
  }

  @Test
  public void getSafeDepositBox_by_id_returns_record_when_found() {
    when(safeDepositBoxMapper.getSafeDepositBox(safeDepositBoxId)).thenReturn(safeDepositBoxRecord);

    final Optional<SafeDepositBoxRecord> actual = subject.getSafeDepositBox(safeDepositBoxId);

    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get()).isEqualTo(safeDepositBoxRecord);
  }

  @Test
  public void getSafeDepositBox_by_id_returns_empty_when_record_not_found() {
    when(safeDepositBoxMapper.getSafeDepositBox(safeDepositBoxId)).thenReturn(null);

    final Optional<SafeDepositBoxRecord> actual = subject.getSafeDepositBox(safeDepositBoxId);

    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void isPathInUse_returns_true_if_record_already_exists_with_path() {
    when(safeDepositBoxMapper.countByPath(path)).thenReturn(1);

    final boolean pathInUse = subject.isPathInUse(path);

    assertThat(pathInUse).isTrue();
  }

  @Test
  public void isPathInUse_returns_false_if_record_does_not_exist_with_path() {
    when(safeDepositBoxMapper.countByPath(path)).thenReturn(0);

    final boolean pathInUse = subject.isPathInUse(path);

    assertThat(pathInUse).isFalse();
  }

  @Test
  public void createSafeDepositBox_returns_record_count() {
    final int recordCount = 1;
    when(safeDepositBoxMapper.createSafeDepositBox(safeDepositBoxRecord)).thenReturn(recordCount);

    final int actualCount = subject.createSafeDepositBox(safeDepositBoxRecord);

    assertThat(actualCount).isEqualTo(recordCount);
  }

  @Test
  public void updateSafeDepositBox_returns_record_count() {
    final int recordCount = 1;
    when(safeDepositBoxMapper.updateSafeDepositBox(safeDepositBoxRecord)).thenReturn(recordCount);

    final int actualCount = subject.updateSafeDepositBox(safeDepositBoxRecord);

    assertThat(actualCount).isEqualTo(recordCount);
  }

  @Test
  public void deleteSafeDepositBox_returns_record_count() {
    final int recordCount = 1;
    when(safeDepositBoxMapper.deleteSafeDepositBox(safeDepositBoxId)).thenReturn(recordCount);

    final int actualCount = subject.deleteSafeDepositBox(safeDepositBoxId);

    assertThat(actualCount).isEqualTo(recordCount);
  }
}
