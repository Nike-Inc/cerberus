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

package com.nike.cerberus.controller.authentication;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.AwsIamKmsAuthRequest;
import com.nike.cerberus.domain.EncryptedAuthDataWrapper;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.service.AuthenticationService;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/v2/auth/iam-principal")
public class AwsIamKmsAuthV2Controller {

  private final AuthenticationService authenticationService;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public AwsIamKmsAuthV2Controller(
      AuthenticationService authenticationService,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.authenticationService = authenticationService;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @RequestMapping(method = POST)
  public EncryptedAuthDataWrapper authenticate(@RequestBody AwsIamKmsAuthRequest request) {
    EncryptedAuthDataWrapper authResponse;

    try {
      authResponse = authenticationService.authenticate(request);
    } catch (ApiException e) {
      auditLoggingFilterDetails.setAction(
          String.format(
              "Failed to authenticate in region %s, for reason: %s",
              request.getRegion(),
              e.getApiErrors().stream()
                  .map(ApiError::getMessage)
                  .collect(Collectors.joining(","))));
      throw e;
    }
    auditLoggingFilterDetails.setAction(
        String.format("Successfully authenticated in region %s", request.getRegion()));

    return authResponse;
  }
}
