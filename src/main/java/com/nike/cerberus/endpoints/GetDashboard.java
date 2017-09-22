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

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.AssetResourceFile;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.StaticAssetManager;
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
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.CerberusHttpHeaders.HEADER_X_CERBERUS_CLIENT;
import static com.nike.cerberus.CerberusHttpHeaders.getClientVersion;
import static com.nike.cerberus.CerberusHttpHeaders.getXForwardedClientIp;

/**
 * Returns the dashboard.
 */
public class GetDashboard extends StandardEndpoint<Void, byte[]> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DASHBOARD_ENDPOINT = "/dashboard/";

    private static final String DASHBOARD_ENDPOINT_NO_TRAILING_SLASH = "/dashboard";

    private static final String VERSION_RESPONSE_FORMAT = "{\"version\": \"%s\"}";

    private static final String DEFAULT_DASHBOARD_ASSET_FILE_NAME = "index.html";

    private static final String VERSION_FILE_NAME = "version";

    private final StaticAssetManager dashboardAssetManager;

    private final String cmsVersion;

    @Inject
    public GetDashboard(@Named("dashboardAssetManager") StaticAssetManager dashboardAssetManager,
                        @Named("service.version") String cmsVersion) {
        this.dashboardAssetManager = dashboardAssetManager;
        this.cmsVersion = cmsVersion;
    }

    @Override
    public CompletableFuture<ResponseInfo<byte[]>> execute(RequestInfo<Void> request,
                                                                        Executor longRunningTaskExecutor,
                                                                        ChannelHandlerContext ctx) {
        if (StringUtils.endsWith(request.getPath(), DASHBOARD_ENDPOINT_NO_TRAILING_SLASH)) {
            /*
             Redirect requests from '/dashboard' to '/dashboard/'

             Without this redirect, assets would be requested with URI '/asset.ext' instead of '/dashboard/asset.ext'.
             This is important because the '/dashboard' prefix is needed in order to match this endpoint.
            */
            return CompletableFuture.completedFuture(
                    ResponseInfo.<byte[]>newBuilder()
                            .withHttpStatusCode(HttpResponseStatus.MOVED_PERMANENTLY.code())
                            .withHeaders(new DefaultHttpHeaders().add(HttpHeaders.LOCATION, DASHBOARD_ENDPOINT))
                            .build());
        } else if (StringUtils.endsWith(request.getPath(), VERSION_FILE_NAME)) {
            String versionJson = String.format(VERSION_RESPONSE_FORMAT, cmsVersion);
            byte[] versionJsonInBytes = versionJson.getBytes(Charset.defaultCharset());
            return CompletableFuture.completedFuture(
                    ResponseInfo.<byte[]>newBuilder()
                            .withDesiredContentWriterMimeType("application/json")
                            .withContentForFullResponse(versionJsonInBytes)
                            .withHttpStatusCode(HttpResponseStatus.OK.code())
                            .build());
        } else {
            return CompletableFuture.completedFuture(getDashboardAsset(request));
        }
    }

    private FullResponseInfo<byte[]> getDashboardAsset(RequestInfo<Void> request) {
        logger.info("{}: {}, Get Dashboard Asset Event: ip: {} is attempting to get dashboard asset: '{}'",
                HEADER_X_CERBERUS_CLIENT,
                getClientVersion(request),
                getXForwardedClientIp(request),
                request.getPath());

        String filePath = getFilePath(request);
        AssetResourceFile dashboardResource = dashboardAssetManager.get(filePath).orElseThrow(() ->
                ApiException.newBuilder()
                        .withApiErrors(DefaultApiError.FAILED_TO_READ_DASHBOARD_ASSET_CONTENT)
                        .withExceptionMessage("Could not load dashboard asset: " + filePath)
                        .build());

        return ResponseInfo.<byte[]>newBuilder()
                .withContentForFullResponse(dashboardResource.getFileContents())
                .withDesiredContentWriterMimeType(dashboardResource.getMimeType())
                .withHttpStatusCode(HttpResponseStatus.OK.code())
                .build();
    }

    private String getFilePath(RequestInfo<Void> request) {
        String filename = StringUtils.substringAfterLast(request.getPath(), DASHBOARD_ENDPOINT);
        filename = filename.isEmpty() ? DEFAULT_DASHBOARD_ASSET_FILE_NAME : filename;

        return filename;
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/dashboard/**", HttpMethod.GET);
    }
}
