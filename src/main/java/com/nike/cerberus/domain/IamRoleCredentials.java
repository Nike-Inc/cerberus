/*
 * Copyright (c) 2016 Nike, Inc.
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
 */

package com.nike.cerberus.domain;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.Pattern;

import static com.nike.cerberus.domain.IamRoleRegex.IAM_ROLE_ACCT_ID_REGEX;
import static com.nike.cerberus.domain.IamRoleRegex.IAM_ROLE_NAME_REGEX;

/**
 * Represents the IAM role credentials sent during authentication.
 */
public class IamRoleCredentials {

    @Pattern(regexp = IAM_ROLE_ACCT_ID_REGEX, message = "IAM_ROLE_ACCT_ID_INVALID")
    private String accountId;

    @Pattern(regexp = IAM_ROLE_NAME_REGEX, message = "AUTH_IAM_ROLE_NAME_INVALID")
    private String roleName;

    @NotBlank(message = "AUTH_IAM_ROLE_AWS_REGION_BLANK")
    private String region;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
