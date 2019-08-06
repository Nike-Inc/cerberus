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
import com.nike.cerberus.domain.SecureDataType;
import com.nike.cerberus.endpoints.AuditableEventEndpoint;
import com.nike.cerberus.endpoints.CustomizableAuditData;
import com.nike.cerberus.endpoints.RiposteEndpoint;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.service.SecureDataService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.endpoints.file.WriteSecureFile.BASE_PATH;

/**
 * Deletes the given secure file.
 */
@RiposteEndpoint
public class DeleteSecureFile extends AuditableEventEndpoint<Void, Void> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SecureDataService secureDataService;

    private final SecureDataRequestService secureDataRequestService;

    @Inject
    public DeleteSecureFile(final SecureDataService secureDataService,
                            final SecureDataRequestService secureDataRequestService) {
        this.secureDataService = secureDataService;
        this.secureDataRequestService = secureDataRequestService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(final RequestInfo<Void> request,
                                                           final Executor longRunningTaskExecutor,
                                                           final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> deleteSecureFile(request), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<Void> deleteSecureFile(final RequestInfo<Void> request) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            SecureDataRequestInfo requestInfo = secureDataRequestService.parseAndValidateRequest(request);

            secureDataService.deleteSecret(requestInfo.getSdbId(),
                requestInfo.getPath(), SecureDataType.FILE, requestInfo.getPrincipal().getName());

            return ResponseInfo.<Void>newBuilder().
                    withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code()).
                    build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.multiMatch(
                Sets.newHashSet(
                        String.format("%s/**", BASE_PATH),
                        BASE_PATH),
                HttpMethod.DELETE);
    }

    @Override
    protected CustomizableAuditData getCustomizableAuditData(RequestInfo<Void> request) {
        SecureDataRequestInfo requestInfo = secureDataRequestService.parseRequestPathInfo(request.getPath());

        return new CustomizableAuditData()
                .setDescription(String.format("Delete secure file: %s", request.getPath()))
                .setSdbNameSlug(requestInfo.getSdbSlug());
    }
}
