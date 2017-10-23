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

package com.nike.cerberus.endpoints.authentication;

import com.nike.cerberus.domain.IamPrincipalCredentials;
import com.nike.cerberus.domain.IamRoleAuthResponse;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.cerberus.service.EventProcessorService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.endpoints.AuditableEventEndpoint.auditableEvent;

/**
 * Authentication endpoint for IAM roles.  If valid, a client token that is encrypted via KMS is returned.  The
 * IAM role will be the only role capable of decrypting the client token via KMS.
 */
public class AuthenticateIamPrincipal extends StandardEndpoint<IamPrincipalCredentials, IamRoleAuthResponse> {

    private final AuthenticationService authenticationService;
    private final EventProcessorService eventProcessorService;

    @Inject
    public AuthenticateIamPrincipal(final AuthenticationService authenticationService,
                                    EventProcessorService eventProcessorService) {

        this.authenticationService = authenticationService;
        this.eventProcessorService = eventProcessorService;
    }

    @Override
    public CompletableFuture<ResponseInfo<IamRoleAuthResponse>> execute(final RequestInfo<IamPrincipalCredentials> request,
                                                                        final Executor longRunningTaskExecutor,
                                                                        final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> authenticate(request), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<IamRoleAuthResponse> authenticate(RequestInfo<IamPrincipalCredentials> request) {
        final IamPrincipalCredentials credentials = request.getContent();

        eventProcessorService.ingestEvent(auditableEvent(
                String.format("[ Name: %s ]", credentials.getIamPrincipalArn()), request, getClass().getSimpleName())
                .withAction(String.format("Attempting to authenticate in region %s", credentials.getRegion()))
                .build()
        );

        return ResponseInfo.newBuilder(authenticationService.authenticate(request.getContent())).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v2/auth/iam-principal", HttpMethod.POST);
    }
}
