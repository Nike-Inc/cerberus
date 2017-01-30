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
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.usergroups.UserGroup;
import com.okta.sdk.models.usergroups.UserGroupProfile;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests the OktaMFAAuthConnector class
 */
public class OktaMFAAuthConnectorTest {

    // class under test
    private OktaMFAAuthConnector oktaMFAAuthConnector;

    // dependencies
    @Mock
    private OktaApiClientHelper oktaApiClientHelper;

    @Mock
    private OktaClientResponseUtils oktaClientResponseUtils;

    @Before
    public void setup() {

        initMocks(this);

        // create test object
        oktaMFAAuthConnector = new OktaMFAAuthConnector(oktaApiClientHelper, oktaClientResponseUtils);
    }

    private Factor mockFactor(String provider, String id, boolean enrolled) {

        Factor factor = mock(Factor.class);
        when(factor.getId()).thenReturn(id);
        when(factor.getProvider()).thenReturn(provider);
        when(oktaClientResponseUtils.getDeviceName(factor)).thenCallRealMethod();

        if (enrolled) {
            when(factor.getStatus()).thenReturn("status");
        } else {
            when(factor.getStatus()).thenReturn(OktaClientResponseUtils.MFA_FACTOR_NOT_SETUP_STATUS);
        }

        return factor;
    }

    /////////////////////////
    // Test Methods
    /////////////////////////

    @Test(expected = ApiException.class)
    public void authenticateFailsNoFactors() {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";

        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getStatus()).thenReturn(OktaClientResponseUtils.AUTHENTICATION_SUCCESS_STATUS);

        when(oktaApiClientHelper.authenticateUser(username, password, null)).thenReturn(authResult);
        when(oktaClientResponseUtils.getUserIdFromAuthResult(authResult)).thenReturn(id);
        when(oktaClientResponseUtils.getUserLoginFromAuthResult(authResult)).thenReturn(email);
        doCallRealMethod().when(oktaClientResponseUtils).validateUserFactors(anyObject());

        // do the call
        AuthResponse result = this.oktaMFAAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
    }

    @Test
    public void authenticateHappyMfaRequiredSuccess() {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";
        String provider = "provider";
        String deviceId = "device id";
        Factor factor = mockFactor(provider, deviceId, true);

        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getStateToken()).thenReturn("state token");

        when(oktaApiClientHelper.authenticateUser(username, password, null)).thenReturn(authResult);
        when(authResult.getStatus()).thenReturn(OktaClientResponseUtils.AUTHENTICATION_MFA_REQUIRED_STATUS);
        when(oktaClientResponseUtils.getUserIdFromAuthResult(authResult)).thenReturn(id);
        when(oktaClientResponseUtils.getUserLoginFromAuthResult(authResult)).thenReturn(email);
        when(oktaClientResponseUtils.getUserFactorsFromAuthResult(authResult)).thenReturn(Lists.newArrayList(factor));

        // do the call
        AuthResponse result = this.oktaMFAAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(1, result.getData().getDevices().size());
        assertEquals(deviceId, result.getData().getDevices().get(0).getId());
        assertEquals(StringUtils.capitalize(provider), result.getData().getDevices().get(0).getName());
    }

    @Test(expected = ApiException.class)
    public void authenticateFailsNoDevicesEnrolled() {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";

        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getStateToken()).thenReturn("state token");

        when(oktaApiClientHelper.authenticateUser(username, password, null)).thenReturn(authResult);
        when(authResult.getStatus()).thenReturn(OktaClientResponseUtils.AUTHENTICATION_MFA_ENROLL_STATUS);
        when(oktaClientResponseUtils.getUserIdFromAuthResult(authResult)).thenReturn(id);
        when(oktaClientResponseUtils.getUserLoginFromAuthResult(authResult)).thenReturn(email);
        when(oktaApiClientHelper.getFactorsByUserId(id)).thenReturn(Lists.newArrayList());
        doCallRealMethod().when(oktaClientResponseUtils).validateUserFactors(anyObject());

        // do the call
        AuthResponse result = this.oktaMFAAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(0, result.getData().getDevices().size());
    }

    @Test
    public void authenticateHappyMfaNotRequiredSuccess() throws Exception {

        String username = "username";
        String password = "password";

        String email = "email";
        String id = "id";
        String provider = "provider";
        String deviceId = "device id";
        Factor factor = mockFactor(provider, deviceId, true);

        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getStateToken()).thenReturn("state token");

        when(oktaApiClientHelper.authenticateUser(username, password, null)).thenReturn(authResult);
        when(authResult.getStatus()).thenReturn(OktaClientResponseUtils.AUTHENTICATION_SUCCESS_STATUS);
        when(oktaClientResponseUtils.getUserIdFromAuthResult(authResult)).thenReturn(id);
        when(oktaClientResponseUtils.getUserLoginFromAuthResult(authResult)).thenReturn(email);
        when(oktaApiClientHelper.getFactorsByUserId(id)).thenReturn(Lists.newArrayList(factor));

        // do the call
        AuthResponse result = this.oktaMFAAuthConnector.authenticate(username, password);

        // verify results
        assertEquals(id, result.getData().getUserId());
        assertEquals(email, result.getData().getUsername());
        assertEquals(1, result.getData().getDevices().size());
        assertEquals(deviceId, result.getData().getDevices().get(0).getId());
        assertEquals(StringUtils.capitalize(provider), result.getData().getDevices().get(0).getName());
    }

    @Test
    public void mfaCheckHappy() {

        String stateToken = "state token";
        String deviceId = "device id";
        String otpToken = "otp token";

        String email = "email";
        String id = "id";

        AuthResult authResult = mock(AuthResult.class);
        when(oktaApiClientHelper.verifyFactor(deviceId, stateToken, otpToken)).thenReturn(authResult);
        when(oktaClientResponseUtils.getUserIdFromAuthResult(authResult)).thenReturn(id);
        when(oktaClientResponseUtils.getUserLoginFromAuthResult(authResult)).thenReturn(email);

        // do the call
        AuthResponse result = this.oktaMFAAuthConnector.mfaCheck(stateToken, deviceId, otpToken);

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
        Set<String> result = this.oktaMFAAuthConnector.getGroups(authData);

        // verify results
        assertTrue(result.contains(name1));
        assertTrue(result.contains(name2));
    }
}
