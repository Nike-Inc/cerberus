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
import com.nike.cerberus.mapper.UserGroupMapper;
import com.nike.cerberus.record.UserGroupPermissionRecord;
import com.nike.cerberus.record.UserGroupRecord;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class UserGroupDaoTest {

  private final String userGroupName = "USER_GROUP_NAME";

  private final String safeDepositBoxId = "SDB_ID";

  private final String roleId = "ROLE_ID";

  private final String userGroupId = "USER_GROUP_ID";

  private final String userGroupPermissionId = "USER_GROUP_PERMISSION_ID";

  private final String createdBy = "system";

  private final String lastUpdatedBy = "system";

  private final OffsetDateTime createdTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private final OffsetDateTime lastUpdatedTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private final UserGroupRecord userGroupRecord =
      new UserGroupRecord()
          .setId(userGroupId)
          .setName(userGroupName)
          .setCreatedBy(createdBy)
          .setLastUpdatedBy(lastUpdatedBy)
          .setCreatedTs(createdTs)
          .setLastUpdatedTs(lastUpdatedTs);

  private final List<UserGroupRecord> userGroupRecordList = Lists.newArrayList(userGroupRecord);

  private final UserGroupPermissionRecord userGroupPermissionRecord =
      new UserGroupPermissionRecord()
          .setId(userGroupPermissionId)
          .setUserGroupId(userGroupId)
          .setSdboxId(safeDepositBoxId)
          .setRoleId(roleId)
          .setCreatedBy(createdBy)
          .setLastUpdatedBy(lastUpdatedBy)
          .setCreatedTs(createdTs)
          .setLastUpdatedTs(lastUpdatedTs);

  private final List<UserGroupPermissionRecord> userGroupPermissionRecordList =
      Lists.newArrayList(userGroupPermissionRecord);

  private UserGroupMapper userGroupMapper;

  private UserGroupDao subject;

  @Before
  public void setUp() throws Exception {
    userGroupMapper = mock(UserGroupMapper.class);
    subject = new UserGroupDao(userGroupMapper);
  }

  @Test
  public void getUserGroup_by_id_returns_record_when_found() {
    when(userGroupMapper.getUserGroup(userGroupId)).thenReturn(userGroupRecord);

    final Optional<UserGroupRecord> actual = subject.getUserGroup(userGroupId);

    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get()).isEqualTo(userGroupRecord);
  }

  @Test
  public void getUserGroup_by_id_returns_empty_when_record_not_found() {
    when(userGroupMapper.getUserGroup(userGroupId)).thenReturn(null);

    final Optional<UserGroupRecord> actual = subject.getUserGroup(userGroupId);

    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void getUserGroupByName_returns_record_when_found() {
    when(userGroupMapper.getUserGroupByName(userGroupName)).thenReturn(userGroupRecord);

    final Optional<UserGroupRecord> actual = subject.getUserGroupByName(userGroupName);

    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get()).isEqualTo(userGroupRecord);
  }

  @Test
  public void getUserGroupByName_returns_empty_when_record_not_found() {
    when(userGroupMapper.getUserGroupByName(userGroupName)).thenReturn(null);

    final Optional<UserGroupRecord> actual = subject.getUserGroupByName(userGroupName);

    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void getUserGroupByRole_returns_record_when_found() {
    when(userGroupMapper.getUserGroupsByRole(safeDepositBoxId, roleId))
        .thenReturn(userGroupRecordList);

    List<UserGroupRecord> actual = subject.getUserGroupsByRole(safeDepositBoxId, roleId);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(userGroupRecordList);
  }

  @Test
  public void createUserGroup_returns_record_count() {
    final int recordCount = 1;
    when(userGroupMapper.createUserGroup(userGroupRecord)).thenReturn(recordCount);

    final int actualCount = subject.createUserGroup(userGroupRecord);

    assertThat(actualCount).isEqualTo(recordCount);
  }

  @Test
  public void getUserGroupPermissions_returns_list_of_records() {
    when(userGroupMapper.getUserGroupPermissions(safeDepositBoxId))
        .thenReturn(userGroupPermissionRecordList);

    List<UserGroupPermissionRecord> actual = subject.getUserGroupPermissions(safeDepositBoxId);

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(userGroupPermissionRecordList);
  }

  @Test
  public void createUserGroupPermission_returns_record_count() {
    final int recordCount = 1;
    when(userGroupMapper.createUserGroupPermission(userGroupPermissionRecord))
        .thenReturn(recordCount);

    final int actualCount = subject.createUserGroupPermission(userGroupPermissionRecord);

    assertThat(actualCount).isEqualTo(recordCount);
  }

  @Test
  public void updateUserGroupPermission_returns_record_count() {
    final int recordCount = 1;
    when(userGroupMapper.updateUserGroupPermission(userGroupPermissionRecord))
        .thenReturn(recordCount);

    final int actualCount = subject.updateUserGroupPermission(userGroupPermissionRecord);

    assertThat(actualCount).isEqualTo(recordCount);
  }

  @Test
  public void deleteUserGroupPermission_returns_record_count() {
    final int recordCount = 1;
    when(userGroupMapper.deleteUserGroupPermission(safeDepositBoxId, userGroupId))
        .thenReturn(recordCount);

    final int actualCount = subject.deleteUserGroupPermission(safeDepositBoxId, userGroupId);

    assertThat(actualCount).isEqualTo(recordCount);
  }

  @Test
  public void deleteUserGroupPermissions_returns_record_count() {
    final int recordCount = 1;
    when(userGroupMapper.deleteUserGroupPermissions(safeDepositBoxId)).thenReturn(recordCount);

    final int actualCount = subject.deleteUserGroupPermissions(safeDepositBoxId);

    assertThat(actualCount).isEqualTo(recordCount);
  }
}
