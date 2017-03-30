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
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.VaultAuthPrincipalV1;
import com.nike.cerberus.service.AuthenticationServiceV1;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Authentication endpoint that allows refreshing the user token to pickup any permission changes.
 */
public class RefreshUserToken extends StandardEndpoint<Void, AuthResponse> {

    private final AuthenticationServiceV1 authenticationService;

    @Inject
    public RefreshUserToken(final AuthenticationServiceV1 authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public CompletableFuture<ResponseInfo<AuthResponse>> execute(final RequestInfo<Void> request,
                                                                      final Executor longRunningTaskExecutor,
                                                                      final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                () -> getRefreshedUserToken(request),
                longRunningTaskExecutor
        );
    }

    public ResponseInfo<AuthResponse> getRefreshedUserToken(final RequestInfo<Void> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            return ResponseInfo.newBuilder(
                    authenticationService.refreshUserToken(
                            (VaultAuthPrincipalV1) securityContext.get().getUserPrincipal())).build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_VAULT_TOKEN_INVALID).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v2/auth/user/refresh", HttpMethod.GET);
    }
}
