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
import com.nike.cerberus.domain.IamPrincipalPermission;
import com.nike.cerberus.domain.Role;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.record.AwsIamRolePermissionRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
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
public class IamPrincipalPermissionService {

    private final UuidSupplier uuidSupplier;

    private final RoleService roleService;

    private final AwsIamRoleDao awsIamRoleDao;

    @Inject
    public IamPrincipalPermissionService(final UuidSupplier uuidSupplier,
                                         final RoleService roleService,
                                         final AwsIamRoleDao awsIamRoleDao) {
        this.uuidSupplier = uuidSupplier;
        this.roleService = roleService;
        this.awsIamRoleDao = awsIamRoleDao;
    }

    /**
     * Grants a set of IAM role permissions.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamPrincipalPermissionSet The set of IAM principal permissions
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void grantIamPrincipalPermissions(final String safeDepositBoxId,
                                             final Set<IamPrincipalPermission> iamPrincipalPermissionSet,
                                             final String user,
                                             final OffsetDateTime dateTime) {
        for (IamPrincipalPermission iamRolePermission : iamPrincipalPermissionSet) {
            grantIamPrincipalPermission(safeDepositBoxId, iamRolePermission, user, dateTime);
        }
    }

    /**
     * Grants a IAM role permission.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamPrincipalPermission The IAM principal permission
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void grantIamPrincipalPermission(final String safeDepositBoxId,
                                            final IamPrincipalPermission iamPrincipalPermission,
                                            final String user,
                                            final OffsetDateTime dateTime) {
        final Optional<AwsIamRoleRecord> possibleIamRoleRecord =
                awsIamRoleDao.getIamRole(iamPrincipalPermission.getIamPrincipalArn());

        final Optional<Role> role = roleService.getRoleById(iamPrincipalPermission.getRoleId());

        if (!role.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.IAM_ROLE_ROLE_ID_INVALID)
                    .build();
        }

        String iamRoleId;
        if (possibleIamRoleRecord.isPresent()) {
            iamRoleId = possibleIamRoleRecord.get().getId();
        } else {
            iamRoleId = uuidSupplier.get();
            AwsIamRoleRecord awsIamRoleRecord = new AwsIamRoleRecord();
            awsIamRoleRecord.setId(iamRoleId);
            awsIamRoleRecord.setAwsIamRoleArn(iamPrincipalPermission.getIamPrincipalArn());
            awsIamRoleRecord.setCreatedBy(user);
            awsIamRoleRecord.setLastUpdatedBy(user);
            awsIamRoleRecord.setCreatedTs(dateTime);
            awsIamRoleRecord.setLastUpdatedTs(dateTime);
            awsIamRoleDao.createIamRole(awsIamRoleRecord);
        }

        AwsIamRolePermissionRecord permissionRecord = new AwsIamRolePermissionRecord();
        permissionRecord.setId(uuidSupplier.get());
        permissionRecord.setAwsIamRoleId(iamRoleId);
        permissionRecord.setRoleId(iamPrincipalPermission.getRoleId());
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
     * @param iamPrincipalPermissionSet The set of IAM principal permissions
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void updateIamPrincipalPermissions(final String safeDepositBoxId,
                                              final Set<IamPrincipalPermission> iamPrincipalPermissionSet,
                                              final String user,
                                              final OffsetDateTime dateTime) {
        for (IamPrincipalPermission iamRolePermission : iamPrincipalPermissionSet) {
            updateIamPrincipalPermission(safeDepositBoxId, iamRolePermission, user, dateTime);
        }
    }

    /**
     * Updates a IAM role permission.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamPrincipalPermission The IAM principal permission
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void updateIamPrincipalPermission(final String safeDepositBoxId,
                                             final IamPrincipalPermission iamPrincipalPermission,
                                             final String user,
                                             final OffsetDateTime dateTime) {
        final Optional<AwsIamRoleRecord> iamRole =
                awsIamRoleDao.getIamRole(iamPrincipalPermission.getIamPrincipalArn());

        if (!iamRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .withExceptionMessage("Unable to update permissions for IAM role that doesn't exist.")
                    .build();
        }

        AwsIamRolePermissionRecord record = new AwsIamRolePermissionRecord();
        record.setSdboxId(safeDepositBoxId);
        record.setAwsIamRoleId(iamRole.get().getId());
        record.setRoleId(iamPrincipalPermission.getRoleId());
        record.setLastUpdatedBy(user);
        record.setLastUpdatedTs(dateTime);
        awsIamRoleDao.updateIamRolePermission(record);
    }

    /**
     * Revokes a set of IAM role permissions.
     *
     * @param safeDepositBoxId The safe deposit box id
     * @param iamPrincipalPermissionSet The set of IAM principal permissions
     * @param user The user making the changes
     * @param dateTime The time of the changes
     */
    @Transactional
    public void revokeIamPrincipalPermissions(final String safeDepositBoxId,
                                              final Set<IamPrincipalPermission> iamPrincipalPermissionSet,
                                              final String user,
                                              final OffsetDateTime dateTime) {
        for (IamPrincipalPermission iamRolePermission : iamPrincipalPermissionSet) {
            revokeIamPrincipalPermission(safeDepositBoxId, iamRolePermission);
        }
    }

    /**
     * Revokes a IAM role permission.
     *  @param safeDepositBoxId The safe deposit box id
     * @param iamPrincipalPermission The IAM principal permission
     */
    @Transactional
    public void revokeIamPrincipalPermission(final String safeDepositBoxId,
                                             final IamPrincipalPermission iamPrincipalPermission) {
        final Optional<AwsIamRoleRecord> iamRole =
                awsIamRoleDao.getIamRole(iamPrincipalPermission.getIamPrincipalArn());

        if (!iamRole.isPresent()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.ENTITY_NOT_FOUND)
                    .withExceptionMessage("Unable to revoke permissions for IAM role that doesn't exist.")
                    .build();
        }

        awsIamRoleDao.deleteIamRolePermission(safeDepositBoxId, iamRole.get().getId());
    }

    public Set<IamPrincipalPermission> getIamPrincipalPermissions(final String safeDepositBoxId) {
        final Set<IamPrincipalPermission> iamPrincipalPermissionSet = Sets.newHashSet();
        final List<AwsIamRolePermissionRecord> permissionRecords = awsIamRoleDao.getIamRolePermissions(safeDepositBoxId);

        permissionRecords.forEach(r -> {
            final Optional<AwsIamRoleRecord> iamRoleRecord = awsIamRoleDao.getIamRoleById(r.getAwsIamRoleId());

            if (iamRoleRecord.isPresent()) {
                final IamPrincipalPermission permission = new IamPrincipalPermission();
                permission.setId(r.getId());
                permission.setIamPrincipalArn(iamRoleRecord.get().getAwsIamRoleArn());
                permission.setRoleId(r.getRoleId());
                permission.setCreatedBy(r.getCreatedBy());
                permission.setLastUpdatedBy(r.getLastUpdatedBy());
                permission.setCreatedTs(r.getCreatedTs());
                permission.setLastUpdatedTs(r.getLastUpdatedTs());
                iamPrincipalPermissionSet.add(permission);
            }
        });

        return iamPrincipalPermissionSet;
    }

    @Transactional
    public void deleteIamPrincipalPermissions(final String safeDepositBoxId) {
        awsIamRoleDao.deleteIamRolePermissions(safeDepositBoxId);
    }
}
