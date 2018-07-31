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

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.InvalidIamRoleArnApiError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for concatenating and parsing AWS IAM role ARNs.
 */
public class AwsIamRoleArnParser {

    public static final String AWS_IAM_ROLE_ARN_TEMPLATE = "arn:aws:iam::%s:role/%s";

    /**
     * Pattern used to determine if an ARN should be allowed in DB.
     *
     * This is also the list of ARN types that are allowed in KMS key policies.
     */
    public static final String AWS_IAM_PRINCIPAL_ARN_REGEX_ALLOWED = "^arn:aws:(iam|sts)::(?<accountId>\\d+?):(role|user|federated-user|assumed-role).*/.+(?<!\\s)$";

    /**
     * Pattern used for generating a role from another ARN.
     *
     * Backwards compatible to allow generating a role name from an instance-profile ARN which only work for certain
     * instance-profile ARNs.  Going forward we don't allow instance-profile ARNs because they can't go in KMS key
     * policies.
     */
    public static final String AWS_IAM_PRINCIPAL_ARN_REGEX_ROLE_GENERATION = "^arn:aws:(iam|sts)::(?<accountId>\\d+?):(?!group).+?/(?<roleName>.+)$";

    private static final String AWS_IAM_ROLE_ARN_REGEX = "^arn:aws:iam::(?<accountId>\\d+?):role/(?<roleName>.+)$";

    private static final String AWS_IAM_ASSUMED_ROLE_ARN_REGEX = "^arn:aws:sts::(?<accountId>\\d+?):assumed-role/(?<roleName>.+)/.+$";

    private static final String GENERIC_ASSUMED_ROLE_REGEX = "^arn:aws:sts::(?<accountId>\\d+?):assumed-role/.+$";

    public static final String AWS_ACCOUNT_ROOT_ARN_REGEX = "^arn:aws:iam::(?<accountId>\\d+?):root$";

    /**
     * Pattern used to determine if an ARN should be allowed in DB
     *
     * This is also the list of ARN types that are allowed in KMS key policies.
     */
    public static final Pattern IAM_PRINCIPAL_ARN_PATTERN_ALLOWED = Pattern.compile(AWS_IAM_PRINCIPAL_ARN_REGEX_ALLOWED);

    /**
     * Pattern used for generating a role from another ARN.
     *
     * Backwards compatible to allow generating a role name from an instance-profile ARN which only work for certain
     * instance-profile ARNs.  Going forward we don't allow instance-profile ARNs because they can't go in KMS key
     * policies.
     */
    private static final Pattern IAM_PRINCIPAL_ARN_PATTERN_ROLE_GENERATION = Pattern.compile(AWS_IAM_PRINCIPAL_ARN_REGEX_ROLE_GENERATION);

    private static final Pattern IAM_ROLE_ARN_PATTERN = Pattern.compile(AWS_IAM_ROLE_ARN_REGEX);

    private static final Pattern IAM_ASSUMED_ROLE_ARN_PATTERN = Pattern.compile(AWS_IAM_ASSUMED_ROLE_ARN_REGEX);

    private static final Pattern GENERIC_ASSUMED_ROLE_PATTERN = Pattern.compile(GENERIC_ASSUMED_ROLE_REGEX);

    private static final Pattern AWS_ACCOUNT_ROOT_ARN_PATTERN = Pattern.compile(AWS_ACCOUNT_ROOT_ARN_REGEX);

    private static final Pattern AWS_IAM_ARN_ACCOUNT_ID_PATTERN = Pattern.compile("arn:aws:(iam|sts)::(?<accountId>\\d+?):.+");

    /**
     * Gets account ID from a 'role' ARN
     * @param roleArn - Role ARN to parse
     * @return - Account ID
     */
    public String getAccountId(final String roleArn) {

        return getNamedGroupFromRegexPattern(IAM_ROLE_ARN_PATTERN, "accountId", roleArn);
    }

    /**
     * Gets role name form a 'role' ARN
     * @param roleArn - Role ARN to parse
     * @return
     */
    public String getRoleName(final String roleArn) {

        return getNamedGroupFromRegexPattern(IAM_ROLE_ARN_PATTERN, "roleName", roleArn);

    }

    /**
     * Returns true if the ARN is in format 'arn:aws:iam::000000000:role/example' and false if not
     * @param arn - ARN to test
     * @return - True if is 'role' ARN, False if not
     */
    public boolean isRoleArn(final String arn) {

        final Matcher iamRoleArnMatcher = IAM_ROLE_ARN_PATTERN.matcher(arn);

        return iamRoleArnMatcher.find();
    }

    /**
     * Returns true if the ARN is in format 'arn:aws:iam::000000000:root' and false if not
     * @param arn - ARN to test
     * @return - True if is 'role' ARN, False if not
     */
    public boolean isAccountRootArn(final String arn) {

        final Matcher accountRootArnMatcher = AWS_ACCOUNT_ROOT_ARN_PATTERN.matcher(arn);

        return accountRootArnMatcher.find();
    }

    public boolean isArnThatCanGoInKeyPolicy(final String arn) {
        final Matcher arnMatcher = IAM_PRINCIPAL_ARN_PATTERN_ALLOWED.matcher(arn);
        final Matcher rootArnMatcher = AWS_ACCOUNT_ROOT_ARN_PATTERN.matcher(arn);

        return arnMatcher.find() || rootArnMatcher.find();
    }

    /**
     * Converts a principal ARN (e.g. 'arn:aws:iam::0000000000:instance-profile/example') to a role ARN,
     * (i.e. 'arn:aws:iam::000000000:role/example')
     * @param principalArn - Principal ARN to convert
     * @return - Role ARN
     */
    public String convertPrincipalArnToRoleArn(final String principalArn) {

        if (isRoleArn(principalArn)) {
            return principalArn;
        }

        final boolean isAssumedRole = GENERIC_ASSUMED_ROLE_PATTERN.matcher(principalArn).find();
        final Pattern patternToMatch = isAssumedRole ? IAM_ASSUMED_ROLE_ARN_PATTERN : IAM_PRINCIPAL_ARN_PATTERN_ROLE_GENERATION;

        final String accountId = getNamedGroupFromRegexPattern(patternToMatch, "accountId", principalArn);
        final String roleName = getNamedGroupFromRegexPattern(patternToMatch, "roleName", principalArn);

        return String.format(AWS_IAM_ROLE_ARN_TEMPLATE, accountId, roleName);
    }

    public String convertPrincipalArnToRootArn(final String principalArn) {

        if (isAccountRootArn(principalArn)) {
            return principalArn;
        }

        final String accountId = getNamedGroupFromRegexPattern(IAM_PRINCIPAL_ARN_PATTERN_ALLOWED, "accountId", principalArn);

        return String.format("arn:aws:iam::%s:root", accountId);
    }

    /**
     * Strip out a description from the supplied ARN, e.g. "123456789/role-name" or the empty string
     */
    public String stripOutDescription(final String principalArn) {
       Matcher matcher = IAM_PRINCIPAL_ARN_PATTERN_ROLE_GENERATION.matcher(principalArn);
        if (matcher.find()) {
            return matcher.group("accountId") + "/" + matcher.group("roleName");
        }
        else {
            // never return null since this is just for descriptive purposes
            return "";
        }
    }

    private String getNamedGroupFromRegexPattern(final Pattern pattern, final String groupName, final String input) {
        final Matcher iamRoleArnMatcher = pattern.matcher(input);

        if (! iamRoleArnMatcher.find()) {
            throw ApiException.newBuilder()
                    .withApiErrors(new InvalidIamRoleArnApiError(input))
                    .withExceptionMessage("ARN does not match pattern: " + pattern.toString())
                    .build();
        }

        return iamRoleArnMatcher.group(groupName);
    }
}
