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

import com.google.common.collect.ImmutableSet;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.SecureDataAction;
import com.nike.cerberus.dao.PermissionsDao;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.domain.IamPrincipalPermission;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.security.CerberusPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static com.nike.cerberus.PrincipalType.IAM;
import static com.nike.cerberus.PrincipalType.USER;
import static com.nike.cerberus.record.RoleRecord.ROLE_OWNER;
import static com.nike.cerberus.record.RoleRecord.ROLE_READ;
import static com.nike.cerberus.record.RoleRecord.ROLE_WRITE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PermissionsServiceTest {

    private static final String OWNER_ID = UUID.randomUUID().toString();
    private static final String WRITE_ID = UUID.randomUUID().toString();
    private static final String READ_ID = UUID.randomUUID().toString();
    private static final String SDB_ID = UUID.randomUUID().toString();
    private static final String IAM_ARN_TEMP = "arn:aws:iam::111111111111:%s";

    @Mock private RoleService roleService;
    @Mock private UserGroupPermissionService userGroupPermissionService;
    @Mock private IamPrincipalPermissionService iamPrincipalPermissionService;
    @Mock private PermissionsDao permissionsDao;
    @Mock private Role ownerRole;
    @Mock private Role writeRole;
    @Mock private Role readRole;


    PermissionsService permissionsService;

    @Before
    public void before() {
        initMocks(this);

        boolean userGroupsCaseSensitive = true;
        permissionsService = new PermissionsService(
                roleService,
                userGroupPermissionService,
                iamPrincipalPermissionService,
                permissionsDao,
                userGroupsCaseSensitive
        );

        when(ownerRole.getId()).thenReturn(OWNER_ID);
        when(roleService.getRoleById(OWNER_ID)).thenReturn(Optional.of(ownerRole));
        when(roleService.getRoleByName(ROLE_OWNER)).thenReturn(Optional.of(ownerRole));
        when(writeRole.getId()).thenReturn(WRITE_ID);
        when(roleService.getRoleById(WRITE_ID)).thenReturn(Optional.of(writeRole));
        when(roleService.getRoleByName(ROLE_WRITE)).thenReturn(Optional.of(writeRole));
        when(readRole.getId()).thenReturn(READ_ID);
        when(roleService.getRoleById(READ_ID)).thenReturn(Optional.of(readRole));
        when(roleService.getRoleByName(ROLE_READ)).thenReturn(Optional.of(readRole));
    }

    @Test
    public void test_that_doesPrincipalHaveOwnerPermissions_returns_false_when_a_iam_principal_does_not_have_permissions() {

        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(IAM)
                .build());

        SafeDepositBoxV2 sdb = SafeDepositBoxV2.Builder.create()
                .withIamPrincipalPermissions(
                        ImmutableSet.of(
                                IamPrincipalPermission.Builder.create()
                                        .withRoleId(READ_ID)
                                        .build()
                        )
                )
                .build();


        Boolean actual = permissionsService.doesPrincipalHaveOwnerPermissions(principal, sdb);
        assertFalse("The principal should not have owner permissions", actual);
    }

    @Test
    public void test_that_doesPrincipalHaveOwnerPermissions_returns_true_when_a_iam_principal_has_permissions() {

        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(IAM)
                .build());

        SafeDepositBoxV2 sdb = SafeDepositBoxV2.Builder.create()
                .withIamPrincipalPermissions(
                        ImmutableSet.of(
                                IamPrincipalPermission.Builder.create()
                                        .withRoleId(OWNER_ID)
                                        .build()
                        )
                )
                .build();


        Boolean actual = permissionsService.doesPrincipalHaveOwnerPermissions(principal, sdb);
        assertTrue("The principal should have owner permissions", actual);
    }

    @Test
    public void test_that_doesPrincipalHaveOwnerPermissions_returns_false_when_a_user_principal_does_not_have_permissions() {

        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(USER)
                .withGroups("group1,group2")
                .build());

        SafeDepositBoxV2 sdb = SafeDepositBoxV2.Builder.create()
                .withOwner("super-awesome-team")
                .build();

        Boolean actual = permissionsService.doesPrincipalHaveOwnerPermissions(principal, sdb);
        assertFalse("The principal should not have owner permissions", actual);
    }

    @Test
    public void test_that_doesPrincipalHaveOwnerPermissions_returns_true_when_a_user_principal_has_permissions() {

        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(USER)
                .withGroups("group1,group2,super-awesome-team")
                .build());

        SafeDepositBoxV2 sdb = SafeDepositBoxV2.Builder.create()
                .withOwner("super-awesome-team")
                .build();

        Boolean actual = permissionsService.doesPrincipalHaveOwnerPermissions(principal, sdb);
        assertTrue("The principal should have owner permissions", actual);
    }

    @Test(expected = ApiException.class)
    public void test_that_assertPrincipalHasOwnerPermissions_throws_api_exception_when_principal_is_not_owner() {
        PermissionsService permissionsServiceSpy = spy(permissionsService); // do not need to re-test doesPrincipalHaveOwnerPermissions()

        doReturn(false).when(permissionsServiceSpy).doesPrincipalHaveOwnerPermissions(any(), any());
        permissionsServiceSpy.assertPrincipalHasOwnerPermissions(null, null);
    }

    @Test
    public void test_that_assertPrincipalHasOwnerPermissions_does_not_throw_api_exception_when_principal_is_owner() {
        PermissionsService permissionsServiceSpy = spy(permissionsService); // do not need to re-test doesPrincipalHaveOwnerPermissions()

        doReturn(true).when(permissionsServiceSpy).doesPrincipalHaveOwnerPermissions(any(), any());
        permissionsServiceSpy.assertPrincipalHasOwnerPermissions(null, null);
    }

    @Test
    public void test_that_doesPrincipalHaveReadPermission_returns_false_for_iam_princ_when_perms_missing() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(IAM)
                .withPrincipal(String.format(IAM_ARN_TEMP, "foo"))
                .build());

        when(iamPrincipalPermissionService.getIamPrincipalPermissions(SDB_ID)).thenReturn(ImmutableSet.of(
            IamPrincipalPermission.Builder.create().withIamPrincipalArn(String.format(IAM_ARN_TEMP, "bar")).build(),
            IamPrincipalPermission.Builder.create().withIamPrincipalArn(String.format(IAM_ARN_TEMP, "bam")).build()
        ));

        assertFalse("The principal should not have read permissions",
                permissionsService.doesPrincipalHaveReadPermission(principal, SDB_ID));
    }

    @Test
    public void test_that_doesPrincipalHaveReadPermission_returns_true_for_iam_princ_when_any_permission_association_exists() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(IAM)
                .withPrincipal(String.format(IAM_ARN_TEMP, "foo"))
                .build());

        when(iamPrincipalPermissionService.getIamPrincipalPermissions(SDB_ID)).thenReturn(ImmutableSet.of(
                IamPrincipalPermission.Builder.create().withIamPrincipalArn(String.format(IAM_ARN_TEMP, "bar")).build(),
                IamPrincipalPermission.Builder.create().withIamPrincipalArn(String.format(IAM_ARN_TEMP, "bam")).build(),
                IamPrincipalPermission.Builder.create().withIamPrincipalArn(String.format(IAM_ARN_TEMP, "foo")).build()
        ));

        assertTrue("The principal should have read permissions",
                permissionsService.doesPrincipalHaveReadPermission(principal, SDB_ID));
    }

    @Test
    public void test_that_doesPrincipalHaveReadPermission_returns_false_for_user_princ_when_perms_missing() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(USER)
                .withGroups("group1,group2")
                .build());

        when(userGroupPermissionService.getUserGroupPermissions(SDB_ID)).thenReturn(ImmutableSet.of(
                UserGroupPermission.Builder.create().withName("group3").build(),
                UserGroupPermission.Builder.create().withName("group4").build(),
                UserGroupPermission.Builder.create().withName("group5").build(),
                UserGroupPermission.Builder.create().withName("group6").build()
        ));

        assertFalse("The principal should not have read permissions",
                permissionsService.doesPrincipalHaveReadPermission(principal, SDB_ID));
    }

    @Test
    public void test_that_doesPrincipalHaveReadPermission_returns_true_for_user_princ_when_any_permission_association_exists() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create()
                .withPrincipalType(USER)
                .withGroups("group1,group2,group5")
                .build());

        when(userGroupPermissionService.getUserGroupPermissions(SDB_ID)).thenReturn(ImmutableSet.of(
                UserGroupPermission.Builder.create().withName("group3").build(),
                UserGroupPermission.Builder.create().withName("group4").build(),
                UserGroupPermission.Builder.create().withName("group5").build(),
                UserGroupPermission.Builder.create().withName("group6").build()
        ));

        assertTrue("The principal should have read permissions",
                permissionsService.doesPrincipalHaveReadPermission(principal, SDB_ID));
    }

    @Test
    public void test_that_doesPrincipalHavePermission_returns_true_for_iam_when_dao_returns_true() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create().withPrincipalType(IAM).build());
        when(permissionsDao.doesIamPrincipalHaveRoleForSdb(any(), any(), any())).thenReturn(true);
        assertTrue(permissionsService.doesPrincipalHavePermission(principal, SDB_ID, SecureDataAction.READ));
    }

    @Test
    public void test_that_doesPrincipalHavePermission_returns_true_for_user_when_dao_returns_true() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create().withPrincipalType(USER).build());
        when(permissionsDao.doesUserPrincipalHaveRoleForSdb(any(), any(), any())).thenReturn(true);
        assertTrue(permissionsService.doesPrincipalHavePermission(principal, SDB_ID, SecureDataAction.READ));
    }


    @Test
    public void test_that_doesPrincipalHavePermission_returns_false_for_iam_when_dao_returns_false() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create().withPrincipalType(IAM).build());
        when(permissionsDao.doesIamPrincipalHaveRoleForSdb(any(), any(), any())).thenReturn(false);
        assertFalse(permissionsService.doesPrincipalHavePermission(principal, SDB_ID, SecureDataAction.READ));
    }

    @Test
    public void test_that_doesPrincipalHavePermission_returns_false_for_user_when_dao_returns_false() {
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create().withPrincipalType(USER).build());
        when(permissionsDao.doesUserPrincipalHaveRoleForSdb(any(), any(), any())).thenReturn(false);
        assertFalse(permissionsService.doesPrincipalHavePermission(principal, SDB_ID, SecureDataAction.READ));
    }

    @Test
    public void test_that_doesPrincipalHavePermission_calls_case_insensitive_method_when_case_sensitive_is_false() {
        PermissionsService permissionsService = new PermissionsService(
                roleService,
                userGroupPermissionService,
                iamPrincipalPermissionService,
                permissionsDao,
                false
        );
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create().withPrincipalType(USER).build());
        when(permissionsDao.doesUserPrincipalHaveRoleForSdb(any(), any(), any())).thenReturn(false);
        when(permissionsDao.doesUserHavePermsForRoleAndSdbCaseInsensitive(any(), any(), any())).thenReturn(true);
        assertTrue(permissionsService.doesPrincipalHavePermission(principal, SDB_ID, SecureDataAction.READ));
    }

    @Test
    public void test_that_doesPrincipalHavePermission_calls_case_sensitive_method_when_case_sensitive_is_true() {
        PermissionsService permissionsService = new PermissionsService(
                roleService,
                userGroupPermissionService,
                iamPrincipalPermissionService,
                permissionsDao,
                true
        );
        CerberusPrincipal principal = new CerberusPrincipal(CerberusAuthToken.Builder.create().withPrincipalType(USER).build());
        when(permissionsDao.doesUserPrincipalHaveRoleForSdb(any(), any(), any())).thenReturn(false);
        when(permissionsDao.doesUserHavePermsForRoleAndSdbCaseInsensitive(any(), any(), any())).thenReturn(true);
        assertFalse(permissionsService.doesPrincipalHavePermission(principal, SDB_ID, SecureDataAction.READ));
    }
}
