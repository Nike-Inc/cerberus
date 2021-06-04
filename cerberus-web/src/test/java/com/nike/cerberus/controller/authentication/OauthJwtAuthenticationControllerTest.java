package com.nike.cerberus.controller.authentication;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.domain.OauthJwtExchangeRequest;
import com.nike.cerberus.jwt.OauthJwksKeyResolver;
import com.nike.cerberus.service.AuthenticationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class OauthJwtAuthenticationControllerTest {
  @Mock private AuthenticationService authenticationService;
  @Mock OauthJwksKeyResolver oauthJwksKeyResolver;
  @InjectMocks private OauthJwtAuthenticationController oauthJwtAuthenticationController;
  private SecretKeySpec key = new SecretKeySpec(new byte[64], "HmacSHA512");
  private SecretKeySpec invalidKey =
      new SecretKeySpec(
          Base64.getDecoder()
              .decode(
                  "badkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkeybadkey"),
          "HmacSHA512");
  private AuthResponse authResponse = new AuthResponse();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(authenticationService.oauthAuthenticate(Mockito.anyString()))
        .thenReturn(new AuthResponse());
    when(oauthJwksKeyResolver.resolveSigningKey(Mockito.any(), Mockito.any(Claims.class)))
        .thenReturn(key);
    when(authenticationService.oauthAuthenticate("foo")).thenReturn(authResponse);
  }

  @Test
  public void handleCerberusTokenExchangeSuccess() {
    String jwtToken =
        Jwts.builder()
            .claim(OauthJwtAuthenticationController.USERNAME_CLAIM_NAME, "foo")
            .signWith(key)
            .compact();
    OauthJwtExchangeRequest oauthJwtExchangeRequest = new OauthJwtExchangeRequest();
    oauthJwtExchangeRequest.setToken(jwtToken);

    AuthResponse authResponse =
        oauthJwtAuthenticationController.handleCerberusTokenExchange(oauthJwtExchangeRequest);
    assertEquals(authResponse, this.authResponse);
  }

  @Test(expected = ApiException.class)
  public void handleCerberusTokenExchangeMissingUsernameClaim() {
    String jwtToken = Jwts.builder().claim("random claim", "bar").signWith(key).compact();
    OauthJwtExchangeRequest oauthJwtExchangeRequest = new OauthJwtExchangeRequest();
    oauthJwtExchangeRequest.setToken(jwtToken);

    AuthResponse authResponse =
        oauthJwtAuthenticationController.handleCerberusTokenExchange(oauthJwtExchangeRequest);
    assertEquals(authResponse, this.authResponse);
  }

  @Test(expected = ApiException.class)
  public void handleCerberusTokenExchangeInvalidSigningKey() {
    String jwtToken =
        Jwts.builder()
            .claim(OauthJwtAuthenticationController.USERNAME_CLAIM_NAME, "bar")
            .signWith(invalidKey)
            .compact();
    OauthJwtExchangeRequest oauthJwtExchangeRequest = new OauthJwtExchangeRequest();
    oauthJwtExchangeRequest.setToken(jwtToken);

    oauthJwtAuthenticationController.handleCerberusTokenExchange(oauthJwtExchangeRequest);
  }

  @Test(expected = ApiException.class)
  public void handleCerberusTokenExchangeExpiredToken() {
    String jwtToken =
        Jwts.builder()
            .claim("random claim", "bar")
            .setExpiration(Date.from(Instant.EPOCH))
            .signWith(key)
            .compact();
    OauthJwtExchangeRequest oauthJwtExchangeRequest = new OauthJwtExchangeRequest();
    oauthJwtExchangeRequest.setToken(jwtToken);

    oauthJwtAuthenticationController.handleCerberusTokenExchange(oauthJwtExchangeRequest);
  }
}
