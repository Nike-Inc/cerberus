/*
 * Copyright (c) 2016 Nike, Inc.
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
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.dao.AwsIamRoleDao;
import com.nike.cerberus.domain.IamRolePermission;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRolePermissionRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import com.nike.cerberus.util.AwsIamRoleArnParser;
import com.nike.cerberus.util.UuidSupplier;
import org.mybatis.guice.transactional.Transactional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Provides operations for granting, updating and revoking IAM role permissions.
 */
@Singleton
public class IamRolePermissionService {

    private final UuidSupplier uuidSupplier;

    private final RoleService roleService;

    private final AwsIamRoleDao awsIamRoleDao;

    private final AwsIamRoleArnParser awsIamRoleArnParser;

    @Inject
    public IamRolePermissionService(final UuidSupplier uuidSupplier,
                                    final RoleService roleService,
                                    final AwsIamRoleDao awsIamRoleDao,
                                    final AwsIamRoleArnParser awsIamRoleArnParser) {
        this.uuidSupplier = uuidSupplier;
        this.roleService = roleService;
        this.awsIamRoleDao = awsIamRoleDao;
        this.awsIamRoleArnParser = awsIamRoleArnParser;
    }

    /**
     * Grants a set of IAM role permissions.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamRolePermissionSet The set of IAM role permissions
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void grantIamRolePermissions(final String safeDepositBoxId,
                                        final Set<IamRolePermission> iamRolePermissionSet,
                                        final String user,
                                        final OffsetDateTime dateTime) {
        for (IamRolePermission iamRolePermission : iamRolePermissionSet) {
            grantIamRolePermission(safeDepositBoxId, iamRolePermission, user, dateTime);
        }
    }

    /**
     * Grants a IAM role permission.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamRolePermission The IAM role permission
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void grantIamRolePermission(final String safeDepositBoxId,
                                       final IamRolePermission iamRolePermission,
                                       final String user,
                                       final OffsetDateTime dateTime) {
        final Optional<AwsIamRoleRecord> possibleIamRoleRecord =
                awsIamRoleDao.getIamRole(iamRolePermission.getIamPrincipalArn());

        final Optional<Role> role = roleService.getRoleById(iamRolePermission.getRoleId());

        if (!role.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.USER_GROUP_ROLE_ID_INVALID)
                    .build();
        }

        String iamRoleId;
        if (possibleIamRoleRecord.isPresent()) {
            iamRoleId = possibleIamRoleRecord.get().getId();
        } else {
            iamRoleId = uuidSupplier.get();
            AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord();
            awsIamRoleRecord.setId(iamRoleId);
            awsIamRoleRecord.setAwsAccountId(iamRolePermission.getAccountId());  // TODO: remove
            awsIamRoleRecord.setAwsIamRoleName(iamRolePermission.getIamRoleName());  // TODO: remove
            awsIamRoleRecord.setAwsIamRoleArn(iamRolePermission.getIamPrincipalArn());
            awsIamRoleRecord.setCreatedBy(user);
            awsIamRoleRecord.setLastUpdatedBy(user);
            awsIamRoleRecord.setCreatedTs(dateTime);
            awsIamRoleRecord.setLastUpdatedTs(dateTime);
            awsIamRoleDao.createIamRole(awsIamRoleRecord);
        }

        AwsIamRolePermissionRecord permissionRecord = new AwsIamRolePermissionRecord();
        permissionRecord.setId(uuidSupplier.get());
        permissionRecord.setAwsIamRoleId(iamRoleId);
        permissionRecord.setRoleId(iamRolePermission.getRoleId());
        permissionRecord.setSdboxId(safeDepositBoxId);
        permissionRecord.setCreatedBy(user);
        permissionRecord.setLastUpdatedBy(user);
        permissionRecord.setCreatedTs(dateTime);
        permissionRecord.setLastUpdatedTs(dateTime);
        awsIamRoleDao.createIamRolePermission(permissionRecord);
    }

    /**
     * Updates a set of IAM role permissions.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamRolePermissionSet The set of IAM role permissions
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void updateIamRolePermissions(final String safeDepositBoxId,
                                         final Set<IamRolePermission> iamRolePermissionSet,
                                         final String user,
                                         final OffsetDateTime dateTime) {
        for (IamRolePermission iamRolePermission : iamRolePermissionSet) {
            updateIamRolePermission(safeDepositBoxId, iamRolePermission, user, dateTime);
        }
    }

    /**
     * Updates a IAM role permission.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamRolePermission The IAM role permission
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void updateIamRolePermission(final String safeDepositBoxId,
                                        final IamRolePermission iamRolePermission,
                                        final String user,
                                        final OffsetDateTime dateTime) {
        final Optional<AwsIamRoleRecord> iamRole =
                awsIamRoleDao.getIamRole(iamRolePermission.getIamPrincipalArn());

        if (!iamRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .withExceptionMessage("Unable to update permissions for IAM role that doesn't exist.")
                    .build();
        }

        AwsIamRolePermissionRecord record = new AwsIamRolePermissionRecord();
        record.setSdboxId(safeDepositBoxId);
        record.setAwsIamRoleId(iamRole.get().getId());
        record.setRoleId(iamRolePermission.getRoleId());
        record.setLastUpdatedBy(user);
        record.setLastUpdatedTs(dateTime);
        awsIamRoleDao.updateIamRolePermission(record);
    }

    /**
     * Revokes a set of IAM role permissions.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamRolePermissionSet The set of IAM role permissions
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void revokeIamRolePermissions(final String safeDepositBoxId,
                                         final Set<IamRolePermission> iamRolePermissionSet,
                                         final String user,
                                         final OffsetDateTime dateTime) {
        for (IamRolePermission iamRolePermission : iamRolePermissionSet) {
            revokeIamRolePermission(safeDepositBoxId, iamRolePermission, user, dateTime);
        }
    }

    /**
     * Revokes a IAM role permission.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamRolePermission The IAM role permission
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void revokeIamRolePermission(final String safeDepositBoxId,
                                        final IamRolePermission iamRolePermission,
                                        final String user,
                                        final OffsetDateTime dateTime) {
        final Optional<AwsIamRoleRecord> iamRole =
                awsIamRoleDao.getIamRole(iamRolePermission.getIamPrincipalArn());

        if (!iamRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .withExceptionMessage("Unable to revoke permissions for IAM role that doesn't exist.")
                    .build();
        }

        awsIamRoleDao.deleteIamRolePermission(safeDepositBoxId, iamRole.get().getId());
    }

    public Set<IamRolePermission> getIamRolePermissions(final String safeDepositBoxId) {
        final Set<IamRolePermission> iamRolePermissionSet = Sets.newHashSet();
        final List<AwsIamRolePermissionRecord> permissionRecords = awsIamRoleDao.getIamRolePermissions(safeDepositBoxId);

        permissionRecords.forEach(r -> {
            final Optional<AwsIamRoleRecord> iamRoleRecord = awsIamRoleDao.getIamRoleById(r.getAwsIamRoleId());

            if (iamRoleRecord.isPresent()) {
                final IamRolePermission permission = new IamRolePermission();
                permission.setId(r.getId());
                permission.setAccountId(awsIamRoleArnParser.getAccountId(iamRoleRecord.get().getAwsIamRoleArn()));  // TODO: remove
                permission.setIamRoleName(awsIamRoleArnParser.getRoleName(iamRoleRecord.get().getAwsIamRoleArn()));  // TODO: remove
                permission.setIamPrincipalArn(iamRoleRecord.get().getAwsIamRoleArn());
                permission.setRoleId(r.getRoleId());
                permission.setCreatedBy(r.getCreatedBy());
                permission.setLastUpdatedBy(r.getLastUpdatedBy());
                permission.setCreatedTs(r.getCreatedTs());
                permission.setLastUpdatedTs(r.getLastUpdatedTs());
                iamRolePermissionSet.add(permission);
            }
        });

        return iamRolePermissionSet;
    }

    @Transactional
    public void deleteIamRolePermissions(final String safeDepositBoxId) {
        awsIamRoleDao.deleteIamRolePermissions(safeDepositBoxId);
    }
}
