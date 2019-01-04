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

import com.google.common.collect.Sets;
import com.google.inject.name.Named;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.SecureDataAction;
import com.nike.cerberus.dao.PermissionsDao;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.domain.UserGroupPermission;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nike.cerberus.record.RoleRecord.ROLE_OWNER;
import static com.nike.cerberus.record.RoleRecord.ROLE_READ;
import static com.nike.cerberus.record.RoleRecord.ROLE_WRITE;

@Singleton
public class PermissionsService {

    public static final String USER_GROUPS_CASE_SENSITIVE = "cms.user.groups.caseSensitive";

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final RoleService roleService;
    private final UserGroupPermissionService userGroupPermissionService;
    private final IamPrincipalPermissionService iamPrincipalPermissionService;
    private final PermissionsDao permissionsDao;
    private final boolean userGroupsCaseSensitive;
    private final AwsIamRoleArnParser awsIamRoleArnParser;

    @Inject
    public PermissionsService(RoleService roleService,
                              UserGroupPermissionService userGroupPermissionService,
                              IamPrincipalPermissionService iamPrincipalPermissionService,
                              PermissionsDao permissionsDao,
                              @Named(USER_GROUPS_CASE_SENSITIVE) boolean userGroupsCaseSensitive,
                              AwsIamRoleArnParser awsIamRoleArnParser) {

        this.roleService = roleService;
        this.userGroupPermissionService = userGroupPermissionService;
        this.iamPrincipalPermissionService = iamPrincipalPermissionService;
        this.permissionsDao = permissionsDao;
        this.userGroupsCaseSensitive = userGroupsCaseSensitive;
        this.awsIamRoleArnParser = awsIamRoleArnParser;
    }

    /**
     * Checks to see if a principal has owner permissions for a given SDB.
     *
     * @param principal The principal under question
     * @param sdb The sdb to check
     * @return True if the principal has owner permissions
     */
    public boolean doesPrincipalHaveOwnerPermissions(CerberusPrincipal principal, SafeDepositBoxV2 sdb) {
        boolean principalHasOwnerPermissions = false;
        switch (principal.getPrincipalType()) {
            case IAM:
                principalHasOwnerPermissions = doesIamPrincipalHavePermission(principal, sdb.getId(), Sets.newHashSet(ROLE_OWNER));
                break;
            case USER:
                principalHasOwnerPermissions = userGroupsCaseSensitive ?
                            principal.getUserGroups().contains(sdb.getOwner()) :
                            containsIgnoreCase(principal.getUserGroups(), sdb.getOwner());
                break;
        }
        return principalHasOwnerPermissions;
    }

    /**
     * Asserts that the given principal has owner permissions on the given SDB
     *
     * @param principal The authenticated principal
     * @param sdb The SDB that the principal is trying to access
     */
    public void assertPrincipalHasOwnerPermissions(final CerberusPrincipal principal, final SafeDepositBoxV2 sdb) {

        boolean principalHasOwnerPermissions = doesPrincipalHaveOwnerPermissions(principal, sdb);

        if (! principalHasOwnerPermissions) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SDB_CALLER_OWNERSHIP_REQUIRED)
                    .build();
        }
    }

    /**
     * Checks to see if a Principal has any permissions to a given SDB.
     *
     * @param principal The authenticated principal
     * @param sdbId The SDB to check.
     * @return True if the user has read permissions
     */
    public boolean doesPrincipalHaveReadPermission(CerberusPrincipal principal, String sdbId) {
        boolean principalHasPermissionAssociationWithSdb = false;

        switch (principal.getPrincipalType()) {
            case IAM:
                // if the authenticated principal is an IAM Principal check to see that the iam principal is associated with the requested sdb
                principalHasPermissionAssociationWithSdb = doesIamPrincipalHavePermission(principal, sdbId, Sets.newHashSet(ROLE_READ, ROLE_OWNER, ROLE_WRITE));
                break;
            case USER:
                // if the the principal is a user principal ensure that one of the users groups is associated with the sdb
                Set<UserGroupPermission> userGroupPermissions = userGroupPermissionService.getUserGroupPermissions(sdbId);
                Set<String> userGroups = userGroupPermissions.stream()
                        .map(UserGroupPermission::getName)
                        .collect(Collectors.toSet());
                principalHasPermissionAssociationWithSdb = userGroupsCaseSensitive ?
                        doesHaveIntersection(userGroups, principal.getUserGroups()) :
                        doesHaveIntersectionIgnoreCase(userGroups, principal.getUserGroups());
                break;
        }
        return principalHasPermissionAssociationWithSdb;
    }

    public boolean doesPrincipalHavePermission(CerberusPrincipal principal, String sdbId, SecureDataAction action) {
        boolean hasPermission = false;
        switch (principal.getPrincipalType()) {
            case IAM:
                hasPermission = doesIamPrincipalHavePermission(principal, sdbId, action.getAllowedRoles());
                break;
            case USER:
                hasPermission = userGroupsCaseSensitive ?
                        permissionsDao.doesUserPrincipalHaveRoleForSdb(sdbId, action.getAllowedRoles(), principal.getUserGroups()) :
                        permissionsDao.doesUserHavePermsForRoleAndSdbCaseInsensitive(sdbId, action.getAllowedRoles(), principal.getUserGroups());
                break;
            default:
                log.error("Unknown Principal Type: {}, returning hasPermission: false", principal.getPrincipalType().getName());
                break;
        }

        log.debug("Principal: {}, Type: {}, SDB: {}, Action: {}, hasPermissions: {}",
                principal.getName(),
                principal.getPrincipalType().getName(),
                sdbId,
                action.name(),
                hasPermission);

        return hasPermission;
    }

    protected boolean doesIamPrincipalHavePermission(CerberusPrincipal principal, String sdbId, Set<String> roles) {
        String iamPrincipalArn = principal.getName();
        String iamRootArn = awsIamRoleArnParser.convertPrincipalArnToRootArn(iamPrincipalArn);
        if (awsIamRoleArnParser.isAssumedRoleArn(iamPrincipalArn)) {
            String iamRoleArn = awsIamRoleArnParser.convertPrincipalArnToRoleArn(iamPrincipalArn);
            return permissionsDao.doesAssumedRoleHaveRoleForSdb(sdbId, iamPrincipalArn, iamRoleArn, iamRootArn, roles);
        } else {
            return permissionsDao.doesIamPrincipalHaveRoleForSdb(sdbId, iamPrincipalArn, iamRootArn, roles);
        }
    }

    /**
     * Does a case-insensitive check to see if the collection contains the given String
     * @param items   List of strings from which to search
     * @param object  String for which to search
     * @return  True if the object exists in the array, false if not
     */
    private boolean containsIgnoreCase(Collection<String> items, String object) {
        return items.stream()
                .anyMatch(item -> item.equalsIgnoreCase(object));
    }

    /**
     * Checks if any string is contained in both the first collection and the second collection
     * @return  True if both collections contain any one string, false if not
     */
    private boolean doesHaveIntersection(Collection<String> co1, Collection<String> co2) {
        return co2.stream()
                .anyMatch(co1::contains);
    }

    /**
     * Checks (ignoring case) if any string is contained in both the first collection and the second collection
     * @return  True if both collections contain any one string, false if not
     */
    private boolean doesHaveIntersectionIgnoreCase(Collection<String> co1, Collection<String> co2) {
        Set<String> co1LowerCase = co1.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return co2.stream()
                .map(String::toLowerCase)
                .anyMatch(co1LowerCase::contains);
    }
}