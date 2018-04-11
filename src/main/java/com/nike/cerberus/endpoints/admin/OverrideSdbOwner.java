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
 */

package com.nike.cerberus.endpoints.admin;

import com.google.inject.Inject;
import com.nike.cerberus.domain.SDBMetadata;
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.endpoints.CustomizableAuditData;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.util.Slugger;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Allows an Admin to override the owner of an SDB
 */
public class OverrideSdbOwner extends AdminStandardEndpoint<SDBMetadata, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SafeDepositBoxService safeDepositBoxService;

    @Inject
    public OverrideSdbOwner(SafeDepositBoxService safeDepositBoxService) {
        this.safeDepositBoxService = safeDepositBoxService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(final RequestInfo<SDBMetadata> request,
                                                           final Executor longRunningTaskExecutor,
                                                           final ChannelHandlerContext ctx,
                                                           final SecurityContext securityContext) {

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> overrideSdbOwner(request, securityContext), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<Void> overrideSdbOwner(RequestInfo<SDBMetadata> request, SecurityContext securityContext) {
        CerberusPrincipal authPrincipal = (CerberusPrincipal) securityContext.getUserPrincipal();
        String sdbName = request.getContent().getName();
        String newOwner = request.getContent().getOwner();
        String principal = authPrincipal.getName();

        safeDepositBoxService.overrideOwner(sdbName, newOwner, principal);

        return ResponseInfo.<Void>newBuilder()
                .withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code())
                .build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/admin/override-owner", HttpMethod.PUT);
    }

    @Override
    public CustomizableAuditData getCustomizableAuditData(RequestInfo<SDBMetadata> request) {
        String sdbName = request.getContent().getName();
        String newOwner = request.getContent().getOwner();
        Optional<String> sdbIdOptional = safeDepositBoxService.getSafeDepositBoxIdByName(sdbName);
        String sdbId = sdbIdOptional.orElse(String.format("(Failed to lookup id from name: %s)", sdbName));
        return new CustomizableAuditData()
                .setDescription(String.format("Override SDB with name: '%s' and id: '%s' with new owner '%s'.", sdbName, sdbId, newOwner))
                .setSdbNameSlug(Slugger.toSlug(sdbName));
    }
}
