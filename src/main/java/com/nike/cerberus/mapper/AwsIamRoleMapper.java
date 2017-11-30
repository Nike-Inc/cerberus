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

package com.nike.cerberus.mapper;

import com.nike.cerberus.record.AwsIamRoleKmsKeyRecord;
import com.nike.cerberus.record.AwsIamRolePermissionRecord;
import com.nike.cerberus.record.AwsIamRoleRecord;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * MyBatis mapper for executing SQL queries related to IAM roles and permissions.
 */
public interface AwsIamRoleMapper {

    AwsIamRoleRecord getIamRoleById(@Param("id") String id);

    AwsIamRoleRecord getIamRole(@Param("awsIamRoleArn") String awsIamRoleArn);

    AwsIamRoleKmsKeyRecord getKmsKey(@Param("awsIamRoleId") String awsIamRoleId,
                                     @Param("awsRegion") String awsRegion);

    int createIamRoleKmsKey(@Param("record") AwsIamRoleKmsKeyRecord record);

    int createIamRole(@Param("record") AwsIamRoleRecord record);

    int createIamRolePermission(@Param("record") AwsIamRolePermissionRecord record);

    int updateIamRolePermission(@Param("record") AwsIamRolePermissionRecord record);

    int deleteIamRolePermission(@Param("safeDepositBoxId") String safeDepositBoxId,
                                @Param("awsIamRoleId") String awsIamRoleId);

    List<AwsIamRolePermissionRecord> getIamRolePermissions(@Param("safeDepositBoxId") String safeDepositBoxId);

    int deleteIamRolePermissions(@Param("safeDepositBoxId") String safeDepositBoxId);

    int updateIamRoleKmsKey(@Param("record") AwsIamRoleKmsKeyRecord record);

    List<AwsIamRoleKmsKeyRecord> getInactiveOrOrphanedKmsKeys(@Param("keyInactiveDateTime") OffsetDateTime keyInactiveDateTime);

    List<AwsIamRoleRecord> getOrphanedIamRoles();

    int getTotalNumberOfUniqueIamRoles();

    int deleteIamRoleById(@Param("id") final String id);

    int deleteKmsKeyById(@Param("id") final String id);
}
