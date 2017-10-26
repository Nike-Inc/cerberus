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

        final AuthResult authResult = oktaApiClientHelper.verifyFactor(deviceId, stateToken, otpToken);
        final String userId = oktaClientResponseUtils.getUserIdFromAuthResult(authResult);
        final String userLogin = oktaClientResponseUtils.getUserLoginFromAuthResult(authResult);

        final AuthData authData = new AuthData()
                .setUserId(userId)
                .setUsername(userLogin);
        final AuthResponse authResponse = new AuthResponse()
                .setData(authData)
                .setStatus(AuthStatus.SUCCESS);

        return authResponse;
    }

    @Override
    public Set<String> getGroups(AuthData authData) {

        Preconditions.checkNotNull(authData, "auth data cannot be null.");

        final List<UserGroup> userGroups = oktaApiClientHelper.getUserGroups(authData.getUserId());

        final Set<String> groups = new HashSet<>();
        if (userGroups.isEmpty()) {
            return groups;
        }

        userGroups.forEach(group -> groups.add(group.getProfile().getName()));

        return groups;
    }

}
