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
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PermissionsDao {

  private final PermissionsMapper permissionsMapper;

  @Autowired
  public PermissionsDao(PermissionsMapper permissionsMapper) {
    this.permissionsMapper = permissionsMapper;
  }

  public Boolean doesIamPrincipalHaveRoleForSdb(
      String sdbId,
      String iamPrincipalArn,
      String iamRootArn,
      Set<String> rolesThatAllowPermission) {
    return permissionsMapper.doesIamPrincipalHaveGivenRoleForSdb(
        sdbId, iamPrincipalArn, iamRootArn, rolesThatAllowPermission);
  }

  public Boolean doesAssumedRoleHaveRoleForSdb(
      String sdbId,
      String assumedRoleArn,
      String iamRoleArn,
      String iamRootArn,
      Set<String> rolesThatAllowPermission) {
    return permissionsMapper.doesAssumedRoleHaveGivenRoleForSdb(
        sdbId, assumedRoleArn, iamRoleArn, iamRootArn, rolesThatAllowPermission);
  }

  public Boolean doesUserPrincipalHaveRoleForSdb(
      String sdbId,
      Set<String> rolesThatAllowPermission,
      Set<String> userGroupsThatPrincipalBelongsTo) {
    return permissionsMapper.doesUserPrincipalHaveGivenRoleForSdb(
        sdbId, rolesThatAllowPermission, userGroupsThatPrincipalBelongsTo);
  }

  public Boolean doesUserHavePermsForRoleAndSdbCaseInsensitive(
      String sdbId,
      Set<String> rolesThatAllowPermission,
      Set<String> userGroupsThatPrincipalBelongsTo) {
    return permissionsMapper.doesUserHavePermsForRoleAndSdbCaseInsensitive(
        sdbId, rolesThatAllowPermission, userGroupsThatPrincipalBelongsTo);
  }
}
