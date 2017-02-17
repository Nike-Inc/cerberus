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
import com.nike.cerberus.dao.UserGroupDao;
import com.nike.cerberus.domain.IamRolePermission;
import com.nike.cerberus.domain.SafeDepositBox;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.record.SafeDepositBoxRecord;
import com.nike.cerberus.util.DateTimeSupplier;
import com.nike.cerberus.util.Slugger;
import com.nike.cerberus.util.UuidSupplier;
import com.nike.vault.client.VaultAdminClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
    private VaultAdminClient vaultAdminClient;

    @Mock
    private VaultPolicyService vaultPolicyService;

    @Mock
    private UserGroupPermissionService userGroupPermissionService;

    @Mock
    private IamRolePermissionService iamRolePermissionService;

    @Mock
    private Slugger slugger;

    @Mock
    private DateTimeSupplier dateTimeSupplier;

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

        SafeDepositBox sdbObject = new SafeDepositBox();
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

        Set<IamRolePermission> iamPerms = new HashSet<>();
        iamPerms.add(new IamRolePermission().withAccountId("1111111111").withIamRoleName("lambda_prod_healthcheck").withRoleId(readId));
        sdbObject.setIamRolePermissions(iamPerms);

        sdbObject.setUserGroupPermissions(userPerms);
        sdbObject.setIamRolePermissions(iamPerms);

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

        SafeDepositBox sdbObject = new SafeDepositBox();
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

        Set<IamRolePermission> iamPerms = new HashSet<>();
        iamPerms.add(new IamRolePermission().withAccountId("1111111111").withIamRoleName("lambda_prod_healthcheck").withRoleId(readId));
        sdbObject.setIamRolePermissions(iamPerms);

        sdbObject.setUserGroupPermissions(userPerms);
        sdbObject.setIamRolePermissions(iamPerms);

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
        doNothing().when(safeDepositBoxServiceSpy).modifyIamRolePermissions(any(), any(), any(), any());
        doReturn(sdbObject).when(safeDepositBoxServiceSpy).getSDBFromRecord(any());

        safeDepositBoxServiceSpy.restoreSafeDepositBox(sdbObject, "admin-user");

        verify(safeDepositBoxDao, times(1)).fullUpdateSafeDepositBox(boxToStore);
    }

}
