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

import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.UserGroupPermissionRecord;
import com.nike.cerberus.record.UserGroupRecord;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Provides operations for granting, updating and revoking user group permissions. */
@Component
public class UserGroupPermissionService {

  private static final String OWNER_ROLE_NAME = "owner";

  private final UuidSupplier uuidSupplier;

  private final RoleService roleService;

  private final UserGroupDao userGroupDao;

  @Autowired
  public UserGroupPermissionService(
      final UuidSupplier uuidSupplier,
      final RoleService roleService,
      final UserGroupDao userGroupDao) {

    this.uuidSupplier = uuidSupplier;
    this.roleService = roleService;
    this.userGroupDao = userGroupDao;
  }

  /**
   * Grants a set of user group permissions.
   *
   * @param safeDepositBoxId The safe deposit box id
   * @param userGroupPermissionSet The set of user group permissions
   * @param user The user making the changes
   * @param dateTime The time of the changes
   */
  @Transactional
  public void grantUserGroupPermissions(
      final String safeDepositBoxId,
      final Set<UserGroupPermission> userGroupPermissionSet,
      final String user,
      final OffsetDateTime dateTime) {
    for (final UserGroupPermission userGroupPermission : userGroupPermissionSet) {
      grantUserGroupPermission(safeDepositBoxId, userGroupPermission, user, dateTime);
    }
  }

  /**
   * Grants a user group permission.
   *
   * @param safeDepositBoxId The safe deposit box id
   * @param userGroupPermission The user group permission
   * @param user The user making the changes
   * @param dateTime The time of the changes
   */
  @Transactional
  public void grantUserGroupPermission(
      final String safeDepositBoxId,
      final UserGroupPermission userGroupPermission,
      final String user,
      final OffsetDateTime dateTime) {
    final Optional<UserGroupRecord> possibleUserGroupRecord =
        userGroupDao.getUserGroupByName(userGroupPermission.getName());

    final Optional<Role> role = roleService.getRoleById(userGroupPermission.getRoleId());

    if (role.isEmpty()) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.USER_GROUP_ROLE_ID_INVALID)
          .build();
    }

    String userGroupId;
    if (possibleUserGroupRecord.isPresent()) {
      userGroupId = possibleUserGroupRecord.get().getId();
    } else {
      userGroupId = uuidSupplier.get();
      UserGroupRecord userGroupRecord = new UserGroupRecord();
      userGroupRecord.setId(userGroupId);
      userGroupRecord.setName(userGroupPermission.getName());
      userGroupRecord.setCreatedBy(user);
      userGroupRecord.setLastUpdatedBy(user);
      userGroupRecord.setCreatedTs(dateTime);
      userGroupRecord.setLastUpdatedTs(dateTime);
      userGroupDao.createUserGroup(userGroupRecord);
    }

    UserGroupPermissionRecord permissionsRecord = new UserGroupPermissionRecord();
    permissionsRecord.setId(uuidSupplier.get());
    permissionsRecord.setUserGroupId(userGroupId);
    permissionsRecord.setRoleId(userGroupPermission.getRoleId());
    permissionsRecord.setSdboxId(safeDepositBoxId);
    permissionsRecord.setCreatedBy(user);
    permissionsRecord.setLastUpdatedBy(user);
    permissionsRecord.setCreatedTs(dateTime);
    permissionsRecord.setLastUpdatedTs(dateTime);
    userGroupDao.createUserGroupPermission(permissionsRecord);
  }

  /**
   * Updates a set of user group permissions.
   *
   * @param safeDepositBoxId The safe deposit box id
   * @param userGroupPermissionSet The set of user group permissions
   * @param user The user making the changes
   * @param dateTime The time of the changes
   */
  @Transactional
  public void updateUserGroupPermissions(
      final String safeDepositBoxId,
      final Set<UserGroupPermission> userGroupPermissionSet,
      final String user,
      final OffsetDateTime dateTime) {
    for (final UserGroupPermission userGroupPermission : userGroupPermissionSet) {
      updateUserGroupPermission(safeDepositBoxId, userGroupPermission, user, dateTime);
    }
  }

  /**
   * Updates a user group permission.
   *
   * @param safeDepositBoxId The safe deposit box id
   * @param userGroupPermission The user group permission
   * @param user The user making the changes
   * @param dateTime The time of the changes
   */
  @Transactional
  public void updateUserGroupPermission(
      final String safeDepositBoxId,
      final UserGroupPermission userGroupPermission,
      final String user,
      final OffsetDateTime dateTime) {

    final Optional<UserGroupRecord> possibleUserGroupRecord =
        userGroupDao.getUserGroupByName(userGroupPermission.getName());

    if (possibleUserGroupRecord.isEmpty()) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
          .withExceptionMessage(
              "Unable to update permissions for user group name that doesn't exist.")
          .build();
    }

    UserGroupPermissionRecord record = new UserGroupPermissionRecord();
    record.setSdboxId(safeDepositBoxId);
    record.setUserGroupId(possibleUserGroupRecord.get().getId());
    record.setRoleId(userGroupPermission.getRoleId());
    record.setLastUpdatedBy(user);
    record.setLastUpdatedTs(dateTime);
    userGroupDao.updateUserGroupPermission(record);
  }

  /**
   * Revokes a set of user group permissions.
   *
   * @param safeDepositBoxId The safe deposit box id
   * @param userGroupPermissionSet The set of user group permissions
   */
  @Transactional
  public void revokeUserGroupPermissions(
      final String safeDepositBoxId, final Set<UserGroupPermission> userGroupPermissionSet) {
    for (final UserGroupPermission userGroupPermission : userGroupPermissionSet) {
      revokeUserGroupPermission(safeDepositBoxId, userGroupPermission);
    }
  }

  /**
   * Revokes a user group permission.
   *
   * @param safeDepositBoxId The safe deposit box id
   * @param userGroupPermission The user group permission
   */
  @Transactional
  public void revokeUserGroupPermission(
      final String safeDepositBoxId, final UserGroupPermission userGroupPermission) {
    final Optional<UserGroupRecord> userGroupRecord =
        userGroupDao.getUserGroupByName(userGroupPermission.getName());

    if (userGroupRecord.isEmpty()) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
          .withExceptionMessage(
              "Unable to revoke permissions for user group name that doesn't exist.")
          .build();
    }

    userGroupDao.deleteUserGroupPermission(safeDepositBoxId, userGroupRecord.get().getId());
  }

  public Set<UserGroupPermission> getUserGroupPermissions(final String safeDepositBoxId) {
    final Set<UserGroupPermission> permissionsSet = Sets.newHashSet();
    final List<UserGroupPermissionRecord> permissionRecords =
        userGroupDao.getUserGroupPermissions(safeDepositBoxId);

    permissionRecords.forEach(
        r -> {
          final Optional<UserGroupRecord> userGroupRecord =
              userGroupDao.getUserGroup(r.getUserGroupId());

          if (userGroupRecord.isPresent()) {
            UserGroupPermission permission = new UserGroupPermission();
            permission.setId(r.getId());
            permission.setRoleId(r.getRoleId());
            permission.setName(userGroupRecord.get().getName());
            permission.setCreatedBy(r.getCreatedBy());
            permission.setCreatedTs(r.getCreatedTs());
            permission.setLastUpdatedBy(r.getLastUpdatedBy());
            permission.setLastUpdatedTs(r.getLastUpdatedTs());
            permissionsSet.add(permission);
          }
        });

    return permissionsSet;
  }

  public int getTotalNumUniqueOwnerGroups() {
    Role ownerRole =
        roleService
            .getRoleByName(OWNER_ROLE_NAME)
            .orElseThrow(
                () -> new RuntimeException("Could not find ID for owner permissions role"));

    return userGroupDao.getTotalNumUniqueUserGroupsByRole(ownerRole.getId());
  }

  public int getTotalNumUniqueNonOwnerGroups() {
    return userGroupDao.getTotalNumUniqueNonOwnerGroups();
  }

  public int getTotalNumUniqueUserGroups() {
    return userGroupDao.getTotalNumUniqueUserGroups();
  }

  @Transactional
  public void deleteUserGroupPermissions(final String safeDepositBoxId) {
    userGroupDao.deleteUserGroupPermissions(safeDepositBoxId);
  }
}
