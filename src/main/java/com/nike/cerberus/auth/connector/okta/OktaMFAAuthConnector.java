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

import com.google.common.base.Preconditions;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthMfaDevice;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.usergroups.UserGroup;
import com.okta.sdk.models.users.User;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Okta Version 1 API implementation of the AuthConnector interface, requires MFA login for all users.
 */
public class OktaMFAAuthConnector implements AuthConnector {

    private final OktaApiClientHelper oktaApiClientHelper;

    private final OktaClientResponseUtils oktaClientResponseUtils;

    private static final String NO_MFA_REQUIRED_IN_OKTA_STATE_TOKEN_PREFIX = "userId:";

    @Inject
    public OktaMFAAuthConnector(final OktaApiClientHelper oktaApiClientHelper,
                                final OktaClientResponseUtils oktaClientResponseUtils) {

        this.oktaApiClientHelper = oktaApiClientHelper;
        this.oktaClientResponseUtils = oktaClientResponseUtils;
    }

    @Override
    public AuthResponse authenticate(String username, String password) {

        final AuthResult authResult = oktaApiClientHelper.authenticateUser(username, password, null);
        final String userId = oktaClientResponseUtils.getUserIdFromAuthResult(authResult);
        final String userLogin = oktaClientResponseUtils.getUserLoginFromAuthResult(authResult);

        final AuthData authData = new AuthData().setUserId(userId).setUsername(userLogin);
        final AuthResponse authResponse = new AuthResponse().setData(authData).setStatus(AuthStatus.MFA_REQUIRED);

        final List<Factor> factors;

        if (StringUtils.equals(authResult.getStatus(), OktaClientResponseUtils.AUTHENTICATION_MFA_REQUIRED_STATUS)
                || StringUtils.equals(authResult.getStatus(), OktaClientResponseUtils.AUTHENTICATION_MFA_ENROLL_STATUS)) {
            factors = oktaClientResponseUtils.getUserFactorsFromAuthResult(authResult);
            authData.setStateToken(authResult.getStateToken());
        } else {
            factors = oktaApiClientHelper.getFactorsByUserId(userId);
        }

        oktaClientResponseUtils.validateUserFactors(factors);

        factors.forEach(factor -> authData.getDevices().add(new AuthMfaDevice()
                .setId(factor.getId())
                .setName(oktaClientResponseUtils.getDeviceName(factor))));

        return authResponse;
    }

    @Override
    public AuthResponse mfaCheck(String stateToken, String deviceId, String otpToken) {

        final AuthResponse authResponse;

        if (StringUtils.startsWith(stateToken, NO_MFA_REQUIRED_IN_OKTA_STATE_TOKEN_PREFIX)) {
            return mfaCheckMfaNotRequiredForUserInOkta(stateToken, deviceId, otpToken);
        } else {

            final AuthResult authResult = oktaApiClientHelper.verifyFactor(deviceId, stateToken, otpToken);
            final String userId = oktaClientResponseUtils.getUserIdFromAuthResult(authResult);
            final String userLogin = oktaClientResponseUtils.getUserLoginFromAuthResult(authResult);

            final AuthData authData = new AuthData()
                    .setUserId(userId)
                    .setUsername(userLogin);
            authResponse = new AuthResponse()
                    .setData(authData)
                    .setStatus(AuthStatus.SUCCESS);

        }

        return authResponse;
    }

    /**
     * Verifies a user's MFA factor without a state token (when MFA is not required for the given user in Okta).
     *
     * This is necessary because a state token is required to make the 'authenticate with factor' Okta API call.
     * Since state tokens are only returned from the 'authenticate' API call when MFA is required for a user
     * in Okta, then multiple other API calls are needed to verify MFA when Okta does not require it.
     *
     * @param stateToken - State token (should be the user's id in format: 'userId:[id]')
     * @param deviceId - ID of the multi-factor device
     * @param otpToken - Passcode fo the multi-factor device
     * @return - The auth response
     */
    protected AuthResponse mfaCheckMfaNotRequiredForUserInOkta(String stateToken, String deviceId, String otpToken) {

        final String userId = StringUtils.removeStart(stateToken, NO_MFA_REQUIRED_IN_OKTA_STATE_TOKEN_PREFIX);
        final User user = oktaApiClientHelper.verifyFactorMfaNotRequiredForUserInOkta(deviceId, userId, otpToken);

        final AuthData authData = new AuthData();
        authData.setUserId(user.getId());
        authData.setUsername(user.getProfile().getLogin());

        final AuthResponse authResponse = new AuthResponse();
        authResponse.setData(authData);
        authResponse.setStatus(AuthStatus.SUCCESS);

        return authResponse;
    }

    @Override
    public Set<String> getGroups(AuthData authData) {

        Preconditions.checkNotNull(authData, "auth data cannot be null.");

        final List<UserGroup> userGroups = oktaApiClientHelper.getUserGroups(authData.getUserId());

        final Set<String> groups = new HashSet<>();
        if (userGroups == null) {
            return groups;
        }

        userGroups.forEach(group -> groups.add(group.getProfile().getName()));

        return groups;
    }

}
