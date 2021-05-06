package com.nike.cerberus.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.UserGroupPermissionRecord;
import com.nike.cerberus.record.UserGroupRecord;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UserGroupPermissionServiceTest {

  private UserGroupPermissionService userGroupPermissionService;

  private UserGroupDao userGroupDao;
  private UuidSupplier uuidSupplier;

  private RoleService roleService;

  @Before
  public void setUp() {
    userGroupDao = mock(UserGroupDao.class);
    uuidSupplier = new UuidSupplier();
    roleService = mock(RoleService.class);
    userGroupPermissionService =
        new UserGroupPermissionService(uuidSupplier, roleService, userGroupDao);
  }

  @Test
  public void testGrantUserGroupPermissionWhenRoleIsNotPresentForGivenRoleId() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.empty());
    List<ApiError> apiErrorList = new ArrayList<>();
    try {
      userGroupPermissionService.grantUserGroupPermission(
          "safeBoxId", userGroupPermission, "user", OffsetDateTime.MAX);
    } catch (ApiException apiException) {
      apiErrorList = apiException.getApiErrors();
    }
    assertFalse(apiErrorList.isEmpty());
    Assert.assertEquals(DefaultApiError.USER_GROUP_ROLE_ID_INVALID, apiErrorList.get(0));
  }

  @Test
  public void testGrantUserGroupPermissionWhenUserGroupRecordIsPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Role role = Mockito.mock(Role.class);
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Optional<UserGroupRecord> userGroupRecord = getUserGroup();
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(userGroupRecord);
    userGroupPermissionService.grantUserGroupPermission(
        "safeBoxId", userGroupPermission, "user", OffsetDateTime.MAX);
    Mockito.verify(userGroupDao)
        .createUserGroupPermission(Mockito.any(UserGroupPermissionRecord.class));
  }

  @Test
  public void testGrantUserGroupPermissionsWhenUserGroupRecordIsPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Role role = Mockito.mock(Role.class);
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Optional<UserGroupRecord> userGroupRecord = getUserGroup();
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(userGroupRecord);
    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    userGroupPermissionService.grantUserGroupPermissions(
        "safeBoxId", userGroupPermissions, "user", OffsetDateTime.MAX);
    Mockito.verify(userGroupDao)
        .createUserGroupPermission(Mockito.any(UserGroupPermissionRecord.class));
  }

  @Test
  public void testGrantUserGroupPermissionWhenUserGroupRecordIsNotPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Role role = Mockito.mock(Role.class);
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(Optional.empty());
    userGroupPermissionService.grantUserGroupPermission(
        "safeBoxId", userGroupPermission, "user", OffsetDateTime.MAX);
    Mockito.verify(userGroupDao)
        .createUserGroupPermission(Mockito.any(UserGroupPermissionRecord.class));
  }

  @Test
  public void testGrantUserGroupPermissionsWhenUserGroupRecordIsNotPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    Role role = Mockito.mock(Role.class);
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(Optional.empty());
    userGroupPermissionService.grantUserGroupPermissions(
        "safeBoxId", userGroupPermissions, "user", OffsetDateTime.MAX);
    Mockito.verify(userGroupDao)
        .createUserGroupPermission(Mockito.any(UserGroupPermissionRecord.class));
  }

  @Test
  public void testUpdateUserGroupPermissionWhenGroupRecordIsNotPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(Optional.empty());
    String exceptionMessage = "";
    List<ApiError> apiErrors = new ArrayList<>();
    try {
      userGroupPermissionService.updateUserGroupPermission(
          "safeBoId", userGroupPermission, "user", OffsetDateTime.MAX);
    } catch (ApiException apiException) {
      apiErrors = apiException.getApiErrors();
      exceptionMessage = apiException.getMessage();
    }
    String expectedExceptionMessage =
        "Unable to update permissions for user group name that doesn't exist.";
    Assert.assertFalse(apiErrors.isEmpty());
    Assert.assertEquals(expectedExceptionMessage, exceptionMessage);
  }

  @Test
  public void testUpdateUserGroupPermissionsWhenGroupRecordIsNotPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(Optional.empty());
    String exceptionMessage = "";
    List<ApiError> apiErrors = new ArrayList<>();
    try {
      userGroupPermissionService.updateUserGroupPermissions(
          "safeBoId", userGroupPermissions, "user", OffsetDateTime.MAX);
    } catch (ApiException apiException) {
      apiErrors = apiException.getApiErrors();
      exceptionMessage = apiException.getMessage();
    }
    String expectedExceptionMessage =
        "Unable to update permissions for user group name that doesn't exist.";
    Assert.assertFalse(apiErrors.isEmpty());
    Assert.assertEquals(expectedExceptionMessage, exceptionMessage);
  }

  @Test
  public void testUpdateUserGroupPermissionWhenGroupRecordIsPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Optional<UserGroupRecord> userGroupRecord = getUserGroup();
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(userGroupRecord);
    userGroupPermissionService.updateUserGroupPermission(
        "safeBoId", userGroupPermission, "user", OffsetDateTime.MAX);
    Mockito.verify(userGroupDao)
        .updateUserGroupPermission(Mockito.any(UserGroupPermissionRecord.class));
  }

  @Test
  public void testUpdateUserGroupPermissionsWhenGroupRecordIsPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    Optional<UserGroupRecord> userGroupRecord = getUserGroup();
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(userGroupRecord);
    userGroupPermissionService.updateUserGroupPermissions(
        "safeBoId", userGroupPermissions, "user", OffsetDateTime.MAX);
    Mockito.verify(userGroupDao)
        .updateUserGroupPermission(Mockito.any(UserGroupPermissionRecord.class));
  }

  @Test
  public void testRevokeUserGroupPermissionWhenGroupRecordIsNotPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(Optional.empty());
    String exceptionMessage = "";
    List<ApiError> apiErrors = new ArrayList<>();
    try {
      userGroupPermissionService.revokeUserGroupPermission("safeBoxId", userGroupPermission);
    } catch (ApiException apiException) {
      exceptionMessage = apiException.getMessage();
      apiErrors = apiException.getApiErrors();
    }
    String expectedExceptionMessage =
        "Unable to revoke permissions for user group name that doesn't exist.";
    Assert.assertFalse(apiErrors.isEmpty());
    Assert.assertEquals(expectedExceptionMessage, exceptionMessage);
  }

  @Test
  public void testRevokeUserGroupPermissionsWhenGroupRecordIsNotPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(Optional.empty());
    String exceptionMessage = "";
    List<ApiError> apiErrors = new ArrayList<>();
    try {
      userGroupPermissionService.revokeUserGroupPermissions("safeBoxId", userGroupPermissions);
    } catch (ApiException apiException) {
      exceptionMessage = apiException.getMessage();
      apiErrors = apiException.getApiErrors();
    }
    String expectedExceptionMessage =
        "Unable to revoke permissions for user group name that doesn't exist.";
    Assert.assertFalse(apiErrors.isEmpty());
    Assert.assertEquals(expectedExceptionMessage, exceptionMessage);
  }

  @Test
  public void testRevokeUserGroupPermissionWhenGroupRecordIsPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Optional<UserGroupRecord> userGroupRecord = getUserGroup();
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(userGroupRecord);
    userGroupPermissionService.revokeUserGroupPermission("safeBoxId", userGroupPermission);
    Mockito.verify(userGroupDao)
        .deleteUserGroupPermission("safeBoxId", userGroupRecord.get().getId());
  }

  @Test
  public void testRevokeUserGroupPermissionsWhenGroupRecordIsPresentForGivenName() {
    UserGroupPermission userGroupPermission =
        mockUserGroupPermissionWithNameAndRoleId("name", "roleId");
    Set<UserGroupPermission> userGroupPermissions = new HashSet<>();
    userGroupPermissions.add(userGroupPermission);
    Optional<UserGroupRecord> userGroupRecord = getUserGroup();
    Mockito.when(userGroupDao.getUserGroupByName("name")).thenReturn(userGroupRecord);
    userGroupPermissionService.revokeUserGroupPermissions("safeBoxId", userGroupPermissions);
    Mockito.verify(userGroupDao)
        .deleteUserGroupPermission("safeBoxId", userGroupRecord.get().getId());
  }

  @Test
  public void testGetUserGroupPermissionsForGivenSafeBoxIdWhenNoUserGroupPermissionRecordPresent() {
    Mockito.when(userGroupDao.getUserGroupPermissions("safeBoxId"))
        .thenReturn(Collections.emptyList());
    Set<UserGroupPermission> userGroupPermissions =
        userGroupPermissionService.getUserGroupPermissions("safeBoxId");
    Assert.assertTrue(userGroupPermissions.isEmpty());
  }

  @Test
  public void testGetUserGroupPermissionsForGivenSafeBoxIdWhenUserGroupPermissionRecordPresent() {
    UserGroupPermissionRecord userGroupPermissionRecord = getUserGroupPermissionRecord();
    List<UserGroupPermissionRecord> userGroupPermissionRecords = new ArrayList<>();
    userGroupPermissionRecords.add(userGroupPermissionRecord);
    Mockito.when(userGroupDao.getUserGroupPermissions("safeBoxId"))
        .thenReturn(userGroupPermissionRecords);
    Optional<UserGroupRecord> userGroupRecord = getUserGroup();
    Mockito.when(userGroupDao.getUserGroup("id")).thenReturn(userGroupRecord);
    Set<UserGroupPermission> userGroupPermissions =
        userGroupPermissionService.getUserGroupPermissions("safeBoxId");
    Assert.assertFalse(userGroupPermissions.isEmpty());
    Assert.assertEquals(1, userGroupPermissions.size());
  }

  @Test
  public void testGetTotalNumUniqueOwnerGroupsIfNoOwnerGroupsArePresent() {
    Mockito.when(roleService.getRoleByName("owner")).thenReturn(Optional.empty());
    String exceptionMessage = "";
    try {
      userGroupPermissionService.getTotalNumUniqueOwnerGroups();
    } catch (RuntimeException runtimeException) {
      exceptionMessage = runtimeException.getMessage();
    }
    assertEquals("Could not find ID for owner permissions role", exceptionMessage);
  }

  @Test
  public void testGetTotalNumUniqueOwnerGroupsIfOwnerRolesArePresent() {
    Role role = mockRoleWithRoleId();
    Mockito.when(roleService.getRoleByName("owner")).thenReturn(Optional.of(role));
    Mockito.when(userGroupDao.getTotalNumUniqueUserGroupsByRole("id")).thenReturn(4);
    int totalNumUniqueUserGroups = userGroupPermissionService.getTotalNumUniqueOwnerGroups();
    Assert.assertEquals(4, totalNumUniqueUserGroups);
  }

  @Test
  public void testGetTotalNumUniqueNonOwnerGroups() {
    Mockito.when(userGroupDao.getTotalNumUniqueNonOwnerGroups()).thenReturn(3);
    int totalNumUniqueNonOwnerGroups = userGroupPermissionService.getTotalNumUniqueNonOwnerGroups();
    Assert.assertEquals(3, totalNumUniqueNonOwnerGroups);
  }

  @Test
  public void testGetTotalNumUniqueUserGroups() {
    Mockito.when(userGroupDao.getTotalNumUniqueUserGroups()).thenReturn(2);
    int totalNumUniqueUserGroups = userGroupPermissionService.getTotalNumUniqueUserGroups();
    Assert.assertEquals(2, totalNumUniqueUserGroups);
  }

  @Test
  public void testDeleteUserGroupPermissions() {
    userGroupPermissionService.deleteUserGroupPermissions("safeBoxId");
    Mockito.verify(userGroupDao).deleteUserGroupPermissions("safeBoxId");
  }

  private Role mockRoleWithRoleId() {
    Role role = Mockito.mock(Role.class);
    Mockito.when(role.getId()).thenReturn("id");
    return role;
  }

  private Optional<UserGroupRecord> getUserGroup() {
    UserGroupRecord userGroupRecords = new UserGroupRecord();
    userGroupRecords.setName("name");
    userGroupRecords.setId("id");
    return Optional.of(userGroupRecords);
  }

  private UserGroupPermission mockUserGroupPermissionWithNameAndRoleId(String name, String roleId) {
    UserGroupPermission userGroupPermission = Mockito.mock(UserGroupPermission.class);
    Mockito.when(userGroupPermission.getName()).thenReturn(name);
    Mockito.when(userGroupPermission.getRoleId()).thenReturn(roleId);
    return userGroupPermission;
  }

  private UserGroupPermissionRecord getUserGroupPermissionRecord() {
    UserGroupPermissionRecord userGroupPermissionRecord =
        UserGroupPermissionRecord.builder()
            .userGroupId("userGroupId")
            .id("id")
            .roleId("roleId")
            .sdboxId("sdBoxId")
            .createdBy("user")
            .lastUpdatedBy("user")
            .createdTs(OffsetDateTime.MAX)
            .lastUpdatedTs(OffsetDateTime.MAX)
            .userGroupId("id")
            .build();
    return userGroupPermissionRecord;
  }
}
