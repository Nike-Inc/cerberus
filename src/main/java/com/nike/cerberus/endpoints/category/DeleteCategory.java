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

package com.nike.cerberus.endpoints.category;

import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.service.CategoryService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Deletes the category for the specified ID.  Admin only operation.
 */
public class DeleteCategory extends AdminStandardEndpoint<Void, Void> {

    public static final String PATH_PARAM_ID = "id";

    private final CategoryService categoryService;

    @Inject
    public DeleteCategory(final CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(final RequestInfo<Void> request,
                                                           final Executor longRunningTaskExecutor,
                                                           final ChannelHandlerContext ctx,
                                                           final SecurityContext securityContext
    ) {
        return CompletableFuture.supplyAsync(
            () -> deleteCategory(request.getPathParam(PATH_PARAM_ID)),
            longRunningTaskExecutor
        );
    }

    public ResponseInfo<Void> deleteCategory(final String id) {
        final boolean isDeleted = categoryService.deleteCategory(id);

        if (isDeleted) {
            return ResponseInfo.<Void>newBuilder().withHttpStatusCode(HttpResponseStatus.NO_CONTENT.code()).build();
        }

        return ResponseInfo.<Void>newBuilder().withHttpStatusCode(HttpResponseStatus.NOT_FOUND.code()).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/category/{id}", HttpMethod.DELETE);
    }
}
