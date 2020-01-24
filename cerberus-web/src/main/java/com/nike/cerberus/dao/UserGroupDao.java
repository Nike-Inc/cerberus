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

import com.nike.cerberus.mapper.UserGroupMapper;
import com.nike.cerberus.record.UserGroupPermissionRecord;
import com.nike.cerberus.record.UserGroupRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Data access layer for the user group and permissions data. */
@Component
public class UserGroupDao {

  private final UserGroupMapper userGroupMapper;

  @Autowired
  public UserGroupDao(final UserGroupMapper userGroupMapper) {
    this.userGroupMapper = userGroupMapper;
  }

  public Optional<UserGroupRecord> getUserGroup(final String id) {
    return Optional.ofNullable(userGroupMapper.getUserGroup(id));
  }

  public Optional<UserGroupRecord> getUserGroupByName(final String name) {
    return Optional.ofNullable(userGroupMapper.getUserGroupByName(name));
  }

  public List<UserGroupRecord> getUserGroupsByRole(
      final String safeDepositBoxId, final String roleId) {
    return userGroupMapper.getUserGroupsByRole(safeDepositBoxId, roleId);
  }

  public int createUserGroup(final UserGroupRecord record) {
    return userGroupMapper.createUserGroup(record);
  }

  public List<UserGroupPermissionRecord> getUserGroupPermissions(final String safeDepositBoxId) {
    return userGroupMapper.getUserGroupPermissions(safeDepositBoxId);
  }

  public int getTotalNumUniqueUserGroupsByRole(String roleId) {
    return userGroupMapper.getTotalNumUniqueUserGroupsByRole(roleId);
  }

  public int getTotalNumUniqueNonOwnerGroups() {
    return userGroupMapper.getTotalNumUniqueNonOwnerGroups();
  }

  public int getTotalNumUniqueUserGroups() {
    return userGroupMapper.getTotalNumUniqueUserGroups();
  }

  public int createUserGroupPermission(final UserGroupPermissionRecord record) {
    return userGroupMapper.createUserGroupPermission(record);
  }

  public int updateUserGroupPermission(final UserGroupPermissionRecord record) {
    return userGroupMapper.updateUserGroupPermission(record);
  }

  public int deleteUserGroupPermission(final String safeDepositBoxId, final String userGroupId) {
    return userGroupMapper.deleteUserGroupPermission(safeDepositBoxId, userGroupId);
  }

  public int deleteUserGroupPermissions(final String safeDepositBoxId) {
    return userGroupMapper.deleteUserGroupPermissions(safeDepositBoxId);
  }
}
