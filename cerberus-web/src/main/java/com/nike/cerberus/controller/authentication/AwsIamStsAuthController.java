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

import static org.springframework.web.bind.annotation.RequestMethod.*;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.sts.AwsStsClient;
import com.nike.cerberus.aws.sts.AwsStsHttpHeader;
import com.nike.cerberus.aws.sts.GetCallerIdentityResponse;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.service.AuthenticationService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v2/auth/sts-identity")
public class AwsIamStsAuthController {

  private static final String HEADER_X_AMZ_DATE = "x-amz-date";
  private static final String HEADER_X_AMZ_SECURITY_TOKEN = "x-amz-security-token";
  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final Integer MAX_RETRIES = 5;
  private Integer waitTime = 30;

  private final AuthenticationService authenticationService;
  private final AwsStsClient awsStsClient;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public AwsIamStsAuthController(
      AuthenticationService authenticationService,
      AwsStsClient awsStsClient,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {

    this.authenticationService = authenticationService;
    this.awsStsClient = awsStsClient;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  /**
   * Sets the wait time method attribute to allow customization during unit testing of sleep
   *
   * @param waitTime How long to sleep in seconds
   */
  protected void setWaitTime(Integer newWaitTime) {
    waitTime = newWaitTime;
  }

  @RequestMapping(method = POST)
  public AuthTokenResponse authenticate(
      @RequestHeader(value = HEADER_X_AMZ_DATE, required = false)
          String headerXAmzDate, // TODO should we make this required = true?
      @RequestHeader(value = HEADER_X_AMZ_SECURITY_TOKEN, required = false)
          String headerXAmzSecurityToken, // TODO should we make this required = true?
      @RequestHeader(value = HEADER_AUTHORIZATION, required = false) String headerAuthorization) {

    String iamPrincipalArn;
    AuthTokenResponse authResponse;

    for (int count = 0; ; count++) {
      try {
        try {
          int sleepTime = waitTime * count;
          TimeUnit.SECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
          log.info(e.getMessage());
        }

        if (headerAuthorization == null || headerXAmzDate == null) {
          throw new ApiException(DefaultApiError.MISSING_AWS_SIGNATURE_HEADERS);
        }
        AwsStsHttpHeader header =
            new AwsStsHttpHeader(headerXAmzDate, headerXAmzSecurityToken, headerAuthorization);

        GetCallerIdentityResponse getCallerIdentityResponse =
            awsStsClient.getCallerIdentity(header);
        iamPrincipalArn = getCallerIdentityResponse.getGetCallerIdentityResult().getArn();

        authResponse = authenticationService.stsAuthenticate(iamPrincipalArn);
        auditLoggingFilterDetails.setAction("Successfully authenticated with AWS IAM STS Auth");

        return authResponse;
      } catch (Exception e) {
        String auditMessage =
            String.format("Failed to authenticate with AWS IAM STS Auth: %s", e.getMessage());
        auditLoggingFilterDetails.setAction(auditMessage);
        if (count >= MAX_RETRIES) {
          throw e;
        }
      }
    }
  }
}
