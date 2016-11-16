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

import com.nike.cerberus.domain.Category;
import com.nike.cerberus.service.CategoryService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.server.http.StandardEndpoint;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Returns the category for the specified ID if found, otherwise not found response.
 */
public class GetCategory extends StandardEndpoint<Void, Category> {

    public static final String PATH_PARAM_ID = "id";

    private final CategoryService categoryService;

    @Inject
    public GetCategory(final CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Category>> execute(
        RequestInfo<Void> request, Executor longRunningTaskExecutor, ChannelHandlerContext ctx
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                String id = request.getPathParam(PATH_PARAM_ID);
                return getCategory(id);
            },
            longRunningTaskExecutor
        );
    }

    public ResponseInfo<Category> getCategory(final String id) {
        final Optional<Category> category = categoryService.getCategory(id);

        if (category.isPresent()) {
            return ResponseInfo.newBuilder(category.get()).build();
        }

        return ResponseInfo.<Category>newBuilder().withHttpStatusCode(HttpResponseStatus.NOT_FOUND.code()).build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match("/v1/category/{id}", HttpMethod.GET);
    }
}
