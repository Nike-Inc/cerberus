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

import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.auth.connector.okta.statehandlers.InitialLoginStateHandler;
import com.nike.cerberus.auth.connector.okta.statehandlers.MfaStateHandler;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.impl.resource.DefaultFactor;
import com.okta.authn.sdk.impl.resource.DefaultVerifyPassCodeFactorRequest;
import com.okta.authn.sdk.resource.*;
import com.okta.sdk.models.usergroups.UserGroup;
import com.okta.sdk.models.usergroups.UserGroupProfile;
import com.okta.sdk.resource.user.factor.FactorProvider;
import com.okta.sdk.resource.user.factor.FactorType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests the OktaAuthConnector class
 */
public class OktaAuthConnectorTest {

    // class under test
    private OktaAuthConnector oktaAuthConnector;

    // dependencies
    @Mock
    private OktaApiClientHelper oktaApiClientHelper;

    @Mock
    private OktaClientResponseUtils oktaClientResponseUtils;

    @Mock
    private AuthenticationClient client;

    @Before
    public void setup() {

    initMocks(this);

        // create test object
        oktaAuthConnector = new OktaAuthConnector(oktaApiClientHelper, oktaClientResponseUtils, client);

        reset(oktaApiClientHelper);
    }

    /////////////////////////
    // Test Methods
    /////////////////////////

    @Test
    public void authenticateSuccess() throws Exception {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";
        AuthStatus status = AuthStatus.SUCCESS;

        AuthenticationResponse response = mock(AuthenticationResponse.class);
        when(response.getStatus()).thenReturn(AuthenticationStatus.SUCCESS);

        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn(email);
        when(response.getUser()).thenReturn(user);

        doAnswer(invocation -> {
            InitialLoginStateHandler stateHandler = (InitialLoginStateHandler) invocation.getArguments()[3];
            stateHandler.authenticationResponseFuture.complete(response);
            return null;
        }).when(client).authenticate(any(), any(), any(), any());

        // do the call
        AuthResponse result = this.oktaAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(status, result.getStatus());
    }

    @Test
    public void authenticateMfaRequiredSuccess() throws Exception {

        String username = "username";
        String password = "password";

        AuthStatus expectedStatus = AuthStatus.MFA_REQUIRED;

        String email = "email";
        String id = "id";
        FactorProvider provider = FactorProvider.OKTA;
        FactorType type = FactorType.TOKEN_SOFTWARE_TOTP;
        String deviceId = "device id";
        String status = "status";
        String deviceName = "Okta Verify TOTP";

        AuthenticationResponse response = mock(AuthenticationResponse.class);
        when(response.getStatus()).thenReturn(AuthenticationStatus.MFA_REQUIRED);

        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn(email);
        when(response.getUser()).thenReturn(user);

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(type);
        when(factor.getProvider()).thenReturn(provider);
        when(factor.getStatus()).thenReturn(status);
        when(factor.getId()).thenReturn(deviceId);

        when(oktaClientResponseUtils.getDeviceName(factor)).thenReturn(deviceName);

        when(oktaClientResponseUtils.isSupportedFactor(factor)).thenReturn(true);
        when(response.getFactors()).thenReturn(Lists.newArrayList(factor));

        doAnswer(invocation -> {
            InitialLoginStateHandler stateHandler = (InitialLoginStateHandler) invocation.getArguments()[3];
            stateHandler.authenticationResponseFuture.complete(response);
            return null;
        }).when(client).authenticate(any(), any(), any(), any());

        // do the call
        AuthResponse result = this.oktaAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(1, result.getData().getDevices().size());
        assertEquals(deviceId, result.getData().getDevices().get(0).getId());
        assertEquals(StringUtils.capitalize(deviceName), result.getData().getDevices().get(0).getName());
        assertEquals(expectedStatus, result.getStatus());
    }

    @Test(expected = ApiException.class)
    public void authenticateMfaEnrollFail() throws Exception {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";

        AuthStatus expectedStatus = AuthStatus.MFA_REQUIRED;

        AuthenticationResponse response = mock(AuthenticationResponse.class);
        when(response.getStatus()).thenReturn(AuthenticationStatus.MFA_ENROLL);

        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn(email);
        when(response.getUser()).thenReturn(user);

        doAnswer(invocation -> {
            InitialLoginStateHandler stateHandler = (InitialLoginStateHandler) invocation.getArguments()[3];
            stateHandler.authenticationResponseFuture.complete(response);
            return null;
        }).when(client).authenticate(any(), any(), any(), any());

        doCallRealMethod().when(oktaClientResponseUtils).validateUserFactors(anyList());

        // do the call
        AuthResponse result = this.oktaAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(expectedStatus, result.getStatus());
    }

    // We currently do not support Okta push, call, and sms.
    @Test
    public void authenticateFailsNoSupportedDevicesEnrolled() throws Exception {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";
        FactorProvider provider = FactorProvider.OKTA;
        FactorType type = FactorType.SMS;
        String deviceId = "device id";
        String status = "status";
        String deviceName = "Okta Text Message Code";

        AuthStatus expectedStatus = AuthStatus.MFA_REQUIRED;

        AuthenticationResponse response = mock(AuthenticationResponse.class);
        when(response.getStatus()).thenReturn(AuthenticationStatus.MFA_REQUIRED);

        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn(email);
        when(response.getUser()).thenReturn(user);

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(type);
        when(factor.getProvider()).thenReturn(provider);
        when(factor.getStatus()).thenReturn(status);
        when(factor.getId()).thenReturn(deviceId);

        when(oktaClientResponseUtils.getDeviceName(factor)).thenReturn(deviceName);

        when(oktaClientResponseUtils.isSupportedFactor(factor)).thenReturn(false);
        when(response.getFactors()).thenReturn(Lists.newArrayList(factor));

        doAnswer(invocation -> {
            InitialLoginStateHandler stateHandler = (InitialLoginStateHandler) invocation.getArguments()[3];
            stateHandler.authenticationResponseFuture.complete(response);
            return null;
        }).when(client).authenticate(any(), any(), any(), any());

        // do the call
        AuthResponse result = this.oktaAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(0, result.getData().getDevices().size());
        assertEquals(expectedStatus, result.getStatus());
    }

    @Test
    public void mfaCheckSuccess() throws Exception {

        String stateToken = "state token";
        String deviceId = "device id";
        String otpToken = "otp token";

        String email = "email";
        String id = "id";

        AuthenticationResponse response = mock(AuthenticationResponse.class);
        when(response.getStatus()).thenReturn(AuthenticationStatus.SUCCESS);

        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn(email);
        when(response.getUser()).thenReturn(user);

        DefaultVerifyPassCodeFactorRequest request = mock(DefaultVerifyPassCodeFactorRequest.class);

        doAnswer(invocation -> {
            request.setPassCode(stateToken);
            request.setStateToken(otpToken);
            return request;
        }).when(client).instantiate(DefaultVerifyPassCodeFactorRequest.class);
        doAnswer(invocation -> {
            MfaStateHandler stateHandler = (MfaStateHandler) invocation.getArguments()[2];
            stateHandler.authenticationResponseFuture.complete(response);
            return null;
        }).when(client).verifyFactor(any(), any(), any());

        // do the call
        AuthResponse result = this.oktaAuthConnector.mfaCheck(stateToken, deviceId, otpToken);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
    }

    @Test
    public void getGroupsHappy() {

        String id = "id";
        AuthData authData = mock(AuthData.class);
        when(authData.getUserId()).thenReturn(id);

        String name1 = "name 1";
        UserGroupProfile profile1 = mock(UserGroupProfile.class);
        UserGroup group1 = mock(UserGroup.class);
        when(profile1.getName()).thenReturn(name1);
        when(group1.getProfile()).thenReturn(profile1);

        String name2 = "name 2";
        UserGroupProfile profile2 = mock(UserGroupProfile.class);
        UserGroup group2 = mock(UserGroup.class);
        when(profile2.getName()).thenReturn(name2);
        when(group2.getProfile()).thenReturn(profile2);

        when(oktaApiClientHelper.getUserGroups(id)).thenReturn(Lists.newArrayList(group1, group2));

        // do the call
        Set<String> result = this.oktaAuthConnector.getGroups(authData);

        // verify results
        assertTrue(result.contains(name1));
        assertTrue(result.contains(name2));
    }
}