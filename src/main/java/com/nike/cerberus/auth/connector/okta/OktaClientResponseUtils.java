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
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import javax.inject.Named;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OktaClientResponseUtils {

    public static final String AUTHENTICATION_MFA_REQUIRED_STATUS = "MFA_REQUIRED";

    public static final String AUTHENTICATION_MFA_ENROLL_STATUS = "MFA_ENROLL";

    public static final String AUTHENTICATION_SUCCESS_STATUS = "SUCCESS";

    public static final String MFA_FACTOR_NOT_SETUP_STATUS = "NOT_SETUP";

    private static final ImmutableMap<String, String> MFA_FACTOR_NAMES = ImmutableMap.of(
            "google", "Google Authenticator",
            "okta"  , "Okta Verify");

    private final ObjectMapper objectMapper;

    private final String baseUrl;

    @Inject
    public OktaClientResponseUtils(final ObjectMapper objectMapper,
                                   @Named("auth.connector.okta.base_url") final String baseUrl) {

        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
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
     * Get list of enrolled MFA factors for a user from their authentication result
     * @param authResult  Okta response object after successful authentication
     * @return List of factors
     */
    protected List<Factor> getUserFactorsFromAuthResult(final AuthResult authResult) {

        final EmbeddedAuthResponseDataV1 embeddedAuthData = getEmbeddedAuthData(authResult);

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
        final EmbeddedAuthResponseDataV1 embeddedAuthData = getEmbeddedAuthData(authResult);

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
        final EmbeddedAuthResponseDataV1 embeddedAuthData = getEmbeddedAuthData(authResult);

        if (embeddedAuthData == null ||
                embeddedAuthData.getUser() == null ||
                embeddedAuthData.getUser().getProfile() == null ||
                embeddedAuthData.getUser().getProfile().getLogin() == null) {

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.INTERNAL_SERVER_ERROR)
                    .withExceptionMessage("Could not parse user data from Okta response.")
                    .build();
        }

        return embeddedAuthData.getUser().getProfile().getLogin();
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
                                    "Please set up a MFA device in Okta: " + baseUrl,
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
