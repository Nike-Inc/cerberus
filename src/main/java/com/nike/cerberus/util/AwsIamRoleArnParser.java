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
// TODO: remove
public class AwsIamRoleArnParser {

    public static final String AWS_IAM_ROLE_ARN_TEMPLATE = "arn:aws:iam::%s:role/%s";

    public static final String AWS_IAM_ROLE_ARN_REGEX = "^arn:aws:iam::(?<accountId>\\d+?):role/(?<roleName>.+)$";

    private static final Pattern IAM_ROLE_ARN_PATTERN = Pattern.compile(AWS_IAM_ROLE_ARN_REGEX);

    public String getAccountId(String roleArn) {

        Matcher iamRoleArnMatcher = IAM_ROLE_ARN_PATTERN.matcher(roleArn);

        if (! iamRoleArnMatcher.find()) {
            throw ApiException.newBuilder()
                    .withApiErrors(new InvalidIamRoleArnApiError(roleArn))
                    .build();
        }

        return iamRoleArnMatcher.group("accountId");
    }

    public String getRoleName(String roleArn) {

        Matcher iamRoleArnMatcher = IAM_ROLE_ARN_PATTERN.matcher(roleArn);

        if (! iamRoleArnMatcher.find()) {
            throw ApiException.newBuilder()
                    .withApiErrors(new InvalidIamRoleArnApiError(roleArn))
                    .build();
        }

        return iamRoleArnMatcher.group("roleName");
    }
}
