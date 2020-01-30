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

package com.nike.cerberus.security;

import static com.nike.cerberus.security.WebSecurityConfiguration.HEADER_X_CERBERUS_TOKEN;
import static com.nike.cerberus.security.WebSecurityConfiguration.LEGACY_AUTH_TOKN_HEADER;

import com.nike.cerberus.service.AuthTokenService;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Slf4j
public class DatabaseTokenAuthenticationProcessingFilter extends CerberusAuthenticationFilter {

  private final AuthTokenService authTokenService;

  public DatabaseTokenAuthenticationProcessingFilter(
      AuthTokenService authTokenService, RequestMatcher requiresAuthenticationRequestMatcher) {
    super(requiresAuthenticationRequestMatcher);
    this.authTokenService = authTokenService;
  }

  /**
   * If the token header is present and valid, this filter extracts it and retrieves its data from
   * the db and converts it into a Cerberus Principal.
   *
   * @param request The request that might have the token headers.
   * @return If the principal is present that means we where able to turn the X-Cerberus-Token
   *     header token value into a * valid cerberus principal by retrieving the token from the data
   *     store
   */
  @Override
  Optional<CerberusPrincipal> extractCerberusPrincipalFromRequest(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader(HEADER_X_CERBERUS_TOKEN))
        .or(() -> Optional.ofNullable(request.getHeader(LEGACY_AUTH_TOKN_HEADER)))
        // If the token is present then use the auth service to map it to a Cerberus Principal
        .flatMap(token -> authTokenService.getCerberusAuthToken(token).map(CerberusPrincipal::new));
  }
}
