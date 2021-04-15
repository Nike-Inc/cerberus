package com.nike.cerberus.security;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;

import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.service.AuthTokenService;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class DatabaseTokenAuthenticationProcessingFilterTest {

  static final String HEADER_X_CERBERUS_TOKEN = "X-Cerberus-Token";
  static final String LEGACY_AUTH_TOKN_HEADER = "X-Vault-Token";

  @InjectMocks
  private DatabaseTokenAuthenticationProcessingFilter databaseTokenAuthenticationProcessingFilter;

  @Mock private AuthTokenService authTokenService;
  @Mock private RequestMatcher requiresAuthenticationRequestMatcher;
  private HttpServletRequest request;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    request = mock(HttpServletRequest.class);
    databaseTokenAuthenticationProcessingFilter =
        new DatabaseTokenAuthenticationProcessingFilter(
            authTokenService, requiresAuthenticationRequestMatcher);
  }

  @Test
  public void testExtractCerberusPrincipalFromRequest() {
    CerberusAuthToken cerberusAuthToken1 = new CerberusAuthToken();
    cerberusAuthToken1.setPrincipal("principal");
    Optional<CerberusAuthToken> cerberusAuthToken = Optional.of(cerberusAuthToken1);
    Mockito.when(authTokenService.getCerberusAuthToken(anyString())).thenReturn(cerberusAuthToken);
    Mockito.when(request.getHeader(HEADER_X_CERBERUS_TOKEN)).thenReturn("token");
    assertNotNull(
        databaseTokenAuthenticationProcessingFilter.extractCerberusPrincipalFromRequest(request));
  }

  @Test
  public void testExtractCerberusPrincipalFromRequestWithAuthToken() {
    CerberusAuthToken cerberusAuthToken1 = new CerberusAuthToken();
    cerberusAuthToken1.setPrincipal("principal");
    Optional<CerberusAuthToken> cerberusAuthToken = Optional.of(cerberusAuthToken1);
    Mockito.when(authTokenService.getCerberusAuthToken(anyString())).thenReturn(cerberusAuthToken);
    Mockito.when(request.getHeader(LEGACY_AUTH_TOKN_HEADER)).thenReturn("token");
    assertNotNull(
        databaseTokenAuthenticationProcessingFilter.extractCerberusPrincipalFromRequest(request));
  }
}
