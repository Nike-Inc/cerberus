/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.util;

import static com.nike.cerberus.domain.DomainConstants.IAM_PRINCIPAL_ARN_PATTERN_ALLOWED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/** Tests the AwsIamRoleArnParser class */
public class AwsIamRoleArnParserTest {

  private AwsIamRoleArnParser awsGlobalIamRoleArnParser;
  private AwsIamRoleArnParser awsChinaIamRoleArnParser;

  @Before
  public void setup() {
    awsGlobalIamRoleArnParser = new AwsIamRoleArnParser(true, false);
    awsChinaIamRoleArnParser = new AwsIamRoleArnParser(false, true);
  }

  @Test
  public void getAccountId_returns_an_account_id_given_a_valid_arn() {
    assertEquals(
        "1111111111",
        awsGlobalIamRoleArnParser.getAccountId("arn:aws:iam::1111111111:role/lamb_dev_health"));
    assertEquals(
        "1111111111",
        awsChinaIamRoleArnParser.getAccountId("arn:aws-cn:iam::1111111111:role/lamb_dev_health"));
  }

  @Test(expected = RuntimeException.class)
  public void getAccountId_fails_on_invalid_arn() {

    awsGlobalIamRoleArnParser.getAccountId("hullabaloo");
  }

  @Test
  public void getRoleNameHappy_returns_the_role_name_given_a_valid_arn() {

    assertEquals(
        "my_roleName",
        awsGlobalIamRoleArnParser.getRoleName("arn:aws:iam::222222:role/my_roleName"));
    assertEquals(
        "my_roleName",
        awsChinaIamRoleArnParser.getRoleName("arn:aws-cn:iam::222222:role/my_roleName"));
  }

  @Test(expected = RuntimeException.class)
  public void getRoleName_fails_on_invalid_arn() {

    awsGlobalIamRoleArnParser.getRoleName("brouhaha");
  }

  @Test
  public void convertPrincipalArnToRoleArn_properly_converts_principals_to_role_arns() {

    assertEquals(
        "arn:aws:iam::1111111111:role/lamb_dev_health",
        awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws:sts::1111111111:federated-user/lamb_dev_health"));
    assertEquals(
        "arn:aws:iam::2222222222:role/prince_role",
        awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws:sts::2222222222:assumed-role/prince_role/session-name"));
    assertEquals(
        "arn:aws:iam::2222222222:role/sir/alfred/role",
        awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws:sts::2222222222:assumed-role/sir/alfred/role/session-name"));
    assertEquals(
        "arn:aws:iam::3333333333:role/path/to/foo",
        awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws:iam::3333333333:role/path/to/foo"));
    assertEquals(
        "arn:aws:iam::4444444444:role/name",
        awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws:iam::4444444444:role/name"));

    assertEquals(
        "arn:aws-cn:iam::1111111111:role/lamb_dev_health",
        awsChinaIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws-cn:sts::1111111111:federated-user/lamb_dev_health"));
    assertEquals(
        "arn:aws-cn:iam::2222222222:role/prince_role",
        awsChinaIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws-cn:sts::2222222222:assumed-role/prince_role/session-name"));
    assertEquals(
        "arn:aws-cn:iam::2222222222:role/sir/alfred/role",
        awsChinaIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws-cn:sts::2222222222:assumed-role/sir/alfred/role/session-name"));
    assertEquals(
        "arn:aws-cn:iam::3333333333:role/path/to/foo",
        awsChinaIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws-cn:iam::3333333333:role/path/to/foo"));
    assertEquals(
        "arn:aws-cn:iam::4444444444:role/name",
        awsChinaIamRoleArnParser.convertPrincipalArnToRoleArn(
            "arn:aws-cn:iam::4444444444:role/name"));
  }

  @Test(expected = RuntimeException.class)
  public void convertPrincipalArnToRoleArn_fails_on_invalid_arn() {

    awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn("foobar");
  }

  @Test(expected = RuntimeException.class)
  public void convertPrincipalArnToRoleArn_fails_on_group_arn() {

    awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn(
        "arn:aws:iam::1111111111:group/path/to/group");
  }

  @Test(expected = RuntimeException.class)
  public void convertPrincipalArnToRoleArn_fails_on_invalid_assumed_role_arn() {

    awsGlobalIamRoleArnParser.convertPrincipalArnToRoleArn(
        "arn:aws:sts::1111111111:assumed-role/blah");
  }

  @Test
  public void isRoleArn_returns_true_when_is_role_arn() {

    assertTrue(awsGlobalIamRoleArnParser.isRoleArn("arn:aws:iam::2222222222:role/fancy/role/path"));
    assertTrue(awsGlobalIamRoleArnParser.isRoleArn("arn:aws:iam::1111111111:role/name"));
    assertFalse(
        awsGlobalIamRoleArnParser.isRoleArn("arn:aws:iam::3333333333:assumed-role/happy/path"));
    assertFalse(
        awsGlobalIamRoleArnParser.isRoleArn("arn:aws:sts::1111111111:federated-user/my_user"));
    assertFalse(awsGlobalIamRoleArnParser.isRoleArn("arn:aws:iam::1111111111:group/path/to/group"));

    assertTrue(
        awsChinaIamRoleArnParser.isRoleArn("arn:aws-cn:iam::2222222222:role/fancy/role/path"));
    assertTrue(awsChinaIamRoleArnParser.isRoleArn("arn:aws-cn:iam::1111111111:role/name"));
    assertFalse(
        awsChinaIamRoleArnParser.isRoleArn("arn:aws-cn:iam::3333333333:assumed-role/happy/path"));
    assertFalse(
        awsChinaIamRoleArnParser.isRoleArn("arn:aws-cn:sts::1111111111:federated-user/my_user"));
    assertFalse(
        awsChinaIamRoleArnParser.isRoleArn("arn:aws-cn:iam::1111111111:group/path/to/group"));
  }

  @Test
  public void test_IAM_PRINCIPAL_ARN_PATTERN_valid_ARNs_accepted_by_KMS() {
    // valid
    assertTrue(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:role/some-role")
            .matches());
    assertTrue(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:role/some/path/some-role")
            .matches());
    assertTrue(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:user/some-user")
            .matches());
    assertTrue(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:sts::12345678901234:assumed-role/some-path/some-role")
            .matches());
    assertTrue(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:sts::12345678901234:assumed-role/some-role")
            .matches());
    assertTrue(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:sts::12345678901234:federated-user/my_user")
            .matches());
  }

  @Test
  public void test_IAM_PRINCIPAL_ARN_PATTERN_valid_ARNs_rejected_by_KMS() {
    // invalid - KMS doesn't allow 'group' or 'instance-profile'
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:group/some-group")
            .matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:instance-profile/some-profile")
            .matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:other/some-value")
            .matches());
  }

  @Test
  public void test_IAM_PRINCIPAL_ARN_PATTERN_invalid_ARNs() {
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:some-role")
            .matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam:::role/some-role").matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:::12345678901234:role/some-role")
            .matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher("arn:aws:iam::12345678901234:").matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn::iam::12345678901234:role/some-role")
            .matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher(":aws:iam::12345678901234:role/some-role")
            .matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:other/some-value")
            .matches());
  }

  @Test
  public void test_isArnThatCanGoInKeyPolicy() {
    assertTrue(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:iam::12345678901234:role/some-role"));
    assertTrue(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:iam::12345678901234:role/some/path/some-role"));
    assertTrue(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:iam::12345678901234:user/some-user"));
    assertTrue(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:sts::12345678901234:assumed-role/some-path/some-role"));
    assertTrue(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:sts::12345678901234:assumed-role/some-role"));
    assertTrue(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:sts::12345678901234:federated-user/my_user"));

    // invalid - KMS doesn't allow 'group' or 'instance-profile'
    assertFalse(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:iam::12345678901234:group/some-group"));
    assertFalse(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:iam::12345678901234:instance-profile/some-profile"));
    assertFalse(
        awsGlobalIamRoleArnParser.isArnThatCanGoInKeyPolicy(
            "arn:aws:iam::12345678901234:other/some-value"));
  }

  @Test
  public void test_stripOutDescription() {
    assertEquals(
        "12345678901234/some-role",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:iam::12345678901234:role/some-role"));
    assertEquals(
        "12345678901234/some/path/some-role",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:iam::12345678901234:role/some/path/some-role"));
    assertEquals(
        "12345678901234/some-user",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:iam::12345678901234:user/some-user"));
    assertEquals(
        "12345678901234/some-path/some-role",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:sts::12345678901234:assumed-role/some-path/some-role"));
    assertEquals(
        "12345678901234/some-role",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:sts::12345678901234:assumed-role/some-role"));
    assertEquals(
        "12345678901234/my_user",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:sts::12345678901234:federated-user/my_user"));

    // invalid - KMS doesn't allow 'group' or 'instance-profile' though some parsing is still
    // possible (this behavior isn't important)
    assertEquals(
        "",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:iam::12345678901234:group/some-group"));
    assertEquals(
        "12345678901234/some-value",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:iam::12345678901234:other/some-value"));
    assertEquals(
        "12345678901234/some-profile",
        awsGlobalIamRoleArnParser.stripOutDescription(
            "arn:aws:iam::12345678901234:instance-profile/some-profile"));
  }

  @Test
  public void test_that_an_iam_role_can_not_end_with_whitespace() {
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:role/some-role ")
            .matches());
    assertFalse(
        IAM_PRINCIPAL_ARN_PATTERN_ALLOWED
            .matcher("arn:aws:iam::12345678901234:role/some-role\t")
            .matches());
  }

  @Test
  public void test_isAccountRootArn() {
    assertTrue(awsGlobalIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:root"));

    assertFalse(awsGlobalIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:role/foo"));
    assertFalse(awsGlobalIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:user/bar"));
    assertFalse(
        awsGlobalIamRoleArnParser.isAccountRootArn("arn:aws:sts::0000000000:assumed-role/baz"));
    assertFalse(awsGlobalIamRoleArnParser.isAccountRootArn("arn:aws:iam::0000000000:group/foobar"));
    assertFalse(
        awsGlobalIamRoleArnParser.isAccountRootArn(
            "arn:aws:sts::0000000000:federated-user/foobaz"));
  }

  @Test(expected = RuntimeException.class)
  public void iamPrincipalPartitionCheck_fails_on_disabled_aws_china_partition() {
    awsGlobalIamRoleArnParser.iamPrincipalPartitionCheck(
        "arn:aws-cn:iam::1111111111:role/lamb_dev_health");
  }

  @Test(expected = RuntimeException.class)
  public void iamPrincipalPartitionCheck_fails_on_disabled_aws_global_partition() {
    awsChinaIamRoleArnParser.iamPrincipalPartitionCheck(
        "arn:aws:iam::1111111111:role/lamb_dev_health");
  }
}
