package com.nike.cerberus.security;

import static com.nike.cerberus.security.WebSecurityConfiguration.HEADER_X_CERBERUS_TOKEN;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class JwtTokenFilter extends CerberusAuthenticationFilter {

  public JwtTokenFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
    super(requiresAuthenticationRequestMatcher);
  }

  @Override
  Optional<CerberusPrincipal> extractCerberusPrincipalFromRequest(HttpServletRequest request) {
    System.out.println("using JWT");
    System.out.println(request.getHeader(HEADER_X_CERBERUS_TOKEN));
    return Optional.empty();
  }
}
