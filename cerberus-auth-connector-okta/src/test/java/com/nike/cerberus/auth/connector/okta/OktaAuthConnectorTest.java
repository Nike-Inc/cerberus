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

package com.nike.cerberus.auth.connector.okta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.InitialLoginStateHandler;
import com.nike.cerberus.auth.connector.okta.statehandlers.MfaStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.impl.resource.DefaultVerifyPassCodeFactorRequest;
import com.okta.sdk.models.usergroups.UserGroup;
import com.okta.sdk.models.usergroups.UserGroupProfile;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/** Tests the OktaAuthConnector class */
public class OktaAuthConnectorTest {

  // class under test
  private OktaAuthConnector oktaAuthConnector;

  // dependencies
  @Mock private OktaApiClientHelper oktaApiClientHelper;

  @Mock private AuthenticationClient client;

  @Before
  public void setup() {

    initMocks(this);
    // create test object
    oktaAuthConnector = new OktaAuthConnector(oktaApiClientHelper, client);

    reset(oktaApiClientHelper);
  }

  /////////////////////////
  // Test Methods
  /////////////////////////

  @Test
  public void authenticateSuccess() throws Exception {

    String username = "username";
    String password = "password";

    AuthResponse expectedResponse = mock(AuthResponse.class);
    when(expectedResponse.getStatus()).thenReturn(AuthStatus.SUCCESS);

    doAnswer(
            invocation -> {
              InitialLoginStateHandler stateHandler =
                  (InitialLoginStateHandler) invocation.getArguments()[3];
              stateHandler.authenticationResponseFuture.complete(expectedResponse);
              return null;
            })
        .when(client)
        .authenticate(any(), any(), any(), any());

    // do the call
    AuthResponse actualResponse = this.oktaAuthConnector.authenticate(username, password);

    //  verify results
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = ApiException.class)
  public void authenticateFails() throws Exception {

    String username = "username";
    String password = "password";

    AuthResponse expectedResponse = mock(AuthResponse.class);
    when(expectedResponse.getStatus()).thenReturn(AuthStatus.MFA_REQUIRED);

    doAnswer(
            invocation -> {
              InitialLoginStateHandler stateHandler =
                  (InitialLoginStateHandler) invocation.getArguments()[3];
              stateHandler.authenticationResponseFuture.cancel(true);
              return null;
            })
        .when(client)
        .authenticate(any(), any(), any(), any());

    // do the call
    AuthResponse actualResponse = this.oktaAuthConnector.authenticate(username, password);

    //  verify results
    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void triggerChallengeSuccess() throws Exception {

    String stateToken = "state token";
    String deviceId = "device id";

    AuthResponse expectedResponse = mock(AuthResponse.class);

    AuthData expectedData = mock(AuthData.class);
    when(expectedData.getStateToken()).thenReturn(stateToken);
    when(expectedResponse.getData()).thenReturn(expectedData);

    doAnswer(
            invocation -> {
              MfaStateHandler stateHandler = (MfaStateHandler) invocation.getArguments()[2];
              stateHandler.authenticationResponseFuture.complete(expectedResponse);
              return null;
            })
        .when(client)
        .challengeFactor(any(), any(), any());

    // do the call
    AuthResponse actualResponse = this.oktaAuthConnector.triggerChallenge(stateToken, deviceId);

    //  verify results
    assertEquals(expectedResponse, actualResponse);
    assertEquals(
        expectedResponse.getData().getStateToken(), actualResponse.getData().getStateToken());
  }

  @Test(expected = ApiException.class)
  public void triggerChallengeFails() throws Exception {

    String stateToken = "state token";
    String deviceId = "device id";

    AuthResponse expectedResponse = mock(AuthResponse.class);

    AuthData expectedData = mock(AuthData.class);
    when(expectedData.getStateToken()).thenReturn(stateToken);
    when(expectedResponse.getData()).thenReturn(expectedData);

    doAnswer(
            invocation -> {
              MfaStateHandler stateHandler = (MfaStateHandler) invocation.getArguments()[2];
              stateHandler.authenticationResponseFuture.cancel(true);
              return null;
            })
        .when(client)
        .challengeFactor(any(), any(), any());

    // do the call
    AuthResponse actualResponse = this.oktaAuthConnector.triggerChallenge(stateToken, deviceId);

    //  verify results
    assertEquals(expectedResponse, actualResponse);
    assertEquals(
        expectedResponse.getData().getStateToken(), actualResponse.getData().getStateToken());
  }

  @Test
  public void mfaCheckSuccess() throws Exception {

    String stateToken = "state token";
    String deviceId = "device id";
    String otpToken = "otp token";

    AuthResponse expectedResponse = mock(AuthResponse.class);
    when(expectedResponse.getStatus()).thenReturn(AuthStatus.SUCCESS);

    DefaultVerifyPassCodeFactorRequest request = mock(DefaultVerifyPassCodeFactorRequest.class);

    doAnswer(
            invocation -> {
              request.setPassCode(stateToken);
              request.setStateToken(otpToken);
              return request;
            })
        .when(client)
        .instantiate(DefaultVerifyPassCodeFactorRequest.class);
    doAnswer(
            invocation -> {
              MfaStateHandler stateHandler = (MfaStateHandler) invocation.getArguments()[2];
              stateHandler.authenticationResponseFuture.complete(expectedResponse);
              return null;
            })
        .when(client)
        .verifyFactor(anyString(), isA(DefaultVerifyPassCodeFactorRequest.class), any());

    // do the call
    AuthResponse actualResponse = this.oktaAuthConnector.mfaCheck(stateToken, deviceId, otpToken);

    //  verify results
    assertEquals(expectedResponse, actualResponse);
  }

  @Test(expected = ApiException.class)
  public void mfaCheckFails() throws Exception {

    String stateToken = "state token";
    String deviceId = "device id";
    String otpToken = "otp token";

    AuthResponse expectedResponse = mock(AuthResponse.class);
    when(expectedResponse.getStatus()).thenReturn(AuthStatus.SUCCESS);

    DefaultVerifyPassCodeFactorRequest request = mock(DefaultVerifyPassCodeFactorRequest.class);

    doAnswer(
            invocation -> {
              request.setPassCode(stateToken);
              request.setStateToken(otpToken);
              return request;
            })
        .when(client)
        .instantiate(DefaultVerifyPassCodeFactorRequest.class);
    doAnswer(
            invocation -> {
              MfaStateHandler stateHandler = (MfaStateHandler) invocation.getArguments()[2];
              stateHandler.authenticationResponseFuture.cancel(true);
              return null;
            })
        .when(client)
        .verifyFactor(any(), isA(DefaultVerifyPassCodeFactorRequest.class), any());

    // do the call
    AuthResponse actualResponse = this.oktaAuthConnector.mfaCheck(stateToken, deviceId, otpToken);

    //  verify results
    assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void getGroupsHappy() {

    String id = "id";
    AuthData authData = mock(AuthData.class);
    when(authData.getUserId()).thenReturn(id);

    String name1 = "name 1";
    UserGroupProfile profile1 = mock(UserGroupProfile.class);
    UserGroup group1 = mock(UserGroup.class);
    when(profile1.getName()).thenReturn(name1);
    when(group1.getProfile()).thenReturn(profile1);

    String name2 = "name 2";
    UserGroupProfile profile2 = mock(UserGroupProfile.class);
    UserGroup group2 = mock(UserGroup.class);
    when(profile2.getName()).thenReturn(name2);
    when(group2.getProfile()).thenReturn(profile2);

    when(oktaApiClientHelper.getUserGroups(id)).thenReturn(Lists.newArrayList(group1, group2));

    // do the call
    Set<String> result = this.oktaAuthConnector.getGroups(authData);

    // verify results
    assertTrue(result.contains(name1));
    assertTrue(result.contains(name2));
  }
}
