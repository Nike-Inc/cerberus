/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.endpoints.version;

import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.endpoints.CustomizableAuditData;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.record.SafeDepositBoxVersionRecord;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.util.Slugger;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import com.nike.riposte.util.MultiMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Extracts the user groups from the security context for the request and returns any safe deposit boxes
 * associated with that list of user groups.
 */
public class GetSafeDepositBoxVersions extends AuditableEventEndpoint<Void, List<SafeDepositBoxVersionRecord>> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SafeDepositBoxService safeDepositBoxService;

    private final PermissionsService permissionsService;

    @Inject
    public GetSafeDepositBoxVersions(SafeDepositBoxService safeDepositBoxService,
                                     PermissionsService permissionsService) {
        this.safeDepositBoxService = safeDepositBoxService;
        this.permissionsService = permissionsService;
    }

    @Override
    public CompletableFuture<ResponseInfo<List<SafeDepositBoxVersionRecord>>> doExecute(final RequestInfo<Void> request,
                                                                                        final Executor longRunningTaskExecutor,
                                                                                        final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> getVersionPathsForSdb(request), ctx),
                longRunningTaskExecutor
        );
    }

    public ResponseInfo<List<SafeDepositBoxVersionRecord>> getVersionPathsForSdb(final RequestInfo<Void> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            final CerberusPrincipal authPrincipal = (CerberusPrincipal) securityContext.get().getUserPrincipal();
            String sdbId = request.getPathParam("id");

            boolean hasPermissionToSdb = permissionsService.doesPrincipalHaveReadPermission(authPrincipal, sdbId);
            if (hasPermissionToSdb) {
                return ResponseInfo.newBuilder(
                        safeDepositBoxService.getSafeDepositBoxVersions(sdbId)).build();
            } else {
                AuditableEvent auditableEvent = auditableEvent(authPrincipal, request, getClass().getSimpleName())
                        .withAction(String.format("Failed to get version paths for SDB with ID: %s. Permission denied.", sdbId))
                        .withSuccess(false)
                        .build();
                eventProcessorService.ingestEvent(auditableEvent);

                throw ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.ACCESS_DENIED)
                        .build();
            }
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
    }

    @Override
    public Matcher requestMatcher() {

        return MultiMatcher.match(Sets.newHashSet("/v1/safe-deposit-box-versions/{id}"), HttpMethod.GET);
    }

    @Override
    protected CustomizableAuditData getCustomizableAuditData(RequestInfo<Void> request) {
        String sdbId = request.getPathParam("id");
        Optional<String> sdbNameOptional = safeDepositBoxService.getSafeDepositBoxNameById(sdbId);
        String sdbName = sdbNameOptional.orElse(String.format("(Failed to lookup name from id: %s)", sdbId));
        return  new CustomizableAuditData()
                .setDescription(String.format("Listing versions for SDB with name: '%s' and id: '%s'", sdbName, sdbId))
                .setSdbNameSlug(sdbNameOptional.map(Slugger::toSlug).orElse("_unknown_"));
    }
}
