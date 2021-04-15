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

import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.domain.OauthJwtExchangeRequest;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.service.AuthenticationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v2/auth")
public class OauthJwtAuthenticationController {

  private final AuthenticationService authenticationService;
  private final AuditLoggingFilterDetails auditLoggingFilterDetails;

  @Autowired
  public OauthJwtAuthenticationController(
      AuthenticationService authenticationService,
      AuditLoggingFilterDetails auditLoggingFilterDetails) {
    this.authenticationService = authenticationService;
    this.auditLoggingFilterDetails = auditLoggingFilterDetails;
  }

  @RequestMapping(value = "/user/oauth/exchange", method = POST, consumes = APPLICATION_JSON_VALUE)
  public AuthResponse handleCerberusTokenExchange(@RequestBody OauthJwtExchangeRequest request) {

    Jws<Claims> claimsJws;
    try {
      claimsJws =
          Jwts.parser()
              //                      .requireIssuer("")
              .setSigningKeyResolver(signingKeyResolver)
              .parseClaimsJws(request.getToken());
    } catch (InvalidClaimException e) {
      //      log.warn("Invalid claim when parsing token: {}", token, e);
      //      return Optional.empty();
    } catch (JwtException e) {
      //      log.warn("Error parsing JWT token: {}", token, e);
      //      return Optional.empty();
    } catch (IllegalArgumentException e) {
      //      log.warn("Error parsing JWT token: {}", token, e);
      //      return Optional.empty();
    }
    Claims claims = claimsJws.getBody();
    //    if (blocklist.contains(claims.getId())) {
    //      log.warn("This JWT token is blocklisted. ID: {}", claims.getId());
    //      return Optional.empty();
    //    }
    String email = claims.get("email", String.class);
    AuthResponse authResponse = authenticationService.authenticate(email);

    return authResponse;

    //    return JWT if JWT is enabled
    //    return session token if
  }
}
