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

package com.nike.cerberus.domain;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;

import static com.nike.cerberus.util.AwsIamRoleArnParser.AWS_IAM_ROLE_ARN_REGEX;

/**
 * Represents the IAM role credentials sent during authentication.
 */
public class IamRoleCredentialsV2 {

    @Pattern(regexp = AWS_IAM_ROLE_ARN_REGEX, message = "AUTH_IAM_ROLE_NAME_INVALID")
    private String iamPrincipalArn;

    @NotBlank(message = "AUTH_IAM_ROLE_AWS_REGION_BLANK")
    private String region;

    public String getIamPrincipalArn() {
        return iamPrincipalArn;
    }

    public void setIamPrincipalArn(String iamPrincipalArn) {
        this.iamPrincipalArn = iamPrincipalArn;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
