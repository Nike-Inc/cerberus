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

package com.nike.cerberus.endpoints;

import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * robots.txt that says never index this site
 */
@RiposteEndpoint
public class RobotsEndpoint extends StandardEndpoint<Void, String> {

    private static final String ROBOTS_DISALLOW = "User-agent: *\nDisallow: /";
    private static final String TEXT_PLAIN = "text/plain";

    @Override
    public CompletableFuture<ResponseInfo<String>> execute(RequestInfo<Void> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx) {
        return CompletableFuture.completedFuture(ResponseInfo.<String>newBuilder()
                .withDesiredContentWriterMimeType(TEXT_PLAIN)
                .withContentForFullResponse(ROBOTS_DISALLOW)
                .withHttpStatusCode(200).build()
        );
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/robots.txt");
    }
}
