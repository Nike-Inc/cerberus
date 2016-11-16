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
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the UniqueOwnerValidator class
 */
public class UniqueOwnerValidatorTest {

    private ConstraintValidatorContext mockConstraintValidatorContext;

    private UniqueOwnerValidator subject;

    @Before
    public void setup() {
        mockConstraintValidatorContext = mock(ConstraintValidatorContext.class);
        subject = new UniqueOwnerValidator();
    }

    @Test
    public void blank_owner_returns_valid() {
        SafeDepositBox safeDepositBox = new SafeDepositBox();
        safeDepositBox.setOwner("   ");
        assertThat(subject.isValid(safeDepositBox, mockConstraintValidatorContext)).isTrue();
    }

    @Test
    public void empty_user_group_set_is_valid() {
        SafeDepositBox safeDepositBox = new SafeDepositBox();
        safeDepositBox.setOwner("owner");
        assertThat(subject.isValid(safeDepositBox, mockConstraintValidatorContext)).isTrue();
    }

    @Test
    public void unique_owner_is_valid() {
        UserGroupPermission userGroupPermission = new UserGroupPermission();
        userGroupPermission.setName("group");
        SafeDepositBox safeDepositBox = new SafeDepositBox();
        safeDepositBox.setOwner("owner");
        safeDepositBox.getUserGroupPermissions().add(userGroupPermission);

        assertThat(subject.isValid(safeDepositBox, mockConstraintValidatorContext)).isTrue();
    }

    @Test
    public void owner_in_group_permissions_is_invalid() {
        UserGroupPermission userGroupPermission = new UserGroupPermission();
        userGroupPermission.setName("owner");
        SafeDepositBox safeDepositBox = new SafeDepositBox();
        safeDepositBox.setOwner("owner");
        safeDepositBox.getUserGroupPermissions().add(userGroupPermission);

        assertThat(subject.isValid(safeDepositBox, mockConstraintValidatorContext)).isFalse();
    }
}