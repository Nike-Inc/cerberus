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

import com.nike.cerberus.service.AuthTokenService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

  static final String HEADER_X_CERBERUS_TOKEN = "X-Cerberus-Token";
  static final String LEGACY_AUTH_TOKN_HEADER = "X-Vault-Token";

  @Value("${cerberus.cors.allowedOriginPattern:#{null}}")
  private String allowedOriginPattern;

  private static final List<String> AUTHENTICATION_NOT_REQUIRED_WHITELIST =
      List.of(
          "/",
          "/info",
          "/dashboard",
          "/dashboard/**",
          "/healthcheck",
          "/v2/auth/exchange",
          "/v2/auth/sts-identity",
          "/v2/auth/iam-principal",
          "/v1/auth/iam-role",
          "/v2/auth/user",
          "/v2/auth/mfa_check");

  @Autowired private AuthTokenService authTokenService;

  @Autowired
  private CerberusPrincipalAuthenticationProvider cerberusPrincipalAuthenticationProvider;

  @Autowired private RequestWasNotAuthenticatedEntryPoint requestWasNotAuthenticatedEntryPoint;

  @Autowired HttpFirewall allowUrlEncodedSlashHttpFirewall;

  RequestMatcher getDoesRequestsRequireAuthMatcher() {

    List<RequestMatcher> whiteListMatchers =
        AUTHENTICATION_NOT_REQUIRED_WHITELIST.stream()
            .map(AntPathRequestMatcher::new)
            .collect(Collectors.toList());
    var whiteListMatcher = new OrRequestMatcher(whiteListMatchers);
    return request -> !whiteListMatcher.matches(request);
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    super.configure(web);

    // Allow double / in URIs to support buggy Cerberus clients that worked with Highland arch.
    web.httpFirewall(allowUrlEncodedSlashHttpFirewall);
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) {
    auth.authenticationProvider(cerberusPrincipalAuthenticationProvider);
  }

  @Override
  @SuppressFBWarnings(value = "SPRING_CSRF_PROTECTION_DISABLED")
  protected void configure(HttpSecurity http) throws Exception {
    var requestDoesNotRequireAuthMatcher = getDoesRequestsRequireAuthMatcher();
    var dbTokenFilter =
        new DatabaseTokenAuthenticationProcessingFilter(
            authTokenService, requestDoesNotRequireAuthMatcher);

    var jwtFilter = new JwtTokenFilter(requestDoesNotRequireAuthMatcher, authTokenService);

    // Disable CSRF (cross site request forgery)
    http.csrf().disable();

    // No session will be created or used by spring security
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    // Allow requests from the white list to be unauthenticated
    http.authorizeRequests()
        .antMatchers(AUTHENTICATION_NOT_REQUIRED_WHITELIST.toArray(new String[0]))
        .permitAll();

    // Force all other requests to be authenticated
    http.authorizeRequests().anyRequest().authenticated();

    // Add our authentication entry point
    http.exceptionHandling().authenticationEntryPoint(requestWasNotAuthenticatedEntryPoint);

    // Add the auth filters
    http.addFilterBefore(dbTokenFilter, UsernamePasswordAuthenticationFilter.class);

    http.addFilterBefore(jwtFilter, dbTokenFilter.getClass());
  }

  @Bean
  CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowCredentials(false);
    if (null == allowedOriginPattern) {
      config.addAllowedOriginPattern(CorsConfiguration.ALL);
    } else {
      config.addAllowedOriginPattern(allowedOriginPattern);
    }
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/v?/**", config);
    return new CorsFilter(source);
  }
}
