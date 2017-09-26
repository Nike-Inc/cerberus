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

package com.nike.cerberus.endpoints.secret;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.SecureDataAction;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class SecureDataEndpointV1<I, O> extends StandardEndpoint<I, O> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String BASE_PATH = "/v1/secret";

    protected final SecureDataService secureDataService;
    protected final PermissionsService permissionService;
    protected final SafeDepositBoxService safeDepositBoxService;

    @Inject
    protected SecureDataEndpointV1(SecureDataService secureDataService,
                                   PermissionsService permissionService,
                                   SafeDepositBoxService safeDepositBoxService) {

        this.secureDataService = secureDataService;
        this.permissionService = permissionService;
        this.safeDepositBoxService = safeDepositBoxService;
    }

    public final CompletableFuture<ResponseInfo<O>> execute(final RequestInfo<I> request,
                                                            final Executor longRunningTaskExecutor,
                                                            final ChannelHandlerContext ctx) {

        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (! securityContext.isPresent() || ! (securityContext.get().getUserPrincipal() instanceof CerberusPrincipal)) {
            throw new ApiException(DefaultApiError.ACCESS_DENIED);
        }

        SecureDataRequestInfo requestInfo = parseInfoFromPath(request.getPath());

        if (! doesRequestHaveRequiredParams(requestInfo.getCategory(), requestInfo.getSdbSlug())) {
            throw new ApiException(DefaultApiError.ACCESS_DENIED);
        }

        CerberusPrincipal principal = (CerberusPrincipal) securityContext.get().getUserPrincipal();

        String sdbBasePath = String.format("%s/%s/", requestInfo.getCategory(), requestInfo.getSdbSlug());
        Optional<String> sdbId = safeDepositBoxService.getSafeDepositBoxIdByPath(sdbBasePath);

        if (! sdbId.isPresent() || ! permissionService.doesPrincipalHavePermission(principal, sdbId.get(),
                        SecureDataAction.fromMethod(request.getMethod()))) {
            throw new ApiException(DefaultApiError.ACCESS_DENIED);
        }

        requestInfo.setSdbid(sdbId.get());

        return doExecute(requestInfo, request, longRunningTaskExecutor, ctx, securityContext.get());
    }

    private boolean doesRequestHaveRequiredParams(String category, String sdbSlug) {
        return isNotBlank(category) || isNotBlank(sdbSlug);
    }

    protected SecureDataRequestInfo parseInfoFromPath(String path) {
        String[] parts = path.split("/", 6);
        SecureDataRequestInfo info = new SecureDataRequestInfo();
        if (parts.length >= 4) {
            info.setCategory(parts[3]);
        }
        if (parts.length >= 5) {
            info.setSdbSlug(parts[4]);
        }

        if (parts.length >= 6) {
            info.setPath(parts[5]);
        }

        return info;
    }

    protected class SecureDataRequestInfo {
        private String category;
        private String sdbSlug;
        private String sdbid;
        private String path;

        public String getCategory() {
            return category;
        }

        public SecureDataRequestInfo setCategory(String category) {
            this.category = category;
            return this;
        }

        public String getSdbSlug() {
            return sdbSlug;
        }

        public SecureDataRequestInfo setSdbSlug(String sdbSlug) {
            this.sdbSlug = sdbSlug;
            return this;
        }

        public String getSdbid() {
            return sdbid;
        }

        public SecureDataRequestInfo setSdbid(String sdbid) {
            this.sdbid = sdbid;
            return this;
        }

        public String getPath() {
            return path;
        }

        public SecureDataRequestInfo setPath(String path) {
            this.path = path;
            return this;
        }

        public String getFullPath() {
            return String.format("%s/%s", sdbSlug, path);
        }
    }

    public abstract CompletableFuture<ResponseInfo<O>> doExecute(SecureDataRequestInfo requestInfo,
                                                                 RequestInfo<I> request,
                                                                 Executor longRunningTaskExecutor,
                                                                 ChannelHandlerContext ctx,
                                                                 SecurityContext securityContext);

}
