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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.util.CustomApiError;
import com.okta.authn.sdk.AuthenticationStateHandlerAdapter;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.authn.sdk.resource.FactorProvider;
import com.okta.authn.sdk.resource.FactorType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

/**
 * Abstract state handler to provide helper methods for authentication and MFA validation. Also
 * handles unknown states.
 */
public abstract class AbstractOktaStateHandler extends AuthenticationStateHandlerAdapter {

  public static final String MFA_FACTOR_NOT_SETUP_STATUS = "NOT_SETUP";

  private static final Map<String, String> MFA_FACTOR_NAMES =
      ImmutableMap.of(
          "google-token:software:totp", "Google Authenticator",
          "okta-token:software:totp", "Okta Verify TOTP",
          "okta-push", "Okta Verify Push",
          "okta-call", "Okta Voice Call",
          "okta-sms", "Okta Text Message Code");

  private static final Map<String, Boolean> MFA_FACTOR_TRIGGER_REQUIRED =
      ImmutableMap.of(
          "google-token:software:totp", false,
          "okta-token:software:totp", false,
          "okta-push", false,
          "okta-call", true,
          "okta-sms", true);

  private static final Map<String, String> STATUS_ERRORS =
      new ImmutableMap.Builder<String, String>()
          .put("UNAUTHENTICATED", "User is not authenticated. Please confirm credentials.")
          .put("PASSWORD_WARN", "Password is about to expire and should be changed.")
          .put("PASSWORD_EXPIRED", "Password has expired. Please update your password.")
          .put(
              "RECOVERY",
              "Please check for a recovery token to reset your password or unlock your account.")
          .put("RECOVERY_CHALLENGE", "Please verify the factor-specific recovery challenge.")
          .put("PASSWORD_RESET", "Please set a new password.")
          .put("LOCKED_OUT", "Your OKTA user account is locked.")
          .put("MFA_ENROLL_ACTIVATE", "Please activate your factor to complete enrollment.")
          .build();

  public final AuthenticationClient client;
  public final CompletableFuture<AuthResponse> authenticationResponseFuture;

  public AbstractOktaStateHandler(
      AuthenticationClient client, CompletableFuture<AuthResponse> authenticationResponseFuture) {
    this.client = client;
    this.authenticationResponseFuture = authenticationResponseFuture;
  }

  /**
   * Combine the provider and factor type to create factor key
   *
   * @param factor Okta MFA factor
   * @return factor key
   */
  public String getFactorKey(Factor factor) {

    final String factorProvider = factor.getProvider().toString().toLowerCase();
    final String factorType = factor.getType().toString().toLowerCase();

    return factorProvider + "-" + factorType;
  }

  /**
   * Print a user-friendly name for a MFA device
   *
   * @param factor Okta MFA factor
   * @return Device name
   */
  public String getDeviceName(final Factor factor) {

    Preconditions.checkArgument(factor != null, "Factor cannot be null.");

    final String factorKey = getFactorKey(factor);

    if (MFA_FACTOR_NAMES.containsKey(factorKey)) {
      return MFA_FACTOR_NAMES.get(factorKey);
    }
    return WordUtils.capitalizeFully(factorKey);
  }

  /**
   * Determines whether a trigger is required for a provided MFA factor
   *
   * @param factor Okta MFA factor
   * @return boolean trigger required
   */
  public boolean isTriggerRequired(Factor factor) {

    Preconditions.checkArgument(factor != null, "Factor cannot be null.");

    final String factorKey = getFactorKey(factor);

    if (MFA_FACTOR_TRIGGER_REQUIRED.containsKey(factorKey)) {
      Boolean isRequired = MFA_FACTOR_TRIGGER_REQUIRED.get(factorKey);
      if (isRequired != null) return isRequired.booleanValue();
      else return false;
    }
    return false;
  }

  /**
   * Determines whether a trigger is required for a provided MFA factor
   *
   * @param factor Okta MFA factor
   * @return boolean trigger required
   */
  public boolean isPush(Factor factor) {

    final FactorType type = factor.getType();
    final FactorProvider provider = factor.getProvider();

    return (provider.equals(FactorProvider.OKTA) && type == FactorType.PUSH);
  }

  /**
   * Ensure the user has at least one active MFA device set up
   *
   * @param factors - List of user factors
   */
  public void validateUserFactors(final List<Factor> factors) {

    if (factors == null
        || factors.isEmpty()
        || factors.stream()
            .allMatch(
                factor -> StringUtils.equals(factor.getStatus(), MFA_FACTOR_NOT_SETUP_STATUS))) {

      throw ApiException.newBuilder()
          .withApiErrors(DefaultApiError.MFA_SETUP_REQUIRED)
          .withExceptionMessage("MFA is required, but user has not set up any devices in Okta.")
          .build();
    }
  }

  /**
   * Handles authentication success.
   *
   * @param successResponse - Authentication response from the Completable Future
   */
  @Override
  public void handleSuccess(AuthenticationResponse successResponse) {

    final String userId = successResponse.getUser().getId();
    final String userLogin = successResponse.getUser().getLogin();

    final AuthData authData = AuthData.builder().userId(userId).username(userLogin).build();
    AuthResponse authResponse =
        AuthResponse.builder().data(authData).status(AuthStatus.SUCCESS).build();

    authenticationResponseFuture.complete(authResponse);
  }

  /**
   * Handles all unknown states that are not specifically dealt with by the other state handlers and
   * reports a relevant API Error for the state
   *
   * @param typedUnknownResponse - Authentication response from the Completable Future
   */
  public void handleUnknown(AuthenticationResponse typedUnknownResponse) {

    String status = typedUnknownResponse.getStatusString();
    String message =
        "MFA is required. Please confirm that you are enrolled in a supported MFA device.";
    if (STATUS_ERRORS.containsKey(status)) {
      message = STATUS_ERRORS.get(status);
    }

    throw ApiException.newBuilder()
        .withApiErrors(CustomApiError.createCustomApiError(DefaultApiError.AUTH_FAILED, message))
        .withExceptionMessage(message)
        .build();
  }
}
