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

package com.nike.cerberus.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Set;

public interface PermissionsMapper {

    Boolean doesIamPrincipalHaveGivenRoleForSdb(@Param("sdbId") String sdbId,
                                                @Param("iamPrincipalArn") String iamPrincipalArn,
                                                @Param("iamRootArn") String iamRootArn,
                                                @Param("rolesThatAllowPermission") Set<String> rolesThatAllowPermission);

    Boolean doesUserPrincipalHaveGivenRoleForSdb(@Param("sdbId") String sdbId,
                                                 @Param("rolesThatAllowPermission") Set<String> rolesThatAllowPermission,
                                                 @Param("userGroupsThatPrincipalBelongsTo") Set<String> userGroupsThatPrincipalBelongsTo);

    Boolean doesUserHavePermsForRoleAndSdbCaseInsensitive(@Param("sdbId") String sdbId,
                                                          @Param("rolesThatAllowPermission") Set<String> rolesThatAllowPermission,
                                                          @Param("userGroupsThatPrincipalBelongsTo") Set<String> userGroupsThatPrincipalBelongsTo);
}
