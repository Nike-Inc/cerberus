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
import com.nike.cerberus.domain.VaultStyleErrorResponse;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class SecureDataEndpointV1<I, Object> extends StandardEndpoint<I, Object> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String BASE_PATH = "/v1/secret";
    protected static final String CATEGORY = "category";
    protected static final String SDB_SLUG = "sdb-slug";
    protected static final String PATH = "path";

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

    public final CompletableFuture<ResponseInfo<Object>> execute(final RequestInfo<I> request,
                                                            final Executor longRunningTaskExecutor,
                                                            final ChannelHandlerContext ctx) {

        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (! securityContext.isPresent() || ! (securityContext.get().getUserPrincipal() instanceof CerberusPrincipal)) {
            throw new ApiException(DefaultApiError.ACCESS_DENIED);
        }

        String category = request.getPathParam(CATEGORY);
        String sdbSlug = request.getPathParam(SDB_SLUG);
        String path = request.getPathParam(PATH);
        if (! doesRequestHaveRequiredParams(category, sdbSlug, path)) {
            return CompletableFuture.supplyAsync(
                    AsyncNettyHelper.supplierWithTracingAndMdc(this::permissionDenied, ctx),
                    longRunningTaskExecutor
            );
        }

        CerberusPrincipal principal = (CerberusPrincipal) securityContext.get().getUserPrincipal();

        String sdbBasePath = String.format("%s/%s/", category, sdbSlug);
        Optional<String> sdbId = safeDepositBoxService.getSafeDepositBoxIdByPath(sdbBasePath);

        if (! sdbId.isPresent() || ! permissionService.doesPrincipalHavePermission(principal, sdbId.get(),
                        SecureDataAction.fromMethod(request.getMethod()))) {

            return CompletableFuture.supplyAsync(
                    AsyncNettyHelper.supplierWithTracingAndMdc(this::permissionDenied, ctx),
                    longRunningTaskExecutor
            );

        }

        return doExecute(sdbId.get(), request, longRunningTaskExecutor, ctx, securityContext.get());
    }

    private boolean doesRequestHaveRequiredParams(String category, String sdbSlug, String path) {
        return isNotBlank(category) || isNotBlank(sdbSlug) || isNotBlank(path);
    }

    private ResponseInfo<Object> permissionDenied() {
        return (ResponseInfo<Object>) ResponseInfo.newBuilder(
                new VaultStyleErrorResponse.VaultStyleErrorResponseBuilder()
                        .withError("permission denied")
                        .build()
        ).withHttpStatusCode(HttpResponseStatus.FORBIDDEN.code())
        .build();
    }

    public abstract CompletableFuture<ResponseInfo<Object>> doExecute(String sdbId,
                                                                      RequestInfo<I> request,
                                                                      Executor longRunningTaskExecutor,
                                                                      ChannelHandlerContext ctx,
                                                                      SecurityContext securityContext);

}
