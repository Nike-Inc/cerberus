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
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.okta.sdk.models.auth.AuthResult;
import com.okta.sdk.models.factors.Factor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Tests the OktaClientResponseUtils class
 */
public class OktaClientResponseUtilsTest {

    // class under test
    private OktaClientResponseUtils oktaClientResponseUtils;

    // dependencies
    @Mock
    private ObjectMapper objectMapper;

    private String baseUrl;


    @Before
    public void setup() {

        initMocks(this);
        baseUrl = "base url";

        // create test object
        this.oktaClientResponseUtils = new OktaClientResponseUtils(objectMapper, baseUrl);
    }

    @Test
    public void getDeviceName() {

        String provider = "provider";
        Factor factor = mock(Factor.class);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaClientResponseUtils.getDeviceName(factor);

        assertEquals(StringUtils.capitalize(provider), result);
    }

    @Test
    public void getDeviceNameGoogle() {

        String provider = "GOOGLE";
        Factor factor = mock(Factor.class);
        when(factor.getProvider()).thenReturn(provider);

        String result = this.oktaClientResponseUtils.getDeviceName(factor);

        assertEquals("Google Authenticator", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeviceNameFailsNullFactor() {

        this.oktaClientResponseUtils.getDeviceName(null);

    }

    @Test
    public void getUserFactorsFromAuthResultHappy() throws Exception {

        String factorId = "factor id";
        String provider = "GOOGLE";

        Factor factor = new Factor();
        factor.setProvider(provider);
        factor.setId(factorId);
        List<Factor> factors = Lists.newArrayList(factor);

        String embeddedStr = "embedded string";
        Map<String, Object> embedded = Maps.newHashMap();
        embedded.put("factors", factors);

        AuthResult authResult = new AuthResult();
        authResult.setEmbedded(embedded);

        EmbeddedAuthResponseDataV1 embeddedAuthResponseDataV1 = new EmbeddedAuthResponseDataV1();
        embeddedAuthResponseDataV1.setFactors(factors);
        when(objectMapper.writeValueAsString(embedded)).thenReturn(embeddedStr);
        when(objectMapper.readValue(embeddedStr, EmbeddedAuthResponseDataV1.class)).thenReturn(embeddedAuthResponseDataV1);

        List<Factor> result = this.oktaClientResponseUtils.getUserFactorsFromAuthResult(authResult);

        assertEquals(1, result.size());
        assertEquals(provider, result.get(0).getProvider());
        assertEquals(factorId, result.get(0).getId());
    }

    @Test(expected = ApiException.class)
    public void getUserFactorsFromAuthResultEmbeddedNull() throws Exception {

        AuthResult authResult = new AuthResult();
        authResult.setEmbedded(null);

        // do the call
        this.oktaClientResponseUtils.getUserFactorsFromAuthResult(authResult);
    }

    @Test
    public void validateUserFactorsSuccess() {

        Factor factor1 = new Factor();
        factor1.setStatus(OktaClientResponseUtils.MFA_FACTOR_NOT_SETUP_STATUS);

        Factor factor2 = new Factor();

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

        Factor factor1 = new Factor();
        factor1.setStatus(OktaClientResponseUtils.MFA_FACTOR_NOT_SETUP_STATUS);

        Factor factor2 = new Factor();
        factor2.setStatus(OktaClientResponseUtils.MFA_FACTOR_NOT_SETUP_STATUS);

        this.oktaClientResponseUtils.validateUserFactors(Lists.newArrayList(factor1, factor2));
    }
}
