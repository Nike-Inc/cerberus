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

package com.nike.cerberus.auth.connector.onelogin;

import static com.nike.cerberus.error.DefaultApiError.MFA_SETUP_REQUIRED;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.error.DefaultApiError;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class OneLoginAuthConnectorTest {

  // constants used by several tests
  private static final long USER_ID = 1001L;
  private static final String USERNAME = "USERNAME";
  private static final String PASSWORD = "PASSWORD";
  private static final Long DEVICE_ID = 1001L;
  private static final String DEVICE_TYPE = "DEVICE_TYPE";
  private static final String STATE_TOKEN = "STATE_TOKEN";
  private static final String OTP_TOKEN = "OTP_TOKEN";

  // mock
  private OneLoginClient oneLoginClient = null;

  // class under test
  private OneLoginAuthConnector oneLoginAuthConnector = null;

  @Before
  public void setup() {
    oneLoginClient = mock(OneLoginClient.class);
    oneLoginAuthConnector = new OneLoginAuthConnector(oneLoginClient);
  }

  @Test
  public void test_authenticate_with_no_mfa() {
    ResponseStatus status = ResponseStatus.builder().error(false).build();

    CreateSessionLoginTokenResponse createSessionLoginTokenResponse =
        CreateSessionLoginTokenResponse.builder().status(status).build();

    SessionUser user = SessionUser.builder().id(USER_ID).username(USERNAME).build();

    SessionLoginTokenData sessionLoginTokenData =
        SessionLoginTokenData.builder().user(user).build();

    createSessionLoginTokenResponse.setData(Lists.newArrayList(sessionLoginTokenData));

    when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD))
        .thenReturn(createSessionLoginTokenResponse);

    // invoke method under test
    AuthResponse response = oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

    assertEquals(Long.toString(user.getId()), response.getData().getUserId());
    assertEquals(user.getUsername(), response.getData().getUsername());
    assertEquals(AuthStatus.SUCCESS, response.getStatus());
  }

  @Test
  public void test_authenticate_with_mfa_required() {
    ResponseStatus status = ResponseStatus.builder().error(false).build();

    CreateSessionLoginTokenResponse createSessionLoginTokenResponse =
        CreateSessionLoginTokenResponse.builder().status(status).build();

    SessionUser user = SessionUser.builder().id(USER_ID).username(USERNAME).build();

    MfaDevice device = MfaDevice.builder().deviceId(DEVICE_ID).deviceType(DEVICE_TYPE).build();

    SessionLoginTokenData sessionLoginTokenData =
        SessionLoginTokenData.builder()
            .user(user)
            .stateToken(STATE_TOKEN)
            .devices(Lists.newArrayList(device))
            .build();

    createSessionLoginTokenResponse.setData(Lists.newArrayList(sessionLoginTokenData));

    when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD))
        .thenReturn(createSessionLoginTokenResponse);

    // invoke method under test
    AuthResponse response = oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

    assertEquals(Long.toString(user.getId()), response.getData().getUserId());
    assertEquals(user.getUsername(), response.getData().getUsername());
    assertEquals(AuthStatus.MFA_REQUIRED, response.getStatus());
    assertEquals(DEVICE_ID.toString(), response.getData().getDevices().get(0).getId());
    assertEquals(DEVICE_TYPE, response.getData().getDevices().get(0).getName());
  }

  @Test
  public void test_authenticate_when_mfa_is_not_setup() {

    setupMockWhereLoginGivesError(400, "mfa something error message");

    try {
      // invoke method under test
      oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

      fail("expected exception not thrown");
    } catch (ApiException e) {
      assertTrue(e.getApiErrors().contains(MFA_SETUP_REQUIRED));
      assertFalse(e.getApiErrors().contains(DefaultApiError.AUTH_BAD_CREDENTIALS));
    }
  }

  @Test
  public void test_authenticate_with_bad_creds() {
    setupMockWhereLoginGivesError(401, "any other error message");

    try {
      // invoke method under test
      oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

      fail("expected exception not thrown");
    } catch (ApiException e) {
      assertTrue(e.getApiErrors().contains(DefaultApiError.AUTH_BAD_CREDENTIALS));
      assertFalse(e.getApiErrors().contains(MFA_SETUP_REQUIRED));
    }
  }

  @Test
  public void test_authenticate_with_500_response() {
    setupMockWhereLoginGivesError(500L, "any error message");

    try {
      // invoke method under test
      oneLoginAuthConnector.authenticate(USERNAME, PASSWORD);

      fail("expected exception not thrown");
    } catch (ApiException e) {
      assertTrue(e.getApiErrors().contains(DefaultApiError.GENERIC_BAD_REQUEST));
    }
  }

  private void setupMockWhereLoginGivesError(long statusCode, String message) {
    ResponseStatus status =
        ResponseStatus.builder().code(statusCode).message(message).error(true).build();

    CreateSessionLoginTokenResponse createSessionLoginTokenResponse =
        CreateSessionLoginTokenResponse.builder().status(status).build();

    when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD))
        .thenReturn(createSessionLoginTokenResponse);
  }

  @Test(expected = ApiException.class)
  public void test_triggerChallenge() {

    String stateToken = "state token";
    String deviceId = "device id";

    oneLoginAuthConnector.triggerChallenge(stateToken, deviceId);
  }

  @Test
  public void test_mfaCheck() {
    SessionUser user = SessionUser.builder().id(USER_ID).username(USERNAME).build();

    SessionLoginTokenData sessionLoginTokenData =
        SessionLoginTokenData.builder().user(user).build();
    sessionLoginTokenData.setUser(user);

    VerifyFactorResponse verifyFactorResponse = mock(VerifyFactorResponse.class);
    when(verifyFactorResponse.getStatus()).thenReturn(ResponseStatus.builder().build());
    when(verifyFactorResponse.getData()).thenReturn(Lists.newArrayList(sessionLoginTokenData));
    when(oneLoginClient.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN))
        .thenReturn(verifyFactorResponse);

    // invoke method under test
    AuthResponse response =
        oneLoginAuthConnector.mfaCheck(STATE_TOKEN, DEVICE_ID.toString(), OTP_TOKEN);
    assertEquals(Long.toString(user.getId()), response.getData().getUserId());
    assertEquals(user.getUsername(), response.getData().getUsername());
    assertEquals(AuthStatus.SUCCESS, response.getStatus());
  }

  @Test
  public void test_parseLdapGroups() {
    String ldapGroups =
        "CN=Application.foo.users,OU=Application,OU=Groups,DC=ad,DC=acme,DC=com;CN=Application.bar.users,OU=Application,OU=Groups,DC=ad,DC=acme,DC=com";

    // invoke method under test
    Set<String> actualResults = oneLoginAuthConnector.parseLdapGroups(ldapGroups);

    Set<String> expectedResults = Sets.newHashSet("Application.bar.users", "Application.foo.users");
    assertEquals(expectedResults, actualResults);
  }

  @Test
  public void test_parseLdapGroups_handles_null() {
    Set<String> actualResults = oneLoginAuthConnector.parseLdapGroups(null);
    Set<String> expectedResults = Sets.newHashSet();
    assertEquals(expectedResults, actualResults);
  }

  @Test
  public void test_getUserById() {
    ResponseStatus status = ResponseStatus.builder().error(false).build();

    UserData userData = UserData.builder().id(USER_ID).build();

    GetUserResponse getUserResponse =
        GetUserResponse.builder().data(Lists.newArrayList(userData)).status(status).build();
    when(oneLoginClient.getUserById(USER_ID)).thenReturn(getUserResponse);

    // invoke method under test
    UserData actualData = oneLoginAuthConnector.getUserById(USER_ID);

    assertEquals(userData.getId(), actualData.getId());
  }

  @Test
  public void test_getUserById_gives_error() {
    ResponseStatus status = ResponseStatus.builder().error(true).build();
    GetUserResponse getUserResponse = GetUserResponse.builder().status(status).build();

    when(oneLoginClient.getUserById(USER_ID)).thenReturn(getUserResponse);

    try {
      // invoke method under test
      oneLoginAuthConnector.getUserById(USER_ID);

      fail("expected exception not thrown");
    } catch (ApiException e) {
      assertTrue(e.getApiErrors().contains(DefaultApiError.SERVICE_UNAVAILABLE));
    }
  }

  @Test
  public void test_verifyFactor_with_400_error() {

    setupMockWhereVerifyGivesError(401, "any error message");

    try {
      // invoke method under test
      oneLoginAuthConnector.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN);

      fail("expected exception not thrown");
    } catch (ApiException e) {
      assertTrue(e.getApiErrors().contains(DefaultApiError.AUTH_BAD_CREDENTIALS));
    }
  }

  @Test
  public void test_verifyFactor_with_500_error() {

    setupMockWhereVerifyGivesError(500L, "any error message");

    try {
      // invoke method under test
      oneLoginAuthConnector.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN);

      fail("expected exception not thrown");
    } catch (ApiException e) {
      assertTrue(e.getApiErrors().contains(DefaultApiError.GENERIC_BAD_REQUEST));
    }
  }

  private void setupMockWhereVerifyGivesError(long statusCode, String message) {
    ResponseStatus status =
        ResponseStatus.builder().code(statusCode).message(message).error(true).build();

    VerifyFactorResponse verifyFactorResponse =
        VerifyFactorResponse.builder().status(status).build();

    when(oneLoginClient.verifyFactor(DEVICE_ID.toString(), STATE_TOKEN, OTP_TOKEN))
        .thenReturn(verifyFactorResponse);
  }

  @Test
  public void test_createSessionLoginToken() {
    ResponseStatus status = ResponseStatus.builder().error(false).build();

    CreateSessionLoginTokenResponse createSessionLoginTokenResponse =
        CreateSessionLoginTokenResponse.builder().status(status).build();

    SessionLoginTokenData expectedData = mock(SessionLoginTokenData.class);
    createSessionLoginTokenResponse.setData(Lists.newArrayList(expectedData));

    when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD))
        .thenReturn(createSessionLoginTokenResponse);

    // invoke method under test
    SessionLoginTokenData actualData =
        oneLoginAuthConnector.createSessionLoginToken(USERNAME, PASSWORD);

    assertEquals(expectedData, actualData);
  }

  @Test
  public void test_createSessionLoginToken_fails_with_401_when_bad_username_is_given() {
    ResponseStatus status =
        ResponseStatus.builder().code(400).message("bad request").error(true).build();

    CreateSessionLoginTokenResponse createSessionLoginTokenResponse =
        CreateSessionLoginTokenResponse.builder().status(status).build();

    when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD))
        .thenReturn(createSessionLoginTokenResponse);

    // invoke method under test
    try {
      oneLoginAuthConnector.createSessionLoginToken(USERNAME, PASSWORD);
    } catch (ApiException ae) {
      assertEquals(401, ae.getApiErrors().get(0).getHttpStatusCode());
    }
  }

  @Test
  public void test_createSessionLoginToken_fails_with_401_when_bad_password_is_given() {
    ResponseStatus status =
        ResponseStatus.builder().code(401).message("Authentication Failed").error(true).build();

    CreateSessionLoginTokenResponse createSessionLoginTokenResponse =
        CreateSessionLoginTokenResponse.builder().status(status).build();

    when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD))
        .thenReturn(createSessionLoginTokenResponse);

    // invoke method under test
    try {
      oneLoginAuthConnector.createSessionLoginToken(USERNAME, PASSWORD);
    } catch (ApiException ae) {
      assertEquals(401, ae.getApiErrors().get(0).getHttpStatusCode());
    }
  }

  @Test
  public void test_createSessionLoginToken_fails_with_when_MFA_setup_is_required() {
    ResponseStatus status =
        ResponseStatus.builder().error(true).code(400).message("MFA: rest doesnt matter").build();

    CreateSessionLoginTokenResponse createSessionLoginTokenResponse =
        CreateSessionLoginTokenResponse.builder().status(status).build();

    when(oneLoginClient.createSessionLoginToken(USERNAME, PASSWORD))
        .thenReturn(createSessionLoginTokenResponse);

    // invoke method under test
    try {
      oneLoginAuthConnector.createSessionLoginToken(USERNAME, PASSWORD);
    } catch (ApiException ae) {
      assertEquals(
          MFA_SETUP_REQUIRED.getHttpStatusCode(), ae.getApiErrors().get(0).getHttpStatusCode());
    }
  }
}
