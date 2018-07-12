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

package com.nike.cerberus.dao;

import com.nike.cerberus.mapper.PermissionsMapper;

import javax.inject.Inject;
import java.util.Set;

public class PermissionsDao {


    private final PermissionsMapper permissionsMapper;

    @Inject
    public PermissionsDao(PermissionsMapper permissionsMapper) {
        this.permissionsMapper = permissionsMapper;
    }

    public Boolean doesIamPrincipalHaveRoleForSdb(String sdbId, String iamPrincipalArn, Set<String> rolesThatAllowPermission) {
        return permissionsMapper.doesIamPrincipalHaveGivenRoleForSdb(sdbId, iamPrincipalArn, rolesThatAllowPermission);
    }

    public Boolean doesUserPrincipalHaveRoleForSdb(String sdbId, Set<String> rolesThatAllowPermission, Set<String> userGroupsThatPrincipalBelongsTo) {
        return permissionsMapper.doesUserPrincipalHaveGivenRoleForSdb(sdbId, rolesThatAllowPermission, userGroupsThatPrincipalBelongsTo);
    }

    public Boolean doesUserHavePermsForRoleAndSdbCaseInsensitive(String sdbId, Set<String> rolesThatAllowPermission, Set<String> userGroupsThatPrincipalBelongsTo) {
        return permissionsMapper.doesUserHavePermsForRoleAndSdbCaseInsensitive(sdbId, rolesThatAllowPermission, userGroupsThatPrincipalBelongsTo);
    }
}
