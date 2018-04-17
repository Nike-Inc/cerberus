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
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.endpoints.CustomizableAuditData;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.HttpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Creates a secure file.
 */
public class WriteSecureFile extends AuditableEventEndpoint<Void, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String BASE_PATH = "/v1/secure-file";

    private static final String SECURE_FILE_CONTENT_MULTIPART = "file-content";

    private final SecureDataService secureDataService;

    private final SecureDataRequestService secureDataRequestService;

    @Inject
    public WriteSecureFile(final SecureDataService secureDataService,
                           final SecureDataRequestService secureDataRequestService) {
        this.secureDataService = secureDataService;
        this.secureDataRequestService = secureDataRequestService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(final RequestInfo<Void> request,
                                                           final Executor longRunningTaskExecutor,
                                                           final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> writeSecureData(request), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<Void> writeSecureData(final RequestInfo<Void> request) {
        SecureDataRequestInfo requestInfo = secureDataRequestService.parseAndValidateRequest(request);
        Map<String, HttpData> formParts = parseFormParts(request);
        validateFormParts(formParts);

        HttpData secureFile = formParts.get(SECURE_FILE_CONTENT_MULTIPART);
        try {
            byte[] fileContents = secureFile.get();
            secureDataService.writeSecureFile(requestInfo.getSdbId(), requestInfo.getPath(), fileContents,
                    fileContents.length,
                    requestInfo.getPrincipal().getName());

            return ResponseInfo.<Void>newBuilder().
                    withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code()).
                    build();
        } catch (IOException ex) {
            throw ApiException.newBuilder()
                    .withExceptionMessage("Failed to parse multipart form.")
                    .withExceptionCause(ex)
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .build();
        }
    }

    private Map<String, HttpData> parseFormParts(RequestInfo<Void> request) {
        if (request.getMultipartParts() == null) {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .withExceptionMessage("Create secure data request is malformed.")
                    .build();
        }

        return request.getMultipartParts().stream()
                .map(formPart -> (HttpData)formPart)
                .collect(
                        Collectors.toMap((formPart) ->
                                formPart.getName().toLowerCase(),
                                formPart -> formPart));
    }

    private void validateFormParts(Map<String, HttpData> formParts) {
        if (! formParts.keySet().contains(SECURE_FILE_CONTENT_MULTIPART.toLowerCase())) {
            throw ApiException.newBuilder()
                    .withExceptionMessage("Incomplete multipart request, missing secure file contents")
                    .withApiErrors(DefaultApiError.GENERIC_BAD_REQUEST)
                    .build();
        }
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.multiMatch(
                Sets.newHashSet(
                        String.format("%s/**", BASE_PATH),
                        BASE_PATH),
                HttpMethod.POST,
                HttpMethod.PUT);
    }

    @Override
    protected CustomizableAuditData getCustomizableAuditData(RequestInfo<Void> request) {
        SecureDataRequestInfo requestInfo = secureDataRequestService.parseRequestPathInfo(request.getPath());

        return new CustomizableAuditData()
                .setDescription(String.format("Write secure file: %s, method: %s", request.getPath(), request.getMethod()))
                .setSdbNameSlug(requestInfo.getSdbSlug());
    }
}
