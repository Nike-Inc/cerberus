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

import com.nike.cerberus.auth.connector.AuthResponse;
import com.nike.cerberus.domain.MfaCheckRequest;
import com.nike.cerberus.service.AuthenticationServiceV1;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Endpoint for verifying the token from the user's MFA device.  Returns the full auth response if verified.
 */
public class MfaCheck extends StandardEndpoint<MfaCheckRequest, AuthResponse> {

    private final AuthenticationServiceV1 authenticationService;

    @Inject
    public MfaCheck(final AuthenticationServiceV1 authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public CompletableFuture<ResponseInfo<AuthResponse>> execute(final RequestInfo<MfaCheckRequest> request,
                                                                 final Executor longRunningTaskExecutor,
                                                                 final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                () -> ResponseInfo.newBuilder(authenticationService.mfaCheck(request.getContent())).build(),
                longRunningTaskExecutor
        );
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v2/auth/mfa_check", HttpMethod.POST);
    }
}
