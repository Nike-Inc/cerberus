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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.domain.MfaCheckRequest;
import com.nike.cerberus.domain.UserCredentials;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.AuthenticationService;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v2/auth")
public class UserAuthenticationController {

  private final AuthenticationService authenticationService;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public UserAuthenticationController(
      AuthenticationService authenticationService,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {
    this.authenticationService = authenticationService;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @RequestMapping(value = "/user", method = GET)
  public AuthResponse authenticate(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {
    final UserCredentials credentials = extractCredentials(authHeader);

    AuthResponse authResponse;
    try {
      authResponse = authenticationService.authenticate(credentials);
    } catch (ApiException e) {
      auditLoggingFilterDetails.setAction("Failed to authenticate");
      throw e;
    }

    auditLoggingFilterDetails.setAction("Authenticated");

    return authResponse;
  }

  @RequestMapping(value = "/mfa_check", method = POST, consumes = APPLICATION_JSON_VALUE)
  public AuthResponse handleMfaCheck(@RequestBody MfaCheckRequest request) {
    if (StringUtils.isBlank(request.getOtpToken())) {
      return authenticationService.triggerChallenge(request);
    } else {
      return authenticationService.mfaCheck(request);
    }
  }

  @RequestMapping(value = "/user/refresh", method = GET)
  public AuthResponse refreshToken(Authentication authentication) {
    var cerberusPrincipal = (CerberusPrincipal) authentication;
    return authenticationService.refreshUserToken(cerberusPrincipal);
  }

  /**
   * Extracts credentials from the Authorization header. Assumes its Basic auth.
   *
   * @param authorizationHeader Value from the authorization header
   * @return User credentials that were extracted
   */
  private UserCredentials extractCredentials(final String authorizationHeader) {
    final String authType = "Basic";
    if (authorizationHeader != null && authorizationHeader.startsWith(authType)) {
      final String encodedCredentials = authorizationHeader.substring(authType.length()).trim();
      final byte[] decodedCredentials = Base64.decodeBase64(encodedCredentials);

      if (ArrayUtils.isNotEmpty(decodedCredentials)) {
        final String[] credentials =
            new String(decodedCredentials, Charset.defaultCharset()).split(":", 2);

        if (credentials.length == 2) {
          return new UserCredentials(
              credentials[0], credentials[1].getBytes(Charset.defaultCharset()));
        }
      }
    }

    throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
  }
}
