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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.InitialLoginStateHandler;
import com.nike.cerberus.auth.connector.okta.statehandlers.MfaStateHandler;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.impl.resource.DefaultVerifyPassCodeFactorRequest;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import com.okta.sdk.client.Client;
import com.okta.sdk.impl.client.DefaultClient;
import com.okta.sdk.impl.error.DefaultError;
import com.okta.sdk.resource.ResourceException;
import com.okta.sdk.resource.group.Group;
import com.okta.sdk.resource.group.GroupList;
import com.okta.sdk.resource.group.GroupProfile;
import com.okta.sdk.resource.user.User;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/** Tests the OktaAuthConnector class */
public class OktaAuthConnectorTest {

  // class under test
  private OktaAuthConnector oktaAuthConnector;

  @Mock private AuthenticationClient client;
  @Mock Client sdkClient;

  @Before
  public void setup() {

    initMocks(this);

    this.oktaAuthConnector =
        new OktaAuthConnector(
            client, sdkClient, "https://foo.bar", "dogs", mock(AccessTokenVerifier.class));
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
  public void testGetValidatedOktaPrincipalOkay() {
    try {
      Map<String, Object> claims = new HashMap<String, Object>();
      claims.put("sub", "tester");
      claims.put("uid", "freeter");

      Jwt mockJwt = mock(Jwt.class);
      when(mockJwt.getClaims()).thenReturn(claims);
      AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);
      when(verifier.decode(anyString())).thenReturn(mockJwt);
      OktaAuthConnector connector =
          new OktaAuthConnector(
              client, sdkClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);
      Map<String, String> principal = connector.getValidatedUserPrincipal("us");

      assertEquals(principal.get("username"), "tester");
      assertEquals(principal.get("userId"), "freeter");
    } catch (JwtVerificationException jve) {
      assert false;
    }
  }

  @Test(expected = ApiException.class)
  public void testGetValidatedOktaPrincipalMissingUserId() {
    try {
      Map<String, Object> claims = new HashMap<String, Object>();
      claims.put("sub", "tester");
      // claims.put("uid", "freeter");

      Jwt mockJwt = mock(Jwt.class);
      when(mockJwt.getClaims()).thenReturn(claims);
      AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);
      when(verifier.decode(anyString())).thenReturn(mockJwt);
      OktaAuthConnector connector =
          new OktaAuthConnector(
              client, sdkClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);
      Map<String, String> principal = connector.getValidatedUserPrincipal("us");

      assertEquals(principal.get("username"), "tester");
      assertEquals(principal.get("userId"), "freeter");
    } catch (JwtVerificationException jve) {
      assert false;
    }
  }

  @Test(expected = ApiException.class)
  public void testGetValidatedOktaPrincipalBadClaims() {
    try {
      Map<String, Object> claims = new HashMap<String, Object>();

      Jwt mockJwt = mock(Jwt.class);
      when(mockJwt.getClaims()).thenReturn(claims);
      AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);
      when(verifier.decode(anyString())).thenReturn(mockJwt);
      OktaAuthConnector connector =
          new OktaAuthConnector(
              client, sdkClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);

      connector.getValidatedUserPrincipal("us");
    } catch (JwtVerificationException jve) {
      assert false;
    }
  }

  @Test
  public void testGetAccessTokenVerifierInitialNull() {
    OktaAuthConnector connector =
        new OktaAuthConnector(
            client, sdkClient, "https://foo.bar/oauth2/skiddleydee", "dogs", null);
    AccessTokenVerifier verifier = connector.getAccessTokenVerifier();
    assertNotNull(verifier);
  }

  @Test
  public void testGetAccessTokenVerifier() {
    AccessTokenVerifier verifier = this.oktaAuthConnector.getAccessTokenVerifier();
    assertNotNull(verifier);
  }

  @Test
  public void testGetGroups() {
    AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);

    GroupProfile groupProfile = mock(GroupProfile.class);
    when(groupProfile.getName()).thenReturn("testGroup");

    Group fakeGroup = mock(Group.class);
    when(fakeGroup.getProfile()).thenReturn(groupProfile);

    List<Group> groupIteraterList = Lists.newArrayList(fakeGroup);
    GroupList groupList = mock(GroupList.class);
    when(groupList.iterator()).thenReturn(groupIteraterList.iterator());

    User mockUser = mock(User.class);
    when(mockUser.listGroups()).thenReturn(groupList);

    DefaultClient mockClient = mock(DefaultClient.class);
    when(mockClient.getUser(anyString())).thenReturn(mockUser);

    OktaAuthConnector connector =
        new OktaAuthConnector(
            client, mockClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);
    AuthData authData = AuthData.builder().userId("deadbeef").build();
    Set<String> groups = connector.getGroups(authData);
    assertEquals(groups, Set.of("testGroup"));
  }

  @Test
  public void testGetGroupsMissingProfile() {
    AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);

    Group fakeGroup = mock(Group.class);
    when(fakeGroup.getProfile()).thenReturn(null);

    List<Group> groupIteraterList = Lists.newArrayList(fakeGroup);
    GroupList groupList = mock(GroupList.class);
    when(groupList.iterator()).thenReturn(groupIteraterList.iterator());

    User mockUser = mock(User.class);
    when(mockUser.listGroups()).thenReturn(groupList);

    DefaultClient mockClient = mock(DefaultClient.class);
    when(mockClient.getUser(anyString())).thenReturn(mockUser);

    OktaAuthConnector connector =
        new OktaAuthConnector(
            client, mockClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);
    AuthData authData = AuthData.builder().userId("deadbeef").build();
    Set<String> groups = connector.getGroups(authData);
    assertEquals(groups, new HashSet<String>());
  }

  @Test
  public void testGetGroupsNullGroups() {
    AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);

    User mockUser = mock(User.class);
    when(mockUser.listGroups()).thenReturn(null);

    DefaultClient mockClient = mock(DefaultClient.class);
    when(mockClient.getUser(anyString())).thenReturn(mockUser);

    OktaAuthConnector connector =
        new OktaAuthConnector(
            client, mockClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);

    AuthData authData = AuthData.builder().userId("deadbeef").build();
    Set<String> groups = connector.getGroups(authData);
    assertEquals(groups, new HashSet<>());
  }

  @Test(expected = ApiException.class)
  public void testBadGetUser() {
    AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);
    Client mockClient = mock(Client.class);
    when(mockClient.getUser(anyString())).thenThrow(new RuntimeException("it's broke"));
    OktaAuthConnector connector =
        new OktaAuthConnector(
            client, mockClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);
    AuthData authData = AuthData.builder().userId("deadbeef").build();
    connector.getGroups(authData);
  }

  @Test
  public void testGetUserFromIdpCompletelyBrokenOkta() {
    AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);
    Client mockClient = mock(Client.class);
    String exceptionMessage = "who knows what broke?";
    when(mockClient.getUser(anyString())).thenThrow(new IllegalStateException(exceptionMessage));
    OktaAuthConnector connector =
        new OktaAuthConnector(
            client, mockClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);
    try {
      connector.getUserFromIDP("fooUser");
    } catch (ApiException exc) {
      String actualMessage = exc.getMessage();
      assertEquals(actualMessage, "Could not communicate properly with identity provider");
      ApiError apiError = exc.getApiErrors().get(0);
      assertEquals(apiError, DefaultApiError.IDENTITY_PROVIDER_BAD_GATEWAY);
      String causeMessage = exc.getCause().getMessage();
      assertEquals(causeMessage, exceptionMessage);
    }
  }

  @Test
  public void testGetUserFromIdpOktaProblem() {
    AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);
    Client mockClient = mock(Client.class);
    String excMessage = "A specific thing had a problem";
    String excpetionPrefix = "Got invalid response from identity providers";
    ResourceException resourceException =
        new ResourceException(new DefaultError(ImmutableMap.of("message", excMessage)));
    when(mockClient.getUser(anyString())).thenThrow(resourceException);
    OktaAuthConnector connector =
        new OktaAuthConnector(
            client, mockClient, "https://foo.bar/oauth2/skiddleydee", "dogs", verifier);
    try {
      connector.getUserFromIDP("fooUser");
    } catch (ApiException exc) {
      String actualMessage = exc.getMessage();
      assert actualMessage.startsWith(excpetionPrefix);
      assertEquals(exc.getApiErrors().get(0), DefaultApiError.IDENTITY_PROVIDER_BAD_GATEWAY);
    }
  }
}
