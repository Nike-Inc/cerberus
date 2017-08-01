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

import com.nike.cerberus.domain.IamRoleAuthResponse;
import com.nike.cerberus.domain.IamRoleCredentials;
import com.nike.cerberus.service.AuthenticationCacheService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticateIamRoleTest {

    private final Executor executor = Executors.newSingleThreadExecutor();

    private AuthenticationCacheService authenticationService;

    private AuthenticateIamRole subject;

    @Before
    public void setUp() throws Exception {
        authenticationService = mock(AuthenticationCacheService.class);
        subject = new AuthenticateIamRole(authenticationService);
    }

    @Test
    public void requestMatcher_is_http_post() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).hasSize(1);
        assertThat(httpMethods).contains(HttpMethod.POST);
    }

    @Test
    public void execute_returns_iam_role_auth_response() {
        final IamRoleAuthResponse iamRoleAuthResponse = new IamRoleAuthResponse();
        iamRoleAuthResponse.setAuthData("AUTH_DATA");
        final IamRoleCredentials credentials = new IamRoleCredentials();
        final RequestInfo<IamRoleCredentials> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getContent()).thenReturn(credentials);
        when(authenticationService.authenticate(credentials)).thenReturn(iamRoleAuthResponse);

        final CompletableFuture<ResponseInfo<IamRoleAuthResponse>> completableFuture =
                subject.execute(requestInfo, executor, null);
        final ResponseInfo<IamRoleAuthResponse> responseInfo = completableFuture.join();

        assertThat(responseInfo.getContentForFullResponse()).isEqualTo(iamRoleAuthResponse);
    }
}