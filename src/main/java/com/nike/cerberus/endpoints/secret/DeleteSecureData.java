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

import com.google.common.collect.Sets;
import com.nike.cerberus.domain.VaultStyleErrorResponse;
import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.PermissionsService;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import com.nike.riposte.util.MultiMatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DeleteSecureData extends SecureDataEndpointV1<Void, Object> {

    @Inject
    protected DeleteSecureData(SecureDataService secureDataService,
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
    public CompletableFuture<ResponseInfo<Object>> executeSecureDataCall(SecureDataRequestInfo requestInfo,
                                                                         RequestInfo<Void> request,
                                                                         Executor longRunningTaskExecutor,
                                                                         ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> deleteSecureData(requestInfo), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<Object> deleteSecureData(SecureDataRequestInfo requestInfo) {
        CerberusPrincipal principal = requestInfo.getPrincipal();

        secureDataService.deleteSecret(requestInfo.getPath(), principal.getName());
        return ResponseInfo.newBuilder().withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code()).build();
    }

    @Override
    public Matcher requestMatcher() {
        return MultiMatcher.match(
                Sets.newHashSet(
                        String.format("%s/**", BASE_PATH),
                        BASE_PATH
                ),
                HttpMethod.DELETE
        );
    }
}
