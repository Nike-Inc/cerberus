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

package com.nike.cerberus.endpoints;

import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Dummy health check endpoint (necessary until we implement a real health check system).
 *
 * @author Nic Munroe
 */
public class HealthCheckEndpoint extends StandardEndpoint<Void, String> {
    @Override
    public CompletableFuture<ResponseInfo<String>> execute(RequestInfo<Void> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx) {
        return CompletableFuture.completedFuture(ResponseInfo.<String>newBuilder()
                .withHttpStatusCode(HttpResponseStatus.OK.code())
                .withContentForFullResponse("CMS is running " + new Date())
                .build());
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/healthcheck");
    }
}
