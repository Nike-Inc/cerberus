package com.nike.cerberus.service;

import static org.junit.Assert.assertNull;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.domain.IamPrincipalPermission;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.record.AwsIamRolePermissionRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.UuidSupplier;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class IamPrincipalPermissionServiceTest {

  @Mock private UuidSupplier uuidSupplier;

  @Mock private RoleService roleService;

  @Mock private AwsIamRoleDao awsIamRoleDao;

  @InjectMocks private IamPrincipalPermissionService iamPrincipalPermissionService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDeletePermissions() {
    iamPrincipalPermissionService.deleteIamPrincipalPermissions("boxId");
    Mockito.verify(awsIamRoleDao).deleteIamRolePermissions("boxId");
  }

  @Test(expected = ApiException.class)
  public void testRevokePermissionWhenIamRoleIsNotPresent() {
    IamPrincipalPermission iamPrincipalPermission = Mockito.mock(IamPrincipalPermission.class);
    Mockito.when(iamPrincipalPermission.getIamPrincipalArn()).thenReturn("arn");
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    iamPrincipalPermissionService.revokeIamPrincipalPermission("boxId", iamPrincipalPermission);
  }

  @Test
  public void testRevokePermissionWhenIamRoleIsPresent() {
    try {
      IamPrincipalPermission iamPrincipalPermission = Mockito.mock(IamPrincipalPermission.class);

      Mockito.when(iamPrincipalPermission.getIamPrincipalArn()).thenReturn("arn");
      AwsIamRoleRecord awsIamRoleRecord = Mockito.mock(AwsIamRoleRecord.class);
      Mockito.when(awsIamRoleRecord.getId()).thenReturn("id");
      Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.of(awsIamRoleRecord));

      iamPrincipalPermissionService.revokeIamPrincipalPermission("boxId", iamPrincipalPermission);
      awsIamRoleDao.deleteIamRolePermission("boxId", "id");
    } catch (Exception e) {
      assertNull(e);
    }
  }

  @Test(expected = ApiException.class)
  public void testRevokePermissionsWhenIamRoleIsNotPresent() {
    IamPrincipalPermission iamPrincipalPermission = Mockito.mock(IamPrincipalPermission.class);
    Mockito.when(iamPrincipalPermission.getIamPrincipalArn()).thenReturn("arn");
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();
    iamPrincipalPermissions.add(iamPrincipalPermission);
    iamPrincipalPermissionService.revokeIamPrincipalPermissions("boxId", iamPrincipalPermissions);
  }

  @Test
  public void testRevokePermissionsWhenIamRoleIsPresent() {
    try {
      IamPrincipalPermission iamPrincipalPermission = Mockito.mock(IamPrincipalPermission.class);

      Mockito.when(iamPrincipalPermission.getIamPrincipalArn()).thenReturn("arn");
      AwsIamRoleRecord awsIamRoleRecord = Mockito.mock(AwsIamRoleRecord.class);
      Mockito.when(awsIamRoleRecord.getId()).thenReturn("id");
      Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.of(awsIamRoleRecord));
      Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();
      iamPrincipalPermissions.add(iamPrincipalPermission);
      iamPrincipalPermissionService.revokeIamPrincipalPermissions("boxId", iamPrincipalPermissions);
      awsIamRoleDao.deleteIamRolePermission("boxId", "id");
    } catch (Exception e) {
      assertNull(e);
    }
  }

  @Test
  public void testGetIamPrincipalPermissionsWhenIamRoleIsNotPresent() {
    Mockito.when(awsIamRoleDao.getIamRolePermissions("boxId")).thenReturn(Collections.EMPTY_LIST);
    Set<IamPrincipalPermission> boxIds =
        iamPrincipalPermissionService.getIamPrincipalPermissions("boxId");
    Assert.assertTrue(boxIds.isEmpty());
  }

  @Test
  public void testGetIamPrincipalPermissionsWhenIamRoleIsPresent() {
    List<AwsIamRolePermissionRecord> awsIamRolePermissionRecords = new ArrayList<>();
    AwsIamRolePermissionRecord awsIamRolePermissionRecord =
        new AwsIamRolePermissionRecord()
            .setId("id")
            .setCreatedBy("createdBy")
            .setLastUpdatedBy("lastUpdatedBy")
            .setRoleId("roleId")
            .setCreatedTs(OffsetDateTime.MAX)
            .setLastUpdatedTs(OffsetDateTime.MAX);
    awsIamRolePermissionRecords.add(awsIamRolePermissionRecord);
    AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord().setAwsIamRoleArn("awsIamRoleArn");
    Mockito.when(awsIamRoleDao.getIamRolePermissions("boxId"))
        .thenReturn(awsIamRolePermissionRecords);
    Mockito.when(awsIamRoleDao.getIamRoleById(Mockito.anyString()))
        .thenReturn(Optional.of(awsIamRoleRecord));
    Set<IamPrincipalPermission> boxIds =
        iamPrincipalPermissionService.getIamPrincipalPermissions("boxId");
    Assert.assertTrue(boxIds.size() == 1);
    boxIds.forEach(
        iamPrincipalPermission -> {
          Assert.assertEquals("id", iamPrincipalPermission.getId());
          Assert.assertEquals("lastUpdatedBy", iamPrincipalPermission.getLastUpdatedBy());
          Assert.assertEquals("createdBy", iamPrincipalPermission.getCreatedBy());
          Assert.assertEquals("roleId", iamPrincipalPermission.getRoleId());
          Assert.assertEquals("awsIamRoleArn", iamPrincipalPermission.getIamPrincipalArn());
          Assert.assertEquals(OffsetDateTime.MAX, iamPrincipalPermission.getCreatedTs());
          Assert.assertEquals(OffsetDateTime.MAX, iamPrincipalPermission.getLastUpdatedTs());
        });
  }

  @Test(expected = ApiException.class)
  public void testIamPrincipalPermissionUpdateWhenIamRoleIsNotPresent() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    iamPrincipalPermissionService.updateIamPrincipalPermission(
        "boxId", iamPrincipalPermission, "user", OffsetDateTime.MAX);
  }

  @Test
  public void testIamPrincipalPermissionUpdateWhenIamRoleIsPresent() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord();
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.of(awsIamRoleRecord));
    iamPrincipalPermissionService.updateIamPrincipalPermission(
        "boxId", iamPrincipalPermission, "user", OffsetDateTime.MAX);
    Mockito.verify(awsIamRoleDao)
        .updateIamRolePermission(Mockito.any(AwsIamRolePermissionRecord.class));
  }

  @Test(expected = ApiException.class)
  public void testIamPrincipalPermissionsUpdateWhenIamRoleIsNotPresent() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();
    iamPrincipalPermissions.add(iamPrincipalPermission);
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    iamPrincipalPermissionService.updateIamPrincipalPermissions(
        "boxId", iamPrincipalPermissions, "user", OffsetDateTime.MAX);
  }

  @Test
  public void testIamPrincipalPermissionsUpdateWhenIamRoleIsPresent() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord();
    Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();
    iamPrincipalPermissions.add(iamPrincipalPermission);
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.of(awsIamRoleRecord));
    iamPrincipalPermissionService.updateIamPrincipalPermissions(
        "boxId", iamPrincipalPermissions, "user", OffsetDateTime.MAX);
    Mockito.verify(awsIamRoleDao)
        .updateIamRolePermission(Mockito.any(AwsIamRolePermissionRecord.class));
  }

  @Test(expected = ApiException.class)
  public void testGrantIamPermissionIfRoleIsNotPresentByRoleId() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    iamPrincipalPermission.setRoleId("roleId");
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.empty());
    iamPrincipalPermissionService.grantIamPrincipalPermission(
        "boxId", iamPrincipalPermission, "user", OffsetDateTime.MAX);
  }

  @Test
  public void testGrantIamPermissionIfRoleIsPresentByRoleIdAndRoleRecordNotFound() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    iamPrincipalPermission.setRoleId("roleId");
    Role role = new Role();
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Mockito.when(uuidSupplier.get()).thenReturn("uuid");
    iamPrincipalPermissionService.grantIamPrincipalPermission(
        "boxId", iamPrincipalPermission, "user", OffsetDateTime.MAX);
    Mockito.verify(awsIamRoleDao).createIamRole(Mockito.any(AwsIamRoleRecord.class));
    Mockito.verify(awsIamRoleDao)
        .createIamRolePermission(Mockito.any(AwsIamRolePermissionRecord.class));
  }

  @Test
  public void testGrantIamPermissionIfRoleIsPresentByRoleIdAndRoleRecordFound() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    iamPrincipalPermission.setRoleId("roleId");
    Role role = new Role();
    AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord();
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.of(awsIamRoleRecord));
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Mockito.when(uuidSupplier.get()).thenReturn("uuid");
    iamPrincipalPermissionService.grantIamPrincipalPermission(
        "boxId", iamPrincipalPermission, "user", OffsetDateTime.MAX);
    Mockito.verify(awsIamRoleDao, Mockito.never())
        .createIamRole(Mockito.any(AwsIamRoleRecord.class));
    Mockito.verify(awsIamRoleDao)
        .createIamRolePermission(Mockito.any(AwsIamRolePermissionRecord.class));
  }

  @Test(expected = ApiException.class)
  public void testGrantIamPermissionsIfRoleIsNotPresentByRoleId() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    iamPrincipalPermission.setRoleId("roleId");
    Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();
    iamPrincipalPermissions.add(iamPrincipalPermission);
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.empty());
    iamPrincipalPermissionService.grantIamPrincipalPermissions(
        "boxId", iamPrincipalPermissions, "user", OffsetDateTime.MAX);
  }

  @Test
  public void testGrantIamPermissionsIfRoleIsPresentByRoleIdAndRoleRecordNotFound() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    iamPrincipalPermission.setRoleId("roleId");
    Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();
    iamPrincipalPermissions.add(iamPrincipalPermission);
    Role role = new Role();
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.empty());
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Mockito.when(uuidSupplier.get()).thenReturn("uuid");
    iamPrincipalPermissionService.grantIamPrincipalPermissions(
        "boxId", iamPrincipalPermissions, "user", OffsetDateTime.MAX);
    Mockito.verify(awsIamRoleDao).createIamRole(Mockito.any(AwsIamRoleRecord.class));
    Mockito.verify(awsIamRoleDao)
        .createIamRolePermission(Mockito.any(AwsIamRolePermissionRecord.class));
  }

  @Test
  public void testGrantIamPermissionsIfRoleIsPresentByRoleIdAndRoleRecordFound() {
    IamPrincipalPermission iamPrincipalPermission = new IamPrincipalPermission();
    iamPrincipalPermission.setIamPrincipalArn("arn");
    iamPrincipalPermission.setRoleId("roleId");
    Set<IamPrincipalPermission> iamPrincipalPermissions = new HashSet<>();
    iamPrincipalPermissions.add(iamPrincipalPermission);
    Role role = new Role();
    AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord();
    Mockito.when(awsIamRoleDao.getIamRole("arn")).thenReturn(Optional.of(awsIamRoleRecord));
    Mockito.when(roleService.getRoleById("roleId")).thenReturn(Optional.of(role));
    Mockito.when(uuidSupplier.get()).thenReturn("uuid");
    iamPrincipalPermissionService.grantIamPrincipalPermissions(
        "boxId", iamPrincipalPermissions, "user", OffsetDateTime.MAX);
    Mockito.verify(awsIamRoleDao, Mockito.never())
        .createIamRole(Mockito.any(AwsIamRoleRecord.class));
    Mockito.verify(awsIamRoleDao)
        .createIamRolePermission(Mockito.any(AwsIamRolePermissionRecord.class));
  }
}
