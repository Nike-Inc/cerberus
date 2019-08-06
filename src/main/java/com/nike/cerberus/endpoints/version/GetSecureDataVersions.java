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

package com.nike.cerberus.endpoints.version;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.SecureDataRequestService;
import com.nike.cerberus.domain.SecureDataRequestInfo;
import com.nike.cerberus.domain.SecureDataVersionsResult;
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.endpoints.CustomizableAuditData;
import com.nike.cerberus.endpoints.RiposteEndpoint;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.event.AuditableEvent;
import com.nike.cerberus.service.PaginationService;
import com.nike.cerberus.service.SecureDataVersionService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Extracts the user groups from the security context for the request and returns any safe deposit boxes
 * associated with that list of user groups.
 */
@RiposteEndpoint
public class GetSecureDataVersions extends AuditableEventEndpoint<Void, SecureDataVersionsResult> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SecureDataVersionService secureDataVersionService;

    private final SecureDataRequestService secureDataRequestService;

    private final PaginationService paginationService;

    @Inject
    public GetSecureDataVersions(SecureDataVersionService secureDataVersionService,
                                 SecureDataRequestService secureDataRequestService,
                                 PaginationService paginationService) {
        this.secureDataVersionService = secureDataVersionService;
        this.secureDataRequestService = secureDataRequestService;
        this.paginationService = paginationService;
    }

    @Override
    public CompletableFuture<ResponseInfo<SecureDataVersionsResult>> doExecute(final RequestInfo<Void> request,
                                                                              final Executor longRunningTaskExecutor,
                                                                              final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> getVersionPathsForSdb(request), ctx),
                longRunningTaskExecutor
        );
    }

    public ResponseInfo<SecureDataVersionsResult> getVersionPathsForSdb(final RequestInfo<Void> request) {
        SecureDataRequestInfo requestInfo = secureDataRequestService.parseAndValidateRequest(request);
        String pathToSecret = requestInfo.getPath();

        SecureDataVersionsResult result = secureDataVersionService.getSecureDataVersionSummariesByPath(
                requestInfo.getSdbId(),
                pathToSecret,
                requestInfo.getCategory(),
                paginationService.getLimit(request),
                paginationService.getOffset(request));
        if (result.getSecureDataVersionSummaries().isEmpty()) {
            AuditableEvent auditableEvent = auditableEvent(requestInfo.getPrincipal(), request, getClass().getSimpleName())
                    .withSuccess(false)
                    .withAction("Failed to find versions for secret with path: " + pathToSecret)
                    .build();
            eventProcessorService.ingestEvent(auditableEvent);

            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .build();
        }

        return ResponseInfo.newBuilder(result).build();

    }

    @Override
    protected CustomizableAuditData getCustomizableAuditData(RequestInfo<Void> request) {
        SecureDataRequestInfo requestInfo = secureDataRequestService.parseRequestPathInfo(request.getPath());
        return  new CustomizableAuditData()
                .setDescription(String.format("Listing secrets versions for SDB with id: '%s'", requestInfo.getSdbId()))
                .setSdbNameSlug(requestInfo.getSdbSlug());
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/secret-versions/**", HttpMethod.GET);
    }

}
