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

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.domain.UserCredentials;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthenticateUserTest {

    private final String username = "username";

    private final String password = "password";

    private final String validAuthorizationHeader = "Basic " +
            Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

    private final String invalidAuthorizationHeader = "Token 123-234-345-123-345-234";

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private EventProcessorService eventProcessorService;

    private AuthenticateUser subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        subject = new AuthenticateUser(authenticationService, eventProcessorService);
    }

    @Test
    public void requestMatcher_is_http_get() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).hasSize(1);
        assertThat(httpMethods).contains(HttpMethod.GET);
    }

    @Test
    public void execute_returns_vault_auth_response() {
        final AuthResponse authResponse = new AuthResponse();
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        final HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(HttpHeaders.Names.AUTHORIZATION, validAuthorizationHeader);
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);
        when(authenticationService.authenticate(any(UserCredentials.class))).thenReturn(authResponse);

        final CompletableFuture<ResponseInfo<AuthResponse>> completableFuture =
                subject.execute(requestInfo, executor, null);
        final ResponseInfo<AuthResponse> responseInfo = completableFuture.join();

        assertThat(responseInfo.getContentForFullResponse()).isEqualTo(authResponse);
    }

    @Test
    public void execute_throws_api_error_when_bad_auth_header() {
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        final HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add(HttpHeaders.Names.AUTHORIZATION, invalidAuthorizationHeader);
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);

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