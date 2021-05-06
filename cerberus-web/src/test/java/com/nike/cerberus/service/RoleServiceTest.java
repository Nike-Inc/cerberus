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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.dao.RoleDao;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.record.RoleRecord;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class RoleServiceTest {

  @Mock private RoleDao roleDao;

  @InjectMocks private RoleService roleService;

  @Before
  public void before() {
    initMocks(this);
  }

  @Test
  public void test_that_getRoleIdToStringMap_returns_valid_map() {
    Map<String, String> expected = new HashMap<>();
    expected.put("abc", "foo");

    when(roleDao.getAllRoles())
        .thenReturn(Arrays.asList(RoleRecord.builder().id("abc").name("foo").build()));

    Map<String, String> actual = roleService.getRoleIdToStringMap();
    assertEquals(expected, actual);
  }

  @Test
  public void testGetAllRolesWhenNoRoleRecordsArePresent() {
    Mockito.when(roleDao.getAllRoles()).thenReturn(Collections.emptyList());
    List<Role> allRoles = roleService.getAllRoles();
    Assert.assertTrue(allRoles.isEmpty());
  }

  @Test
  public void testGetAllRolesWhenRoleRecordsArePresent() {
    List<RoleRecord> roleRecords = new ArrayList<>();
    RoleRecord roleRecord = createRoleRecord();
    roleRecords.add(roleRecord);
    Mockito.when(roleDao.getAllRoles()).thenReturn(roleRecords);
    List<Role> allRoles = roleService.getAllRoles();
    Assert.assertFalse(allRoles.isEmpty());
    Assert.assertEquals(1, allRoles.size());
  }

  @Test
  public void testGetRoleByIdIfNoRoleIsPresentForGivenId() {
    Mockito.when(roleDao.getRoleById("id")).thenReturn(Optional.empty());
    Optional<Role> roleById = roleService.getRoleById("id");
    Assert.assertFalse(roleById.isPresent());
  }

  @Test
  public void testGetRoleByIdIfRoleIsPresentForGivenId() {
    RoleRecord roleRecord = createRoleRecord();
    Mockito.when(roleDao.getRoleById("id")).thenReturn(Optional.of(roleRecord));
    Optional<Role> roleById = roleService.getRoleById("id");
    Assert.assertTrue(roleById.isPresent());
  }

  @Test
  public void testGetRoleByIdIfNoRoleIsPresentForGivenName() {
    Mockito.when(roleDao.getRoleByName("name")).thenReturn(Optional.empty());
    Optional<Role> roleById = roleService.getRoleByName("name");
    Assert.assertFalse(roleById.isPresent());
  }

  @Test
  public void testGetRoleByIdIfRoleIsPresentForGivenName() {
    RoleRecord roleRecord = createRoleRecord();
    Mockito.when(roleDao.getRoleByName("name")).thenReturn(Optional.of(roleRecord));
    Optional<Role> roleById = roleService.getRoleByName("name");
    Assert.assertTrue(roleById.isPresent());
  }

  private RoleRecord createRoleRecord() {
    RoleRecord roleRecord =
        RoleRecord.builder()
            .id("id")
            .createdBy("user")
            .name("name")
            .lastUpdatedBy("user")
            .createdTs(OffsetDateTime.MAX)
            .lastUpdatedTs(OffsetDateTime.MAX)
            .build();
    return roleRecord;
  }
}
