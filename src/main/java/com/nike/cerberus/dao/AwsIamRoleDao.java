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

package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.AwsIamRoleMapper;
import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRolePermissionRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for the IAM role and permissions data.
 */
public class AwsIamRoleDao {

    private final AwsIamRoleMapper awsIamRoleMapper;

    @Inject
    public AwsIamRoleDao(final AwsIamRoleMapper awsIamRoleMapper) {
        this.awsIamRoleMapper = awsIamRoleMapper;
    }

    public Optional<AwsIamRoleRecord> getIamRoleById(final String id) {
        return Optional.ofNullable(awsIamRoleMapper.getIamRoleById(id));
    }

    // TODO: remove
    public Optional<AwsIamRoleRecord> getIamRole(final String awsAccountId, final String awsIamRoleName) {
        return Optional.ofNullable(awsIamRoleMapper.getIamRole(awsAccountId, awsIamRoleName));
    }

    public Optional<AwsIamRoleRecord> getIamRole(final String awsIamRoleArn) {
        return Optional.ofNullable(awsIamRoleMapper.getIamRole(awsIamRoleArn));
    }

    public int createIamRole(final AwsIamRoleRecord record) {
        return awsIamRoleMapper.createIamRole(record);
    }

    public List<AwsIamRolePermissionRecord> getIamRolePermissions(final String safeDepositBoxId) {
        return awsIamRoleMapper.getIamRolePermissions(safeDepositBoxId);
    }

    public int createIamRolePermission(final AwsIamRolePermissionRecord record) {
        return awsIamRoleMapper.createIamRolePermission(record);
    }

    public int updateIamRolePermission(final AwsIamRolePermissionRecord record) {
        return awsIamRoleMapper.updateIamRolePermission(record);
    }

    public int deleteIamRolePermission(final String safeDepositBoxId, final String awsIamRoleId) {
        return awsIamRoleMapper.deleteIamRolePermission(safeDepositBoxId, awsIamRoleId);
    }

    public int deleteIamRolePermissions(final String safeDepositBoxId) {
        return awsIamRoleMapper.deleteIamRolePermissions(safeDepositBoxId);
    }

    public Optional<AwsIamRoleKmsKeyRecord> getKmsKey(final String awsIamRoleId, final String awsRegion) {
        return Optional.ofNullable(awsIamRoleMapper.getKmsKey(awsIamRoleId, awsRegion));
    }

    public int createIamRoleKmsKey(final AwsIamRoleKmsKeyRecord record) {
        return awsIamRoleMapper.createIamRoleKmsKey(record);
    }
}
