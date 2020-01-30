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

import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

public abstract class CerberusAuthenticationFilter extends OncePerRequestFilter {

  private final RequestMatcher requiresAuthenticationRequestMatcher;

  public CerberusAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher) {

    Assert.notNull(
        requiresAuthenticationRequestMatcher,
        "requiresAuthenticationRequestMatcher cannot be null");
    this.requiresAuthenticationRequestMatcher = requiresAuthenticationRequestMatcher;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // Don't bother doing authentication logic if the endpoint requested doesn't require
    // authentication
    if (requiresAuthentication(request)) {
      var securityContext = SecurityContextHolder.getContext();
      boolean isPrincipalAlreadyAuthenticated =
          Optional.ofNullable(securityContext.getAuthentication())
              .map(Authentication::isAuthenticated)
              .orElse(false);
      // Don't bother doing authentication logic if the security context says the principal is
      // already authenticated
      if (!isPrincipalAlreadyAuthenticated) {
        Optional<CerberusPrincipal> cerberusPrincipalOptional =
            extractCerberusPrincipalFromRequest(request);

        cerberusPrincipalOptional.ifPresent(
            cerberusPrincipal -> {
              cerberusPrincipal.setAuthenticated(true);
              securityContext.setAuthentication(cerberusPrincipal);
              SecurityContextHolder.setContext(securityContext);
            });
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Indicates whether this filter should attempt to process authentication for the current
   * invocation.
   *
   * @return <code>true</code> if the filter should attempt authentication, <code>false</code>
   *     otherwise.
   */
  protected boolean requiresAuthentication(HttpServletRequest request) {
    return requiresAuthenticationRequestMatcher.matches(request);
  }

  /**
   * If this method returns a optional that contains a Cerberus Principal it is assumed that the
   * Principal is authenticated.
   *
   * @param request The request that might have the token headers.
   * @return empty if the request is not authenticated and return a Cerberus Principal if the
   *     request is authenticated.
   */
  abstract Optional<CerberusPrincipal> extractCerberusPrincipalFromRequest(
      HttpServletRequest request);
}
