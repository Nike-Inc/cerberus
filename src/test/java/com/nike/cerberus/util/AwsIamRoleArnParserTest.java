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

import static com.nike.cerberus.util.AwsIamRoleArnParser.IAM_PRINCIPAL_ARN_PATTERN_ALLOWED;
import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void convertPrincipalArnToRoleArn_properly_converts_principals_to_role_arns() {

        assertEquals("arn:aws:iam::1111111111:role/lamb_dev_health", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::1111111111:federated-user/lamb_dev_health"));
        assertEquals("arn:aws:iam::2222222222:role/prince_role", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::2222222222:assumed-role/prince_role/session-name"));
        assertEquals("arn:aws:iam::2222222222:role/sir/alfred/role", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::2222222222:assumed-role/sir/alfred/role/session-name"));
        assertEquals("arn:aws:iam::3333333333:role/path/to/foo", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:iam::3333333333:role/path/to/foo"));
        assertEquals("arn:aws:iam::4444444444:role/name", awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:iam::4444444444:role/name"));
    }

    @Test(expected = RuntimeException.class)
    public void convertPrincipalArnToRoleArn_fails_on_invalid_arn() {

        awsIamRoleArnParser.convertPrincipalArnToRoleArn("foobar");
    }

    @Test(expected = RuntimeException.class)
    public void convertPrincipalArnToRoleArn_fails_on_group_arn() {

        awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:iam::1111111111:group/path/to/group");
    }

    @Test(expected = RuntimeException.class)
    public void convertPrincipalArnToRoleArn_fails_on_invalid_assumed_role_arn() {

        awsIamRoleArnParser.convertPrincipalArnToRoleArn("arn:aws:sts::1111111111:assumed-role/blah");
    }

    @Test
    public void isRoleArn_returns_true_when_is_role_arn()  {

        assertTrue(awsIamRoleArnParser.isRoleArn("arn:aws:iam::2222222222:role/fancy/role/path"));
        assertTrue(awsIamRoleArnParser.isRoleArn("arn:aws:iam::1111111111:role/name"));
        assertFalse(awsIamRoleArnParser.isRoleArn("arn:aws:iam::3333333333:assumed-role/happy/path"));
        assertFalse(awsIamRoleArnParser.isRoleArn("arn:aws:sts::1111111111:federated-user/my_user"));
        assertFalse(awsIamRoleArnParser.isRoleArn("arn:aws:iam::1111111111:group/path/to/group"));
    }

    @Test
    public void test_IAM_PRINCIPAL_ARN_PATTERN_valid_ARNs_accepted_by_KMS() {
        // valid
        assertTrue(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:role/some-role").matches());
        assertTrue(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:role/some/path/some-role").matches());
        assertTrue(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:user/some-user").matches());
        assertTrue(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:sts::12345678901234:assumed-role/some-path/some-role").matches());
        assertTrue(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:sts::12345678901234:assumed-role/some-role").matches());
        assertTrue(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:sts::12345678901234:federated-user/my_user").matches());
    }

    @Test
    public void test_IAM_PRINCIPAL_ARN_PATTERN_valid_ARNs_rejected_by_KMS() {
        // invalid - KMS doesn't allow 'group' or 'instance-profile'
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:group/some-group").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:instance-profile/some-profile").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:other/some-value").matches());
    }

    @Test
    public void test_IAM_PRINCIPAL_ARN_PATTERN_invalid_ARNs() {
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:some-role").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam:::role/some-role").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:::12345678901234:role/some-role").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn::iam::12345678901234:role/some-role").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher(":aws:iam::12345678901234:role/some-role").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:other/some-value").matches());
    }

    @Test
    public void test_isArnThatCanGoInKeyPolicy() {
        assertTrue(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:iam::12345678901234:role/some-role"));
        assertTrue(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:iam::12345678901234:role/some/path/some-role"));
        assertTrue(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:iam::12345678901234:user/some-user"));
        assertTrue(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:sts::12345678901234:assumed-role/some-path/some-role"));
        assertTrue(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:sts::12345678901234:assumed-role/some-role"));
        assertTrue(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:sts::12345678901234:federated-user/my_user"));

        // invalid - KMS doesn't allow 'group' or 'instance-profile'
        assertFalse(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:iam::12345678901234:group/some-group"));
        assertFalse(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:iam::12345678901234:instance-profile/some-profile"));
        assertFalse(awsIamRoleArnParser.isArnThatCanGoInKeyPolicy("arn:aws:iam::12345678901234:other/some-value"));
    }

    @Test
    public void test_stripOutDescription() {
        assertEquals("12345678901234/some-role", awsIamRoleArnParser.stripOutDescription("arn:aws:iam::12345678901234:role/some-role"));
        assertEquals("12345678901234/some/path/some-role", awsIamRoleArnParser.stripOutDescription("arn:aws:iam::12345678901234:role/some/path/some-role"));
        assertEquals("12345678901234/some-user", awsIamRoleArnParser.stripOutDescription("arn:aws:iam::12345678901234:user/some-user"));
        assertEquals("12345678901234/some-path/some-role", awsIamRoleArnParser.stripOutDescription("arn:aws:sts::12345678901234:assumed-role/some-path/some-role"));
        assertEquals("12345678901234/some-role", awsIamRoleArnParser.stripOutDescription("arn:aws:sts::12345678901234:assumed-role/some-role"));
        assertEquals("12345678901234/my_user", awsIamRoleArnParser.stripOutDescription("arn:aws:sts::12345678901234:federated-user/my_user"));

        // invalid - KMS doesn't allow 'group' or 'instance-profile' though some parsing is still possible (this behavior isn't important)
        assertEquals("", awsIamRoleArnParser.stripOutDescription("arn:aws:iam::12345678901234:group/some-group"));
        assertEquals("12345678901234/some-value", awsIamRoleArnParser.stripOutDescription("arn:aws:iam::12345678901234:other/some-value"));
        assertEquals("12345678901234/some-profile", awsIamRoleArnParser.stripOutDescription("arn:aws:iam::12345678901234:instance-profile/some-profile"));
    }

    @Test
    public void test_that_an_iam_role_can_not_end_with_whitespace() {
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:role/some-role ").matches());
        assertFalse(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:role/some-role\t").matches());
    }

    @Test
    public void test_isAccountRootArn() {
        assertTrue(awsIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:root"));

        assertFalse(awsIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:role/foo"));
        assertFalse(awsIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:user/bar"));
        assertFalse(awsIamRoleArnParser.isAccountRootArn("arn:aws:sts::0000000000:assumed-role/baz"));
        assertFalse(awsIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:group/foobar"));
        assertFalse(awsIamRoleArnParser.isAccountRootArn("arn:aws:sts::0000000000:federated-user/foobaz"));

    }

    @Test
    public void test_arnAccountIdsDoMatch_returns_true_when_account_ids_match() {
        assertTrue(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::0000000000:root",
                "arn:aws:iam::0000000000:role/foo"));
        assertTrue(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::1111111111:role/foo",
                "arn:aws:iam::1111111111:user/bar"));
        assertTrue(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::2222222222:user/bar",
                "arn:aws:sts::2222222222:assumed-role/baz"));
        assertTrue(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:sts::3333333333:assumed-role/baz",
                "arn:aws:iam::3333333333:group/foobar"));
        assertTrue(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::4444:group/foobar",
                "arn:aws:sts::4444:federated-user/foobaz"));

    }

    @Test
    public void test_arnAccountIdsDoMatch_returns_false_when_account_ids_do_not_match() {
        assertFalse(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::0000000000:root",
                "arn:aws:iam::1111111111:role/foo"));
        assertFalse(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::1111111111:role/foo",
                "arn:aws:iam::0000000000:user/bar"));
        assertFalse(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::2222222222:user/bar",
                "arn:aws:sts::3333333333:assumed-role/baz"));
        assertFalse(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:sts::3333333333:assumed-role/baz",
                "arn:aws:iam::2222222222:group/foobar"));
        assertFalse(awsIamRoleArnParser.arnAccountIdsDoMatch(
                "arn:aws:iam::4444:group/foobar",
                "arn:aws:sts::3333333333:federated-user/foobaz"));

    }
}