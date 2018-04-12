/*
 * Copyright (c) 2017 Nike, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.nike.cerberus.service;

import com.nike.cerberus.dao.SafeDepositBoxDao;
import com.nike.cerberus.dao.SecureDataVersionDao;
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.domain.IamPrincipalPermission;
import com.nike.cerberus.domain.IamRolePermission;
import com.nike.cerberus.domain.Role;
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
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SafeDepositBoxServiceTest {

    @Mock
    private SafeDepositBoxDao safeDepositBoxDao;

    @Mock
    private UserGroupDao userGroupDao;

    @Mock
    private UuidSupplier uuidSupplier;

    @Mock
    private CategoryService categoryService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserGroupPermissionService userGroupPermissionService;

    @Mock
    private IamPrincipalPermissionService iamPrincipalPermissionService;

    @Mock
    private Slugger slugger;

    @Mock
    private DateTimeSupplier dateTimeSupplier;

    @Mock
    private AwsIamRoleArnParser awsIamRoleArnParser;

    @Mock
    private SecureDataService secureDataService;

    @Mock
    private SecureDataVersionDao secureDataVersionDao;

    @Mock
    private PermissionsService permissionsService;

    @InjectMocks
    private SafeDepositBoxService safeDepositBoxService;

    private SafeDepositBoxService safeDepositBoxServiceSpy;

    @Before
    public void before() {
        initMocks(this);

        safeDepositBoxServiceSpy = spy(safeDepositBoxService);
    }

    @Test
    public void test_that_restore_safe_deposit_box_creates_with_expected_sdb_record_from_safe_depot_box_object() {
        String id = "111";
        String categoryId = "222";
        String readId = "333";
        String sdbName = "HEALTH CHECK BUCKET";

        SafeDepositBoxV2 sdbObject = new SafeDepositBoxV2();
        sdbObject.setId(id);
        sdbObject.setPath("app/health-check-bucket/");
        sdbObject.setCategoryId(categoryId);
        sdbObject.setName(sdbName);
        sdbObject.setOwner("Lst-Squad.Carebears");
        sdbObject.setDescription("This SDB is read by the Health Check Lambda...");
        sdbObject.setCreatedTs(OffsetDateTime.parse("2016-09-08T15:39:31Z"));
        sdbObject.setLastUpdatedTs(OffsetDateTime.parse("2016-12-13T17:28:00Z"));
        sdbObject.setCreatedBy("justin.field@nike.com");
        sdbObject.setLastUpdatedBy("todd.lisonbee@nike.com");

        Set<UserGroupPermission> userPerms = new HashSet<>();
        userPerms.add(new UserGroupPermission().withName("Foundation.Prod.Support").withRoleId(readId));
        userPerms.add(new UserGroupPermission().withName("Lst-NIKE.FOO.ISL").withRoleId(readId));
        sdbObject.setUserGroupPermissions(userPerms);

        Set<IamPrincipalPermission> iamPerms = new HashSet<>();
        iamPerms.add(new IamPrincipalPermission().withIamPrincipalArn("arn:aws:iam::1111111111:role/lambda_prod_healthcheck").withRoleId(readId));
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

        when(safeDepositBoxDao.getSafeDepositBox(sdbObject.getId())).thenReturn(Optional.ofNullable(null));
        doNothing().when(safeDepositBoxServiceSpy).addOwnerPermission(any(), any());

        safeDepositBoxServiceSpy.restoreSafeDepositBox(sdbObject, "admin-user");

        verify(safeDepositBoxDao, times(1)).createSafeDepositBox(boxToStore);
    }

    @Test
    public void test_that_restore_safe_deposit_box_updates_with_expected_sdb_record_from_safe_depot_box_object_when_the_sdb_already_exists() {
        String id = "111";
        String categoryId = "222";
        String readId = "333";
        String sdbName = "HEALTH CHECK BUCKET";
        String sdbId = "asdf-1231-23sad-asd";

        SafeDepositBoxV2 sdbObject = new SafeDepositBoxV2();
        sdbObject.setId(id);
        sdbObject.setPath("app/health-check-bucket/");
        sdbObject.setCategoryId(categoryId);
        sdbObject.setName(sdbName);
        sdbObject.setOwner("Lst-Squad.Carebears");
        sdbObject.setDescription("This SDB is read by the Health Check Lambda...");
        sdbObject.setCreatedTs(OffsetDateTime.parse("2016-09-08T15:39:31Z"));
        sdbObject.setLastUpdatedTs(OffsetDateTime.parse("2016-12-13T17:28:00Z"));
        sdbObject.setCreatedBy("justin.field@nike.com");
        sdbObject.setLastUpdatedBy("todd.lisonbee@nike.com");

        Set<UserGroupPermission> userPerms = new HashSet<>();
        userPerms.add(new UserGroupPermission().withName("Foundation.Prod.Support").withRoleId(readId));
        userPerms.add(new UserGroupPermission().withName("Lst-NIKE.FOO.ISL").withRoleId(readId));
        sdbObject.setUserGroupPermissions(userPerms);

        Set<IamPrincipalPermission> iamPerms = new HashSet<>();
        iamPerms.add(new IamPrincipalPermission().withIamPrincipalArn("arn:aws:iam::1111111111:role/lambda_prod_healthcheck").withRoleId(readId));
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
        when(safeDepositBoxDao.getSafeDepositBox(sdbObject.getId())).thenReturn(Optional.of(existingRecord));
        doNothing().when(safeDepositBoxServiceSpy).updateOwner(any(), any(), any(), any());
        doNothing().when(safeDepositBoxServiceSpy).modifyUserGroupPermissions(any(), any(), any(), any());
        doNothing().when(safeDepositBoxServiceSpy).modifyIamPrincipalPermissions(any(), any(), any(), any());
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
        UserGroupPermission userGroupPermission = new UserGroupPermission();
        userGroupPermissions.add(userGroupPermission);

        Set<IamPrincipalPermission> iamRolePermissions = Sets.newHashSet();
        IamPrincipalPermission iamRolePermission = new IamPrincipalPermission().withIamPrincipalArn(arn).withRoleId(roleId);
        iamRolePermissions.add(iamRolePermission);

        SafeDepositBoxV2 safeDepositBoxV2 = new SafeDepositBoxV2();
        safeDepositBoxV2.setId(id);
        safeDepositBoxV2.setName(name);
        safeDepositBoxV2.setDescription(description);
        safeDepositBoxV2.setPath(path);
        safeDepositBoxV2.setCategoryId(categoryId);
        safeDepositBoxV2.setCreatedBy(createdBy);
        safeDepositBoxV2.setLastUpdatedBy(lastUpdatedBy);
        safeDepositBoxV2.setCreatedTs(createdTs);
        safeDepositBoxV2.setLastUpdatedTs(lastUpdatedTs);
        safeDepositBoxV2.setOwner(owner);
        safeDepositBoxV2.setUserGroupPermissions(userGroupPermissions);
        safeDepositBoxV2.setIamPrincipalPermissions(iamRolePermissions);

        when(awsIamRoleArnParser.getAccountId(arn)).thenReturn(accountId);
        when(awsIamRoleArnParser.getRoleName(arn)).thenReturn(roleName);

        SafeDepositBoxV1 resultantSDBV1 = safeDepositBoxService.convertSafeDepositBoxV2ToV1(safeDepositBoxV2);

        SafeDepositBoxV1 expectedSdbV1 = new SafeDepositBoxV1();
        expectedSdbV1.setId(id);
        expectedSdbV1.setName(name);
        expectedSdbV1.setDescription(description);
        expectedSdbV1.setPath(path);
        expectedSdbV1.setCategoryId(categoryId);
        expectedSdbV1.setCreatedBy(createdBy);
        expectedSdbV1.setLastUpdatedBy(lastUpdatedBy);
        expectedSdbV1.setCreatedTs(createdTs);
        expectedSdbV1.setLastUpdatedTs(lastUpdatedTs);
        expectedSdbV1.setOwner(owner);
        expectedSdbV1.setUserGroupPermissions(userGroupPermissions);
        Set<IamRolePermission> expectedIamRolePermissionsV1 = Sets.newHashSet();
        IamRolePermission expectedIamRolePermission = new IamRolePermission().withAccountId(accountId).withIamRoleName(roleName).withRoleId(roleId);
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
        UserGroupPermission userGroupPermission = new UserGroupPermission();
        userGroupPermissions.add(userGroupPermission);

        Set<IamRolePermission> iamRolePermissions = Sets.newHashSet();
        IamRolePermission iamRolePermission = new IamRolePermission().withAccountId(accountId).withIamRoleName(roleName).withRoleId(roleId);
        iamRolePermissions.add(iamRolePermission);

        SafeDepositBoxV1 safeDepositBoxV1 = new SafeDepositBoxV1();
        safeDepositBoxV1.setId(id);
        safeDepositBoxV1.setName(name);
        safeDepositBoxV1.setDescription(description);
        safeDepositBoxV1.setPath(path);
        safeDepositBoxV1.setCategoryId(categoryId);
        safeDepositBoxV1.setCreatedBy(createdBy);
        safeDepositBoxV1.setLastUpdatedBy(lastUpdatedBy);
        safeDepositBoxV1.setCreatedTs(createdTs);
        safeDepositBoxV1.setLastUpdatedTs(lastUpdatedTs);
        safeDepositBoxV1.setOwner(owner);
        safeDepositBoxV1.setUserGroupPermissions(userGroupPermissions);
        safeDepositBoxV1.setIamRolePermissions(iamRolePermissions);

        SafeDepositBoxV2 resultantSDBV1 = safeDepositBoxService.convertSafeDepositBoxV1ToV2(safeDepositBoxV1);

        SafeDepositBoxV2 expectedSdbV2 = new SafeDepositBoxV2();
        expectedSdbV2.setId(id);
        expectedSdbV2.setName(name);
        expectedSdbV2.setDescription(description);
        expectedSdbV2.setPath(path);
        expectedSdbV2.setCategoryId(categoryId);
        expectedSdbV2.setCreatedBy(createdBy);
        expectedSdbV2.setLastUpdatedBy(lastUpdatedBy);
        expectedSdbV2.setCreatedTs(createdTs);
        expectedSdbV2.setLastUpdatedTs(lastUpdatedTs);
        expectedSdbV2.setOwner(owner);
        expectedSdbV2.setUserGroupPermissions(userGroupPermissions);
        Set<IamPrincipalPermission> expectedIamRolePermissionsV2 = Sets.newHashSet();
        IamPrincipalPermission expectedIamPrincipalPermission = new IamPrincipalPermission().withIamPrincipalArn(arn).withRoleId(roleId);
        expectedIamRolePermissionsV2.add(expectedIamPrincipalPermission);
        expectedSdbV2.setIamPrincipalPermissions(expectedIamRolePermissionsV2);

        assertEquals(expectedSdbV2, resultantSDBV1);
    }

    @Test
    public void test_that_deleteSafeDepositBox_deletes_permissions_secrets_and_versions() {
        String sdbPathNoCategory = "safedepositbox-zzz-fake";
        String sdbPath = "category/" + sdbPathNoCategory;
        CerberusPrincipal principal = new CerberusPrincipal(new CerberusAuthToken());
        String sdbId = "sdb id";
        SafeDepositBoxRecord safeDepositBox = new SafeDepositBoxRecord()
                .setId(sdbId)
                .setPath(sdbPath);

        when(safeDepositBoxDao.getSafeDepositBox(sdbId)).thenReturn(Optional.of(safeDepositBox));
        when(roleService.getRoleByName(RoleRecord.ROLE_OWNER)).thenReturn(Optional.of(new Role()));

        when(permissionsService.doesPrincipalHaveReadPermission(principal, sdbId)).thenReturn(true);

        safeDepositBoxService.deleteSafeDepositBox(principal, sdbId);

        verify(iamPrincipalPermissionService).deleteIamPrincipalPermissions(sdbId);
        verify(userGroupPermissionService).deleteUserGroupPermissions(sdbId);
        verify(secureDataVersionDao).deleteAllVersionsThatStartWithPartialPath(sdbPathNoCategory);
        verify(secureDataService).deleteAllSecretsThatStartWithGivenPartialPath(sdbPathNoCategory);
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
        verify(safeDepositBoxServiceSpy, times(1)).updateOwner(id, "new-owner", "admin-user", offsetDateTime);
    }
}
