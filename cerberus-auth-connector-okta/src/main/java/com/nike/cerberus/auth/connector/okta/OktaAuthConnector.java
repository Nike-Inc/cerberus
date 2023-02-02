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
import com.google.common.collect.ImmutableMap;
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
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import com.okta.jwt.JwtVerifiers;
import com.okta.sdk.authc.credentials.TokenClientCredentials;
import com.okta.sdk.client.Client;
import com.okta.sdk.client.Clients;
import com.okta.sdk.resource.ResourceException;
import com.okta.sdk.resource.group.Group;
import com.okta.sdk.resource.group.GroupList;
import com.okta.sdk.resource.group.GroupProfile;
import com.okta.sdk.resource.user.User;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Okta version 1 API implementation of the AuthConnector interface. */
@Component
public class OktaAuthConnector implements AuthConnector {

  private final AuthenticationClient oktaAuthenticationClient;

  private final Client sdkClient;

  private final String jwtIssuer;

  private final String jwtAudience;

  protected AccessTokenVerifier jwtVerifier;

  @Autowired
  public OktaAuthConnector(
      AuthenticationClient oktaAuthenticationClient,
      OktaConfigurationProperties oktaConfigurationProperties,
      @Value("${cerberus.auth.jwt.issuer}") String jwtIssuer,
      @Value("${cerberus.auth.jwt.audience}") String jwtAudience) {
    this.oktaAuthenticationClient = oktaAuthenticationClient;
    this.sdkClient = getSdkClient(oktaConfigurationProperties);
    this.jwtIssuer = jwtIssuer;
    this.jwtAudience = jwtAudience;
  }

  /** Alternate constructor to facilitate unit testing */
  public OktaAuthConnector(
      AuthenticationClient oktaAuthenticationClient,
      Client sdkClient,
      String jwtIssuer,
      String jwtAudience,
      AccessTokenVerifier jwtVerifier) {
    this.oktaAuthenticationClient = oktaAuthenticationClient;
    this.sdkClient = sdkClient;
    this.jwtIssuer = jwtIssuer;
    this.jwtAudience = jwtAudience;
    this.jwtVerifier = jwtVerifier;
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

  /**
   * Get a valid user from the identity provider if possible
   *
   * @param userId
   * @return User corresponding to the id
   * @throws ApiException if user cannot be resolved
   */
  protected User getUserFromIDP(String userId) {
    try {
      return sdkClient.getUser(userId);
    } catch (IllegalStateException ise) {
      throw ApiException.newBuilder()
          .withExceptionCause(ise)
          .withApiErrors(DefaultApiError.IDENTITY_PROVIDER_BAD_GATEWAY)
          .withExceptionMessage("Could not communicate properly with identity provider")
          .build();
    } catch (ResourceException rexc) {
      String msg =
          String.format("Got invalid response from identity providers: %s", rexc.getMessage());
      throw ApiException.newBuilder()
          .withExceptionCause(rexc)
          .withApiErrors(DefaultApiError.IDENTITY_PROVIDER_BAD_GATEWAY)
          .withExceptionMessage(msg)
          .build();
    } catch (Exception exc) {
      throw ApiException.newBuilder()
          .withExceptionCause(exc)
          .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
          .withExceptionMessage("Unknown error trying to getUser from identity provider")
          .build();
    }
  }

  /** Obtains groups user belongs to. */
  @Override
  public Set<String> getGroups(AuthData authData) {

    Preconditions.checkNotNull(authData, "auth data cannot be null.");

    String userId = authData.getUserId();
    User user = getUserFromIDP(userId);
    GroupList userGroups = user.listGroups();

    final Set<String> groups = new HashSet<>();
    if (userGroups == null) {
      return groups;
    }

    for (Group group : userGroups) {
      GroupProfile profile = group.getProfile();
      if (profile != null) {
        groups.add(profile.getName());
      }
    }

    return groups;
  }

  /**
   * Validates a JWT and retunrs the subject and userId in a map
   *
   * @param jwtString String jwt access token
   * @return Map of username and userId
   * @throws ApiException if JWT cannot be verified
   */
  @Override
  public Map<String, String> getValidatedUserPrincipal(String jwtString) {
    try {
      Jwt jwt = this.getAccessTokenVerifier().decode(jwtString);
      Map<String, Object> claims = jwt.getClaims();

      String username = claims.getOrDefault("sub", "").toString();
      String userId = claims.getOrDefault("uid", "").toString();

      if (username.isEmpty() || userId.isEmpty()) {
        throw new JwtVerificationException("sub and uid claims are required");
      }

      Map<String, String> principalInfoMap =
          ImmutableMap.of("username", username, "userId", userId);
      return principalInfoMap;
    } catch (JwtVerificationException jve) {
      throw this.buildJwtVerificationApiException(jve, "Failed to verify JWT access token");
    }
  }

  /**
   * Convert JwtVerificationException to ApiException
   *
   * @param jve JwtVerificationException
   * @param msg Message
   * @return ApiException
   */
  private ApiException buildJwtVerificationApiException(JwtVerificationException jve, String msg) {
    ApiException exc =
        ApiException.Builder.newBuilder()
            .withApiErrors(DefaultApiError.BEARER_TOKEN_INVALID)
            .withExceptionMessage(msg)
            .withExceptionCause(jve)
            .build();
    return exc;
  }

  /**
   * Creates an access token verifier with the configured issuer and audience
   *
   * @return AccessTokenVerifier
   */
  protected AccessTokenVerifier getAccessTokenVerifier() {
    if (this.jwtVerifier == null) {
      this.jwtVerifier =
          JwtVerifiers.accessTokenVerifierBuilder()
              .setIssuer(this.jwtIssuer)
              .setAudience(this.jwtAudience)
              .build();
    }
    return this.jwtVerifier;
  }
}
