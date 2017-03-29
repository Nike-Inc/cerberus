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

    @Test(expected = RuntimeException.class)
    public void getArnFailsNullRoleName() {

        awsIamRoleArnParser.getAccountId("hullabaloo");
    }

    @Test
    public void getAccountIdHappy() {

        assertEquals("1111111111", awsIamRoleArnParser.getAccountId("arn:aws:iam::1111111111:role/lamb_dev_health"));
    }

    @Test(expected = RuntimeException.class)
    public void getAccountIdFailsInvalidArn() {

        awsIamRoleArnParser.getAccountId("hullabaloo");
    }

    @Test
    public void getRoleNameHappy() {

        assertEquals("my_roleName", awsIamRoleArnParser.getRoleName("arn:aws:iam::222222:role/my_roleName"));
    }

    @Test(expected = RuntimeException.class)
    public void getRoleNameFailsInvalidArn() {

        awsIamRoleArnParser.getRoleName("brouhaha");
    }
}