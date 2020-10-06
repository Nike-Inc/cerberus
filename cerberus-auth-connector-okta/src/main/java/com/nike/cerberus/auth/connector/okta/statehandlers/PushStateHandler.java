package com.nike.cerberus.auth.connector.okta.statehandlers;

import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.FactorType;
import com.okta.sdk.impl.resource.MapProperty;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PushStateHandler extends AbstractOktaStateHandler {

  private static final MapProperty NESTED__CHALLENGE_PROPERTY = new MapProperty("challenge");
  private static final MapProperty NESTED__CORRECT_ANSWER_PROPERTY = new MapProperty("challenge");

  public PushStateHandler(
      AuthenticationClient client, CompletableFuture<AuthResponse> authenticationResponseFuture) {
    super(client, authenticationResponseFuture);
  }

  /**
   * Handles MFA Challenge, when a MFA challenge has been initiated for push notification.
   *
   * @param mfaChallengeResponse - Authentication response from the Completable Future
   */
  @Override
  public void handleMfaChallenge(AuthenticationResponse mfaChallengeResponse) {

    final String userId = mfaChallengeResponse.getUser().getId();
    final String userLogin = mfaChallengeResponse.getUser().getLogin();
    final String factorResult = mfaChallengeResponse.getFactorResult();
    final Integer challengeCorrectAnswer =
        getChallengeCorrectAnswer(mfaChallengeResponse.getFactors());

    final AuthData authData =
        new AuthData()
            .setUserId(userId)
            .setUsername(userLogin)
            .setFactorResult(factorResult)
            .setChallengeCorrectAnswer(challengeCorrectAnswer);
    AuthResponse authResponse =
        new AuthResponse().setData(authData).setStatus(AuthStatus.MFA_CHALLENGE);

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
        new AuthData().setUserId(userId).setUsername(userLogin).setFactorResult(factorResult);
    AuthResponse authResponse = new AuthResponse().setData(authData).setStatus(AuthStatus.SUCCESS);

    authenticationResponseFuture.complete(authResponse);
  }

  private Integer getChallengeCorrectAnswer(List<Factor> factors) {
    for (Factor factor : factors) {
      if (factor.getType() == FactorType.PUSH) {
        if (factor.getEmbedded() != null) {
          Map<String, Integer> challenge =
              (Map<String, Integer>) factor.getEmbedded().get(NESTED__CHALLENGE_PROPERTY.getName());
          if (challenge != null) {
            return challenge.get(NESTED__CORRECT_ANSWER_PROPERTY);
          }
        }
      }
    }
    return null;
  }
}
