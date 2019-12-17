package com.nike.cerberus.security;

import com.nike.backstopper.handler.spring.SpringApiExceptionHandler;
import com.nike.cerberus.service.AuthTokenService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

  static final String HEADER_X_CERBERUS_TOKEN = "X-Cerberus-Token";
  static final String LEGACY_AUTH_TOKN_HEADER = "X-Vault-Token";

  private static final List<String> AUTHENTICATION_NOT_REQUIRED_WHITELIST =
      List.of(
          "/",
          "/info",
          "/dashboard",
          "/dashboard/**",
          "/healthcheck",
          "/v2/auth/sts-identity",
          "/v2/auth/iam-principal",
          "/v1/auth/iam-role",
          "/v2/auth/user",
          "/v2/auth/mfa_check");

  @Autowired private AuthTokenService authTokenService;

  @Autowired
  private CerberusPrincipalAuthenticationProvider cerberusPrincipalAuthenticationProvider;

  @Autowired private SpringApiExceptionHandler springApiExceptionHandler;

  RequestMatcher getDoesRequestsRequireAuthMatcher() {

    List<RequestMatcher> whiteListMatchers =
        AUTHENTICATION_NOT_REQUIRED_WHITELIST.stream()
            .map(AntPathRequestMatcher::new)
            .collect(Collectors.toList());
    var whiteListMatcher = new OrRequestMatcher(whiteListMatchers);
    return request -> !whiteListMatcher.matches(request);
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) {
    auth.authenticationProvider(cerberusPrincipalAuthenticationProvider);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    var requestDoesNotRequireAuthMatcher = getDoesRequestsRequireAuthMatcher();
    var dbTokenFilter =
        new DatabaseTokenAuthenticationProcessingFilter(
            requestDoesNotRequireAuthMatcher, authTokenService, springApiExceptionHandler);

    // Disable CSRF (cross site request forgery)
    http.csrf().disable();

    // No session will be created or used by spring security
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    // Entry points
    http.authorizeRequests()
        .antMatchers(AUTHENTICATION_NOT_REQUIRED_WHITELIST.toArray(new String[0]))
        .permitAll()
        .and()
        .authorizeRequests()
        .anyRequest()
        .authenticated()
        .and()
        .addFilterBefore(dbTokenFilter, UsernamePasswordAuthenticationFilter.class);

    http.exceptionHandling()
        .accessDeniedHandler(
            (request, response, accessDeniedException) -> {
              log.info("I am here");
            });
  }
}
