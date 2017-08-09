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
import com.nike.cerberus.service.AuthenticationCacheService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.CerberusHttpHeaders.HEADER_X_CERBERUS_CLIENT;

/**
 * Authentication endpoint for IAM roles.  If valid, a client token that is encrypted via KMS is returned.  The
 * IAM role will be the only role capable of decrypting the client token via KMS.
 */
public class AuthenticateIamPrincipal extends StandardEndpoint<IamPrincipalCredentials, IamRoleAuthResponse> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AuthenticationCacheService authenticationService;

    @Inject
    public AuthenticateIamPrincipal(final AuthenticationCacheService authenticationService) {
        this.authenticationService = authenticationService;
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
        final HttpHeaders headers = request.getHeaders();
        final boolean clientHeaderExists = headers != null && headers.get(HEADER_X_CERBERUS_CLIENT) != null;
        final String clientHeader = clientHeaderExists ? headers.get(HEADER_X_CERBERUS_CLIENT) : "Unknown";

        log.info("{}: {}, IAM Auth Event: the IAM principal {} in attempting to authenticate in region {}",
                HEADER_X_CERBERUS_CLIENT,
                clientHeader,
                credentials.getIamPrincipalArn(),
                credentials.getRegion());

        return ResponseInfo.newBuilder(authenticationService.authenticate(request.getContent())).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v2/auth/iam-principal", HttpMethod.POST);
    }
}
