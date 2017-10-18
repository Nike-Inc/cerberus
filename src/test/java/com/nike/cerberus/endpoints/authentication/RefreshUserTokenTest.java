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

package com.nike.cerberus.endpoints.authentication;

import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthData;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.auth.connector.AuthStatus;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.CerberusSecurityContext;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.vault.client.model.VaultAuthResponse;
import com.nike.vault.client.model.VaultClientTokenResponse;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RefreshUserTokenTest {

    private final Executor executor = Executors.newSingleThreadExecutor();

    private AuthenticationService authenticationService;

    private RefreshUserToken subject;

    @Before
    public void setUp() throws Exception {
        authenticationService = mock(AuthenticationService.class);
        subject = new RefreshUserToken(authenticationService);
    }

    @Test
    public void requestMatcher_is_http_get() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).hasSize(1);
        assertThat(httpMethods).contains(HttpMethod.GET);
    }

    @Test
    public void execute_returns_new_token_if_replacing_valid_one() {
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        final Map<String, String> meta = Maps.newHashMap();
        meta.put(CerberusPrincipal.METADATA_KEY_IS_ADMIN, Boolean.TRUE.toString());
        meta.put(CerberusPrincipal.METADATA_KEY_USERNAME, "username");
        meta.put(CerberusPrincipal.METADATA_KEY_GROUPS, "group1,group2");
        final CerberusPrincipal authPrincipal = new CerberusPrincipal(new VaultClientTokenResponse().setMeta(meta));
        requestAttributes.put(CmsRequestSecurityValidator.SECURITY_CONTEXT_ATTR_KEY,
                new CerberusSecurityContext(authPrincipal, "https"));
        final VaultAuthResponse vaultAuthResponse = new VaultAuthResponse();
        final AuthResponse authResponse = new AuthResponse();
        authResponse.setStatus(AuthStatus.SUCCESS);
        authResponse.setData(new AuthData().setUsername("username").setClientToken(vaultAuthResponse));
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);
        when(authenticationService.refreshUserToken(authPrincipal)).thenReturn(authResponse);

        final CompletableFuture<ResponseInfo<AuthResponse>> completableFuture =
                subject.execute(requestInfo, executor, null);
        final ResponseInfo<AuthResponse> responseInfo = completableFuture.join();

        assertThat(responseInfo.getContentForFullResponse()).isEqualTo(authResponse);
    }

    @Test
    public void execute_throws_api_error_if_no_security_context() {
        final Map<String, Object> requestAttributes = Maps.newHashMap();
        requestAttributes.put(CmsRequestSecurityValidator.SECURITY_CONTEXT_ATTR_KEY, null);
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getRequestAttributes()).thenReturn(requestAttributes);

        try {
            final CompletableFuture<ResponseInfo<AuthResponse>> completableFuture =
                    subject.execute(requestInfo, executor, null);
            completableFuture.join();
            fail("Expected exception not thrown.");
        } catch (CompletionException cex) {
            assertThat(cex.getCause()).isInstanceOf(ApiException.class);
        }
    }
}