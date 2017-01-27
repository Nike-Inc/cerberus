/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nike.cerberus.auth.connector.okta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.sdk.clients.AuthApiClient;
import com.okta.sdk.clients.FactorsApiClient;
import com.okta.sdk.clients.UserApiClient;
import com.okta.sdk.framework.ApiClientConfiguration;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.usergroups.UserGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Helper methods to authenticate with Okta.
 */
public class OktaAuthHelper {

    public static final String AUTHENTICATION_MFA_REQUIRED_STATUS = "MFA_REQUIRED";

    public static final String AUTHENTICATION_MFA_ENROLL_STATUS = "MFA_ENROLL";

    public static final String AUTHENTICATION_SUCCESS_STATUS = "SUCCESS";

    public static final String MFA_FACTOR_NOT_SETUP_STATUS = "NOT_SETUP";

    public static final String MFA_FACTOR_ACTIVE_STATUS = "ACTIVE";

    private static final ImmutableMap<String, String> MFA_FACTOR_NAMES = ImmutableMap.of(
            "google", "Google Authenticator",
            "okta"  , "Okta Verify");

    private final ObjectMapper objectMapper;

    private final AuthApiClient authClient;

    private final UserApiClient userApiClient;

    private final FactorsApiClient factorsApiClient;

    private final String baseUrl;

    public OktaAuthHelper(final AuthApiClient authClient,
                          final UserApiClient userApiClient,
                          final FactorsApiClient factorsApiClient,
                          final ObjectMapper objectMapper,
                          final String baseUrl) {

        this.authClient = authClient;
        this.userApiClient = userApiClient;
        this.objectMapper = objectMapper;
        this.factorsApiClient = factorsApiClient;
        this.baseUrl = baseUrl;
    }

    @Inject
    public OktaAuthHelper(@Named("auth.connector.okta.api_key") final String oktaApiKey,
                          @Named("auth.connector.okta.base_url") final String baseUrl,
                          final ObjectMapper objectMapper) {

        Preconditions.checkArgument(oktaApiKey != null, "okta api key cannot be null");
        Preconditions.checkArgument(baseUrl != null, "okta base url cannot be null");

        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;

        final ApiClientConfiguration clientConfiguration = new ApiClientConfiguration(baseUrl, oktaApiKey);
        this.authClient = new AuthApiClient(clientConfiguration);
        this.userApiClient = new UserApiClient(clientConfiguration);
        this.factorsApiClient = new FactorsApiClient(clientConfiguration);
    }

    /**
     * Request to get user group data by the user's ID.
     *
     * @param userId User ID
     * @return User groups
     */
    protected List<UserGroup> getUserGroups(final String userId) {

        try {
            return this.userApiClient.getUserGroups(userId);
        } catch (IOException ioe) {
            final String msg = String.format("failed to get user groups for user (%s) for reason: %s", userId,
                    ioe.getMessage());

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .withExceptionMessage(msg)
                    .build();
        }
    }

    /**
     * Request for verifying a MFA factor.
     *
     * @param factorId MFA factor id
     * @param stateToken State token
     * @param passCode One Time Passcode from MFA factor
     * @return Session login token
     */
    protected AuthResult verifyFactor(final String factorId,
                                      final String stateToken,
                                      final String passCode) {

        final AuthResult authResult;
        try {
            authResult = this.authClient.authenticateWithFactor(stateToken, factorId, passCode);
        } catch (IOException ioe) {
            final String msg = String.format("stateToken: %s failed to verify 2nd factor for reason: %s",
                    stateToken, ioe.getMessage());

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                    .withExceptionMessage(msg)
                    .build();
        }

        return authResult;
    }

    /**
     * Authenticate a user with Okta
     *
     * @param username   Okta username
     * @param password   Okta password
     * @param relayState Deep link to redirect user to after authentication
     * @return Session login token
     */
    protected AuthResult authenticateUser(final String username, final String password,
                                          final String relayState) {

        try {
            return this.authClient.authenticate(username, password, relayState);
        } catch (IOException ioe) {
            final String msg = String.format("failed to authenticate user (%s) for reason: %s", username,
                    ioe.getMessage());

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                    .withExceptionMessage(msg)
                    .build();        }
    }

    /**
     * Print a user-friendly name for a MFA device
     * @param factor  Okta MFA factor
     * @return Device name
     */
    protected String getDeviceName(final Factor factor) {

        Preconditions.checkArgument(factor != null, "factor cannot be null.");

        final String factorProvider = factor.getProvider().toLowerCase();
        if (MFA_FACTOR_NAMES.containsKey(factorProvider)) {
            return MFA_FACTOR_NAMES.get(factorProvider);
        }

        return WordUtils.capitalizeFully(factorProvider);
    }

    /**
     * Get list of enrolled MFA factors for a user
     * @param userId  Okta user ID
     * @return List of factors
     */
    protected List<Factor> getFactorsByUserId(final String userId) {

        Preconditions.checkArgument(userId != null, "user id cannot be null.");

        try {
            return this.factorsApiClient.getUserLifecycleFactors(userId);
        } catch (IOException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionCause(e)
                    .withExceptionMessage("Error parsing the embedded auth data from Okta.")
                    .build();
        }
    }

    /**
     * Get list of enrolled MFA factors for a user from their authentication result
     * @param authResult  Okta response object after successful authentication
     * @return List of factors
     */
    protected List<Factor> getUserFactorsFromAuthResult(final AuthResult authResult) {

        final EmbeddedAuthResponseDataV1 embeddedAuthData = this.getEmbeddedAuthData(authResult);

        if (embeddedAuthData != null && embeddedAuthData.getFactors() != null) {
            return embeddedAuthData.getFactors();
        } else {

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionMessage("Could not parse user factors from Okta response.")
                    .build();
        }
    }

    /**
     * Get user id from authentication result
     * @param authResult Okta response object after successful authentication
     * @return The u
     * ser ID
     */
    protected String getUserIdFromAuthResult(final AuthResult authResult) {
        final EmbeddedAuthResponseDataV1 embeddedAuthData = this.getEmbeddedAuthData(authResult);

        if (embeddedAuthData.getUser() != null) {
            return embeddedAuthData.getUser().getId();
        } else {

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionMessage("Could not parse user data from Okta response.")
                    .build();
        }
    }

    /**
     * Get username from authentication result
     * @param authResult Okta response object after successful authentication
     * @return The username
     */
    String getUserLoginFromAuthResult(final AuthResult authResult) {
        final EmbeddedAuthResponseDataV1 embeddedAuthData = this.getEmbeddedAuthData(authResult);

        try {
            return embeddedAuthData.getUser().getProfile().getLogin();
        } catch(NullPointerException npe) {

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionMessage("Could not parse user data from Okta response.")
                    .build();
        }
    }

    /**
     * Ensure the user has at least one active MFA device set up.
     * @param factors - List of user factors
     */
    protected void validateUserFactors(final List<Factor> factors) {

       if(factors == null || factors.isEmpty() || factors.stream()
               .allMatch(factor -> StringUtils.equals(factor.getStatus(), MFA_FACTOR_NOT_SETUP_STATUS)))
       {

           throw ApiException.newBuilder()
                   .withApiErrors(new ApiErrorBase(
                           DefaultApiError.MFA_SETUP_REQUIRED.getName(),
                           DefaultApiError.MFA_SETUP_REQUIRED.getErrorCode(),
                           "MFA is required, but user has not set up any devices in Okta.\n" +
                                   "Please set up a MFA device in Okta: " + this.baseUrl,
                           DefaultApiError.MFA_SETUP_REQUIRED.getHttpStatusCode()))
                   .withExceptionMessage("MFA is required, but user has not set up any devices in Okta.")
                   .build();
       }
    }

    /**
     * Convenience method for parsing the Okta response and mapping it to a class.
     *
     * @param authResult  The Okta authentication result object
     * @return Deserialized object from the response body
     */
    private EmbeddedAuthResponseDataV1 getEmbeddedAuthData(final AuthResult authResult) {

        Preconditions.checkArgument(authResult != null, "auth result cannot be null.");

        final Map<String, Object> embedded = authResult.getEmbedded();

        try {
            final String embeddedJson = objectMapper.writeValueAsString(embedded);
            return objectMapper.readValue(embeddedJson, EmbeddedAuthResponseDataV1.class);
        } catch (IOException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionCause(e)
                    .withExceptionMessage("Error parsing the embedded auth data from Okta.")
                    .build();
        }
    }

}
