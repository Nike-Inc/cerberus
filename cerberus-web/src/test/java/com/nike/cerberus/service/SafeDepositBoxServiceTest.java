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

import static java.time.ZoneOffset.UTC;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.PrincipalType;
import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.IamPrincipalPermission;
import com.nike.cerberus.domain.IamRolePermission;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.SafeDepositBoxSummary;
import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.record.RoleRecord;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SafeDepositBoxServiceTest {

  @Mock private SafeDepositBoxDao safeDepositBoxDao;

  @Mock private UserGroupDao userGroupDao;

  @Mock private UuidSupplier uuidSupplier;

  @Mock private CategoryService categoryService;

  @Mock private RoleService roleService;

  @Mock private UserGroupPermissionService userGroupPermissionService;

  @Mock private IamPrincipalPermissionService iamPrincipalPermissionService;

  @Mock private Slugger slugger;

  @Mock private DateTimeSupplier dateTimeSupplier;

  @Mock private AwsIamRoleArnParser awsIamRoleArnParser;

  @Mock private SecureDataService secureDataService;

  @Mock private SecureDataVersionDao secureDataVersionDao;

  @InjectMocks private SafeDepositBoxService safeDepositBoxService;

  private SafeDepositBoxService safeDepositBoxServiceSpy;

  @Before
  public void before() {
    initMocks(this);

    safeDepositBoxServiceSpy = spy(safeDepositBoxService);
  }

  @Test
  public void
      test_that_restore_safe_deposit_box_creates_with_expected_sdb_record_from_safe_depot_box_object() {
    String id = "111";
    String categoryId = "222";
    String readId = "333";
    String sdbName = "HEALTH CHECK BUCKET";

    SafeDepositBoxV2 sdbObject =
        SafeDepositBoxV2.builder()
            .id(id)
            .path("app/health-check-bucket/")
            .categoryId(categoryId)
            .name(sdbName)
            .owner("Lst-Squad.Carebears")
            .description("This SDB is read by the Health Check Lambda...")
            .createdTs(OffsetDateTime.parse("2016-09-08T15:39:31Z"))
            .lastUpdatedTs(OffsetDateTime.parse("2016-12-13T17:28:00Z"))
            .createdBy("justin.field@nike.com")
            .lastUpdatedBy("todd.lisonbee@nike.com")
            .build();

    Set<UserGroupPermission> userPerms = new HashSet<>();
    userPerms.add(
        UserGroupPermission.builder().name("Foundation.Prod.Support").roleId(readId).build());
    userPerms.add(UserGroupPermission.builder().name("Lst-NIKE.FOO.ISL").roleId(readId).build());
    sdbObject.setUserGroupPermissions(userPerms);

    Set<IamPrincipalPermission> iamPerms = new HashSet<>();
    iamPerms.add(
        IamPrincipalPermission.builder()
            .iamPrincipalArn("arn:aws:iam::1111111111:role/lambda_prod_healthcheck")
            .roleId(readId)
            .build());
    sdbObject.setIamPrincipalPermissions(iamPerms);

    sdbObject.setUserGroupPermissions(userPerms);
    sdbObject.setIamPrincipalPermissions(iamPerms);

    SafeDepositBoxRecord boxToStore = new SafeDepositBoxRecord();
    boxToStore.setId(sdbObject.getId());
    boxToStore.setPath(sdbObject.getPath());
    boxToStore.setCategoryId(sdbObject.getCategoryId());
    boxToStore.setName(sdbObject.getName());
    boxToStore.setDescription(sdbObject.getDescription());
    boxToStore.setCreatedTs(sdbObject.getCreatedTs());
    boxToStore.setLastUpdatedTs(sdbObject.getLastUpdatedTs());
    boxToStore.setCreatedBy(sdbObject.getCreatedBy());
    boxToStore.setLastUpdatedBy(sdbObject.getLastUpdatedBy());

    when(safeDepositBoxDao.getSafeDepositBox(sdbObject.getId()))
        .thenReturn(Optional.ofNullable(null));
    doNothing().when(safeDepositBoxServiceSpy).addOwnerPermission(any(), any());

    safeDepositBoxServiceSpy.restoreSafeDepositBox(sdbObject, "admin-user");

    verify(safeDepositBoxDao, times(1)).createSafeDepositBox(boxToStore);
  }

  @Test
  public void
      test_that_restore_safe_deposit_box_updates_with_expected_sdb_record_from_safe_depot_box_object_when_the_sdb_already_exists() {
    String id = "111";
    String categoryId = "222";
    String readId = "333";
    String sdbName = "HEALTH CHECK BUCKET";
    String sdbId = "asdf-1231-23sad-asd";

    SafeDepositBoxV2 sdbObject =
        SafeDepositBoxV2.builder()
            .id(id)
            .path("app/health-check-bucket/")
            .categoryId(categoryId)
            .name(sdbName)
            .owner("Lst-Squad.Carebears")
            .description("This SDB is read by the Health Check Lambda...")
            .createdTs(OffsetDateTime.parse("2016-09-08T15:39:31Z"))
            .lastUpdatedTs(OffsetDateTime.parse("2016-12-13T17:28:00Z"))
            .createdBy("justin.field@nike.com")
            .lastUpdatedBy("todd.lisonbee@nike.com")
            .build();

    Set<UserGroupPermission> userPerms = new HashSet<>();
    userPerms.add(
        UserGroupPermission.builder().name("Foundation.Prod.Support").roleId(readId).build());
    userPerms.add(UserGroupPermission.builder().name("Lst-NIKE.FOO.ISL").roleId(readId).build());
    sdbObject.setUserGroupPermissions(userPerms);

    Set<IamPrincipalPermission> iamPerms = new HashSet<>();
    iamPerms.add(
        IamPrincipalPermission.builder()
            .iamPrincipalArn("arn:aws:iam::1111111111:role/lambda_prod_healthcheck")
            .roleId(readId)
            .build());
    sdbObject.setIamPrincipalPermissions(iamPerms);

    sdbObject.setUserGroupPermissions(userPerms);
    sdbObject.setIamPrincipalPermissions(iamPerms);

    SafeDepositBoxRecord boxToStore = new SafeDepositBoxRecord();
    boxToStore.setId(sdbObject.getId());
    boxToStore.setPath(sdbObject.getPath());
    boxToStore.setCategoryId(sdbObject.getCategoryId());
    boxToStore.setName(sdbObject.getName());
    boxToStore.setDescription(sdbObject.getDescription());
    boxToStore.setCreatedTs(sdbObject.getCreatedTs());
    boxToStore.setLastUpdatedTs(sdbObject.getLastUpdatedTs());
    boxToStore.setCreatedBy(sdbObject.getCreatedBy());
    boxToStore.setLastUpdatedBy(sdbObject.getLastUpdatedBy());

    SafeDepositBoxRecord existingRecord = new SafeDepositBoxRecord();
    existingRecord.setId(sdbId);
    when(safeDepositBoxDao.getSafeDepositBox(sdbObject.getId()))
        .thenReturn(Optional.of(existingRecord));
    doNothing().when(safeDepositBoxServiceSpy).updateOwner(any(), any(), any(), any());
    doNothing()
        .when(safeDepositBoxServiceSpy)
        .modifyUserGroupPermissions(any(), any(), any(), any());
    doNothing()
        .when(safeDepositBoxServiceSpy)
        .modifyIamPrincipalPermissions(any(), any(), any(), any());
    doReturn(sdbObject).when(safeDepositBoxServiceSpy).getSDBFromRecordV2(any());

    safeDepositBoxServiceSpy.restoreSafeDepositBox(sdbObject, "admin-user");

    verify(safeDepositBoxDao, times(1)).fullUpdateSafeDepositBox(boxToStore);
  }

  @Test
  public void test_that_convertSafeDepositBoxV1ToV2_creates_expected_safe_deposit_box_v2() {

    String id = "id";
    String name = "name";
    String description = "description";
    String path = "path";
    String categoryId = "category id";
    String createdBy = "created by";
    String lastUpdatedBy = "last updated by";
    OffsetDateTime createdTs = OffsetDateTime.now();
    OffsetDateTime lastUpdatedTs = OffsetDateTime.now();
    String owner = "owner";
    String accountId = "123";
    String roleName = "abc";
    String arn = "arn:aws:iam::123:role/abc";
    String roleId = "role id";

    Set<UserGroupPermission> userGroupPermissions = Sets.newHashSet();
    UserGroupPermission userGroupPermission = UserGroupPermission.builder().build();
    userGroupPermissions.add(userGroupPermission);

    Set<IamPrincipalPermission> iamRolePermissions = Sets.newHashSet();
    IamPrincipalPermission iamRolePermission =
        IamPrincipalPermission.builder().iamPrincipalArn(arn).roleId(roleId).build();
    iamRolePermissions.add(iamRolePermission);

    SafeDepositBoxV2 safeDepositBoxV2 =
        SafeDepositBoxV2.builder()
            .id(id)
            .name(name)
            .description(description)
            .path(path)
            .categoryId(categoryId)
            .createdBy(createdBy)
            .lastUpdatedBy(lastUpdatedBy)
            .createdTs(createdTs)
            .lastUpdatedTs(lastUpdatedTs)
            .owner(owner)
            .userGroupPermissions(userGroupPermissions)
            .iamPrincipalPermissions(iamRolePermissions)
            .build();

    when(awsIamRoleArnParser.getAccountId(arn)).thenReturn(accountId);
    when(awsIamRoleArnParser.getRoleName(arn)).thenReturn(roleName);

    SafeDepositBoxV1 resultantSDBV1 =
        safeDepositBoxService.convertSafeDepositBoxV2ToV1(safeDepositBoxV2);

    SafeDepositBoxV1 expectedSdbV1 =
        SafeDepositBoxV1.builder()
            .id(id)
            .name(name)
            .description(description)
            .path(path)
            .categoryId(categoryId)
            .createdBy(createdBy)
            .lastUpdatedBy(lastUpdatedBy)
            .createdTs(createdTs)
            .lastUpdatedTs(lastUpdatedTs)
            .owner(owner)
            .userGroupPermissions(userGroupPermissions)
            .build();
    Set<IamRolePermission> expectedIamRolePermissionsV1 = Sets.newHashSet();
    IamRolePermission expectedIamRolePermission =
        IamRolePermission.builder()
            .accountId(accountId)
            .iamRoleName(roleName)
            .roleId(roleId)
            .build();
    expectedIamRolePermissionsV1.add(expectedIamRolePermission);
    expectedSdbV1.setIamRolePermissions(expectedIamRolePermissionsV1);

    assertEquals(expectedSdbV1, resultantSDBV1);
  }

  @Test
  public void test_that_convertSafeDepositBoxV2ToV1_creates_expected_safe_deposit_box_v1() {

    String id = "id";
    String name = "name";
    String description = "description";
    String path = "path";
    String categoryId = "category id";
    String createdBy = "created by";
    String lastUpdatedBy = "last updated by";
    OffsetDateTime createdTs = OffsetDateTime.now();
    OffsetDateTime lastUpdatedTs = OffsetDateTime.now();
    String owner = "owner";
    String accountId = "123";
    String roleName = "abc";
    String arn = "arn:aws:iam::123:role/abc";
    String roleId = "role id";

    Set<UserGroupPermission> userGroupPermissions = Sets.newHashSet();
    UserGroupPermission userGroupPermission = UserGroupPermission.builder().build();
    userGroupPermissions.add(userGroupPermission);

    Set<IamRolePermission> iamRolePermissions = Sets.newHashSet();
    IamRolePermission iamRolePermission =
        IamRolePermission.builder()
            .accountId(accountId)
            .iamRoleName(roleName)
            .roleId(roleId)
            .build();
    iamRolePermissions.add(iamRolePermission);

    SafeDepositBoxV1 safeDepositBoxV1 =
        SafeDepositBoxV1.builder()
            .id(id)
            .name(name)
            .description(description)
            .path(path)
            .categoryId(categoryId)
            .createdBy(createdBy)
            .lastUpdatedBy(lastUpdatedBy)
            .createdTs(createdTs)
            .lastUpdatedTs(lastUpdatedTs)
            .owner(owner)
            .userGroupPermissions(userGroupPermissions)
            .iamRolePermissions(iamRolePermissions)
            .build();

    SafeDepositBoxV2 resultantSDBV1 =
        safeDepositBoxService.convertSafeDepositBoxV1ToV2(safeDepositBoxV1);

    SafeDepositBoxV2 expectedSdbV2 =
        SafeDepositBoxV2.builder()
            .id(id)
            .name(name)
            .description(description)
            .path(path)
            .categoryId(categoryId)
            .createdBy(createdBy)
            .lastUpdatedBy(lastUpdatedBy)
            .createdTs(createdTs)
            .lastUpdatedTs(lastUpdatedTs)
            .owner(owner)
            .userGroupPermissions(userGroupPermissions)
            .build();
    Set<IamPrincipalPermission> expectedIamRolePermissionsV2 = Sets.newHashSet();
    IamPrincipalPermission expectedIamPrincipalPermission =
        IamPrincipalPermission.builder().iamPrincipalArn(arn).roleId(roleId).build();
    expectedIamRolePermissionsV2.add(expectedIamPrincipalPermission);
    expectedSdbV2.setIamPrincipalPermissions(expectedIamRolePermissionsV2);

    assertEquals(expectedSdbV2, resultantSDBV1);
  }

  @Test
  public void test_that_deleteSafeDepositBox_deletes_permissions_secrets_and_versions() {
    String sdbPathNoCategory = "safedepositbox-zzz-fake";
    String sdbPath = "category/" + sdbPathNoCategory;

    String sdbId = "sdb id";
    SafeDepositBoxRecord safeDepositBox = new SafeDepositBoxRecord().setId(sdbId).setPath(sdbPath);

    when(safeDepositBoxDao.getSafeDepositBox(sdbId)).thenReturn(Optional.of(safeDepositBox));
    when(roleService.getRoleByName(RoleRecord.ROLE_OWNER))
        .thenReturn(Optional.of(Role.builder().build()));

    safeDepositBoxService.deleteSafeDepositBox(sdbId);

    verify(iamPrincipalPermissionService).deleteIamPrincipalPermissions(sdbId);
    verify(userGroupPermissionService).deleteUserGroupPermissions(sdbId);
    verify(secureDataVersionDao).deleteAllVersionsThatStartWithPartialPath(sdbPathNoCategory);
    verify(secureDataService)
        .deleteAllSecretsThatStartWithGivenPartialPath(sdbId, sdbPathNoCategory);
  }

  @Test
  public void test_that_overrideSdbOwner_calls_update_owner() {
    String id = "111";
    String sdbName = "test sdb name";
    OffsetDateTime offsetDateTime = OffsetDateTime.now(UTC);
    doReturn(offsetDateTime).when(dateTimeSupplier).get();
    doReturn(Optional.of(id)).when(safeDepositBoxServiceSpy).getSafeDepositBoxIdByName(sdbName);
    doNothing().when(safeDepositBoxServiceSpy).updateOwner(any(), any(), any(), any());
    safeDepositBoxServiceSpy.overrideOwner(sdbName, "new-owner", "admin-user");
    verify(safeDepositBoxServiceSpy, times(1))
        .updateOwner(id, "new-owner", "admin-user", offsetDateTime);
  }

  @Test
  @SuppressFBWarnings
  public void test_that_getAssociatedSafeDepositBoxes_checks_assumed_role_and_its_base_iam_role() {
    String assumedRoleArn = "arn:aws:sts::123456789012:assumed-role/Accounting-Role/Mary";
    String iamRoleArn = "arn:aws:iam::123456789012:role/Accounting-Role";
    String rootArn = "arn:aws:iam::123456789012:root";

    CerberusPrincipal AssumedRoleArnPrincipal = mock(CerberusPrincipal.class);
    doReturn(PrincipalType.IAM).when(AssumedRoleArnPrincipal).getPrincipalType();
    doReturn(assumedRoleArn).when(AssumedRoleArnPrincipal).getName();

    when(awsIamRoleArnParser.isAssumedRoleArn(assumedRoleArn)).thenReturn(true);
    when(awsIamRoleArnParser.convertPrincipalArnToRoleArn(assumedRoleArn)).thenReturn(iamRoleArn);
    when(awsIamRoleArnParser.convertPrincipalArnToRootArn(assumedRoleArn)).thenReturn(rootArn);

    SafeDepositBoxRecord safeDepositBoxRecord1 = new SafeDepositBoxRecord();
    SafeDepositBoxRecord safeDepositBoxRecord2 = new SafeDepositBoxRecord();
    List<SafeDepositBoxRecord> assumedRoleArnRecords =
        Lists.newArrayList(safeDepositBoxRecord1, safeDepositBoxRecord2);
    when(safeDepositBoxDao.getAssumedRoleAssociatedSafeDepositBoxes(
            assumedRoleArn, iamRoleArn, rootArn))
        .thenReturn(assumedRoleArnRecords);

    List<SafeDepositBoxSummary> sdbSummaries =
        safeDepositBoxServiceSpy.getAssociatedSafeDepositBoxes(AssumedRoleArnPrincipal);
    assertEquals(assumedRoleArnRecords.size(), sdbSummaries.size());
  }

  @Test
  @SuppressFBWarnings
  public void test_that_getAssociatedSafeDepositBoxes_checks_iam_role() {
    String iamRoleArn = "arn:aws:iam::123456789012:role/Accounting-Role";
    String rootArn = "arn:aws:iam::123456789012:root";

    SafeDepositBoxRecord safeDepositBoxRecord1 = new SafeDepositBoxRecord();

    List<SafeDepositBoxRecord> roleArnRecords = Lists.newArrayList(safeDepositBoxRecord1);
    when(safeDepositBoxDao.getIamPrincipalAssociatedSafeDepositBoxes(iamRoleArn, rootArn))
        .thenReturn(roleArnRecords);
    when(awsIamRoleArnParser.isAssumedRoleArn(iamRoleArn)).thenReturn(false);
    when(awsIamRoleArnParser.convertPrincipalArnToRootArn(iamRoleArn)).thenReturn(rootArn);

    CerberusPrincipal roleArnPrincipal = mock(CerberusPrincipal.class);
    doReturn(PrincipalType.IAM).when(roleArnPrincipal).getPrincipalType();
    doReturn(iamRoleArn).when(roleArnPrincipal).getName();

    List<SafeDepositBoxSummary> roleArnSdbSummaries =
        safeDepositBoxServiceSpy.getAssociatedSafeDepositBoxes(roleArnPrincipal);
    assertEquals(roleArnRecords.size(), roleArnSdbSummaries.size());
  }
}
