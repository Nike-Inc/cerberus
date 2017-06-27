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

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Extracts the user groups from the security context for the request and attempts to get details about the safe
 * deposit box by its unique id.
 */
@Deprecated
public class GetSafeDepositBoxV1 extends StandardEndpoint<Void, SafeDepositBoxV1> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SafeDepositBoxService safeDepositBoxService;

    @Inject
    public GetSafeDepositBoxV1(final SafeDepositBoxService safeDepositBoxService) {
        this.safeDepositBoxService = safeDepositBoxService;
    }

    @Override
    public CompletableFuture<ResponseInfo<SafeDepositBoxV1>> execute(final RequestInfo<Void> request,
                                                                     final Executor longRunningTaskExecutor,
                                                                     final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> getSafeDepositBox(request), ctx),
                longRunningTaskExecutor
        );
    }

    public ResponseInfo<SafeDepositBoxV1> getSafeDepositBox(final RequestInfo<Void> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            final VaultAuthPrincipal vaultAuthPrincipal = (VaultAuthPrincipal) securityContext.get().getUserPrincipal();

            String sdbId = request.getPathParam("id");
            Optional<String> sdbNameOptional = safeDepositBoxService.getSafeDepositBoxNameById(sdbId);
            String sdbName = sdbNameOptional.isPresent() ? sdbNameOptional.get() :
                    String.format("(Failed to lookup name from id: %s)", sdbId);
            log.info("Read SDB Event: the principal: {} is attempting to read sdb name: '{}' and id: '{}'",
                    vaultAuthPrincipal.getName(), sdbName, sdbId);

            final SafeDepositBoxV1 safeDepositBox =
                    safeDepositBoxService.getSDBAndValidatePrincipalAssociationV1(
                            vaultAuthPrincipal,
                            sdbId);

            return ResponseInfo.newBuilder(safeDepositBox).build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/safe-deposit-box/{id}", HttpMethod.GET);
    }
}
