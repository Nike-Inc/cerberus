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

package com.nike.cerberus.endpoints.sdb;

import com.google.common.collect.Maps;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.SafeDepositBoxV1;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.cerberus.service.SafeDepositBoxService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.nike.cerberus.CerberusHttpHeaders.HEADER_X_CERBERUS_CLIENT;
import static com.nike.cerberus.CerberusHttpHeaders.HEADER_X_REFRESH_TOKEN;
import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;

/**
 * Creates a new safe deposit box.  Returns the assigned unique identifier.
 */
@Deprecated
public class CreateSafeDepositBoxV1 extends StandardEndpoint<SafeDepositBoxV1, Map<String, String>> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String BASE_PATH = "/v1/safe-deposit-box";

    private final SafeDepositBoxService safeDepositBoxService;

    @Inject
    public CreateSafeDepositBoxV1(final SafeDepositBoxService safeDepositBoxService) {
        this.safeDepositBoxService = safeDepositBoxService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Map<String, String>>> execute(final RequestInfo<SafeDepositBoxV1> request,
                                                                        final Executor longRunningTaskExecutor,
                                                                        final ChannelHandlerContext ctx) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> createSafeDepositBox(request, BASE_PATH), ctx),
                longRunningTaskExecutor
        );
    }

    private ResponseInfo<Map<String, String>> createSafeDepositBox(final RequestInfo<SafeDepositBoxV1> request,
                                                                   final String basePath) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (securityContext.isPresent()) {
            final VaultAuthPrincipal vaultAuthPrincipal = (VaultAuthPrincipal) securityContext.get().getUserPrincipal();
            final HttpHeaders headers = request.getHeaders();
            final boolean clientHeaderExists = headers != null && headers.get(HEADER_X_CERBERUS_CLIENT) != null;
            final String clientHeader = clientHeaderExists ? headers.get(HEADER_X_CERBERUS_CLIENT) : "Unknown";

            log.info("{}: {}, Create SDB Event: the principal: {} is attempting to create sdb name: '{}'",
                    HEADER_X_CERBERUS_CLIENT,
                    clientHeader,
                    vaultAuthPrincipal.getName(),
                    request.getContent().getName());

            final String id =
                    safeDepositBoxService.createSafeDepositBoxV1(request.getContent(), vaultAuthPrincipal.getName());

            final String location = basePath + "/" + id;
            final Map<String, String> map = Maps.newHashMap();
            map.put("id", id);
            return ResponseInfo.newBuilder(map)
                    .withHeaders(new DefaultHttpHeaders()
                            .set(LOCATION, location)
                            .set(HEADER_X_REFRESH_TOKEN, Boolean.TRUE.toString()))
                    .withHttpStatusCode(HttpResponseStatus.CREATED.code())
                    .build();
        }

        throw ApiException.newBuilder().withApiErrors(DefaultApiError.AUTH_BAD_CREDENTIALS).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match(BASE_PATH, HttpMethod.POST);
    }
}
