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

package com.nike.cerberus.auth.connector.onelogin;

import static com.nike.cerberus.auth.connector.config.OneLoginConfiguration.*;

import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Client for calling OneLogin APIs */
@Component
class OneLoginClient {

  private final String clientId;
  private final String clientSecret;
  private final String subdomain;
  private final OneLoginHttpClient httpClient;

  @Autowired
  public OneLoginClient(
      OneLoginConfigurationProperties oneLoginConfigurationProperties,
      OneLoginHttpClient httpClient) {

    this.clientId = oneLoginConfigurationProperties.getClientId();
    this.clientSecret = oneLoginConfigurationProperties.getClientSecret();
    this.subdomain = oneLoginConfigurationProperties.getSubDomain();
    this.httpClient = httpClient;
  }

  /** Attempt to login a user */
  public CreateSessionLoginTokenResponse createSessionLoginToken(
      final String username, final String password) {
    CreateSessionLoginTokenRequest request =
        new CreateSessionLoginTokenRequest()
            .setUsernameOrEmail(username)
            .setPassword(password)
            .setSubdomain(subdomain);

    return httpClient.execute(
        "api/1/login/auth",
        "POST",
        buildAuthorizationBearerHeader(),
        request,
        CreateSessionLoginTokenResponse.class);
  }

  /** Verify MFA */
  public VerifyFactorResponse verifyFactor(
      final String deviceId, final String stateToken, final String otpToken) {
    VerifyFactorRequest request =
        new VerifyFactorRequest()
            .setDeviceId(deviceId)
            .setStateToken(stateToken)
            .setOtpToken(otpToken);

    return httpClient.execute(
        "api/1/login/verify_factor",
        "POST",
        buildAuthorizationBearerHeader(),
        request,
        VerifyFactorResponse.class);
  }

  /** Get info about a user */
  public GetUserResponse getUserById(long userId) {
    return httpClient.execute(
        "api/1/users/" + userId,
        "GET",
        buildAuthorizationBearerHeader(),
        null,
        GetUserResponse.class);
  }

  /**
   * Builds a map containing the Authorization header with a valid bearer token.
   *
   * @return Map containing the Authorization header and value.
   */
  protected Map<String, String> buildAuthorizationBearerHeader() {
    final Map<String, String> headers = Maps.newHashMap();
    headers.put("Authorization", String.format("bearer:%s", requestAccessToken().getAccessToken()));
    return headers;
  }

  /**
   * Requests an access token using the configured client id and secret.
   *
   * @return Access token
   */
  protected GenerateTokenResponseData requestAccessToken() {

    final GenerateTokenResponse generateTokenResponse =
        httpClient.execute(
            "auth/oauth2/token",
            "POST",
            buildAuthorizationHeader(),
            new GenerateTokenRequest(),
            GenerateTokenResponse.class);

    if (generateTokenResponse.getStatus().isError()) {
      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
          .withExceptionMessage("Failed to generate an access token with OneLogin!")
          .build();
    }

    return generateTokenResponse.getData().get(0);
  }

  /** Used in GenerateTokenRequest */
  protected Map<String, String> buildAuthorizationHeader() {
    final Map<String, String> headers = Maps.newHashMap();
    headers.put(
        "Authorization", String.format("client_id:%s, client_secret:%s", clientId, clientSecret));
    return headers;
  }
}
