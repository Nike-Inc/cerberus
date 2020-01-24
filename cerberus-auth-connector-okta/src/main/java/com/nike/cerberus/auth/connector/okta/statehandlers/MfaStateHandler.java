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

package com.nike.cerberus.auth.connector.okta.statehandlers;

import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import java.util.concurrent.CompletableFuture;

/** MFA state handler to handle MFA challenge when verifying an MFA factor. */
public class MfaStateHandler extends AbstractOktaStateHandler {

  public MfaStateHandler(
      AuthenticationClient client, CompletableFuture<AuthResponse> authenticationResponseFuture) {
    super(client, authenticationResponseFuture);
  }

  /**
   * Handles MFA Challenge, when a MFA challenge has been initiated for call or sms.
   *
   * @param mfaChallengeResponse - Authentication response from the Completable Future
   */
  @Override
  public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {

    final String userId = mfaChallengeResponse.getUser().getId();
    final String userLogin = mfaChallengeResponse.getUser().getLogin();

    final AuthData authData = new AuthData().setUserId(userId).setUsername(userLogin);
    AuthResponse authResponse =
        new AuthResponse().setData(authData).setStatus(AuthStatus.MFA_CHALLENGE);

    authenticationResponseFuture.complete(authResponse);
  }
}
