package com.nike.cerberus.auth.connector.okta.statehandlers;

import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthMfaDevice;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    final AuthData authData = new AuthData().setUserId(userId).setUsername(userLogin);
    final AuthResponse authResponse = new AuthResponse().setData(authData);

    authData.setStateToken(mfaResponse.getStateToken());
    authResponse.setStatus(AuthStatus.MFA_REQUIRED);

    final List<Factor> factors =
        mfaResponse.getFactors().stream()
            .filter(this::isSupportedFactor)
            .collect(Collectors.toList());

    validateUserFactors(factors);

    factors.forEach(
        factor ->
            authData
                .getDevices()
                .add(
                    new AuthMfaDevice()
                        .setId(factor.getId())
                        .setName(getDeviceName(factor))
                        .setRequiresTrigger(isTriggerRequired(factor))));

    authenticationResponseFuture.complete(authResponse);
  }
}
