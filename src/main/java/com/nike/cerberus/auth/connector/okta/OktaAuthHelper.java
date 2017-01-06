/*
 * Copyright (c) 2016 Nike, Inc.
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
 */

package com.nike.cerberus.auth.connector.okta;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.sdk.clients.AuthApiClient;
import com.okta.sdk.clients.UserApiClient;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.usergroups.UserGroup;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Helper methods to authenticate with Okta.
 */
public class OktaAuthHelper {

    public static final String AUTHENTICATION_MFA_REQUIRED_STATUS = "MFA_REQUIRED";

    public static final String AUTHENTICATION_SUCCESS_STATUS = "SUCCESS";

    private final ObjectMapper objectMapper;

    private final AuthApiClient authClient;

    private final UserApiClient userApiClient;

    @Inject
    public OktaAuthHelper(final AuthApiClient authApiClient, final UserApiClient userApiClient) {

        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.authClient = authApiClient;
        this.userApiClient = userApiClient;
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
            final String msg = String.format("failed to get user groups for reason: %s", ioe.getMessage());

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
            final String msg = String.format("failed to get user groups for reason: %s", ioe.getMessage());

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .withExceptionMessage(msg)
                    .build();        }
    }

    /**
     * Convenience method for parsing the Okta response and mapping it to a class.
     *
     * @param authResult  The Okta authentication result object
     * @return Deserialized object from the response body
     */
    protected EmbeddedAuthResponseDataV1 getEmbeddedAuthData(final AuthResult authResult) {

        Preconditions.checkArgument(authResult != null, "auth result cannot be null.");

        final Map<String, Object> embedded = authResult.getEmbedded();

        try {
            final String embeddedJson = objectMapper.writeValueAsString(embedded);
            return objectMapper.readValue(embeddedJson, EmbeddedAuthResponseDataV1.class);
        } catch (IOException e) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionCause(e)
                    .withExceptionMessage("Error parsing the response body from OneLogin.")
                    .build();
        }
    }

    /**
     * Print a user-friendly name for a MFA device
     * @param factor - Okta MFA factor
     * @return Device name
     */
    protected String getDeviceName(final Factor factor) {

        Preconditions.checkArgument(factor != null, "factor cannot be null.");

        final String factorProvider = factor.getProvider();
        if (StringUtils.equalsIgnoreCase(factorProvider, "Google")) {
            return "Google Authenticator";
        }

        return StringUtils.capitalize(factorProvider.toLowerCase());
    }
}
