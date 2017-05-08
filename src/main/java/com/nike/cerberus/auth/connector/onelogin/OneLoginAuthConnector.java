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

package com.nike.cerberus.auth.connector.onelogin;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.*;
import com.nike.cerberus.error.DefaultApiError;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * OneLogin version 1 API implementation of the AuthConnector interface.
 */
public class OneLoginAuthConnector implements AuthConnector {

    private final OneLoginClient oneLoginClient;

    @Inject
    public OneLoginAuthConnector(final OneLoginClient oneLoginClient) {
        this.oneLoginClient = oneLoginClient;
    }

    @Override
    public AuthResponse authenticate(String username, String password) {
        final SessionLoginTokenData sessionLoginToken = createSessionLoginToken(username, password);
        final AuthData authData = new AuthData();
        final AuthResponse authResponse = new AuthResponse().setData(authData);

        if (StringUtils.isNotBlank(sessionLoginToken.getStateToken())) {
            authResponse.setStatus(AuthStatus.MFA_REQUIRED);
            authData.setStateToken(sessionLoginToken.getStateToken());

            sessionLoginToken.getDevices().forEach(d -> authData.getDevices().add(new AuthMfaDevice()
                    .setId(String.valueOf(d.getDeviceId()))
                    .setName(d.getDeviceType())));
        } else {
            authResponse.setStatus(AuthStatus.SUCCESS);
        }

        authData.setUserId(String.valueOf(sessionLoginToken.getUser().getId()));
        authData.setUsername(sessionLoginToken.getUser().getUsername());

        return authResponse;
    }

    @Override
    public AuthResponse mfaCheck(String stateToken, String deviceId, String otpToken) {
        final SessionLoginTokenData sessionLoginToken = verifyFactor(deviceId, stateToken, otpToken);
        final AuthData authData = new AuthData();
        final AuthResponse authResponse = new AuthResponse().setData(authData);

        authResponse.setStatus(AuthStatus.SUCCESS);
        authData.setUserId(String.valueOf(sessionLoginToken.getUser().getId()));
        authData.setUsername(sessionLoginToken.getUser().getUsername());

        return authResponse;
    }

    @Override
    public Set<String> getGroups(AuthData data) {
        final UserData userData = getUserById(Long.parseLong(data.getUserId()));
        return parseLdapGroups(userData.getMemberOf());
    }

    /**
     * Takes the list of ldapGroups received from OneLogin and parses them in to a set of Strings
     *
     * @param ldapGroups A string consisting of ldap groups received from OneLogin
     * @return A set of Strings consisting of the ldap groups that were parsed from the provided string
     */
    protected Set<String> parseLdapGroups(final String ldapGroups) {
        Set<String> groups = new HashSet<>();
        if (ldapGroups == null) {
            return groups;
        }

        // One Login double xml escapes entries
        String escapedLdapGroups = StringEscapeUtils.unescapeXml(StringEscapeUtils.unescapeXml(ldapGroups));

        Iterable<String> canonicalNameIterable;
        Iterable<String> piecesIterable;
        Iterable<String> canonicalNames = Splitter.on(";").split(escapedLdapGroups);
        for (String canonicalName : canonicalNames) {
            canonicalNameIterable = Splitter.on(",").split(canonicalName);
            String[] pieces = Iterables.toArray(canonicalNameIterable, String.class);

            piecesIterable = Splitter.on("=").split(pieces[0]);
            String[] parts = Iterables.toArray(piecesIterable, String.class);
            if (parts.length >= 2) {
                groups.add(parts[1]);
            } else {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                        .withExceptionMessage("OneLogin user info member-of field is malformed!")
                        .build();
            }
        }

        return groups;
    }

    /**
     * Request for getting all user data by their ID.
     *
     * @param userId User ID
     * @return User data
     */
    protected UserData getUserById(final long userId) {

        final GetUserResponse getUserResponse = oneLoginClient.getUserById(userId);

        if (getUserResponse.getStatus().isError()) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.SERVICE_UNAVAILABLE)
                    .withExceptionMessage(getUserResponse.getStatus().getMessage())
                    .build();
        }

        return getUserResponse.getData().get(0);
    }

    /**
     * Request for verifying a MFA factor.
     *
     * @param deviceId   MFA device id
     * @param stateToken State token
     * @param otpToken   Token from MFA device
     * @return Session login token
     */
    protected SessionLoginTokenData verifyFactor(final String deviceId,
                                                 final String stateToken,
                                                 final String otpToken) {

        final VerifyFactorResponse verifyFactorResponse = oneLoginClient.verifyFactor(deviceId, stateToken, otpToken);

        if (verifyFactorResponse.getStatus().isError()) {
            String msg = String.format("stateToken: %s failed to verify 2nd factor for reason: %s",
                    stateToken, verifyFactorResponse.getStatus().getMessage());

            if (verifyFactorResponse.getStatus().getCode() == 401) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                        .withExceptionMessage(msg)
                        .build();
            } else {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                        .withExceptionMessage(msg)
                        .build();
            }
        }

        return verifyFactorResponse.getData().get(0);
    }

    /**
     * Request for creating a session login token.  This is used to validate a user's credentials with OneLogin.
     *
     * @param username OneLogin username
     * @param password OneLogin password
     * @return Session login token
     */
    protected SessionLoginTokenData createSessionLoginToken(final String username, final String password) {

        final CreateSessionLoginTokenResponse createSessionLoginTokenResponse = oneLoginClient.createSessionLoginToken(username, password);

        long statusCode = createSessionLoginTokenResponse.getStatus().getCode();

        if (createSessionLoginTokenResponse.getStatus().isError()) {
            String msg = String.format("The user %s failed to authenticate for reason: %s", username, createSessionLoginTokenResponse.getStatus().getMessage());
            if (statusCode == 400 &&
                    StringUtils.startsWithIgnoreCase(createSessionLoginTokenResponse.getStatus().getMessage(), "MFA")) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.MFA_SETUP_REQUIRED)
                        .withExceptionMessage(msg)
                        .build();
            } else if (statusCode == 400 || statusCode == 401) {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS)
                        .withExceptionMessage(msg)
                        .build();
            } else {
                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                        .withExceptionMessage(msg)
                        .build();
            }
        }

        return createSessionLoginTokenResponse.getData().get(0);
    }


}
