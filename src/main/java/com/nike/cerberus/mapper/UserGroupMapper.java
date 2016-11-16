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

import com.nike.cerberus.record.UserGroupPermissionRecord;
import com.nike.cerberus.record.UserGroupRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for executing SQL queries related to user group and permissions.
 */
public interface UserGroupMapper {

    UserGroupRecord getUserGroup(@Param("id") String id);

    UserGroupRecord getUserGroupByName(@Param("name") String name);

    List<UserGroupRecord> getUserGroupsByRole(@Param("safeDepositBoxId") String safeDepositBoxId,
                                                  @Param("roleId") String roleId);

    int createUserGroup(@Param("record") UserGroupRecord record);

    int createUserGroupPermission(@Param("record") UserGroupPermissionRecord record);

    int updateUserGroupPermission(@Param("record") UserGroupPermissionRecord record);

    int deleteUserGroupPermission(@Param("safeDepositBoxId") String safeDepositBoxId,
                                      @Param("userGroupId") String userGroupId);

    List<UserGroupPermissionRecord> getUserGroupPermissions(@Param("safeDepositBoxId") String safeDepositBoxId);

    int deleteUserGroupPermissions(@Param("safeDepositBoxId") String safeDepositBoxId);
}
