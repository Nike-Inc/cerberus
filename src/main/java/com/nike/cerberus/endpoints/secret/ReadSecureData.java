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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.nike.cerberus.domain.SecureDataResponse;
import com.nike.cerberus.domain.VaultStyleErrorResponse;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import com.nike.riposte.util.MultiMatcher;
import com.nike.wingtips.Tracer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ReadSecureData extends SecureDataEndpointV1<Void, Object> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    protected ReadSecureData(SecureDataService secureDataService,
                             PermissionsService permissionService,
                             SafeDepositBoxService safeDepositBoxService) {

        super(secureDataService, permissionService, safeDepositBoxService);
    }

    @Override
    protected ResponseInfo<Object> generateVaultStyleResponse(VaultStyleErrorResponse response, int statusCode) {
        return ResponseInfo.newBuilder()
                .withContentForFullResponse(response)
                .withHttpStatusCode(statusCode)
                .build();
    }

    @Override
    public CompletableFuture<ResponseInfo<Object>> doExecute(SecureDataRequestInfo requestInfo,
                                                                         RequestInfo<Void> request,
                                                                         Executor longRunningTaskExecutor,
                                                                         ChannelHandlerContext ctx,
                                                                         SecurityContext securityContext) {

        if (StringUtils.equalsIgnoreCase(request.getQueryParamSingle("list"), "true")) {
            return CompletableFuture.supplyAsync(
                    AsyncNettyHelper.supplierWithTracingAndMdc(() -> listKeys(requestInfo), ctx),
                    longRunningTaskExecutor
            );
        }

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> readSecureData(requestInfo), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<Object> listKeys(SecureDataRequestInfo info) {
        Set<String> keys = secureDataService.listKeys(info.getPath());

        if (keys.isEmpty()) {
            return generateVaultStyleResponse(VaultStyleErrorResponse.Builder.create().build(),
                    HttpResponseStatus.NOT_FOUND.code());
        }

        SecureDataResponse response = new SecureDataResponse();

        String requestId;
        Tracer tracer = Tracer.getInstance();
        if (tracer != null &&
                tracer.getCurrentSpan() != null &&
                StringUtils.isNotBlank(tracer.getCurrentSpan().getTraceId())) {

            requestId = tracer.getCurrentSpan().getTraceId();
        } else {
            requestId = UUID.randomUUID().toString();
        }

        response.setRequestId(requestId); // maybe use trace id?
        response.setData(ImmutableMap.of("keys", keys));

        return ResponseInfo.newBuilder()
                .withContentForFullResponse(response)
                .withHttpStatusCode(HttpResponseStatus.OK.code())
                .build();
    }
    private ResponseInfo<Object> readSecureData(SecureDataRequestInfo info) {

        Optional<String> data = secureDataService.readSecret(info.getPath());

        if (! data.isPresent()) {
            return generateVaultStyleResponse(VaultStyleErrorResponse.Builder.create().build(),
                    HttpResponseStatus.NOT_FOUND.code());
        }

        SecureDataResponse response = new SecureDataResponse();
        response.setRequestId(UUID.randomUUID().toString());
        try {
            response.setData(new ObjectMapper().readTree(data.get()));
        } catch (IOException e) {
            log.error("Failed to deserialize stored data", e);
        }

        return ResponseInfo.newBuilder()
                .withContentForFullResponse(response)
                .withHttpStatusCode(HttpResponseStatus.OK.code())
                .build();
    }

    @Override
    public Matcher requestMatcher() {
        return MultiMatcher.match(
                Sets.newHashSet(
                        String.format("%s/**", BASE_PATH),
                        BASE_PATH
                ),
                HttpMethod.GET
        );
    }

    @Override
    protected String describeActionForAuditEvent(RequestInfo<Void> request) {
        String path = request.getPath();
        if (StringUtils.equalsIgnoreCase(request.getQueryParamSingle("list"), "true")) {
            return String.format("Listing keys under path: %s", path);
        } else {
            return String.format("Reading secret with path: %s", path);
        }
    }
}
