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

import com.nike.cerberus.domain.IamPrincipalPermission;
import org.junit.Test;

import javax.validation.Validation;
import javax.validation.Validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the IamPrincipalPermission class
 */
public class IamPrincipalPermissionTest {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void test_that_IamPrincipalPermission_can_be_constructed_with_a_user_iam_principal_arn() {

        assertTrue(validator.validate(
                new IamPrincipalPermission().withIamPrincipalArn("arn:aws:iam::123456789012:user/Bob").withRoleId("role id")).isEmpty());

    }

    @Test
    public void test_that_IamPrincipalPermission_fails_with_invalid_iam_principal_arn() {

        assertFalse(validator.validate(
                new IamPrincipalPermission().withIamPrincipalArn("arn:aws:foo::123456789012:user/Bob").withRoleId("role id")).isEmpty());

    }

    @Test
    public void test_that_IamPrincipalPermission_can_be_constructed_with_a_federated_user_iam_principal_arn() {

        assertTrue(validator.validate(
                new IamPrincipalPermission().withIamPrincipalArn("arn:aws:sts::123456789012:federated-user/Bob").withRoleId("role id")).isEmpty());

    }

    @Test
    public void test_that_IamPrincipalPermission_can_be_constructed_with_a_assumed_role_iam_principal_arn() {

        assertTrue(validator.validate(
                new IamPrincipalPermission().withIamPrincipalArn("arn:aws:sts::123456789012:assumed-role/Accounting-Role/Mary").withRoleId("role id")).isEmpty());

    }


}
