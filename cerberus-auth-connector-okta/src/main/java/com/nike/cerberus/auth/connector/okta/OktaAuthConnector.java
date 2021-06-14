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

import static java.lang.Thread.sleep;

import com.google.common.base.Preconditions;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.okta.statehandlers.InitialLoginStateHandler;
import com.nike.cerberus.auth.connector.okta.statehandlers.MfaStateHandler;
import com.nike.cerberus.auth.connector.okta.statehandlers.PushStateHandler;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.authn.sdk.FactorValidationException;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.impl.resource.DefaultVerifyPassCodeFactorRequest;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.group.GroupList;
import com.okta.sdk.resource.user.User;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Okta version 1 API implementation of the AuthConnector interface. */
@Component
public class OktaAuthConnector implements AuthConnector {

  private final AuthenticationClient oktaAuthenticationClient;

  private final Client sdkClient;

  @Autowired
  public OktaAuthConnector(
      AuthenticationClient oktaAuthenticationClient,
      OktaConfigurationProperties oktaConfigurationProperties) {
    this.oktaAuthenticationClient = oktaAuthenticationClient;
    this.sdkClient = getSdkClient(oktaConfigurationProperties);
  }

  private Client getSdkClient(OktaConfigurationProperties oktaConfigurationProperties) {
    return Clients.builder()
        .setOrgUrl(oktaConfigurationProperties.getBaseUrl())
        .setClientCredentials(new TokenClientCredentials(oktaConfigurationProperties.getApiKey()))
        .build();
  }

  /** Authenticates user using Okta Auth SDK. */
  @Override
  public AuthResponse authenticate(String username, String password) {

    CompletableFuture<AuthResponse> authResponse = new CompletableFuture<>();
    InitialLoginStateHandler stateHandler =
        new InitialLoginStateHandler(oktaAuthenticationClient, authResponse);

    try {
      oktaAuthenticationClient.authenticate(username, password.toCharArray(), null, stateHandler);
      return authResponse.get(45, TimeUnit.SECONDS);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw ApiException.newBuilder()
          .withExceptionCause(e)
          .withApiErrors(DefaultApiError.LOGIN_FAILED)
          .withExceptionMessage(
              "Failed to login or failed to wait for Okta Auth Completable Future to complete.")
          .build();
    }
  }

  /** Triggers challenge for SMS or Call factors using Okta Auth SDK. */
  public AuthResponse triggerChallenge(String stateToken, String deviceId) {

    CompletableFuture<AuthResponse> authResponse = new CompletableFuture<>();
    MfaStateHandler stateHandler = new MfaStateHandler(oktaAuthenticationClient, authResponse);

    try {
      oktaAuthenticationClient.challengeFactor(deviceId, stateToken, stateHandler);
      return authResponse.get(45, TimeUnit.SECONDS);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw ApiException.newBuilder()
          .withExceptionCause(e)
          .withApiErrors(DefaultApiError.AUTH_RESPONSE_WAIT_FAILED)
          .withExceptionMessage("Failed to trigger challenge due to timeout. Please try again.")
          .build();
    }
  }

  /** Triggers challenge for SMS or Call factors using Okta Auth SDK. */
  public AuthResponse triggerPush(String stateToken, String deviceId) {

    CompletableFuture<AuthResponse> authResponseFuture = new CompletableFuture<>();
    PushStateHandler stateHandler =
        new PushStateHandler(oktaAuthenticationClient, authResponseFuture);

    try {
      oktaAuthenticationClient.verifyFactor(deviceId, stateToken, stateHandler);

      AuthResponse authResponse = authResponseFuture.get(45, TimeUnit.SECONDS);
      long startTime = System.currentTimeMillis();
      while (authResponse.getData().getFactorResult().equals("WAITING")
          && System.currentTimeMillis() - startTime <= 55000) {
        sleep(100);
        authResponseFuture = new CompletableFuture<>();
        stateHandler = new PushStateHandler(oktaAuthenticationClient, authResponseFuture);
        oktaAuthenticationClient.verifyFactor(deviceId, stateToken, stateHandler);
        authResponse = authResponseFuture.get(45, TimeUnit.SECONDS);
      }
      String factorResult = authResponse.getData().getFactorResult();
      if (!factorResult.equals("SUCCESS")) {
        if (factorResult.equals("TIMEOUT") || factorResult.equals("WAITING")) {
          throw ApiException.newBuilder()
              .withApiErrors(DefaultApiError.OKTA_PUSH_MFA_TIMEOUT)
              .withExceptionMessage(DefaultApiError.OKTA_PUSH_MFA_TIMEOUT.getMessage())
              .build();
        } else if (factorResult.equals("REJECTED")) {
          throw ApiException.newBuilder()
              .withApiErrors(DefaultApiError.OKTA_PUSH_MFA_REJECTED)
              .withExceptionMessage(DefaultApiError.OKTA_PUSH_MFA_REJECTED.getMessage())
              .build();
        }
      }
      return authResponseFuture.get(45, TimeUnit.SECONDS);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw ApiException.newBuilder()
          .withExceptionCause(e)
          .withApiErrors(DefaultApiError.AUTH_RESPONSE_WAIT_FAILED)
          .withExceptionMessage("Failed to trigger challenge due to timeout. Please try again.")
          .build();
    }
  }

  /** Verifies user's MFA factor using Okta Auth SDK. */
  @Override
  public AuthResponse mfaCheck(String stateToken, String deviceId, String otpToken) {

    CompletableFuture<AuthResponse> authResponse = new CompletableFuture<>();
    MfaStateHandler stateHandler = new MfaStateHandler(oktaAuthenticationClient, authResponse);

    DefaultVerifyPassCodeFactorRequest request =
        oktaAuthenticationClient.instantiate(DefaultVerifyPassCodeFactorRequest.class);
    request.setPassCode(otpToken);
    request.setStateToken(stateToken);

    try {
      oktaAuthenticationClient.verifyFactor(deviceId, request, stateHandler);
      return authResponse.get(45, TimeUnit.SECONDS);
    } catch (ApiException e) {
      throw e;
    } catch (FactorValidationException e) {
      throw ApiException.newBuilder()
          .withExceptionCause(e)
          .withApiErrors(DefaultApiError.FACTOR_VALIDATE_FAILED)
          .withExceptionMessage("Failed to validate factor.")
          .build();
    } catch (Exception e) {
      throw ApiException.newBuilder()
          .withExceptionCause(e)
          .withApiErrors(DefaultApiError.AUTH_RESPONSE_WAIT_FAILED)
          .withExceptionMessage("Failed to wait for Okta Auth Completable Future to complete.")
          .build();
    }
  }

  /** Obtains groups user belongs to. */
  @Override
  public Set<String> getGroups(AuthData authData) {

    Preconditions.checkNotNull(authData, "auth data cannot be null.");

    User user = sdkClient.getUser(authData.getUserId());
    GroupList userGroups = user.listGroups();

    final Set<String> groups = new HashSet<>();
    if (userGroups == null) {
      return groups;
    }
    userGroups.forEach(group -> groups.add(group.getProfile().getName()));

    return groups;
  }
}
