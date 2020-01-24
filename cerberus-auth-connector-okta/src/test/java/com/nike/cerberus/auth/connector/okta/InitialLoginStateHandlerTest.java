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
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.InitialLoginStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.impl.resource.DefaultFactor;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.User;
import com.okta.sdk.resource.user.factor.FactorProvider;
import com.okta.sdk.resource.user.factor.FactorType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class InitialLoginStateHandlerTest {

  // class under test
  private InitialLoginStateHandler initialLoginStateHandler;

  // Dependencies
  @Mock private AuthenticationClient client;
  private CompletableFuture<AuthResponse> authenticationResponseFuture;

  @Before
  public void setup() {

    initMocks(this);

    authenticationResponseFuture = new CompletableFuture<>();

    // create test object
    this.initialLoginStateHandler =
        new InitialLoginStateHandler(client, authenticationResponseFuture) {};
  }

  /////////////////////////
  // Test Methods
  /////////////////////////

  @Test
  public void handleMfaRequired() throws Exception {

    String email = "email";
    String id = "id";
    AuthStatus expectedStatus = AuthStatus.MFA_REQUIRED;

    FactorProvider provider = FactorProvider.OKTA;
    FactorType type = FactorType.TOKEN_SOFTWARE_TOTP;
    String deviceId = "device id";
    String status = "status";

    AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getLogin()).thenReturn(email);
    when(expectedResponse.getUser()).thenReturn(user);

    DefaultFactor factor = mock(DefaultFactor.class);

    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);
    when(factor.getStatus()).thenReturn(status);
    when(factor.getId()).thenReturn(deviceId);
    when(expectedResponse.getFactors()).thenReturn(Lists.newArrayList(factor));

    // do the call
    initialLoginStateHandler.handleMfaRequired(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    assertEquals(id, actualResponse.getData().getUserId());
    assertEquals(email, actualResponse.getData().getUsername());
    assertEquals(expectedStatus, actualResponse.getStatus());
  }

  @Test(expected = ApiException.class)
  public void handleMfaRequiredFailNoSupportedDevicesEnrolled() throws Exception {

    String email = "email";
    String id = "id";
    AuthStatus expectedStatus = AuthStatus.MFA_REQUIRED;

    FactorProvider provider = FactorProvider.OKTA;
    FactorType type = FactorType.PUSH;
    String deviceId = "device id";
    String status = "status";

    AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getLogin()).thenReturn(email);
    when(expectedResponse.getUser()).thenReturn(user);

    DefaultFactor factor = mock(DefaultFactor.class);

    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);
    when(factor.getStatus()).thenReturn(status);
    when(factor.getId()).thenReturn(deviceId);
    when(expectedResponse.getFactors()).thenReturn(Lists.newArrayList(factor));

    // do the call
    initialLoginStateHandler.handleMfaRequired(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    assertEquals(id, actualResponse.getData().getUserId());
    assertEquals(email, actualResponse.getData().getUsername());
    assertEquals(expectedStatus, actualResponse.getStatus());
  }

  @Test
  public void handleMfaEnroll() throws Exception {

    String email = "email";
    String id = "id";
    AuthStatus expectedStatus = AuthStatus.MFA_REQUIRED;

    FactorProvider provider = FactorProvider.OKTA;
    FactorType type = FactorType.TOKEN_SOFTWARE_TOTP;
    String deviceId = "device id";
    String status = "status";

    AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getLogin()).thenReturn(email);
    when(expectedResponse.getUser()).thenReturn(user);

    DefaultFactor factor = mock(DefaultFactor.class);

    when(factor.getType()).thenReturn(type);
    when(factor.getProvider()).thenReturn(provider);
    when(factor.getStatus()).thenReturn(status);
    when(factor.getId()).thenReturn(deviceId);
    when(expectedResponse.getFactors()).thenReturn(Lists.newArrayList(factor));

    // do the call
    initialLoginStateHandler.handleMfaEnroll(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    assertEquals(id, actualResponse.getData().getUserId());
    assertEquals(email, actualResponse.getData().getUsername());
    assertEquals(expectedStatus, actualResponse.getStatus());
  }

  @Test(expected = ApiException.class)
  public void handleMfaEnrollFails() throws Exception {

    String email = "email";
    String id = "id";
    AuthStatus expectedStatus = AuthStatus.MFA_REQUIRED;

    AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getLogin()).thenReturn(email);
    when(expectedResponse.getUser()).thenReturn(user);

    // do the call
    initialLoginStateHandler.handleMfaEnroll(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    assertEquals(id, actualResponse.getData().getUserId());
    assertEquals(email, actualResponse.getData().getUsername());
    assertEquals(expectedStatus, actualResponse.getStatus());
  }
}
