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
 *
 */

package com.nike.cerberus.validation;

import com.nike.cerberus.domain.IamRolePermissionV2;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validator class for validating that a set of IAM role permissions contain no duplicate user group names.
 */
public class IamRolePermissionsValidatorV2
        implements ConstraintValidator<UniqueIamRolePermissionsV2, Set<IamRolePermissionV2>> {

    public void initialize(UniqueIamRolePermissionsV2 constraint) {
        // no-op
    }

    public boolean isValid(Set<IamRolePermissionV2> iamRolePermissionSet, ConstraintValidatorContext context) {
        if (iamRolePermissionSet == null || iamRolePermissionSet.isEmpty()) {
            return true;
        }

        boolean isValid = true;
        Set<String> iamRoles = new HashSet<>();

        for (IamRolePermissionV2 iamRolePermission : iamRolePermissionSet) {
            final String key = buildKey(iamRolePermission);
            if (iamRoles.contains(key)) {
                isValid = false;
                break;
            } else {
                iamRoles.add(key);
            }
        }

        return isValid;
    }

    private String buildKey(IamRolePermissionV2 iamRolePermission) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(StringUtils.lowerCase(iamRolePermission.getIamPrincipalArn(), Locale.ENGLISH));

        return stringBuilder.toString();
    }

}
