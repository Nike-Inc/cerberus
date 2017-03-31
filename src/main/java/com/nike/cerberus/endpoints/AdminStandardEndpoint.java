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

package com.nike.cerberus.endpoints;

import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.cerberus.security.CmsRequestSecurityValidator;
import com.nike.cerberus.security.VaultAuthPrincipal;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import io.netty.channel.ChannelHandlerContext;

import javax.ws.rs.core.SecurityContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Extension endpoint class for validating caller is admin before executing.
 */
public abstract class AdminStandardEndpoint<I, O> extends StandardEndpoint<I, O> {

    public final CompletableFuture<ResponseInfo<O>> execute(final RequestInfo<I> request,
                                                            final Executor longRunningTaskExecutor,
                                                            final ChannelHandlerContext ctx) {
        final Optional<SecurityContext> securityContext =
                CmsRequestSecurityValidator.getSecurityContextForRequest(request);

        if (!securityContext.isPresent() || !securityContext.get().isUserInRole(VaultAuthPrincipal.ROLE_ADMIN)) {
            throw new ApiException(DefaultApiError.ACCESS_DENIED);
        }

        return doExecute(request, longRunningTaskExecutor, ctx, securityContext.get());
    }

    public abstract CompletableFuture<ResponseInfo<O>> doExecute(RequestInfo<I> request,
                                                                 Executor longRunningTaskExecutor,
                                                                 ChannelHandlerContext ctx,
                                                                 SecurityContext securityContext);
}
