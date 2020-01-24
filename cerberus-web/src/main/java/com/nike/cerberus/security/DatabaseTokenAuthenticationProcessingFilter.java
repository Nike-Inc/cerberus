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

import static com.nike.cerberus.error.DefaultApiError.AUTH_TOKEN_INVALID;
import static com.nike.cerberus.security.WebSecurityConfiguration.*;

import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.spring.SpringApiExceptionHandler;
import com.nike.cerberus.service.AuthTokenService;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Slf4j
public class DatabaseTokenAuthenticationProcessingFilter
    extends AbstractAuthenticationProcessingFilter {

  private final AuthTokenService authTokenService;
  private final SpringApiExceptionHandler springApiExceptionHandler;

  public DatabaseTokenAuthenticationProcessingFilter(
      RequestMatcher matcher,
      AuthTokenService authTokenService,
      SpringApiExceptionHandler springApiExceptionHandler) {

    super(matcher);
    this.authTokenService = authTokenService;
    this.springApiExceptionHandler = springApiExceptionHandler;
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    return Optional.ofNullable(request.getHeader(HEADER_X_CERBERUS_TOKEN))
        .or(() -> Optional.ofNullable(request.getHeader(LEGACY_AUTH_TOKN_HEADER)))
        .flatMap(token -> authTokenService.getCerberusAuthToken(token).map(CerberusPrincipal::new))
        .orElseThrow(() -> new BadCredentialsException(AUTH_TOKEN_INVALID.getMessage()));
  }

  @Override
  protected void successfulAuthentication(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain,
      Authentication authResult)
      throws IOException, ServletException {

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    authResult.setAuthenticated(true);
    context.setAuthentication(authResult);
    SecurityContextHolder.setContext(context);
    chain.doFilter(request, response);
  }

  @Override
  protected void unsuccessfulAuthentication(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {

    SecurityContextHolder.clearContext();

    var nullableResponse =
        springApiExceptionHandler.resolveException(
            request, response, null, new ApiException(AUTH_TOKEN_INVALID));

    Optional.ofNullable(nullableResponse)
        .flatMap(res -> Optional.ofNullable(res.getView()))
        .ifPresent(
            view -> {
              try {
                view.render(nullableResponse.getModel(), request, response);
              } catch (Exception e) {
                throw new RuntimeException("Failed to render Backstopper error");
              }
            });
  }
}
