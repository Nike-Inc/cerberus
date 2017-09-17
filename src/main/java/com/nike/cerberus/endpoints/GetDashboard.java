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

import com.google.common.collect.ImmutableList;
import com.nike.cerberus.domain.DashboardResourceFile;
import com.nike.cerberus.service.DashboardAssetService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.server.http.impl.FullResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.CerberusHttpHeaders.HEADER_X_CERBERUS_CLIENT;
import static com.nike.cerberus.CerberusHttpHeaders.getClientVersion;
import static com.nike.cerberus.CerberusHttpHeaders.getXForwardedClientIp;

/**
 * Returns the dashboard.
 */
public class GetDashboard extends StandardEndpoint<Void, ImmutableList<Byte>> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String DASHBOARD_ENDPOINT_NO_TRAILING_SLASH = "/dashboard";

    private final DashboardAssetService dashboardAssetService;

    @Inject
    public GetDashboard(DashboardAssetService dashboardAssetService) {
        this.dashboardAssetService = dashboardAssetService;
    }

    @Override
    public CompletableFuture<ResponseInfo<ImmutableList<Byte>>> execute(RequestInfo<Void> request,
                                                                        Executor longRunningTaskExecutor,
                                                                        ChannelHandlerContext ctx) {
        if (StringUtils.endsWith(request.getPath(), DASHBOARD_ENDPOINT_NO_TRAILING_SLASH)) {
            /*
             Redirect requests from '/dashboard' to '/dashboard/'

             This is done to ensure that dashboard assets are loaded from this endpoint. Without this redirect,
              assets would be requested from the Dashboard with path '/asset.ext' instead of path '/dashboard/asset.ext'
            */
            return CompletableFuture.completedFuture(
                    ResponseInfo.<ImmutableList<Byte>>newBuilder()
                            .withHttpStatusCode(HttpResponseStatus.MOVED_PERMANENTLY.code())
                            .withHeaders(new DefaultHttpHeaders().add("Location", "/dashboard/"))
                            .build());
        }

        return CompletableFuture.completedFuture(getDashboardAsset(request));
    }

    private FullResponseInfo<ImmutableList<Byte>> getDashboardAsset(RequestInfo<Void> request) {
        DashboardResourceFile dashboardResource = dashboardAssetService.getFileContents(request);

        logger.debug("{}: {}, Get Dashboard Asset Event: ip: {} is attempting to get dashboard asset: '{}'",
                HEADER_X_CERBERUS_CLIENT,
                getClientVersion(request),
                getXForwardedClientIp(request),
                dashboardResource.getFileName());

        return ResponseInfo.<ImmutableList<Byte>>newBuilder()
                .withContentForFullResponse(dashboardResource.getFileContents())
                .withDesiredContentWriterMimeType(dashboardResource.getMimeType())
                .withHttpStatusCode(HttpResponseStatus.OK.code())
                .build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/dashboard/**", HttpMethod.GET);
    }
}
