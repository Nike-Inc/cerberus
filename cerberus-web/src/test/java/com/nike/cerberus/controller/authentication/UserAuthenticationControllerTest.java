package com.nike.cerberus.controller.authentication;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.domain.MfaCheckRequest;
import com.nike.cerberus.domain.UserCredentials;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.filter.AuditLoggingFilterDetails;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.AuthenticationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
// import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class UserAuthenticationControllerTest {

  @Mock private AuthenticationService authenticationService;
  @Mock private AuditLoggingFilterDetails auditLoggingFilterDetails;
  private UserAuthenticationController userAuthenticationController;

  // @InjectMocks private UserAuthenticationController userAuthenticationController;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.userAuthenticationController =
        new UserAuthenticationController(authenticationService, auditLoggingFilterDetails, true);
  }

  @Test
  @SuppressFBWarnings
  public void testAuthenticateWhenAuthHeaderIsNull() {
    ApiException apiException = null;
    try {
      userAuthenticationController.authenticate(null);
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertNotNull(apiException);
    Assert.assertEquals(DefaultApiError.AUTH_BAD_CREDENTIALS, apiException.getApiErrors().get(0));
  }

  @Test
  @SuppressFBWarnings
  public void testAuthenticateWhenAuthHeaderIsEmpty() {
    ApiException apiException = null;
    try {
      userAuthenticationController.authenticate("");
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertNotNull(apiException);
    Assert.assertEquals(DefaultApiError.AUTH_BAD_CREDENTIALS, apiException.getApiErrors().get(0));
  }

  @Test
  @SuppressFBWarnings
  public void testAuthenticateWhenAuthHeaderIsDoesNotStartWithBasic() {
    ApiException apiException = null;
    try {
      userAuthenticationController.authenticate("token");
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertNotNull(apiException);
    Assert.assertEquals(DefaultApiError.AUTH_BAD_CREDENTIALS, apiException.getApiErrors().get(0));
  }

  @Test
  @SuppressFBWarnings
  public void testAuthenticateWhenAuthenticationServiceAuthenticateThrowsException() {
    ApiException apiException = null;
    byte[] encodedBytes =
        Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8));
    Mockito.when(authenticationService.authenticate(Mockito.any(UserCredentials.class)))
        .thenThrow(ApiException.newBuilder().withApiErrors(DefaultApiError.LOGIN_FAILED).build());
    try {
      userAuthenticationController.authenticate(
          "Basic" + new String(encodedBytes, StandardCharsets.UTF_8));
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertNotNull(apiException);
    Assert.assertEquals(DefaultApiError.LOGIN_FAILED, apiException.getApiErrors().get(0));
    Mockito.verify(auditLoggingFilterDetails).setAction("Failed to authenticate");
  }

  @Test
  public void testAuthenticate() {
    byte[] encodedBytes =
        Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8));
    AuthResponse authResponse = Mockito.mock(AuthResponse.class);
    Mockito.when(authenticationService.authenticate(Mockito.any(UserCredentials.class)))
        .thenReturn(authResponse);
    AuthResponse actualAuthResponse =
        userAuthenticationController.authenticate(
            "Basic" + new String(encodedBytes, StandardCharsets.UTF_8));
    Assert.assertSame(authResponse, actualAuthResponse);
    Mockito.verify(auditLoggingFilterDetails).setAction("Authenticated");
  }

  @Test
  public void testHandleMfaCheckWhenRequestIsPush() {
    MfaCheckRequest mfaCheckRequest = Mockito.mock(MfaCheckRequest.class);
    Mockito.when(mfaCheckRequest.isPush()).thenReturn(true);
    userAuthenticationController.handleMfaCheck(mfaCheckRequest);
    Mockito.verify(authenticationService).triggerPush(mfaCheckRequest);
  }

  @Test
  public void testHandleMfaCheckWhenOtpTokenIsEmpty() {
    MfaCheckRequest mfaCheckRequest = Mockito.mock(MfaCheckRequest.class);
    userAuthenticationController.handleMfaCheck(mfaCheckRequest);
    Mockito.verify(authenticationService, Mockito.never()).triggerPush(mfaCheckRequest);
    Mockito.verify(authenticationService).triggerChallenge(mfaCheckRequest);
  }

  @Test
  public void testHandleMfaCheckWhenOtpTokenIsNotEmpty() {
    MfaCheckRequest mfaCheckRequest = Mockito.mock(MfaCheckRequest.class);
    Mockito.when(mfaCheckRequest.getOtpToken()).thenReturn("token");
    userAuthenticationController.handleMfaCheck(mfaCheckRequest);
    Mockito.verify(authenticationService, Mockito.never()).triggerPush(mfaCheckRequest);
    Mockito.verify(authenticationService, Mockito.never()).triggerChallenge(mfaCheckRequest);
    Mockito.verify(authenticationService).mfaCheck(mfaCheckRequest);
  }

  @Test
  public void testRefreshToken() {
    CerberusPrincipal cerberusPrincipal = Mockito.mock(CerberusPrincipal.class);
    userAuthenticationController.refreshToken(cerberusPrincipal);
    Mockito.verify(authenticationService).refreshUserToken(cerberusPrincipal);
  }

  @Test
  public void testExchangeTokenDisabled() {
    ApiException apiException = null;
    try {
      UserAuthenticationController uac =
          new UserAuthenticationController(authenticationService, auditLoggingFilterDetails, false);
      uac.exchangeToken("bearer foobar");
    } catch (ApiException exc) {
      apiException = exc;
    }
    Assert.assertNotNull(apiException);
    Assert.assertEquals(DefaultApiError.ENTITY_NOT_FOUND, apiException.getApiErrors().get(0));
  }

  @Test
  public void testExchangeTokenNullHeader() {
    ApiException apiException = null;
    try {
      userAuthenticationController.exchangeToken(null);
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertNotNull(apiException);
    Assert.assertEquals(DefaultApiError.BEARER_TOKEN_INVALID, apiException.getApiErrors().get(0));
  }

  @Test
  public void testExchangeTokenWrongHeader() {
    ApiException apiException = null;
    try {
      userAuthenticationController.exchangeToken("bear dogs");
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertNotNull(apiException);
    Assert.assertEquals(DefaultApiError.BEARER_TOKEN_INVALID, apiException.getApiErrors().get(0));
  }

  @Test
  public void testExchangeTokenDecodeError() {
    Mockito.when(authenticationService.exchangeJwtAccessToken(Mockito.anyString()))
        .thenThrow(buildApiException("error"));
    ApiException apiException = null;
    try {
      userAuthenticationController.exchangeToken("bearer dogs");
    } catch (ApiException e) {
      apiException = e;
    }
    Assert.assertNotNull(apiException);
    String expected = DefaultApiError.BEARER_TOKEN_INVALID.toString();
    String actual = apiException.getApiErrors().get(0).toString();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testExchangeTokenHappy() {
    final AuthData authData = AuthData.builder().username("tester").userId("aardvark").build();
    final AuthResponse authResponse =
        AuthResponse.builder().data(authData).status(AuthStatus.SUCCESS).build();
    Mockito.when(authenticationService.exchangeJwtAccessToken(Mockito.anyString()))
        .thenReturn(authResponse);

    AuthResponse response = userAuthenticationController.exchangeToken("bearer dogs");
    Assert.assertEquals(response.getData().getUserId(), "aardvark");
  }

  private ApiException buildApiException(String msg) {
    ApiException exc =
        ApiException.Builder.newBuilder()
            .withApiErrors(DefaultApiError.BEARER_TOKEN_INVALID)
            .withExceptionMessage(msg)
            .build();
    return exc;
  }
}
