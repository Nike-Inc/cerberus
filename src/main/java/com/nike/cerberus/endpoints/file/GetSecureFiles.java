/*
 * Copyright (c) 2018 Nike, Inc.
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

package com.nike.cerberus.endpoints.file;

import com.google.common.collect.Sets;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.SecureDataRequestService;
import com.nike.cerberus.domain.SecureDataRequestInfo;
import com.nike.cerberus.domain.SecureFileSummaryResult;
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.service.PaginationService;
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

/**
 * Lists metadata for all files under the given path.
 */
public class GetSecureFiles extends AuditableEventEndpoint<Void, SecureFileSummaryResult> {

    public static final String BASE_PATH = "/v1/secure-files";

    private final SecureDataService secureDataService;
    private final SecureDataRequestService secureDataRequestService;
    private final PaginationService paginationService;

    @Inject
    protected GetSecureFiles(SecureDataService secureDataService,
                             SecureDataRequestService secureDataRequestService,
                             PaginationService paginationService) {

        this.secureDataService = secureDataService;
        this.secureDataRequestService = secureDataRequestService;
        this.paginationService = paginationService;
    }

    @Override
    public CompletableFuture<ResponseInfo<SecureFileSummaryResult>> doExecute(RequestInfo<Void> request,
                                                                              Executor longRunningTaskExecutor,
                                                                              ChannelHandlerContext ctx) {

        SecureDataRequestInfo info = secureDataRequestService.parseAndValidateRequest(request);
        SecureFileSummaryResult fileSummaryResult = secureDataService.listSecureFilesSummaries(
                info.getPath(),
                paginationService.getLimit(request),
                paginationService.getOffset(request));

        if (fileSummaryResult.getSecureFileSummaries().isEmpty()) {
            AuditableEvent auditableEvent = auditableEvent(info.getPrincipal(), request, getClass().getSimpleName())
                    .withSuccess(false)
                    .withAction("Failed to find files under path: " + info.getPath())
                    .build();
            eventProcessorService.ingestEvent(auditableEvent);

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .withExceptionMessage("Failed to find files under given path.")
                    .build();
        }

        final ResponseInfo response = ResponseInfo.newBuilder()
                .withContentForFullResponse(fileSummaryResult)
                .withHttpStatusCode(HttpResponseStatus.OK.code())
                .build();

        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> response, ctx),
                longRunningTaskExecutor);
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

        return String.format("List file metadata for SDB path: %s", path);
    }
}
