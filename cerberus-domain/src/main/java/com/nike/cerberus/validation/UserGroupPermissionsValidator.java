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

package com.nike.cerberus.validation;

import com.nike.cerberus.domain.UserGroupPermission;
import java.util.HashSet;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator class for validating that a set of user group permissions contain no duplicate user
 * group names.
 */
public class UserGroupPermissionsValidator
    implements ConstraintValidator<UniqueUserGroupPermissions, Set<UserGroupPermission>> {

  public void initialize(UniqueUserGroupPermissions constraint) {
    // no-op
  }

  public boolean isValid(
      Set<UserGroupPermission> userGroupPermissionSet, ConstraintValidatorContext context) {
    if (userGroupPermissionSet == null || userGroupPermissionSet.isEmpty()) {
      return true;
    }

    boolean isValid = true;
    Set<String> userGroups = new HashSet<>();

    for (UserGroupPermission userGroupPermission : userGroupPermissionSet) {
      if (userGroups.contains(userGroupPermission.getName())) {
        isValid = false;
        break;
      } else {
        userGroups.add(userGroupPermission.getName());
      }
    }

    return isValid;
  }
}
