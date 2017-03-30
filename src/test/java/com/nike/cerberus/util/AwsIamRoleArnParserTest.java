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

package com.nike.cerberus.util;

import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the AwsIamRoleArnParser class
 */
public class AwsIamRoleArnParserTest {

    private AwsIamRoleArnParser awsIamRoleArnParser;

    @Before
    public void setup() {

        awsIamRoleArnParser = new AwsIamRoleArnParser();
    }

    @Test
    public void getAccountId_returns_an_account_id_given_a_valid_arn() {

        assertEquals("1111111111", awsIamRoleArnParser.getAccountId("arn:aws:iam::1111111111:role/lamb_dev_health"));
    }

    @Test(expected = RuntimeException.class)
    public void getAccountId_fails_on_invalid_arn() {

        awsIamRoleArnParser.getAccountId("hullabaloo");
    }

    @Test
    public void getRoleNameHappy_returns_the_role_name_given_a_valid_arn() {

        assertEquals("my_roleName", awsIamRoleArnParser.getRoleName("arn:aws:iam::222222:role/my_roleName"));
    }

    @Test(expected = RuntimeException.class)
    public void getRoleName_fails_on_invalid_arn() {

        awsIamRoleArnParser.getRoleName("brouhaha");
    }
}