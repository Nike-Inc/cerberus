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

import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.domain.Stats;
import com.nike.cerberus.service.MetaDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Returns basic stats about the safe deposit boxes in Cerberus.
 */
@Deprecated
public class GetStats extends AdminStandardEndpoint<Void, Stats> {

    private final MetaDataService metaDataService;

    @Inject
    public GetStats(final MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Stats>> doExecute(final RequestInfo<Void> request,
                                                            final Executor longRunningTaskExecutor,
                                                            final ChannelHandlerContext ctx,
                                                            final SecurityContext securityContext) {
        return CompletableFuture.supplyAsync(
                () -> ResponseInfo.newBuilder(metaDataService.getStats()).build(),
                longRunningTaskExecutor
        );
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/stats", HttpMethod.GET);
    }
}
