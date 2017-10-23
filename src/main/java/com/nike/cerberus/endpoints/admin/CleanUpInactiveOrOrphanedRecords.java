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
import com.nike.cerberus.domain.CleanUpRequest;
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.service.CleanUpService;
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
 * Cleans up inactive or orphaned KMS keys and IAM roles.
 *
 * This is required because orphaned CMKs, KMS key DB records, and IAM role DB records are created when an SDB is deleted.
 * Thus this endpoint exists to clean up those already existing orphaned records in the DB, as well as orphaned records
 * made from future SDB deletions.
 *
 * The reason that this clean up is not done at the time of SDB deletion is to lessen code complexity and give control
 * to the administrators over when KMS keys are deleted.
 */
public class CleanUpInactiveOrOrphanedRecords extends AdminStandardEndpoint<CleanUpRequest, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CleanUpService cleanUpService;

    @Inject
    public CleanUpInactiveOrOrphanedRecords(CleanUpService cleanUpService) {
        this.cleanUpService = cleanUpService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(final RequestInfo<CleanUpRequest> request,
                                                           final Executor longRunningTaskExecutor,
                                                           final ChannelHandlerContext ctx,
                                                           final SecurityContext securityContext) {

        longRunningTaskExecutor.execute(AsyncNettyHelper.runnableWithTracingAndMdc(
                () -> cleanUpService.cleanUp(request.getContent()),
                ctx
        ));

        return CompletableFuture.completedFuture(
                ResponseInfo.<Void>newBuilder()
                        .withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code())
                        .build()
        );
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/cleanup", HttpMethod.PUT);
    }

}
