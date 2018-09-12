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
import com.google.common.collect.Lists;
import com.nike.backstopper.exception.ApiException;
import com.okta.authn.sdk.client.AuthenticationClient;
import com.okta.authn.sdk.impl.resource.DefaultFactor;
import com.okta.authn.sdk.resource.AuthenticationResponse;
import com.okta.sdk.resource.user.factor.FactorProvider;
import com.okta.sdk.resource.user.factor.FactorType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;


import java.util.concurrent.CompletableFuture;
import static groovy.util.GroovyTestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests the OktaClientResponseUtils class
 */
public class OktaClientResponseUtilsTest {

    // class under test
    private OktaClientResponseUtils oktaClientResponseUtils;


    @Before
    public void setup() {

        initMocks(this);
        String baseUrl = "base url";

        // create test object
        this.oktaClientResponseUtils = new OktaClientResponseUtils(baseUrl);

    }

    @Test
    public void getFactorKey() {

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(FactorType.PUSH);
        when(factor.getProvider()).thenReturn(FactorProvider.OKTA);

        String expected = "okta-push";
        String actual = oktaClientResponseUtils.getFactorKey(factor);

        assertEquals(expected, actual);
    }


    @Test
    public void isSupportedFactorFalse() {

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(FactorType.PUSH);
        when(factor.getProvider()).thenReturn(FactorProvider.OKTA);

        boolean expected = false;
        boolean actual = oktaClientResponseUtils.isSupportedFactor(factor);

        assertEquals(expected, actual);
    }

    @Test
    public void isSupportedFactorTrue() {

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(FactorType.TOKEN_SOFTWARE_TOTP);
        when(factor.getProvider()).thenReturn(FactorProvider.OKTA);

        boolean expected = true;
        boolean actual = oktaClientResponseUtils.isSupportedFactor(factor);

        assertEquals(expected, actual);
    }

    @Test
    public void getDeviceNameGoogleTotp() {

        FactorProvider provider = FactorProvider.GOOGLE;
        FactorType type = FactorType.TOKEN_SOFTWARE_TOTP;

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(type);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaClientResponseUtils.getDeviceName(factor);

        assertEquals("Google Authenticator", result);
    }

    @Test
    public void getDeviceNameOktaTotp() {

        FactorProvider provider = FactorProvider.OKTA;
        FactorType type = FactorType.TOKEN_SOFTWARE_TOTP;

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(type);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaClientResponseUtils.getDeviceName(factor);

        assertEquals("Okta Verify TOTP", result);
    }

    @Test
    public void getDeviceNameOktaPush() {

        FactorProvider provider = FactorProvider.OKTA;
        FactorType type = FactorType.PUSH;

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(type);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaClientResponseUtils.getDeviceName(factor);

        assertEquals("Okta Verify Push", result);

    }

    @Test
    public void getDeviceNameOktaCall() {

        FactorProvider provider = FactorProvider.OKTA;
        FactorType type = FactorType.CALL;

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(type);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaClientResponseUtils.getDeviceName(factor);

        assertEquals("Okta Voice Call", result);
    }

    @Test
    public void getDeviceNameOktaSms() {

        FactorProvider provider = FactorProvider.OKTA;
        FactorType type = FactorType.SMS;

        DefaultFactor factor = mock(DefaultFactor.class);
        when(factor.getType()).thenReturn(type);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaClientResponseUtils.getDeviceName(factor);

        assertEquals("Okta Text Message Code", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeviceNameFailsNullFactor() {

        this.oktaClientResponseUtils.getDeviceName(null);

    }

    @Test
    public void validateUserFactorsSuccess() {

        DefaultFactor factor1 = mock(DefaultFactor.class);
        when(factor1.getStatus()).thenReturn(OktaClientResponseUtils.MFA_FACTOR_NOT_SETUP_STATUS);
        DefaultFactor factor2 = mock(DefaultFactor.class);

        this.oktaClientResponseUtils.validateUserFactors(Lists.newArrayList(factor1, factor2));
    }

    @Test(expected = ApiException.class)
    public void validateUserFactorsFailsNull() {

        this.oktaClientResponseUtils.validateUserFactors(null);
    }

    @Test(expected = ApiException.class)
    public void validateUserFactorsFailsEmpty() {

        this.oktaClientResponseUtils.validateUserFactors(Lists.newArrayList());
    }

    @Test(expected = ApiException.class)
    public void validateUserFactorsFailsAllFactorsNotSetUp() {

        String status = OktaClientResponseUtils.MFA_FACTOR_NOT_SETUP_STATUS;

        DefaultFactor factor1 = mock(DefaultFactor.class);
        when(factor1.getStatus()).thenReturn(status);

        DefaultFactor factor2 = mock(DefaultFactor.class);
        when(factor2.getStatus()).thenReturn(status);

        this.oktaClientResponseUtils.validateUserFactors(Lists.newArrayList(factor1, factor2));
    }

}
