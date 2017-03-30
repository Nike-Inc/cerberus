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

package com.nike.cerberus.endpoints.sdb;

import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SafeDepositBox;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.VaultAuthPrincipalV1;
import com.nike.cerberus.service.SafeDepositBoxService;
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
 * Extracts the user groups from the security context for the request and attempts to get details about the safe
 * deposit box by its unique id.
 */
public class GetSafeDepositBox extends StandardEndpoint<Void, SafeDepositBox> {

    private final SafeDepositBoxService safeDepositBoxService;

    @Inject
    public GetSafeDepositBox(final SafeDepositBoxService safeDepositBoxService) {
        this.safeDepositBoxService = safeDepositBoxService;
    }

    @Override
    public CompletableFuture<ResponseInfo<SafeDepositBox>> execute(final RequestInfo<Void> request,
                                                                                final Executor longRunningTaskExecutor,
                                                                                final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(() -> getSafeDepositBox(request), longRunningTaskExecutor);
    }

    public ResponseInfo<SafeDepositBox> getSafeDepositBox(final RequestInfo<Void> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            final VaultAuthPrincipalV1 vaultAuthPrincipal = (VaultAuthPrincipalV1) securityContext.get().getUserPrincipal();
            final Optional<SafeDepositBox> safeDepositBox =
                    safeDepositBoxService.getAssociatedSafeDepositBox(
                            vaultAuthPrincipal.getUserGroups(),
                            request.getPathParam("id"));

            if (safeDepositBox.isPresent()) {
                return ResponseInfo.newBuilder(safeDepositBox.get()).build();
            }

            throw ApiException.newBuilder().withApiErrors(SampleCoreApiError.NOT_FOUND).build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/safe-deposit-box/{id}", HttpMethod.GET);
    }
}
