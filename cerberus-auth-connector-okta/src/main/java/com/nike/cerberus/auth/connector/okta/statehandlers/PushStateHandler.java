package com.nike.cerberus.auth.connector.okta.statehandlers;

import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import java.util.concurrent.CompletableFuture;

public class PushStateHandler extends AbstractOktaStateHandler {
  public PushStateHandler(
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
    final String factorResult = mfaChallengeResponse.getFactorResult();

    final AuthData authData =
        AuthData.builder().userId(userId).username(userLogin).factorResult(factorResult).build();
    AuthResponse authResponse =
        AuthResponse.builder().data(authData).status(AuthStatus.MFA_CHALLENGE).build();

    authenticationResponseFuture.complete(authResponse);
  }

  /**
   * Handles MFA Challenge, when a MFA challenge has been initiated for call or sms.
   *
   * @param mfaChallengeResponse - Authentication response from the Completable Future
   */
  @Override
  public void handleSuccess(AuthenticationResponse mfaChallengeResponse) {

    final String userId = mfaChallengeResponse.getUser().getId();
    final String userLogin = mfaChallengeResponse.getUser().getLogin();
    final String factorResult = mfaChallengeResponse.getStatus().toString();

    final AuthData authData =
        AuthData.builder().userId(userId).username(userLogin).factorResult(factorResult).build();
    AuthResponse authResponse =
        AuthResponse.builder().data(authData).status(AuthStatus.SUCCESS).build();

    authenticationResponseFuture.complete(authResponse);
  }
}
