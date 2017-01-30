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
import com.okta.sdk.clients.AuthApiClient;
import com.okta.sdk.clients.FactorsApiClient;
import com.okta.sdk.clients.UserApiClient;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.usergroups.UserGroup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests the OktaApiClientHelper class
 */
public class OktaApiClientHelperTest {

    // class under test
    private OktaApiClientHelper oktaApiClientHelper;

    // dependencies
    @Mock
    private AuthApiClient authApiClient;

    @Mock
    private UserApiClient userApiClient;

    @Mock
    private FactorsApiClient factorsApiClient;

    @Before
    public void setup() {

        initMocks(this);

        // create test object
        this.oktaApiClientHelper = new OktaApiClientHelper(authApiClient, userApiClient, factorsApiClient);
    }


    /////////////////////////
    // Test Methods
    /////////////////////////

    @Test
    public void getUserGroupsHappy() throws Exception {

        String id = "id";
        UserGroup group = mock(UserGroup.class);
        when(userApiClient.getUserGroups(id)).thenReturn(Lists.newArrayList(group));

        // do the call
        List<UserGroup> result = this.oktaApiClientHelper.getUserGroups(id);

        // verify results
        assertTrue(result.contains(group));
    }

    @Test(expected = ApiException.class)
    public void getUserGroupsFails() throws Exception {

        when(userApiClient.getUserGroups(anyString())).thenThrow(IOException.class);

        // do the call
        this.oktaApiClientHelper.getUserGroups("id");
    }

    @Test
    public void verifyFactorHappy() throws Exception {

        String factorId = "factor id";
        String stateToken = "state token";
        String passCode = "pass code";

        AuthResult authResult = mock(AuthResult.class);
        when(authApiClient.authenticateWithFactor(stateToken, factorId, passCode)).thenReturn(authResult);

        AuthResult result = this.oktaApiClientHelper.verifyFactor(factorId, stateToken, passCode);

        assertEquals(authResult, result);
    }

    @Test(expected = ApiException.class)
    public void verifyFactorFailsIO() throws Exception {

        when(authApiClient.authenticateWithFactor(anyString(), anyString(), anyString())).thenThrow(IOException.class);

        // do the call
        this.oktaApiClientHelper.verifyFactor("factor id", "state token", "pass code");
    }

    @Test
    public void authenticateUserHappy() throws Exception {

        String username = "username";
        String password = "password";
        String relayState = "relay state";

        AuthResult authResult = mock(AuthResult.class);
        when(this.authApiClient.authenticate(username, password, relayState)).thenReturn(authResult);

        AuthResult result = this.oktaApiClientHelper.authenticateUser(username, password, relayState);

        assertEquals(result, authResult);
    }

    @Test(expected = ApiException.class)
    public void authenticateUserFails() throws Exception {

        when(authApiClient.authenticate(anyString(), anyString(), anyString())).thenThrow(IOException.class);

        // do the call
        this.oktaApiClientHelper.authenticateUser("username", "password", "relay state");
    }

}