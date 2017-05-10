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
 */

package com.nike.cerberus.endpoints.admin;

import com.google.inject.Inject;
import com.nike.cerberus.domain.SDBMetadata;
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.cerberus.service.MetadataService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Allows an Admin to restore (create or update) Metadata for an SDB
 */
public class PutSDBMetadata extends AdminStandardEndpoint<SDBMetadata, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final MetadataService metadataService;

    @Inject
    public PutSDBMetadata(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(RequestInfo<SDBMetadata> request,
                                                           Executor longRunningTaskExecutor,
                                                           ChannelHandlerContext ctx,
                                                           SecurityContext securityContext) {

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> restoreSdbMetadata(request, securityContext), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<Void> restoreSdbMetadata(RequestInfo<SDBMetadata> request, SecurityContext securityContext) {
        VaultAuthPrincipal vaultAuthPrincipal = (VaultAuthPrincipal) securityContext.getUserPrincipal();

        String principal = vaultAuthPrincipal.getName();

        log.info("Metadata Restore Event: the principal {} is attempting to restore sdb name: '{}'", principal, request.getContent().getName());

        metadataService.restoreMetadata(request.getContent(), principal);

        return ResponseInfo.<Void>newBuilder()
                .withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code())
                .build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/metadata", HttpMethod.PUT);
    }
}
