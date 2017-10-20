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

package com.nike.cerberus.security;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.CerberusAuthToken;
import com.nike.cerberus.service.AuthTokenService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.Endpoint;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.nike.cerberus.security.CmsRequestSecurityValidator.SECURITY_CONTEXT_ATTR_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CmsRequestSecurityValidatorTest {

    private String token = "123-123-123-123-123";

    private Endpoint<Void> securedEndpoint = () -> null;

    private Collection<Endpoint<?>> securedEndpoints = Lists.newArrayList(securedEndpoint);

    @Mock
    AuthTokenService authTokenService;

    private CmsRequestSecurityValidator subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        subject = new CmsRequestSecurityValidator(securedEndpoints, authTokenService);
    }

    @Test
    public void test_validateSecureRequestForEndpoint_adds_security_context_to_request() {
        RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getUri()).thenReturn("https://localhost");
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(CmsRequestSecurityValidator.LEGACY_AUTH_TOKN_HEADER, token);
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);

        CerberusAuthToken authToken = CerberusAuthToken.Builder.create()
                .withToken(token)
                .withPrincipal("username")
                .withGroups("group1,group2")
                .withIsAdmin(true)
                .build();

        when(authTokenService.getCerberusAuthToken(token)).thenReturn(Optional.of(authToken));

        subject.validateSecureRequestForEndpoint(requestInfo, securedEndpoint);

        verify(requestInfo).addRequestAttribute(eq(SECURITY_CONTEXT_ATTR_KEY), any(SecurityContext.class));
    }

    @Test(expected = ApiException.class)
    public void test_validateSecureRequestForEndpoint_throws_error_when_no_vault_token_header() {
        RequestInfo<?> requestInfo = mock(RequestInfo.class);
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);

        subject.validateSecureRequestForEndpoint(requestInfo, securedEndpoint);
    }

    @Test
    public void test_getSecurityContextForRequest_returns_optional_populated_with_security_context() {
        RequestInfo<?> requestInfo = mock(RequestInfo.class);
        Map<String, Object> requestAttributes = Maps.newHashMap();
        Map<String, String> meta = Maps.newHashMap();

        CerberusAuthToken authToken = CerberusAuthToken.Builder.create()
                .withToken(token)
                .withPrincipal("username")
                .withGroups("group1,group2")
                .withIsAdmin(true)
                .build();

        requestAttributes.put(SECURITY_CONTEXT_ATTR_KEY,
                new CerberusSecurityContext(new CerberusPrincipal(authToken), "https"));

        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(requestInfo);

        assertThat(securityContext).isPresent();
        assertThat(securityContext.get()).isInstanceOf(CerberusSecurityContext.class);
    }

    @Test
    public void test_getSecurityContextForRequest_returns_empty_optional_when_no_security_context() {
        RequestInfo<?> requestInfo = mock(RequestInfo.class);
        Map<String, Object> requestAttributes = Maps.newHashMap();
        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(requestInfo);

        assertThat(securityContext).isEmpty();
    }

    @Test
    public void test_endpointsToValidate_returns_endpoints_given_to_constructor() {
        assertThat(subject.endpointsToValidate()).containsExactlyElementsOf(securedEndpoints);
    }
}