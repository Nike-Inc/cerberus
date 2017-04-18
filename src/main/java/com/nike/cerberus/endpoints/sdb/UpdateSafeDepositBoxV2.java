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

package com.nike.cerberus.endpoints.sdb;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SafeDepositBoxV2;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.validation.group.Updatable;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Endpoint for updating a safe deposit box.
 */
public class UpdateSafeDepositBoxV2 extends StandardEndpoint<SafeDepositBoxV2, SafeDepositBoxV2> {

    public static final String HEADER_X_REFRESH_TOKEN = "X-Refresh-Token";

    private final SafeDepositBoxService safeDepositBoxService;

    @Inject
    public UpdateSafeDepositBoxV2(final SafeDepositBoxService safeDepositBoxService) {
        this.safeDepositBoxService = safeDepositBoxService;
    }

    @Override
    public CompletableFuture<ResponseInfo<SafeDepositBoxV2>> execute(RequestInfo<SafeDepositBoxV2> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(() -> updateSafeDepositBox(request), longRunningTaskExecutor);
    }

    private ResponseInfo<SafeDepositBoxV2> updateSafeDepositBox(final RequestInfo<SafeDepositBoxV2> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            final VaultAuthPrincipal vaultAuthPrincipal = (VaultAuthPrincipal) securityContext.get().getUserPrincipal();
            SafeDepositBoxV2 safeDepositBoxV2 = safeDepositBoxService.updateSafeDepositBoxV2(request.getContent(),
                    vaultAuthPrincipal.getUserGroups(),
                    vaultAuthPrincipal.getName(),
                    request.getPathParam("id"));
            return ResponseInfo.newBuilder(safeDepositBoxV2)
                    .withHeaders(new DefaultHttpHeaders().set(HEADER_X_REFRESH_TOKEN, Boolean.TRUE.toString()))
                    .withHttpStatusCode(HttpResponseStatus.OK.code())
                    .build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v2/safe-deposit-box/{id}", HttpMethod.PUT);
    }

    @Override
    public Class[] validationGroups(RequestInfo<?> request) {
        return new Class[] {
            Updatable.class
        };
    }
}
