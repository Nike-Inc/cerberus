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

import com.google.inject.name.Named;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.DashboardResourceFile;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.util.DashboardResourceFileHelper;
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
import java.util.Map;
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

    private static final String DASHBOARD_URI_NO_TRAILING_SLASH = "/dashboard";

    private static final String DEFAULT_DASHBOARD_ASSET_FILE_NAME = "index.html";

    private static final String RESOURCE_FILENAME_SEPARATOR = "dashboard/";

    private static final String VERSION_ENDPOINT_NAME = "version";

    private static final String VERSION_RESPONSE_FORMAT = "{\"version\": \"%s\"}";

    private final  Map<String, DashboardResourceFile> dashboardAssetMap;

    private final String cmsVersion;

    @Inject
    public GetDashboard(@Named("dashboardAssetMap") Map<String, DashboardResourceFile> dashboardAssetMap,
                        @Named("service.version") String cmsVersion) {
        this.dashboardAssetMap = dashboardAssetMap;
        this.cmsVersion = cmsVersion;
    }

    @Override
    public CompletableFuture<ResponseInfo<byte[]>> execute(RequestInfo<Void> request,
                                                           Executor longRunningTaskExecutor,
                                                           ChannelHandlerContext ctx) {
        if (StringUtils.endsWith(request.getPath(), DASHBOARD_URI_NO_TRAILING_SLASH)) {
            /*
             Redirect requests from '/dashboard' to '/dashboard/'

             This is done to ensure that dashboard assets are loaded from this endpoint. Without this redirect,
              assets would be requested from the Dashboard with path '/asset.ext' instead of path '/dashboard/asset.ext'
            */
            return CompletableFuture.completedFuture(
                    ResponseInfo.<byte[]>newBuilder()
                            .withHttpStatusCode(HttpResponseStatus.MOVED_PERMANENTLY.code())
                            .withHeaders(new DefaultHttpHeaders().add("Location", "/dashboard/"))
                            .build());
        }

        String filename = StringUtils.substringAfterLast(request.getPath(), RESOURCE_FILENAME_SEPARATOR);
        filename = filename.isEmpty() ? DEFAULT_DASHBOARD_ASSET_FILE_NAME : filename;

        return CompletableFuture.completedFuture(getDashboardAsset(request, filename));
    }

    private FullResponseInfo<byte[]> getDashboardAsset(RequestInfo<Void> request, String filename) {
        logger.info("{}: {}, Get Dashboard Asset Event: ip: {} is attempting to get dashboard asset: '{}'",
                HEADER_X_CERBERUS_CLIENT,
                getClientVersion(request),
                getXForwardedClientIp(request),
                filename);

        if (filename.equals(VERSION_ENDPOINT_NAME)) {
            String versionJson = String.format(VERSION_RESPONSE_FORMAT, cmsVersion);
            return ResponseInfo.<byte[]>newBuilder()
                    .withContentForFullResponse(versionJson.getBytes())
                    .withDesiredContentWriterMimeType(DashboardResourceFileHelper.FILE_EXT_TO_MIME_TYPE_MAP.get("json"))
                    .withHttpStatusCode(HttpResponseStatus.OK.code())
                    .build();
        } else if (dashboardAssetMap.containsKey(filename)){
            DashboardResourceFile resource = dashboardAssetMap.get(filename);
            return ResponseInfo.<byte[]>newBuilder()
                    .withContentForFullResponse(resource.getFileContents())
                    .withDesiredContentWriterMimeType(resource.getMimeType())
                    .withHttpStatusCode(HttpResponseStatus.OK.code())
                    .build();
        } else {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.FAILED_TO_READ_DASHBOARD_ASSET_CONTENT)
                    .build();
        }
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/dashboard/**", HttpMethod.GET);
    }
}
