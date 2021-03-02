package com.nike.cerberus.security;

import static com.nike.cerberus.security.WebSecurityConfiguration.HEADER_X_CERBERUS_TOKEN;
import static com.nike.cerberus.security.WebSecurityConfiguration.LEGACY_AUTH_TOKN_HEADER;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import com.nike.cerberus.service.AuthTokenService;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class JwtTokenFilter extends CerberusAuthenticationFilter {

  private final AuthTokenService authTokenService;

  public JwtTokenFilter(RequestMatcher requiresAuthenticationRequestMatcher, AuthTokenService authTokenService) {
    super(requiresAuthenticationRequestMatcher);
    this.authTokenService = authTokenService;
  }

  @Override
  Optional<CerberusPrincipal> extractCerberusPrincipalFromRequest(HttpServletRequest request) {
//    System.out.println("using JWT");
////    System.out.println(request.getHeader(HEADER_X_CERBERUS_TOKEN));
////    return Optional.empty();
    return Optional.ofNullable(request.getHeader(HEADER_X_CERBERUS_TOKEN))
            .or(() -> Optional.ofNullable(request.getHeader(LEGACY_AUTH_TOKN_HEADER)))
            // If the token is present then use the auth service to map it to a Cerberus Principal
            .flatMap(token -> authTokenService.getCerberusAuthToken(token).map(CerberusPrincipal::new));
  }
}
