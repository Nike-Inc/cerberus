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
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the IamRolePermissionsValidatorV1 class
 */
public class IamRolePermissionsValidatorV2Test {

    private ConstraintValidatorContext mockConstraintValidatorContext;

    private IamRolePermissionsValidatorV2 subject;

    @Before
    public void setup() {
        mockConstraintValidatorContext = mock(ConstraintValidatorContext.class);
        subject = new IamRolePermissionsValidatorV2();
    }

    @Test
    public void null_set_is_valid() {
        assertThat(subject.isValid(null, mockConstraintValidatorContext)).isTrue();
    }

    @Test
    public void empty_set_is_valid() {
        assertThat(subject.isValid(new HashSet<>(), mockConstraintValidatorContext)).isTrue();
    }

    @Test
    public void unique_set_is_valid() {

        IamRolePermissionV2 a = new IamRolePermissionV2();
        a.withIamPrincipalArn("arn:aws:iam::123:role/abc");
        IamRolePermissionV2 b = new IamRolePermissionV2();
        b.withIamPrincipalArn("arn:aws:iam::123:role/def");

        assertThat(subject.isValid(Sets.newSet(a, b), mockConstraintValidatorContext)).isTrue();
    }

    @Test
    public void duplicate_set_is_invalid() {

        IamRolePermissionV2 a = new IamRolePermissionV2();
        a.withIamPrincipalArn("arn:aws:iam::123:role/abc");
        IamRolePermissionV2 b = new IamRolePermissionV2();
        b.withIamPrincipalArn("arn:aws:iam::123:role/ABC");

        assertThat(subject.isValid(Sets.newSet(a, b), mockConstraintValidatorContext)).isFalse();
    }
}