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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.MfaStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.User;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/** Tests the MfaOktaStateHandler class */
public class MfaStateHandlerTest {

  // class under test
  private MfaStateHandler mfaStateHandler;

  // Dependencies
  @Mock private AuthenticationClient client;
  private CompletableFuture<AuthResponse> authenticationResponseFuture;

  @Before
  public void setup() {

    initMocks(this);

    authenticationResponseFuture = new CompletableFuture<>();

    // create test object
    this.mfaStateHandler = new MfaStateHandler(client, authenticationResponseFuture) {};
  }

  /////////////////////////
  // Test Methods
  /////////////////////////

  @Test
  public void handleMfaChallenge() throws Exception {

    String email = "email";
    String id = "id";
    AuthStatus status = AuthStatus.MFA_CHALLENGE;

    AuthenticationResponse expectedResponse = mock(AuthenticationResponse.class);

    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    when(user.getLogin()).thenReturn(email);
    when(expectedResponse.getUser()).thenReturn(user);

    // do the call
    mfaStateHandler.handleMfaChallenge(expectedResponse);

    AuthResponse actualResponse = authenticationResponseFuture.get(1, TimeUnit.SECONDS);

    //  verify results
    assertEquals(id, actualResponse.getData().getUserId());
    assertEquals(email, actualResponse.getData().getUsername());
    assertEquals(status, actualResponse.getStatus());
  }
}
