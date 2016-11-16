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
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.nike.cerberus.endpoints.category.GetCategory.PATH_PARAM_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetCategoryTest {

    private final String categoryId = "CATEGORY_ID";

    private final Category category = new Category().setId(categoryId);

    private final Executor executor = Executors.newSingleThreadExecutor();

    private CategoryService categoryService;

    private GetCategory subject;

    @Before
    public void setUp() throws Exception {
        categoryService = mock(CategoryService.class);
        subject = new GetCategory(categoryService);
    }

    @Test
    public void requestMatcher_is_http_post() {
        final Collection<HttpMethod> httpMethods = subject.requestMatcher().matchingMethods();

        assertThat(httpMethods).hasSize(1);
        assertThat(httpMethods).contains(HttpMethod.GET);
    }

    @Test
    public void execute_returns_category_if_found() {
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getPathParam(PATH_PARAM_ID)).thenReturn(categoryId);
        when(categoryService.getCategory(categoryId)).thenReturn(Optional.of(category));

        final CompletableFuture<ResponseInfo<Category>> completableFuture =
                subject.execute(requestInfo, executor, null);
        final ResponseInfo<Category> responseInfo = completableFuture.join();

        assertThat(responseInfo.getContentForFullResponse()).isEqualToComparingFieldByField(category);
    }

    @Test
    public void execute_returns_not_found_if_no_category() {
        final RequestInfo<Void> requestInfo = mock(RequestInfo.class);
        when(requestInfo.getPathParam(PATH_PARAM_ID)).thenReturn(categoryId);
        when(categoryService.getCategory(categoryId)).thenReturn(Optional.empty());

        final CompletableFuture<ResponseInfo<Category>> completableFuture =
                subject.execute(requestInfo, executor, null);
        final ResponseInfo<Category> responseInfo = completableFuture.join();

        assertThat(responseInfo.getHttpStatusCode()).isEqualTo(HttpResponseStatus.NOT_FOUND.code());
        assertThat(responseInfo.getContentForFullResponse()).isNull();
    }
}