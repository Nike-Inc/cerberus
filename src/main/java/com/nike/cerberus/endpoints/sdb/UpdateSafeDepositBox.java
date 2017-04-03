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
import com.nike.cerberus.domain.SafeDepositBox;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Endpoint for updating a safe deposit box.
 */
public class UpdateSafeDepositBox extends StandardEndpoint<SafeDepositBox, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String HEADER_X_REFRESH_TOKEN = "X-Refresh-Token";

    private final SafeDepositBoxService safeDepositBoxService;

    @Inject
    public UpdateSafeDepositBox(final SafeDepositBoxService safeDepositBoxService) {
        this.safeDepositBoxService = safeDepositBoxService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> execute(RequestInfo<SafeDepositBox> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(() -> updateSafeDepositBox(request), longRunningTaskExecutor);
    }

    private ResponseInfo<Void> updateSafeDepositBox(final RequestInfo<SafeDepositBox> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            final VaultAuthPrincipal vaultAuthPrincipal = (VaultAuthPrincipal) securityContext.get().getUserPrincipal();

            String sdbId = request.getPathParam("id");
            Optional<String> sdbNameOptional = safeDepositBoxService.getSafeDepositBoxNameById(sdbId);
            String sdbName = sdbNameOptional.isPresent() ? sdbNameOptional.get() : String.format("(Failed to lookup name from id: %s)", sdbId);
            log.info("Update SDB Event: the principal: {} is attempting to update an SDB with name: {}", vaultAuthPrincipal.getName(), sdbName);

            safeDepositBoxService.updateSafeDepositBox(request.getContent(),
                    vaultAuthPrincipal.getUserGroups(),
                    vaultAuthPrincipal.getName(),
                    sdbId);
            return ResponseInfo.<Void>newBuilder().withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code())
                    .withHeaders(new DefaultHttpHeaders().set(HEADER_X_REFRESH_TOKEN, Boolean.TRUE.toString()))
                    .build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/safe-deposit-box/{id}", HttpMethod.PUT);
    }

    @Override
    public Class[] validationGroups(RequestInfo<?> request) {
        return new Class[] {
            Updatable.class
        };
    }
}
