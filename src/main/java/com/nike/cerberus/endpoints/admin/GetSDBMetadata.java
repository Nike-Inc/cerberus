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
import com.nike.cerberus.domain.SDBMetadataResult;
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.endpoints.RiposteEndpoint;
import com.nike.cerberus.service.MetadataService;
import com.nike.cerberus.service.PaginationService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.impl.FullResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Returns meta data for all SDBs in CMS
 */
@RiposteEndpoint
public class GetSDBMetadata extends AdminStandardEndpoint<Void, SDBMetadataResult> {

    private final MetadataService metadataService;

    private final PaginationService paginationService;

    @Inject
    public GetSDBMetadata(MetadataService metadataService,
                          PaginationService paginationService) {
        this.metadataService = metadataService;
        this.paginationService = paginationService;
    }

    @Override
    public CompletableFuture<ResponseInfo<SDBMetadataResult>> doExecute(final RequestInfo<Void> request,
                                                                        final Executor longRunningTaskExecutor,
                                                                        final ChannelHandlerContext ctx,
                                                                        final SecurityContext securityContext) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> getMetadata(request), ctx),
                longRunningTaskExecutor
        );
    }

    private FullResponseInfo<SDBMetadataResult> getMetadata(RequestInfo<Void> request) {
        String sdbNameFilter = request.getQueryParamSingle("sdbName");
        return ResponseInfo.newBuilder(
            metadataService.getSDBMetadata(
                paginationService.getLimit(request),
                paginationService.getOffset(request),
                sdbNameFilter
            )).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/metadata", HttpMethod.GET);
    }

}
