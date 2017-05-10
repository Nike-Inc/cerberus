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

import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.domain.Category;
import com.nike.cerberus.endpoints.AdminStandardEndpoint;
import com.nike.cerberus.service.CategoryService;
import com.nike.riposte.server.http.RequestInfo;
import com.nike.riposte.server.http.ResponseInfo;
import com.nike.riposte.util.AsyncNettyHelper;
import com.nike.riposte.util.Matcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;

/**
 * Creates a category and returns a location header.
 */
public class CreateCategory extends AdminStandardEndpoint<Category, Void> {

    public static final String CATEGORY_PATH = "/v1/category";

    private final CategoryService categoryService;

    @Inject
    public CreateCategory(final CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public CompletableFuture<ResponseInfo<Void>> doExecute(final RequestInfo<Category> request,
                                                           final Executor longRunningTaskExecutor,
                                                           final ChannelHandlerContext ctx,
                                                           final SecurityContext securityContext) {
        return CompletableFuture.supplyAsync(
                AsyncNettyHelper.supplierWithTracingAndMdc(() -> createCategory(request.getContent(), securityContext, CATEGORY_PATH), ctx),
                longRunningTaskExecutor
        );
    }

    public ResponseInfo<Void> createCategory(final Category category,
                                             final SecurityContext securityContext,
                                             final String basePathNoId) {
        if (category == null) {
            throw ApiException.newBuilder().withApiErrors(SampleCoreApiError.MISSING_EXPECTED_CONTENT).build();
        }

        final String id = categoryService.createCategory(category, securityContext.getUserPrincipal().getName());
        final String location = basePathNoId + "/" + id;
        return ResponseInfo.<Void>newBuilder().withHeaders(new DefaultHttpHeaders().set(LOCATION, location))
                .withHttpStatusCode(201)
                .build();
    }

    @Override
    public Matcher requestMatcher() {
        return Matcher.match(CATEGORY_PATH, HttpMethod.POST);
    }
}
