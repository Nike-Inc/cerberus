/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.endpoints.authentication;

import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.aws.sts.AwsStsClient;
import com.nike.cerberus.aws.sts.GetCallerIdentityResponse;
import com.nike.cerberus.domain.AuthTokenResponse;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthenticateStsIdentityTest {

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private EventProcessorService eventProcessorService;

    @Mock
    private AwsStsClient awsStsClient;

    private AuthenticateStsIdentity subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        subject = new AuthenticateStsIdentity(authenticationService, eventProcessorService, awsStsClient);
    }

    @Test
    public void requestMatcher_is_http_post() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).hasSize(1);
        assertThat(httpMethods).contains(HttpMethod.POST);
    }

    @Test
    public void execute_returns_iam_role_auth_response() {
        GetCallerIdentityResult getCallerIdentityResult = new GetCallerIdentityResult();
        getCallerIdentityResult.setArn("test arn");

        GetCallerIdentityResponse getCallerIdentityResponse = new GetCallerIdentityResponse();
        getCallerIdentityResponse.setGetCallerIdentityResult(getCallerIdentityResult);

        when(awsStsClient.getCallerIdentity(any())).thenReturn(getCallerIdentityResponse);

        final AuthTokenResponse authTokenResponse = new AuthTokenResponse();
        authTokenResponse.setClientToken("test token");

        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.get("Date")).thenReturn("test date");
        when(httpHeaders.get("x-amz-date")).thenReturn("test date");
        when(httpHeaders.get("x-amz-security-token")).thenReturn("test date");
        when(httpHeaders.get("Authorization")).thenReturn("test date");

        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getContent()).thenReturn(null);
        when(requestInfo.getHeaders()).thenReturn(httpHeaders);
        when(authenticationService.keylessAuthenticate("test arn")).thenReturn(authTokenResponse);

        final CompletableFuture<ResponseInfo<AuthTokenResponse>> completableFuture =
                subject.execute(requestInfo, executor, null);
        final ResponseInfo<AuthTokenResponse> responseInfo = completableFuture.join();

        assertThat(responseInfo.getContentForFullResponse()).isEqualTo(authTokenResponse);
    }

    @Test
    public void test_authenticate_with_400_response() {
        try {
            final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
            when(requestInfo.getContent()).thenReturn(null);
            final CompletableFuture<ResponseInfo<AuthTokenResponse>> completableFuture = subject.execute(requestInfo, executor, null);
            completableFuture.join();
            fail("expected exception not thrown");
        } catch (CompletionException cex) {
            assertThat(cex.getCause().getClass()).isEqualTo(ApiException.class);
        }
    }
}