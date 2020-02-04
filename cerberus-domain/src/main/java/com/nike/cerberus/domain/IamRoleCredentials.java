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

import static com.nike.cerberus.domain.IamRoleRegex.IAM_ROLE_ACCT_ID_REGEX;
import static com.nike.cerberus.domain.IamRoleRegex.IAM_ROLE_NAME_REGEX;

import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

/** Represents the IAM role credentials sent during authentication. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IamRoleCredentials {

  @Pattern(regexp = IAM_ROLE_ACCT_ID_REGEX, message = "IAM_ROLE_ACCT_ID_INVALID")
  private String accountId;

  @Pattern(regexp = IAM_ROLE_NAME_REGEX, message = "AUTH_IAM_ROLE_NAME_INVALID")
  private String roleName;

  @NotBlank(message = "AUTH_IAM_ROLE_AWS_REGION_BLANK")
  private String region;
}
