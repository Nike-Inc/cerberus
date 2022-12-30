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
import com.nike.cerberus.auth.connector.AuthMfaDevice;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Initial state handler to handle relevant states during authentication. */
public class InitialLoginStateHandler extends AbstractOktaStateHandler {

  public InitialLoginStateHandler(
      AuthenticationClient client, CompletableFuture<AuthResponse> authenticationResponseFuture) {
    super(client, authenticationResponseFuture);
  }

  /**
   * Handles MFA required state
   *
   * @param mfaRequiredResponse - Authentication response from the Completable Future
   */
  @Override
  public void handleMfaRequired(AuthenticationResponse mfaRequiredResponse) {
    handleMfaResponse(mfaRequiredResponse);
  }

  /**
   * Handles MFA enroll state, when a user is not enrolled in any MFA factors.
   *
   * @param mfaEnroll - Authentication response from the Completable Future
   */
  @Override
  public void handleMfaEnroll(AuthenticationResponse mfaEnroll) {
    handleMfaResponse(mfaEnroll);
  }

  /**
   * Handles MFA states by determining valid user MFA factors.
   *
   * @param mfaResponse - Authentication response from the Completable Future
   */
  private void handleMfaResponse(AuthenticationResponse mfaResponse) {
    final String userId = mfaResponse.getUser().getId();
    final String userLogin = mfaResponse.getUser().getLogin();

    final AuthData authData = AuthData.builder().userId(userId).username(userLogin).build();
    final AuthResponse authResponse = AuthResponse.builder().data(authData).build();

    authData.setStateToken(mfaResponse.getStateToken());
    authResponse.setStatus(AuthStatus.MFA_REQUIRED);

    final List<Factor> factors = new ArrayList<>(mfaResponse.getFactors());

    factors.removeIf(this::shouldSkip);

    validateUserFactors(factors);

    factors.forEach(
        factor ->
            authData
                .getDevices()
                .add(
                    AuthMfaDevice.builder()
                        .id(factor.getId())
                        .name(getDeviceName(factor))
                        .requiresTrigger(isTriggerRequired(factor))
                        .isPush(isPush(factor))
                        .build()));

    authenticationResponseFuture.complete(authResponse);
  }
}
