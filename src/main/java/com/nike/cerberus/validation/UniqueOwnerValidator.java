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

import com.nike.cerberus.domain.SafeDepositBox;
import com.nike.cerberus.domain.UserGroupPermission;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator that checks if the owner field is included in the user group permissions.
 */
public class UniqueOwnerValidator implements ConstraintValidator<UniqueOwner, SafeDepositBox> {

    public void initialize(UniqueOwner constraint) {
        // no-op
    }

    public boolean isValid(SafeDepositBox safeDepositBox, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(safeDepositBox.getOwner())
                || safeDepositBox.getUserGroupPermissions() == null
                || safeDepositBox.getUserGroupPermissions().isEmpty()) {
            return true;
        }

        final Set<String> userGroupNameSet = safeDepositBox.getUserGroupPermissions().stream().map(UserGroupPermission::getName).collect(Collectors.toSet());

        return !userGroupNameSet.contains(safeDepositBox.getOwner());
    }
}
