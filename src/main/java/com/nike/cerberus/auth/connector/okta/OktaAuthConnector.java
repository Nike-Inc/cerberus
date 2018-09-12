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
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthConnector;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthMfaDevice;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.InitialLoginStateHandler;
import com.nike.cerberus.auth.connector.okta.statehandlers.MfaStateHandler;
import com.nike.cerberus.error.DefaultApiError;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.client.AuthenticationClients;
import com.okta.authn.sdk.impl.resource.DefaultVerifyPassCodeFactorRequest;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.authn.sdk.resource.Factor;
import com.okta.sdk.models.usergroups.UserGroup;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Okta version 1 API implementation of the AuthConnector interface.
 */
public class OktaAuthConnector implements AuthConnector {

    private final OktaApiClientHelper oktaApiClientHelper;

    private final OktaClientResponseUtils oktaClientResponseUtils;

    private final AuthenticationClient client;

    @Inject
    public OktaAuthConnector(final OktaApiClientHelper oktaApiClientHelper,
                             final OktaClientResponseUtils oktaClientResponseUtils,
                             @Named("auth.connector.okta.base_url") String oktaUrl,
                             @Named("auth.connector.okta.api_key") String oktaApiKey) {

        System.setProperty("okta.client.token", oktaApiKey);

        client = AuthenticationClients.builder()
                .setOrgUrl(oktaUrl)
                .build();

        this.oktaApiClientHelper = oktaApiClientHelper;
        this.oktaClientResponseUtils = oktaClientResponseUtils;
    }

//    For testing
    public OktaAuthConnector(final OktaApiClientHelper oktaApiClientHelper,
                             final OktaClientResponseUtils oktaClientResponseUtils,
                             final AuthenticationClient client) {

        this.oktaApiClientHelper = oktaApiClientHelper;
        this.oktaClientResponseUtils = oktaClientResponseUtils;
        this.client = client;
    }

    @Override
    public AuthResponse authenticate(String username, String password) {

        CompletableFuture<AuthenticationResponse> authenticationResponseCompletableFuture = new CompletableFuture<>();
        InitialLoginStateHandler stateHandler = new InitialLoginStateHandler(authenticationResponseCompletableFuture);

        AuthenticationResponse oktaAuthResponse;
        try {
            client.authenticate(username, password.toCharArray(), null, stateHandler);
            oktaAuthResponse = authenticationResponseCompletableFuture.get(45, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw ApiException.newBuilder()
                    .withExceptionCause(e)
                    .withApiErrors(DefaultApiError.LOGIN_FAILED)
                    .withExceptionMessage("Failed to login or failed to wait for Okta Auth Completeable Future to complete.")
                    .build();
        }

        final String userId = oktaAuthResponse.getUser().getId();
        final String userLogin = oktaAuthResponse.getUser().getLogin();

        final AuthData authData = new AuthData()
                .setUserId(userId)
                .setUsername(userLogin);
        final AuthResponse authResponse = new AuthResponse().setData(authData);

        String oktaResponseStatus = oktaAuthResponse.getStatus().name();

        if (StringUtils.equals(oktaResponseStatus, OktaClientResponseUtils.AUTHENTICATION_MFA_REQUIRED_STATUS) ||
                StringUtils.equals(oktaResponseStatus, OktaClientResponseUtils.AUTHENTICATION_MFA_ENROLL_STATUS)) {

            authData.setStateToken(oktaAuthResponse.getStateToken());
            authResponse.setStatus(AuthStatus.MFA_REQUIRED);

            // Filter out Okta push, call, and sms because we don't currently support them.
            final List<Factor> factors = oktaAuthResponse.getFactors()
                    .stream()
                    .filter(factor -> oktaClientResponseUtils.isSupportedFactor(factor))
                    .collect(Collectors.toList());

            oktaClientResponseUtils.validateUserFactors(factors);

            factors.forEach(factor -> authData.getDevices().add(new AuthMfaDevice()
                .setId(factor.getId())
                .setName(oktaClientResponseUtils.getDeviceName(factor))));
        }
        else {
            authResponse.setStatus(AuthStatus.SUCCESS);
        }

        return authResponse;
    }

    @Override
    public AuthResponse mfaCheck(String stateToken, String deviceId, String otpToken) {

        CompletableFuture<AuthenticationResponse> authenticationResponseCompletableFuture = new CompletableFuture<>();
        MfaStateHandler stateHandler = new MfaStateHandler(client, authenticationResponseCompletableFuture);

        AuthenticationResponse oktaAuthResponse;


        DefaultVerifyPassCodeFactorRequest request = client.instantiate(DefaultVerifyPassCodeFactorRequest.class);

        request.setPassCode(otpToken);
        request.setStateToken(stateToken);

        try {
            client.verifyFactor(deviceId, request, stateHandler);
            oktaAuthResponse = authenticationResponseCompletableFuture.get(45, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw ApiException.newBuilder()
                    .withExceptionCause(e)
                    .withApiErrors(DefaultApiError.AUTH_RESPONSE_WAIT_FAILED)
                    .withExceptionMessage("Failed to wait for Okta Auth Completeable Future to complete.")
                    .build();
        }

        final String userId = oktaAuthResponse.getUser().getId();
        final String userLogin = oktaAuthResponse.getUser().getLogin();

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
