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

package com.nike.cerberus.domain;

import java.util.regex.Pattern;

public class DomainConstants {

  public static final String AWS_IAM_ROLE_ARN_TEMPLATE = "arn:aws:iam::%s:role/%s";

  /**
   * Pattern used to determine if an ARN should be allowed in DB.
   *
   * <p>This is also the list of ARN types that are allowed in KMS key policies.
   */
  public static final String AWS_IAM_PRINCIPAL_ARN_REGEX_ALLOWED =
      "^arn:(aws|aws-cn):(iam|sts)::(?<accountId>\\d+?):(role|user|federated-user|assumed-role).*/.+(?<!\\s)$";

  /**
   * Pattern used to determine if an ARN should be allowed in DB
   *
   * <p>This is also the list of ARN types that are allowed in KMS key policies.
   */
  public static final Pattern IAM_PRINCIPAL_ARN_PATTERN_ALLOWED =
      Pattern.compile(AWS_IAM_PRINCIPAL_ARN_REGEX_ALLOWED);

  /**
   * Pattern used for generating a role from another ARN.
   *
   * <p>Backwards compatible to allow generating a role name from an instance-profile ARN which only
   * work for certain instance-profile ARNs. Going forward we don't allow instance-profile ARNs
   * because they can't go in KMS key policies.
   */
  public static final String AWS_IAM_PRINCIPAL_ARN_REGEX_ROLE_GENERATION =
      "^arn:(aws|aws-cn):(iam|sts)::(?<accountId>\\d+?):(?!group).+?/(?<roleName>.+)$";

  /**
   * Pattern used for generating a role from another ARN.
   *
   * <p>Backwards compatible to allow generating a role name from an instance-profile ARN which only
   * work for certain instance-profile ARNs. Going forward we don't allow instance-profile ARNs
   * because they can't go in KMS key policies.
   */
  public static final Pattern IAM_PRINCIPAL_ARN_PATTERN_ROLE_GENERATION =
      Pattern.compile(AWS_IAM_PRINCIPAL_ARN_REGEX_ROLE_GENERATION);

  public static final String AWS_ACCOUNT_ROOT_ARN_REGEX =
      "^arn:(aws|aws-cn):iam::(?<accountId>\\d+?):root$";
  public static final Pattern AWS_ACCOUNT_ROOT_ARN_PATTERN =
      Pattern.compile(AWS_ACCOUNT_ROOT_ARN_REGEX);
  private static final String AWS_IAM_ROLE_ARN_REGEX =
      "^arn:(aws|aws-cn):iam::(?<accountId>\\d+?):role/(?<roleName>.+)$";
  public static final Pattern IAM_ROLE_ARN_PATTERN = Pattern.compile(AWS_IAM_ROLE_ARN_REGEX);
  private static final String AWS_IAM_ASSUMED_ROLE_ARN_REGEX =
      "^arn:(aws|aws-cn):sts::(?<accountId>\\d+?):assumed-role/(?<roleName>.+)/.+$";
  public static final Pattern IAM_ASSUMED_ROLE_ARN_PATTERN =
      Pattern.compile(AWS_IAM_ASSUMED_ROLE_ARN_REGEX);
  private static final String GENERIC_ASSUMED_ROLE_REGEX =
      "^arn:(aws|aws-cn):sts::(?<accountId>\\d+?):assumed-role/.+$";
  public static final Pattern GENERIC_ASSUMED_ROLE_PATTERN =
      Pattern.compile(GENERIC_ASSUMED_ROLE_REGEX);
  private static final Pattern AWS_IAM_ARN_ACCOUNT_ID_PATTERN =
      Pattern.compile("arn:(aws|aws-cn):(iam|sts)::(?<accountId>\\d+?):.+");
}
