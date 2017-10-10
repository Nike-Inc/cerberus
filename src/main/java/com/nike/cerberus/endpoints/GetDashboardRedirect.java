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
 *
 */

package com.nike.cerberus.endpoints;

import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.http.HttpHeaders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.endpoints.GetDashboard.DASHBOARD_ENDPOINT;

/**
 * Redirect endpoint to the dashboard.
 */
public class GetDashboardRedirect extends StandardEndpoint<Void, Void> {

    @Override
    public CompletableFuture<ResponseInfo<Void>> execute(RequestInfo<Void> request,
                                                         Executor longRunningTaskExecutor,
                                                         ChannelHandlerContext ctx) {
        return CompletableFuture.completedFuture(
                ResponseInfo.<Void>newBuilder()
                        .withHttpStatusCode(HttpResponseStatus.MOVED_PERMANENTLY.code())
                        .withHeaders(new DefaultHttpHeaders().add(HttpHeaders.LOCATION, DASHBOARD_ENDPOINT))
                        .build());
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/", HttpMethod.GET);
    }
}
