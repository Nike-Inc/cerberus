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

import static com.nike.cerberus.domain.DomainConstants.AWS_IAM_PRINCIPAL_ARN_REGEX_ROLE_GENERATION;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

/** Represents the IAM principal credentials sent during authentication. */
@Data
@Builder
public class AwsIamKmsAuthRequest {

  @Pattern(
      regexp = AWS_IAM_PRINCIPAL_ARN_REGEX_ROLE_GENERATION,
      message = "AUTH_IAM_PRINCIPAL_INVALID")
  private String iamPrincipalArn;

  @NotBlank(message = "AUTH_IAM_PRINCIPAL_AWS_REGION_BLANK")
  private String region;
}
