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
import com.nike.cerberus.mapper.RoleMapper;
import com.nike.cerberus.record.RoleRecord;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class RoleDaoTest {

  private static final String roleId = "ROLE_ID";

  private static final String name = "NAME";

  private static final String createdBy = "system";

  private static final String lastUpdatedBy = "system";

  private final OffsetDateTime createdTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private final OffsetDateTime lastUpdatedTs = OffsetDateTime.now(ZoneId.of("UTC"));

  private final RoleRecord roleRecord =
      new RoleRecord()
          .setId(roleId)
          .setName(name)
          .setCreatedBy(createdBy)
          .setLastUpdatedBy(lastUpdatedBy)
          .setCreatedTs(createdTs)
          .setLastUpdatedTs(lastUpdatedTs);

  private final List<RoleRecord> roleRecordList = Lists.newArrayList(roleRecord);

  private RoleMapper roleMapper;

  private RoleDao subject;

  @Before
  public void setUp() throws Exception {
    roleMapper = mock(RoleMapper.class);
    subject = new RoleDao(roleMapper);
  }

  @Test
  public void getAllRoles_returns_list_of_records() {
    when(roleMapper.getAllRoles()).thenReturn(roleRecordList);

    List<RoleRecord> actual = subject.getAllRoles();

    assertThat(actual).isNotEmpty();
    assertThat(actual).hasSameElementsAs(roleRecordList);
  }

  @Test
  public void getRoleById_returns_record_when_found() {
    when(roleMapper.getRoleById(roleId)).thenReturn(roleRecord);

    final Optional<RoleRecord> actual = subject.getRoleById(roleId);

    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get()).isEqualTo(roleRecord);
  }

  @Test
  public void getRoleById_returns_empty_when_record_not_found() {
    when(roleMapper.getRoleById(roleId)).thenReturn(null);

    final Optional<RoleRecord> actual = subject.getRoleById(roleId);

    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void getRoleByName_returns_record_when_found() {
    when(roleMapper.getRoleByName(name)).thenReturn(roleRecord);

    final Optional<RoleRecord> actual = subject.getRoleByName(name);

    assertThat(actual.isPresent()).isTrue();
    assertThat(actual.get()).isEqualTo(roleRecord);
  }

  @Test
  public void getRoleByName_returns_empty_when_record_not_found() {
    when(roleMapper.getRoleByName(name)).thenReturn(null);

    final Optional<RoleRecord> actual = subject.getRoleByName(name);

    assertThat(actual.isPresent()).isFalse();
  }
}
