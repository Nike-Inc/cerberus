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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.authn.sdk.resource.Factor;
import com.okta.sdk.resource.user.factor.FactorProvider;
import com.okta.sdk.resource.user.factor.FactorType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import javax.inject.Named;
import java.util.List;


public class OktaClientResponseUtils {

    public static final String AUTHENTICATION_MFA_REQUIRED_STATUS = "MFA_REQUIRED";

    public static final String AUTHENTICATION_MFA_ENROLL_STATUS = "MFA_ENROLL";

    public static final String AUTHENTICATION_SUCCESS_STATUS = "SUCCESS";

    public static final String MFA_FACTOR_NOT_SETUP_STATUS = "NOT_SETUP";

    private static final ImmutableMap<String, String> MFA_FACTOR_NAMES = ImmutableMap.of(
            "google-token:software:totp",   "Google Authenticator",
            "okta-token:software:totp",     "Okta Verify TOTP",
            "okta-push",                    "Okta Verify Push",
            "okta-call",                    "Okta Voice Call",
            "okta-sms",                     "Okta Text Message Code");

    private static final ImmutableSet UNSUPPORTED_OKTA_MFA_TYPES = ImmutableSet.of(FactorType.PUSH, FactorType.CALL, FactorType.SMS);

    private final String baseUrl;

    @Inject
    public OktaClientResponseUtils(@Named("auth.connector.okta.base_url") final String baseUrl) {

        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.baseUrl = baseUrl;
    }

    /**
     * Combine the provider and factor type to create factor key
     * @param factor Okta MFA factor
     * @return factor key
     */
    protected String getFactorKey(Factor factor) {

        final String factorProvider = factor.getProvider().toString().toLowerCase();
        final String factorType = factor.getType().toString().toLowerCase();

        return factorProvider + "-" + factorType;
    }

    /**
     * Print a user-friendly name for a MFA device
     * @param factor  Okta MFA factor
     * @return Device name
     */
    protected String getDeviceName(final Factor factor) {

        Preconditions.checkArgument(factor != null, "factor cannot be null.");

        final String factorKey = getFactorKey(factor);

        if (MFA_FACTOR_NAMES.containsKey(factorKey)) {
            return MFA_FACTOR_NAMES.get(factorKey);
        }

        return WordUtils.capitalizeFully(factorKey);
    }

    /**
     * Determines if a MFA factor is currently supported by Cerberus or not
     * @param factor Okta MFA factor
     * @return boolean
     */
    protected boolean isSupportedFactor(Factor factor) {

        final FactorType type = factor.getType();
        final FactorProvider provider = factor.getProvider();

        return ! (provider.equals(FactorProvider.OKTA) &&
                UNSUPPORTED_OKTA_MFA_TYPES.contains(type));
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
                            "MFA is required. Please set up a supported device, either Okta Verify or Google Authenticator. " + baseUrl,
                            DefaultApiError.MFA_SETUP_REQUIRED.getHttpStatusCode()))
                    .withExceptionMessage("MFA is required, but user has not set up any devices in Okta.")
                    .build();
        }
    }

}
