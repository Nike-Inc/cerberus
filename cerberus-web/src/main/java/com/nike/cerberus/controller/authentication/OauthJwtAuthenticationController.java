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
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.domain.OauthJwtExchangeRequest;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.jwt.OauthJwksKeyResolver;
import com.nike.cerberus.service.AuthenticationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v2/auth/user/oauth")
public class OauthJwtAuthenticationController {

  private final AuthenticationService authenticationService;
  private OauthJwksKeyResolver oauthJwksKeyResolver;

  @Autowired
  public OauthJwtAuthenticationController(
      AuthenticationService authenticationService, OauthJwksKeyResolver oauthJwksKeyResolver) {
    this.oauthJwksKeyResolver = oauthJwksKeyResolver;
    this.authenticationService = authenticationService;
  }

  @RequestMapping(value = "/exchange", method = POST, consumes = APPLICATION_JSON_VALUE)
  public AuthResponse handleCerberusTokenExchange(@RequestBody OauthJwtExchangeRequest request) {
    final String email;
    try {
      Jws<Claims> claimsJws =
          Jwts.parser()
              .setSigningKeyResolver(oauthJwksKeyResolver)
              .parseClaimsJws(request.getToken());
      email = claimsJws.getBody().get("email", String.class);
    } catch (Exception e) {
      throw ApiException.newBuilder().withApiErrors(DefaultApiError.OAUTH_JWT_INVALID).build();
    }
    if (StringUtils.isBlank(email)) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.OAUTH_JWT_EMAIL_INVALID)
          .build();
    }
    return authenticationService.authenticate(email);
  }
}
