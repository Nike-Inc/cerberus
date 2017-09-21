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
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.AuthenticationService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.CerberusHttpHeaders.HEADER_X_CERBERUS_CLIENT;
import static com.nike.cerberus.CerberusHttpHeaders.getClientVersion;
import static com.nike.cerberus.CerberusHttpHeaders.getXForwardedClientIp;

/**
 * Revokes the token supplied in the Vault token header.
 */
public class RevokeToken extends StandardEndpoint<Void, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AuthenticationService authenticationService;

    @Inject
    public RevokeToken(final AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> execute(final RequestInfo<Void> request,
                                                         final Executor longRunningTaskExecutor,
                                                         final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> revokeToken(request), ctx),
                longRunningTaskExecutor
        );
    }

    public ResponseInfo<Void> revokeToken(RequestInfo<Void> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            final CerberusPrincipal vaultAuthPrincipal =
                    (CerberusPrincipal) securityContext.get().getUserPrincipal();

            log.info("{}: {}, Delete Token Auth Event: the principal: {} with ip: {} is attempting to delete a token",
                    HEADER_X_CERBERUS_CLIENT,
                    getClientVersion(request),
                    vaultAuthPrincipal.getName(),
                    getXForwardedClientIp(request));

            authenticationService.revoke(vaultAuthPrincipal.getToken());
            return ResponseInfo.<Void>newBuilder().withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code()).build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/auth", HttpMethod.DELETE);
    }
}
