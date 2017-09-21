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
import com.nike.cerberus.hystrix.HystrixVaultAdminClient;
import com.nike.vault.client.VaultClientException;
import com.nike.vault.client.VaultServerException;
import com.nike.vault.client.model.VaultClientTokenResponse;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.Endpoint;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;

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

public class CmsRequestSecurityValidatorTest {

    private final String vaultToken = "123-123-123-123-123";

    private final Endpoint<Void> securedEndpoint = () -> null;

    private final Collection<Endpoint<?>> securedEndpoints = Lists.newArrayList(securedEndpoint);

    private HystrixVaultAdminClient vaultAdminClient;

    private CmsRequestSecurityValidator subject;

    @Before
    public void setUp() throws Exception {
        vaultAdminClient = mock(HystrixVaultAdminClient.class);
        subject = new CmsRequestSecurityValidator(securedEndpoints, vaultAdminClient);
    }

    @Test
    public void test_validateSecureRequestForEndpoint_adds_security_context_to_request() {
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getUri()).thenReturn("https://localhost");
        final HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(CmsRequestSecurityValidator.HEADER_X_VAULT_TOKEN, vaultToken);
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);

        final Map<String, String> meta = Maps.newHashMap();
        meta.put(CerberusPrincipal.METADATA_KEY_IS_ADMIN, Boolean.TRUE.toString());
        meta.put(CerberusPrincipal.METADATA_KEY_USERNAME, "username");
        meta.put(CerberusPrincipal.METADATA_KEY_GROUPS, "group1,group2");
        final VaultClientTokenResponse clientTokenResponse = new VaultClientTokenResponse()
                .setId(vaultToken)
                .setMeta(meta);
        when(vaultAdminClient.lookupToken(vaultToken)).thenReturn(clientTokenResponse);

        subject.validateSecureRequestForEndpoint(requestInfo, securedEndpoint);

        verify(requestInfo).addRequestAttribute(eq(SECURITY_CONTEXT_ATTR_KEY), any(SecurityContext.class));
    }

    @Test(expected = ApiException.class)
    public void test_validateSecureRequestForEndpoint_throws_error_when_no_vault_token_header() {
        final RequestInfo<?> requestInfo = mock(RequestInfo.class);
        final HttpHeaders httpHeaders = new DefaultHttpHeaders();
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);

        subject.validateSecureRequestForEndpoint(requestInfo, securedEndpoint);
    }

    @Test(expected = ApiException.class)
    public void test_validateSecureRequestForEndpoint_throws_error_when_vault_server_exception_caught() {
        final RequestInfo<?> requestInfo = mock(RequestInfo.class);
        final HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(CmsRequestSecurityValidator.HEADER_X_VAULT_TOKEN, vaultToken);
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);

        when(vaultAdminClient.lookupToken(vaultToken)).thenThrow(new VaultServerException(1, Lists.newArrayList()));

        subject.validateSecureRequestForEndpoint(requestInfo, securedEndpoint);
    }

    @Test(expected = ApiException.class)
    public void test_validateSecureRequestForEndpoint_throws_error_when_vault_client_exception_caught() {
        final RequestInfo<?> requestInfo = mock(RequestInfo.class);
        final HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(CmsRequestSecurityValidator.HEADER_X_VAULT_TOKEN, vaultToken);
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);

        when(vaultAdminClient.lookupToken(vaultToken)).thenThrow(new VaultClientException("Failure"));

        subject.validateSecureRequestForEndpoint(requestInfo, securedEndpoint);
    }

    @Test
    public void test_getSecurityContextForRequest_returns_optional_populated_with_security_context() {
        final RequestInfo<?> requestInfo = mock(RequestInfo.class);
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        final Map<String, String> meta = Maps.newHashMap();
        meta.put(CerberusPrincipal.METADATA_KEY_IS_ADMIN, Boolean.TRUE.toString());
        meta.put(CerberusPrincipal.METADATA_KEY_USERNAME, "username");
        meta.put(CerberusPrincipal.METADATA_KEY_GROUPS, "group1,group2");
        final CerberusPrincipal authPrincipal = new CerberusPrincipal(new VaultClientTokenResponse().setMeta(meta));
        requestAttributes.put(SECURITY_CONTEXT_ATTR_KEY,
                new VaultSecurityContext(authPrincipal, "https"));

        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(requestInfo);

        assertThat(securityContext).isPresent();
        assertThat(securityContext.get()).isInstanceOf(VaultSecurityContext.class);
    }

    @Test
    public void test_getSecurityContextForRequest_returns_empty_optional_when_no_security_context() {
        final RequestInfo<?> requestInfo = mock(RequestInfo.class);
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(requestInfo);

        assertThat(securityContext).isEmpty();
    }

    @Test
    public void test_endpointsToValidate_returns_endpoints_given_to_constructor() {
        assertThat(subject.endpointsToValidate()).containsExactlyElementsOf(securedEndpoints);
    }
}