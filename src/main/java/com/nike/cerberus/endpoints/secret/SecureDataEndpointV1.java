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
import com.nike.cerberus.domain.SecureDataRequestInfo;
import com.nike.cerberus.domain.VaultStyleErrorResponse;
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.SecureDataRequestService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class SecureDataEndpointV1<I, O> extends AuditableEventEndpoint<I, O> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String BASE_PATH = "/v1/secret";

    protected final SecureDataService secureDataService;
    protected final PermissionsService permissionService;
    protected final SafeDepositBoxService safeDepositBoxService;
    protected final SecureDataRequestService secureDataRequestService;

    @Inject
    protected SecureDataEndpointV1(SecureDataService secureDataService,
                                   PermissionsService permissionService,
                                   SafeDepositBoxService safeDepositBoxService,
                                   SecureDataRequestService secureDataRequestService) {

        this.secureDataService = secureDataService;
        this.permissionService = permissionService;
        this.safeDepositBoxService = safeDepositBoxService;
        this.secureDataRequestService = secureDataRequestService;
    }

    public final CompletableFuture<ResponseInfo<O>> doExecute(RequestInfo<I> request,
                                                            Executor longRunningTaskExecutor,
                                                            ChannelHandlerContext ctx) {

        SecureDataRequestInfo requestInfo;
        try {
            requestInfo = secureDataRequestService.parseAndValidateRequest(request);
        } catch (ApiException ae) {
            return generateVaultStyleResponse(longRunningTaskExecutor,
                    ctx,
                    VaultStyleErrorResponse.Builder.create()
                            .withError("permission denied")
                            .build(),
                    HttpResponseStatus.FORBIDDEN.code()
            );
        }

        return executeSecureDataCall(requestInfo, request, longRunningTaskExecutor, ctx);
    }

    protected CompletableFuture<ResponseInfo<O>> generateVaultStyleResponse(Executor longRunningTaskExecutor,
                                                         ChannelHandlerContext ctx,
                                                         VaultStyleErrorResponse response,
                                                         int statusCode) {

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> generateVaultStyleResponse(response, statusCode), ctx),
                longRunningTaskExecutor
        );

    }

    @Override
    protected String getSlugifiedSdbName(RequestInfo<I> request) {
        SecureDataRequestInfo requestInfo = secureDataRequestService.parseRequestPathInfo(request.getPath());

        return requestInfo.getSdbSlug();
    }

    protected abstract ResponseInfo<O> generateVaultStyleResponse(VaultStyleErrorResponse response, int statusCode);

    protected abstract CompletableFuture<ResponseInfo<O>> executeSecureDataCall(SecureDataRequestInfo requestInfo,
                                                                                RequestInfo<I> request,
                                                                                Executor longRunningTaskExecutor,
                                                                                ChannelHandlerContext ctx);

}
