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
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.okta.sdk.clients.AuthApiClient;
import com.okta.sdk.clients.UserApiClient;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import com.okta.sdk.models.usergroups.UserGroup;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the OktaAuthHelper class
 */
public class OktaAuthHelperTest {

    // class under test
    private OktaAuthHelper oktaAuthHelper;

    // dependencies
    private AuthApiClient authApiClient;
    private UserApiClient userApiClient;

    @Before
    public void setup() {

        // mock dependencies
        authApiClient = mock(AuthApiClient.class);
        userApiClient = mock(UserApiClient.class);

        // create test object
        oktaAuthHelper = new OktaAuthHelper(authApiClient, userApiClient);
    }

    /////////////////////////
    // Helper Methods
    /////////////////////////


    /////////////////////////
    // Test Methods
    /////////////////////////

    @Test
    public void getUserGroupsHappy() throws Exception {

        String id = "id";
        UserGroup group = mock(UserGroup.class);
        when(userApiClient.getUserGroups(id)).thenReturn(Lists.newArrayList(group));

        // do the call
        List<UserGroup> result = this.oktaAuthHelper.getUserGroups(id);

        // verify results
        assertTrue(result.contains(group));
    }

    @Test(expected = ApiException.class)
    public void getUserGroupsFails() throws Exception {

        when(userApiClient.getUserGroups(anyString())).thenThrow(IOException.class);

        // do the call
        this.oktaAuthHelper.getUserGroups("id");
    }

    @Test
    public void verifyFactorHappy() throws Exception {

        String factorId = "factor id";
        String stateToken = "state token";
        String passCode = "pass code";

        AuthResult authResult = mock(AuthResult.class);
        when(authApiClient.authenticateWithFactor(stateToken, factorId, passCode)).thenReturn(authResult);

        AuthResult result = this.oktaAuthHelper.verifyFactor(factorId, stateToken, passCode);

        assertEquals(authResult, result);
    }

    @Test(expected = ApiException.class)
    public void verifyFactorFailsIO() throws Exception {

        when(authApiClient.authenticateWithFactor(anyString(), anyString(), anyString())).thenThrow(IOException.class);

        // do the call
        this.oktaAuthHelper.verifyFactor("factor id", "state token", "pass code");
    }

    @Test
    public void authenticateUserHappy() throws Exception {

        String username = "username";
        String password = "password";
        String relayState = "relay state";

        AuthResult authResult = mock(AuthResult.class);
        when(this.authApiClient.authenticate(username, password, relayState)).thenReturn(authResult);

        AuthResult result = this.oktaAuthHelper.authenticateUser(username, password, relayState);

        assertEquals(result, authResult);
    }

    @Test(expected = ApiException.class)
    public void authenticateUserFails() throws Exception {

        when(authApiClient.authenticate(anyString(), anyString(), anyString())).thenThrow(IOException.class);

        // do the call
        this.oktaAuthHelper.authenticateUser("username", "password", "relay state");
    }

    @Test
    public void getEmbbeddedAuthDataHappy() {

        Map<String, String> user = Maps.newHashMap();
        List<Map<String, Object>> factors = Lists.newArrayList(Maps.newHashMap());
        Map<String, Object> embedded = Maps.newHashMap();
        embedded.put("user", user);
        embedded.put("factors", factors);

        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getEmbedded()).thenReturn(embedded);

        EmbeddedAuthResponseDataV1 result = this.oktaAuthHelper.getEmbeddedAuthData(authResult);

        assertNotNull(result.getUser());
        assertNotNull(result.getFactors());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getEmbbeddedAuthDataFailsNullResult() {

        this.oktaAuthHelper.getEmbeddedAuthData(null);

    }

    @Test
    public void getDeviceName() {

        String provider = "provider";
        Factor factor = mock(Factor.class);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaAuthHelper.getDeviceName(factor);

        assertEquals(StringUtils.capitalize(provider), result);
    }

    @Test
    public void getDeviceNameGoogle() {

        String provider = "GOOGLE";
        Factor factor = mock(Factor.class);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaAuthHelper.getDeviceName(factor);

        assertEquals("Google Authenticator", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeviceNameFailsNullFactor() {

        this.oktaAuthHelper.getDeviceName(null);

    }
}